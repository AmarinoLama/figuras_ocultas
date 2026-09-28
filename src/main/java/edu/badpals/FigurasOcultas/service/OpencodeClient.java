package edu.badpals.FigurasOcultas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Cliente mínimo para el servidor HTTP que expone "opencode serve".
 * Se usa exclusivamente como motor de lenguaje del chatbot de la web.
 */
@Service
public class OpencodeClient {

    private static final Logger log = LoggerFactory.getLogger(OpencodeClient.class);

    @Value("${app.chatbot.url:http://localhost:4096}")
    private String baseUrl;

    @Value("${app.chatbot.password:}")
    private String password;

    @Value("${app.chatbot.model:opencode/ling-3.0-flash-fin-free}")
    private String modelo;

    /** Modelo de reserva: se usa si el principal no responde (proveedor caído). */
    @Value("${app.chatbot.model.fallback:opencode/mimo-v2.6-flash-free}")
    private String modeloAlternativo;

    @Value("${app.chatbot.agent:chatbot}")
    private String agente;

    /** Último fallo del modelo principal (epoch ms); 0 si nunca ha fallado. */
    private volatile long ultimoFalloPrincipal;

    /** Tiempo que se salta el modelo principal y va directo al de reserva. */
    private static final long ENFRIAMIENTO_PRINCIPAL_MS = 5L * 60L * 1000L;

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Error de comunicación (o de respuesta) con opencode serve. */
    public static class OpencodeException extends RuntimeException {
        private final int codigo;

        public OpencodeException(String mensaje) {
            this(mensaje, 0);
        }

        public OpencodeException(String mensaje, int codigo) {
            super(mensaje);
            this.codigo = codigo;
        }

        public int getCodigo() {
            return codigo;
        }
    }

    public boolean estaDisponible() {
        try {
            return enviar("GET", "/global/health", null).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String crearSesion(String titulo) {
        ObjectNode cuerpo = mapper.createObjectNode();
        if (titulo != null && !titulo.isBlank()) {
            cuerpo.put("title", titulo);
        }
        JsonNode respuesta = json(enviar("POST", "/session", cuerpo));
        JsonNode id = respuesta.get("id");
        if (id == null || id.isNull() || id.asText().isBlank()) {
            throw new OpencodeException("opencode serve no devolvió id de sesión");
        }
        return id.asText();
    }

    public boolean sesionExiste(String sesionId) {
        try {
            return enviar("GET", "/session/" + sesionId, null).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void borrarSesion(String sesionId) {
        try {
            enviar("DELETE", "/session/" + sesionId, null);
        } catch (Exception e) {
            log.debug("No se pudo borrar la sesión {}: {}", sesionId, e.getMessage());
        }
    }

    /** Devuelve el array de sesiones del servidor (puede venir vacío). */
    public JsonNode listarSesiones() {
        return json(enviar("GET", "/session", null));
    }

    /**
     * Envía un mensaje de usuario a una sesión y devuelve la respuesta completa
     * del servidor (incluye info.structured cuando se pide formato json_schema).
     * Se usa el agente "chatbot", que va sin herramientas: solo texto.
     */
    public JsonNode enviarMensaje(String sesionId, String system, JsonNode format, String texto) {
        if (principalEnfriado()) {
            return enviarConModelo(sesionId, system, format, texto, modeloAlternativo);
        }
        try {
            JsonNode respuesta = enviarConModelo(sesionId, system, format, texto, modelo);
            ultimoFalloPrincipal = 0L;
            return respuesta;
        } catch (RuntimeException e) {
            if (sinModeloAlternativo()) {
                throw e;
            }
            ultimoFalloPrincipal = System.currentTimeMillis();
            log.warn("El modelo principal {} no responde ({}), pruebo con {}",
                    modelo, e.getMessage(), modeloAlternativo);
            return enviarConModelo(sesionId, system, format, texto, modeloAlternativo);
        }
    }

    /** Igual que enviarMensaje, pero fijando el modelo a usar en la petición. */
    private JsonNode enviarConModelo(String sesionId, String system, JsonNode format,
                                     String texto, String modeloElegido) {
        ObjectNode cuerpo = mapper.createObjectNode();
        cuerpo.set("model", modeloNode(modeloElegido));
        if (agente != null && !agente.isBlank()) {
            cuerpo.put("agent", agente);
        }
        if (system != null && !system.isBlank()) {
            cuerpo.put("system", system);
        }
        if (format != null && !format.isNull()) {
            cuerpo.set("format", format);
        }
        ArrayNode partes = cuerpo.putArray("parts");
        ObjectNode parte = partes.addObject();
        parte.put("type", "text");
        parte.put("text", texto);

        HttpResponse<String> respuesta = enviar("POST", "/session/" + sesionId + "/message", cuerpo);
        JsonNode nodo = json(respuesta);
        if (nodo == null || !nodo.has("info")) {
            throw new OpencodeException("Respuesta inesperada de opencode serve", respuesta.statusCode());
        }
        JsonNode error = nodo.path("info").path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new OpencodeException(
                    error.path("data").path("message").asText("error desconocido del modelo"));
        }
        return nodo;
    }

    private boolean sinModeloAlternativo() {
        return modeloAlternativo == null || modeloAlternativo.isBlank()
                || modeloAlternativo.equals(modelo);
    }

    /** Tras un fallo, durante unos minutos se usa el modelo de reserva sin reintentar el principal. */
    private boolean principalEnfriado() {
        return !sinModeloAlternativo()
                && ultimoFalloPrincipal > 0L
                && System.currentTimeMillis() - ultimoFalloPrincipal < ENFRIAMIENTO_PRINCIPAL_MS;
    }

    private JsonNode modeloNode(String modeloElegido) {
        ObjectNode nodo = mapper.createObjectNode();
        int corte = modeloElegido.indexOf('/');
        if (corte > 0) {
            nodo.put("providerID", modeloElegido.substring(0, corte));
            nodo.put("modelID", modeloElegido.substring(corte + 1));
        } else {
            nodo.put("providerID", "opencode");
            nodo.put("modelID", modeloElegido);
        }
        return nodo;
    }

    private HttpResponse<String> enviar(String metodo, String path, JsonNode cuerpo) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(180));

        if (password != null && !password.isBlank()) {
            String credenciales = "opencode:" + password;
            String base64 = Base64.getEncoder()
                    .encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + base64);
        }

        HttpRequest.BodyPublisher publisher;
        if (cuerpo != null) {
            builder.header("Content-Type", "application/json");
            publisher = HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8);
        } else {
            publisher = switch (metodo) {
                case "POST" -> HttpRequest.BodyPublishers.noBody();
                default -> HttpRequest.BodyPublishers.noBody();
            };
        }
        builder.method(metodo, publisher);

        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new OpencodeException("No puedo conectar con opencode serve (" + baseUrl + "): " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpencodeException("Solicitud interrumpida hacia opencode serve");
        }

        if (respuesta.statusCode() >= 400) {
            String detalle = respuesta.body() == null ? "" : respuesta.body();
            if (detalle.length() > 300) {
                detalle = detalle.substring(0, 300);
            }
            throw new OpencodeException("opencode serve respondió " + respuesta.statusCode() + ": " + detalle,
                    respuesta.statusCode());
        }
        return respuesta;
    }

    private JsonNode json(HttpResponse<String> respuesta) {
        try {
            String cuerpo = respuesta.body();
            if (cuerpo == null || cuerpo.isBlank()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(cuerpo);
        } catch (Exception e) {
            throw new OpencodeException("No se pudo interpretar la respuesta de opencode serve");
        }
    }
}
