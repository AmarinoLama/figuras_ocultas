package edu.badpals.FigurasOcultas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.badpals.FigurasOcultas.model.dto.ChatbotRequest;
import edu.badpals.FigurasOcultas.model.dto.ChatbotResponse;
import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Curso;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Chatbot de la web: usa opencode serve (modelo principal ling-3.0-flash-fin-free
 * y, si el proveedor no responde, el de reserva mimo-v2.6-flash-free)
 * para interpretar lo que pide el profe y ejecutar acciones sobre los alumnos.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    public static final String ACCION_NINGUNA = "ninguna";
    private static final String ATRIBUTO_SESION = "chatbotSesionOpencode";
    public static final String ATRIBUTO_CSV = "chatbotCsv";
    public static final String ATRIBUTO_CSV_NOMBRE = "chatbotCsvNombre";
    private static final String TITULO_SESION = "chatbot-web";
    private static final int MAX_ALUMNOS_PROMPT = 300;
    private static final int SESIONES_BORRADAS_POR_PETICION = 5;
    private static final long SESIONES_TTL_MS = 24L * 60L * 60L * 1000L;

    private static final Set<String> ACCIONES = Set.of(
            "crear_alumno", "modificar_alumno", "borrar_alumno", "dar_experiencia", "modificar_en_masa",
            "crear_alumnos", "crear_curso", "modificar_curso", "borrar_curso", "compartir_curso");
    private static final Set<String> ACCIONES_DESTRUCTIVAS = Set.of("borrar_alumno", "borrar_curso");

    /** Avisos internos del motor de IA que jamás deben llegar al profe. */
    private static final Pattern AVISO_INTERNO = Pattern.compile(
            "(?i)[^.\\n]*m[aá]ximo (n[uú]mero )?de pasos[^.\\n]*\\.?"
                    + "|[^.\\n]*no se puede continuar con m[aá]s acciones[^.\\n]*\\.?"
                    + "|[^.\\n]*maximum (number of )?steps[^.\\n]*\\.?"
                    + "|[^.\\n]*reached the (maximum|limit)[^.\\n]*\\.?");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TarjetaAlumnoService tarjetaAlumnoService;

    @Autowired
    private HistorialTransaccionesService historialService;

    @Autowired
    private CursoService cursoService;

    @Autowired
    private OpencodeClient opencode;

    // ------------------------------------------------------------------ API

    public ChatbotResponse chat(ChatbotRequest request, HttpSession sesionHttp, Long profesorId) {
        String mensaje = request.getMensaje() == null ? "" : request.getMensaje().trim();
        boolean confirmado = request.isConfirmado() && esAccionValida(request.getAccion());

        if (!confirmado && mensaje.isEmpty()) {
            return new ChatbotResponse("Escríbeme algo y te echo una mano con el alumnado.");
        }

        String sesion;
        try {
            sesion = obtenerSesion(sesionHttp);
        } catch (OpencodeClient.OpencodeException e) {
            log.warn("No se pudo crear la sesión de opencode: {}", e.getMessage());
            return new ChatbotResponse("Ahora mismo no puedo hablar con el asistente de IA. "
                    + "Comprueba que opencode serve esté en marcha y vuelve a probar.");
        }

        try {
            if (confirmado) {
                JsonNode paramsConfirmados = normalizarParams(request.getParams());
                JsonNode resultado = ejecutarAccion(request.getAccion(), paramsConfirmados, profesorId);
                ChatbotResponse respuesta = new ChatbotResponse(oDefecto(limpiar(
                        resumir(sesion, mensajeConfirmacion(request.getAccion(), paramsConfirmados, resultado), resultado)),
                        resultado.path("mensaje").asText("Hecho.")));
                respuesta.setAccionEjecutada(resultado.path("ok").asBoolean(false));
                respuesta.setDescarga(guardarCsv(resultado, sesionHttp));
                return respuesta;
            }

            JsonNode estructurado = llamar(sesion, sistemaAccion(profesorId), formatoAccion(), mensaje);
            String accion = texto(estructurado, "accion");
            String bruto = texto(estructurado, "respuesta");
            String propia = limpiar(bruto);

            // Si el modelo se enreda (aviso interno, respuesta vacía...), se repite una vez.
            boolean sospechoso = (bruto != null && AVISO_INTERNO.matcher(bruto).find())
                    || (!esAccionValida(accion) && (propia == null || propia.isBlank()));
            if (sospechoso) {
                estructurado = llamar(sesion, sistemaAccion(profesorId), formatoAccion(),
                        mensaje + "\n\nIMPORTANTE: contesta SOLO con el JSON que pide el esquema. "
                                + "No uses herramientas, no hables de límites internos y, si no hace falta "
                                + "ninguna acción, pon \"accion\": \"ninguna\" y explica en \"respuesta\" lo que "
                                + "le dirías al profe.");
                accion = texto(estructurado, "accion");
                bruto = texto(estructurado, "respuesta");
                propia = limpiar(bruto);
            }
            JsonNode params = normalizarParams(estructurado.has("params") && estructurado.get("params").isObject()
                    ? estructurado.get("params") : null);
            log.info("Chatbot acción '{}' con params: {}", accion, paramsTexto(params));

            if (!esAccionValida(accion)) {
                return new ChatbotResponse(propia != null && !propia.isBlank() ? propia
                        : "No he entendido bien lo que me pides, ¿me lo puedes reformular?");
            }

            if (requiereConfirmacion(accion, params)) {
                ChatbotResponse respuesta = new ChatbotResponse();
                respuesta.setRespuesta(propia != null && !propia.isBlank() ? propia
                        : "Esta acción cambia datos de muchos alumnos. ¿Quieres que continúe?");
                respuesta.setAccion(accion);
                respuesta.setParams(params);
                respuesta.setConfirmacionRequerida(true);
                respuesta.setTextoConfirmacion(textoConfirmacion(accion, params));
                return respuesta;
            }

            JsonNode resultado = ejecutarAccion(accion, params, profesorId);
            ChatbotResponse respuesta = new ChatbotResponse(oDefecto(limpiar(
                    resumir(sesion, mensajeAccion(accion, params, resultado), resultado)),
                    resultado.path("mensaje").asText("Hecho.")));
            respuesta.setAccionEjecutada(resultado.path("ok").asBoolean(false));
            respuesta.setDescarga(guardarCsv(resultado, sesionHttp));
            return respuesta;

        } catch (OpencodeClient.OpencodeException e) {
            log.warn("Fallo hablando con opencode serve: {}", e.getMessage());
            return new ChatbotResponse("Uy, ha fallado la conexión con el asistente de IA ("
                    + e.getMessage() + "). Inténtalo de nuevo en unos segundos.");
        }
    }

    // ------------------------------------------------------- sesiones opencode

    private String obtenerSesion(HttpSession sesionHttp) {
        Object guardada = sesionHttp.getAttribute(ATRIBUTO_SESION);
        if (guardada instanceof String id && !id.isBlank() && opencode.sesionExiste(id)) {
            return id;
        }
        String nueva = opencode.crearSesion(TITULO_SESION);
        sesionHttp.setAttribute(ATRIBUTO_SESION, nueva);
        limpiarSesionesViejas();
        return nueva;
    }

    /** Borra sesiones de chatbot antiguas para que no se acumulen en el servidor. */
    private void limpiarSesionesViejas() {
        try {
            JsonNode sesiones = opencode.listarSesiones();
            if (!sesiones.isArray()) {
                return;
            }
            long ahora = System.currentTimeMillis();
            int borradas = 0;
            for (JsonNode sesion : sesiones) {
                if (borradas >= SESIONES_BORRADAS_POR_PETICION) {
                    break;
                }
                if (!sesion.path("title").asText("").startsWith(TITULO_SESION)) {
                    continue;
                }
                long actualizada = sesion.path("time").path("updated").asLong(0L);
                long creada = sesion.path("time").path("created").asLong(0L);
                long referencia = actualizada > 0L ? actualizada : creada;
                if (referencia > 0L && ahora - referencia > SESIONES_TTL_MS) {
                    String id = sesion.path("id").asText("");
                    if (!id.isBlank()) {
                        opencode.borrarSesion(id);
                        borradas++;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("No se pudieron limpiar sesiones del chatbot: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------ llamadas IA

    private JsonNode llamar(String sesion, String system, JsonNode formato, String mensaje) {
        JsonNode respuesta = opencode.enviarMensaje(sesion, system, formato, mensaje);
        JsonNode info = respuesta.path("info");
        JsonNode error = info.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String detalle = error.path("data").path("message").asText("error desconocido");
            throw new OpencodeClient.OpencodeException(detalle);
        }

        JsonNode estructurado = info.get("structured");
        if (estructurado != null && estructurado.isObject()) {
            log.info("Chatbot modelo: {}", estructurado);
            return estructurado;
        }

        // Respaldo: si no hubo salida estructurada, usamos el último texto.
        JsonNode partes = respuesta.path("parts");
        if (partes.isArray()) {
            for (int i = partes.size() - 1; i >= 0; i--) {
                JsonNode parte = partes.get(i);
                if ("text".equals(parte.path("type").asText()) && !parte.path("text").asText("").isBlank()) {
                    String texto = parte.path("text").asText();
                    JsonNode json = jsonDesdeTexto(texto);
                    if (json != null && (json.has("accion") || json.has("respuesta"))) {
                        if (!json.has("accion")) {
                            ((ObjectNode) json).put("accion", ACCION_NINGUNA);
                        }
                        return json;
                    }
                    ObjectNode salida = mapper.createObjectNode();
                    salida.put("accion", ACCION_NINGUNA);
                    salida.put("respuesta", texto);
                    return salida;
                }
            }
        }
        throw new OpencodeClient.OpencodeException("el modelo no ha devuelto ninguna respuesta");
    }

    /**
     * Extrae un objeto JSON del texto del modelo (por si responde con vallas
     * de Markdown o con una frase alrededor del JSON).
     */
    private JsonNode jsonDesdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        int inicio = limpio.indexOf('{');
        int fin = limpio.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            return null;
        }
        try {
            JsonNode nodo = mapper.readTree(limpio.substring(inicio, fin + 1));
            return nodo.isObject() ? nodo : null;
        } catch (Exception e) {
            log.debug("El texto del modelo no contenía JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Segunda llamada: pide al modelo que cuente al usuario lo que ha ocurrido
     * tras ejecutar la acción. Si falla, se devuelve un texto local.
     */
    private String resumir(String sesion, String mensaje, JsonNode resultado) {
        try {
            JsonNode estructurado = llamar(sesion, sistemaResultado(), formatoRespuesta(), mensaje);
            String respuesta = limpiar(texto(estructurado, "respuesta"));
            if (respuesta != null && !respuesta.isBlank()) {
                return respuesta;
            }
        } catch (OpencodeClient.OpencodeException e) {
            log.warn("No se pudo redactar el resumen de la acción: {}", e.getMessage());
        }
        String local = texto(resultado, "mensaje");
        return local != null ? local : "Hecho.";
    }

    /** Quita los avisos internos del motor de IA (límites de pasos, herramientas...). */
    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = AVISO_INTERNO.matcher(texto).replaceAll("");
        limpio = limpio.replaceAll("\\s+", " ").trim();
        return limpio;
    }

    private String oDefecto(String texto, String defecto) {
        return texto == null || texto.isBlank() ? defecto : texto;
    }

    /**
     * El modelo a veces envía los argumentos anidados, por ejemplo
     * {"nivel": 4, "alcance": {"curso": "1ESOA"}} o {"cambio": {"electronios": 5}}.
     * Aquí se suben esos objetos al nivel superior para que las acciones los encuentren.
     */
    private JsonNode normalizarParams(JsonNode params) {
        if (params == null || !params.isObject()) {
            return params;
        }
        ObjectNode normalizado = params.deepCopy();
        for (String clave : new String[] {"alcance", "cambio", "cambios", "valores", "datos", "nuevos_datos", "curso"}) {
            JsonNode hijo = normalizado.get(clave);
            if (hijo != null && hijo.isObject()) {
                Iterator<String> campos = hijo.fieldNames();
                while (campos.hasNext()) {
                    String campo = campos.next();
                    if (!normalizado.has(campo)) {
                        normalizado.set(campo, hijo.get(campo));
                    }
                }
            }
        }
        JsonNode alumnos = normalizado.get("alumnos");
        if (alumnos != null && alumnos.isTextual()
                && !"todos".equalsIgnoreCase(alumnos.asText().trim())) {
            ArrayNode lista = mapper.createArrayNode();
            lista.add(alumnos.asText());
            normalizado.set("alumnos", lista);
        }
        return normalizado;
    }

    private String mensajeAccion(String accion, JsonNode params, JsonNode resultado) {
        return "El usuario te ha pedido la acción \"" + accion + "\" con estos parámetros: "
                + paramsTexto(params) + ".\n"
                + "El resultado real de ejecutarla ha sido: " + resultadoSinCsv(resultado) + "\n"
                + "Cuéntaselo al usuario en español, breve y natural, indicando si ha ido bien o qué ha fallado. "
                + "Solo texto plano (sin Markdown, sin listas, sin emojis) y sin mencionar avisos internos del sistema. "
                + "No propongas nuevas acciones ni uses herramientas.";
    }

    private String mensajeConfirmacion(String accion, JsonNode params, JsonNode resultado) {
        return "El usuario ha confirmado que quiere ejecutar la acción \"" + accion + "\" con estos parámetros: "
                + paramsTexto(params) + ".\n"
                + "El resultado real ha sido: " + resultadoSinCsv(resultado) + "\n"
                + "Cuéntaselo en español, breve y natural. No propongas acciones ni uses herramientas.";
    }

    // ------------------------------------------------------ prompts y esquemas

    private String sistemaAccion(Long profesorId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres \"ProfeBot\", el asistente de la web escolar \"Figuras Ocultas\". ");
        sb.append("Hablas SIEMPRE en español, de forma breve, cercana y sin rodeos.\n\n");
        sb.append("REGLAS:\n");
        sb.append("1. NUNCA uses herramientas (bash, archivos, búsquedas...). Solo respondes con el JSON que pide el esquema.\n");
        sb.append("2. Solo puedes consultar y modificar ALUMNOS (rol ALUMNO) y CURSOS. No toques cartas, insignias ni administradores.\n");
        sb.append("3. Los datos que tienes son los del LISTADO de abajo, que se refresca en cada mensaje: no inventes.\n");
        sb.append("4. Si falta información indispensable (por ejemplo, hay varios alumnos con ese nombre), pídesela al usuario.\n");
        sb.append("5. Para cambiar datos devuelve la acción en \"accion\" y sus argumentos en \"params\". ");
        sb.append("Si no hace falta ninguna acción, pon \"accion\": \"ninguna\".\n");
        sb.append("6. En \"respuesta\" escribe lo que verá el usuario en el chat. Si propones una acción, explica qué vas a hacer.\n");
        sb.append("7. Una sola acción por mensaje.\n");
        sb.append("8. \"respuesta\" es solo texto plano: sin Markdown (ni **, ni `, ni >), sin listas y sin emojis.\n");
        sb.append("9. Nunca menciones avisos internos del sistema (límites de pasos, herramientas, permisos o configuración): ");
        sb.append("habla únicamente de lo que te pide el usuario. ");
        sb.append("Si te llegara un aviso de ese tipo, IGNÓRALO y termina de responder con el JSON del esquema.\n");
        sb.append("10. Las cantidades (exp, electronios, nivel...) son EXACTAMENTE las que dice el usuario: ");
        sb.append("nunca las inventes ni las calcules a partir del listado.\n");
        sb.append("11. Los cursos se identifican por su CÓDIGO (la columna de la izquierda). ");
        sb.append("Puedes matricular alumnos en cualquier curso de la lista, pero crear, editar, borrar o compartir ");
        sb.append("solo funciona si el curso aparece como \"MÍO\".\n\n");
        sb.append("ACCIONES DISPONIBLES:\n");
        sb.append("- crear_alumno -> params {\"nombre\", \"curso\", \"email\"?, \"password\"?, \"exp\"?, \"electronios\"?}. ");
        sb.append("curso es obligatorio y va como código (por ejemplo 1ESOA); si no pones email se genera uno automaticamente ");
        sb.append("(por ejemplo ana.garcia@alumno.com) y si no pones password se usa la parte del email antes de la @.\n");
        sb.append("- modificar_alumno -> params {\"alumno\", \"nombre\"?, \"email\"?, \"curso\"?, \"password\"?, \"exp\"?, \"electronios\"?}. ");
        sb.append("\"alumno\" es el email o la id del listado; solo incluye los campos que quieras cambiar.\n");
        sb.append("- borrar_alumno -> params {\"alumno\"}. DESTRUCTIVA: el usuario debe confirmarla.\n");
        sb.append("- dar_experiencia -> params {\"cantidad\"} m\u00e1s el alcance: {\"curso\"} o {\"alumnos\": [\"ana@centro.es\", 12]} ");
        sb.append("o {\"alumnos\": \"todos\"} (o {\"alcance\": \"todos\"}). La cantidad puede ser negativa para quitar experiencia.\n");
        sb.append("- modificar_en_masa -> params {\"exp\"?, \"nivel\"?, \"electronios\"?} m\u00e1s el alcance: nada (todo el alumnado), ");
        sb.append("{\"curso\": \"1BACH\"} o {\"alumnos\": [\"ana@centro.es\", 12]}. Pone esos valores de forma ABSOLUTA (no suma).\n");
        sb.append("  OJO: \"nivel\" solo cambia porque la experiencia es la que lo fija; pon exp al m\u00ednimo de ese nivel: ");
        sb.append("0->0, 1->100, 2->250, 3->500, 4->900, 5->1200, 6->1700. Si quieres subir experiencia en vez de fijarla, usa dar_experiencia.\n");
        sb.append("  Si \"modificar_en_masa\" afecta a TODOS los alumnos, el usuario tendr\u00e1 que confirmar; si es de un curso o de una lista, se ejecuta ya.\n");
        sb.append("- crear_alumnos -> params {\"curso\": \"1ESOA\", \"alumnos\": [\"Ana García\", \"Luis Pérez\"]}. ");
        sb.append("Crea en ese curso a los de la lista que no existan (les genera email y contraseña) y matricula a los que ya existan. ");
        sb.append("Siempre que la lista no est\u00e9 vac\u00eda, el usuario recibir\u00e1 adem\u00e1s un CSV descargable con nombre, usuario y contrase\u00f1a. ");
        sb.append("\u00dasala tambi\u00e9n cuando el usuario pida a\u00f1adir UNA sola persona a un curso (\"a\u00f1ade a Ana Garc\u00eda al curso 1ESOA\"): ");
        sb.append("modificar_alumno es solo para cambiar campos de un alumno ya existente, no para matricular.\n");
        sb.append("- crear_curso -> params {\"nombre\", \"codigo\"?, \"etapa\"?, \"alumnos\"?}. Crea un curso a nombre del usuario. ");
        sb.append("etapa solo puede ser ESO, BACHILLERATO, FP u OTRO (si no pones código se inventa uno a partir del nombre); ");
        sb.append("\"alumnos\" es opcional: la misma lista de nombres que en crear_alumnos y entonces también se recibe el CSV.\n");
        sb.append("- modificar_curso -> params {\"curso\", \"nombre\"?, \"codigo\"?, \"etapa\"?}. Solo sobre cursos MÍOS.\n");
        sb.append("- borrar_curso -> params {\"curso\"}. DESTRUCTIVA: borra el curso y deja a sus alumnos sin curso; ");
        sb.append("el usuario debe confirmarla.\n");
        sb.append("- compartir_curso -> params {\"curso\", \"destino\"} con \"quitar\"? (true para dejar de compartir). ");
        sb.append("destino es el email de un profesor o \"todos\". Solo sobre cursos MÍOS.\n");
        sb.append("EJEMPLOS DE params (el alcance va SIEMPRE como clave suelta, nunca anidado dentro de otro objeto):\n");
        sb.append("  - dar 10 de exp al curso 1ESOA: {\"cantidad\": 10, \"curso\": \"1ESOA\"}\n");
        sb.append("  - quitar 5 de exp a dos alumnos: {\"cantidad\": -5, \"alumnos\": [\"juan@alumno.com\", 156]}\n");
        sb.append("  - dar 10 de exp a todo el alumnado: {\"cantidad\": 10, \"alumnos\": \"todos\"}\n");
        sb.append("  - nivel 4 en 1ESOA: {\"nivel\": 4, \"curso\": \"1ESOA\"}\n");
        sb.append("  - electronios a 5 en todo el alumnado: {\"electronios\": 5, \"alumnos\": \"todos\"}\n");
        sb.append("  - crear alumno solo con nombre: {\"nombre\": \"Ana Garc\u00eda\", \"curso\": \"1ESOA\"}\n");
        sb.append("  - meter una lista de alumnos en un curso: {\"curso\": \"1ESOA\", \"alumnos\": [\"Ana Garc\u00eda\", \"Luis P\u00e9rez\"]}\n");
        sb.append("  - crear curso: {\"nombre\": \"1\u00ba Bachillerato B\", \"codigo\": \"1BACHB\", \"etapa\": \"BACHILLERATO\"}\n");
        sb.append("  - crear curso con su lista de alumnos: {\"nombre\": \"1\u00ba ESO D\", \"codigo\": \"1ESOD\", \"alumnos\": [\"Ana Garc\u00eda\", \"Luis P\u00e9rez\"]}\n");
        sb.append("  - compartir curso con otro profe: {\"curso\": \"1ESOA\", \"destino\": \"profe@centro.es\"}\n\n");
        sb.append("CURSOS (código (nombre) | etapa | dueño):\n");
        sb.append(cursosValidos(profesorId)).append("\n\n");
        sb.append("LISTADO ACTUAL DE ALUMNOS (id | nombre | email | curso | nivel | exp | electronios):\n");
        sb.append(listadoAlumnos());
        return sb.toString();
    }

    private String sistemaResultado() {
        return "Eres \"ProfeBot\", el asistente de la web escolar \"Figuras Ocultas\". "
                + "Respondes SIEMPRE en español, breve y natural, solo con texto plano: "
                + "sin Markdown (ni **, ni `, ni >), sin listas y sin emojis. "
                + "Te llegarán el resultado de una acción que ya se ha ejecutado. "
                + "Cuéntaselo al usuario confirmando lo que ha ocurrido de verdad (éxito o error). "
                + "Nunca menciones avisos internos del sistema (límites de pasos, herramientas, permisos o configuración). "
                + "No propongas nuevas acciones ni uses herramientas: responde solo con el JSON del esquema.";
    }

    private JsonNode formatoAccion() {
        return leer("{\"type\":\"json_schema\",\"schema\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"accion\":{\"type\":\"string\",\"enum\":[\"" + ACCION_NINGUNA
                + "\",\"crear_alumno\",\"modificar_alumno\",\"borrar_alumno\",\"dar_experiencia\",\"modificar_en_masa\","
                + "\"crear_alumnos\",\"crear_curso\",\"modificar_curso\",\"borrar_curso\",\"compartir_curso\"]},"
                + "\"params\":{\"type\":\"object\"},"
                + "\"respuesta\":{\"type\":\"string\"}"
                + "},"
                + "\"required\":[\"accion\",\"respuesta\"],"
                + "\"additionalProperties\":false}}");
    }

    private JsonNode formatoRespuesta() {
        return leer("{\"type\":\"json_schema\",\"schema\":{"
                + "\"type\":\"object\","
                + "\"properties\":{\"respuesta\":{\"type\":\"string\"}},"
                + "\"required\":[\"respuesta\"],"
                + "\"additionalProperties\":false}}");
    }

    private JsonNode leer(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Esquema JSON inválido", e);
        }
    }

    /** Cursos que el profesor puede usar, con el dueño para saber cuáles son suyos. */
    private String cursosValidos(Long profesorId) {
        StringBuilder sb = new StringBuilder();
        for (Curso curso : cursoService.visiblesPara(profesorId)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(curso.getCodigo()).append(" (").append(curso.getNombre()).append(") | ")
                    .append(curso.getEtapa()).append(" | ")
                    .append(curso.esDueno(profesorId) ? "MÍO" : curso.getPropietario().getNombre());
        }
        if (sb.length() == 0) {
            sb.append("(todavía no hay cursos: créalos con la acción crear_curso)");
        }
        return sb.toString();
    }

    /** Lista de códigos de curso, para los mensajes de error. */
    private String codigosCursos(Long profesorId) {
        List<String> codigos = new ArrayList<>();
        for (Curso curso : cursoService.visiblesPara(profesorId)) {
            codigos.add(curso.getCodigo());
        }
        return String.join(", ", codigos);
    }

    private String listadoAlumnos() {
        List<UsuarioDTO> alumnos = usuarioService.getAllAlumnos();
        if (alumnos.isEmpty()) {
            return "(no hay alumnos dados de alta)\n";
        }
        StringBuilder sb = new StringBuilder();
        int total = alumnos.size();
        int limite = Math.min(total, MAX_ALUMNOS_PROMPT);
        for (int i = 0; i < limite; i++) {
            UsuarioDTO alumno = alumnos.get(i);
            sb.append(alumno.getId()).append(" | ")
                    .append(alumno.getNombre()).append(" | ")
                    .append(alumno.getEmail()).append(" | ")
                    .append(alumno.getCurso() == null ? "-" : alumno.getCurso().getCodigo()).append(" | ")
                    .append(nivel(alumno)).append(" | ")
                    .append(exp(alumno)).append(" | ")
                    .append(electronios(alumno)).append("\n");
        }
        if (total > limite) {
            sb.append("(...solo se muestran ").append(limite).append(" de ").append(total).append(" alumnos...)\n");
        }
        return sb.toString();
    }

    // --------------------------------------------------------------- acciones

    private boolean esAccionValida(String accion) {
        return accion != null && ACCIONES.contains(accion);
    }

    /** true si la acción debe esperar al "Sí" del usuario antes de ejecutarse. */
    private boolean requiereConfirmacion(String accion, JsonNode params) {
        if (ACCIONES_DESTRUCTIVAS.contains(accion)) {
            return true;
        }
        // Cambiar datos de TODOS los alumnos de golpe también pide confirmación.
        return "modificar_en_masa".equals(accion) && alcanceEsTodos(params);
    }

    private boolean alcanceEsTodos(JsonNode params) {
        if (params == null) {
            return true;
        }
        if (textoCurso(params) != null) {
            return false;
        }
        JsonNode alumnos = params.get("alumnos");
        if (alumnos != null && alumnos.isArray()) {
            return alumnos.size() == 0;
        }
        if (alumnos != null && alumnos.isTextual()) {
            return "todos".equalsIgnoreCase(alumnos.asText().trim());
        }
        String alcance = texto(params, "alcance");
        if (alcance != null && !alcance.isBlank()) {
            return "todos".equalsIgnoreCase(alcance);
        }
        // Sin curso ni alumnos concretos: el alcance es todo el alumnado.
        return true;
    }

    private String textoConfirmacion(String accion, JsonNode params) {
        if ("borrar_curso".equals(accion)) {
            return "⚠️ Esta acción borra el curso de forma definitiva y deja a sus alumnos sin curso. ¿Continúo?";
        }
        if (ACCIONES_DESTRUCTIVAS.contains(accion)) {
            return "⚠️ Esta acción borra datos de forma definitiva. ¿Continúo?";
        }
        return "⚠️ Esta acción cambiará datos de TODO el alumnado a la vez. ¿Continúo?";
    }

    private JsonNode ejecutarAccion(String accion, JsonNode params, Long profesorId) {
        try {
            return switch (accion) {
                case "crear_alumno" -> crearAlumno(params, profesorId);
                case "crear_alumnos" -> crearAlumnos(params, profesorId);
                case "modificar_alumno" -> modificarAlumno(params, profesorId);
                case "borrar_alumno" -> borrarAlumno(params);
                case "dar_experiencia" -> darExperiencia(params, profesorId);
                case "modificar_en_masa" -> modificarEnMasa(params, profesorId);
                case "crear_curso" -> crearCurso(params, profesorId);
                case "modificar_curso" -> modificarCurso(params, profesorId);
                case "borrar_curso" -> borrarCurso(params, profesorId);
                case "compartir_curso" -> compartirCurso(params, profesorId);
                default -> resultado(false, "Acción desconocida: " + accion);
            };
        } catch (CursoService.CursoException e) {
            return resultado(false, e.getMessage());
        } catch (Exception e) {
            log.error("Error ejecutando la acción '{}' del chatbot", accion, e);
            return resultado(false, "Error interno al ejecutar la acción: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------- acciones curso

    private JsonNode crearCurso(JsonNode params, Long profesorId) {
        String nombre = texto(params, "nombre");
        String codigo = texto(params, "codigo");
        String etapa = texto(params, "etapa");
        if (nombre == null) {
            return resultado(false, "El curso necesita un nombre.");
        }
        Curso curso = cursoService.crear(nombre, codigo, etapa, profesorId);
        String mensaje = "Curso " + curso.getNombre() + " creado con el código " + curso.getCodigo()
                + " (etapa " + curso.getEtapa() + "). Sus alumnos se matriculan con ese código.";
        List<String> alumnos = listaAlumnos(params);
        if (alumnos.isEmpty()) {
            return resultado(true, mensaje);
        }
        JsonNode matriculados = matricularLista(curso, alumnos);
        ObjectNode res = resultado(true, mensaje + " " + matriculados.path("mensaje").asText(""));
        copiarCsv(matriculados, res);
        return res;
    }

    private JsonNode modificarCurso(JsonNode params, Long profesorId) {
        Curso curso = curso(params, profesorId);
        if (curso == null) {
            return cursoNoEncontrado(params, profesorId);
        }
        Curso actualizado = cursoService.modificar(curso.getId(),
                texto(params, "nombre"), texto(params, "codigo"), texto(params, "etapa"), profesorId);
        return resultado(true, "Curso " + actualizado.getNombre() + " actualizado (código "
                + actualizado.getCodigo() + ", etapa " + actualizado.getEtapa() + ").");
    }

    private JsonNode borrarCurso(JsonNode params, Long profesorId) {
        Curso curso = curso(params, profesorId);
        if (curso == null) {
            return cursoNoEncontrado(params, profesorId);
        }
        String nombre = curso.getNombre();
        int alumnos = curso.getAlumnos();
        cursoService.borrar(curso.getId(), profesorId);
        return resultado(true, "Curso " + nombre + " borrado. " + alumnos
                + " alumno(s) se han quedado sin curso (siguen dados de alta).");
    }

    private JsonNode compartirCurso(JsonNode params, Long profesorId) {
        Curso curso = curso(params, profesorId);
        if (curso == null) {
            return cursoNoEncontrado(params, profesorId);
        }
        String destino = texto(params, "destino");
        if (destino == null) {
            destino = texto(params, "profesor");
        }
        if (destino == null) {
            destino = texto(params, "con");
        }
        boolean quitar = params != null && params.path("quitar").asBoolean(false);
        if (destino == null) {
            return resultado(false, "Indica con quién compartir: email del profesor o \"todos\".");
        }
        cursoService.compartir(curso.getId(), destino, profesorId, quitar);
        return resultado(true, quitar
                ? "Curso " + curso.getNombre() + " dejado de compartir con " + destino + "."
                : "Curso " + curso.getNombre() + " compartido con " + destino + ".");
    }

    /** Curso del que habla el profe (por código o nombre) dentro de los que puede usar. */
    private Curso curso(JsonNode params, Long profesorId) {
        if (params == null) {
            return null;
        }
        return curso(textoCurso(params), profesorId);
    }

    /**
     * Referencia textual al curso: funciona tanto si el modelo lo manda como texto
     * ({"curso": "1ESOA"}) como si lo anida en un objeto ({"curso": {"codigo": ...}}).
     */
    private String textoCurso(JsonNode params) {
        if (params == null) {
            return null;
        }
        JsonNode nodo = params.get("curso");
        if (nodo != null && nodo.isObject()) {
            String codigo = texto(nodo, "codigo");
            if (codigo != null) {
                return codigo;
            }
            String nombre = texto(nodo, "nombre");
            if (nombre != null) {
                return nombre;
            }
            return texto(nodo, "id");
        }
        String valor = texto(params, "curso");
        if (valor == null) {
            valor = texto(params, "codigo");
        }
        if (valor == null) {
            valor = texto(params, "nombre_curso");
        }
        return valor;
    }

    /** Curso por código o nombre dentro de los que el profesor puede usar. */
    private Curso curso(String valor, Long profesorId) {
        return cursoService.buscar(valor, profesorId);
    }

    private JsonNode cursoNoEncontrado(JsonNode params, Long profesorId) {
        String valor = textoCurso(params);
        if (valor == null && params != null) {
            valor = texto(params, "nombre");
        }
        return resultado(false, "El curso '" + (valor == null ? "" : valor)
                + "' no existe entre los que puedes usar. Códigos disponibles: " + codigosCursos(profesorId) + ".");
    }

    private JsonNode crearAlumno(JsonNode params, Long profesorId) {
        String nombre = texto(params, "nombre");
        String email = texto(params, "email");
        String cursoTexto = textoCurso(params);
        String password = texto(params, "password");
        Integer exp = entero(params, "exp");
        Integer electronios = entero(params, "electronios");

        if (nombre == null) {
            return resultado(false, "Falta el nombre del alumno.");
        }
        if (nombre.length() > 25) {
            nombre = nombre.substring(0, 25).trim();
        }
        Curso curso = curso(cursoTexto, profesorId);
        if (curso == null) {
            return resultado(false, "El curso '" + (cursoTexto == null ? "" : cursoTexto)
                    + "' no existe. Usa uno de: " + codigosCursos(profesorId));
        }
        if (email == null) {
            email = generarEmail(nombre);
        }
        if (!email.contains("@")) {
            return resultado(false, "El email '" + email + "' no es válido.");
        }
        if (usuarioService.getUserByEmail(email) != null) {
            return resultado(false, "Ya existe un alumno con el email " + email);
        }
        String clave = password != null ? password : email.substring(0, email.indexOf('@'));

        UsuarioDTO nuevo = new UsuarioDTO();
        nuevo.setNombre(nombre);
        nuevo.setEmail(email);
        nuevo.setPassword(clave);
        nuevo.setCurso(curso);
        nuevo.setRol(RolUsuario.ALUMNO);
        nuevo.setTarjetaAlumno(new TarjetaAlumnoDTO());
        usuarioService.saveUser(nuevo);

        UsuarioDTO creado = usuarioService.getUserByEmail(email);
        if (creado == null) {
            return resultado(false, "No se ha podido crear el alumno " + nombre + ".");
        }

        if (exp != null || electronios != null) {
            aplicarTarjeta(creado.getId(), exp, electronios);
            creado = usuarioService.getUserById(creado.getId());
        }

        ObjectNode res = resultado(true, "Alumno " + nombre + " creado con el email " + email
                + " en " + curso + ". Contraseña inicial: " + clave + ".");
        res.set("alumno", alumnoJson(creado));
        res.put("csv", csvAlumnos(List.of(
                new String[] {"nombre", "usuario", "contrasena"},
                new String[] {creado.getNombre(), creado.getEmail(), clave})));
        return res;
    }

    /** Acción masiva: crea los alumnos que falten y matricula la lista entera en el curso. */
    private JsonNode crearAlumnos(JsonNode params, Long profesorId) {
        String cursoTexto = textoCurso(params);
        Curso curso = curso(cursoTexto, profesorId);
        if (curso == null) {
            return cursoNoEncontrado(params, profesorId);
        }
        List<String> lista = listaAlumnos(params);
        if (lista.isEmpty()) {
            return resultado(false, "No me has dado ningún alumno. Por ejemplo: "
                    + "{\"curso\": \"" + curso.getCodigo()
                    + "\", \"alumnos\": [\"Ana García\", \"Luis Pérez\"]}.");
        }
        return matricularLista(curso, lista);
    }

    /** Nombres (o emails/ids) que el modelo ha enviado en params.alumnos. */
    private List<String> listaAlumnos(JsonNode params) {
        List<String> lista = new ArrayList<>();
        if (params == null) {
            return lista;
        }
        JsonNode alumnos = params.get("alumnos");
        if (alumnos == null || !alumnos.isArray()) {
            return lista;
        }
        for (JsonNode item : alumnos) {
            String valor = null;
            if (item.isTextual()) {
                valor = item.asText();
            } else if (item.isNumber()) {
                UsuarioDTO alumno = usuarioService.getUserById(item.asLong());
                valor = alumno == null ? null : alumno.getEmail();
            } else if (item.isObject()) {
                valor = texto(item, "nombre");
                if (valor == null) {
                    valor = texto(item, "email");
                }
            }
            if (valor == null || valor.isBlank()) {
                continue;
            }
            if (valor.contains(",")) {
                for (String parte : valor.split(",")) {
                    if (!parte.isBlank()) {
                        lista.add(parte.trim());
                    }
                }
            } else {
                lista.add(valor.trim());
            }
        }
        return lista;
    }

    /**
     * Matricula la lista en el curso: crea a los que no existan (con email y contraseña
     * generados) y mueve al curso a los que ya existían. El resultado lleva el CSV.
     */
    private JsonNode matricularLista(Curso curso, List<String> lista) {
        int creados = 0;
        int anadidos = 0;
        int yaEstaban = 0;
        List<String> fallos = new ArrayList<>();
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"nombre", "usuario", "contrasena"});

        for (String referencia : lista) {
            UsuarioDTO alumno = alumnoExistente(referencia);
            if (alumno != null && alumno.isAdmin()) {
                fallos.add(referencia + " (es una cuenta de administración)");
                continue;
            }
            if (alumno == null) {
                String nombre = referencia.length() > 25 ? referencia.substring(0, 25).trim() : referencia;
                String email = generarEmail(nombre);
                String clave = email.substring(0, email.indexOf('@'));
                UsuarioDTO nuevo = new UsuarioDTO();
                nuevo.setNombre(nombre);
                nuevo.setEmail(email);
                nuevo.setPassword(clave);
                nuevo.setCurso(curso);
                nuevo.setRol(RolUsuario.ALUMNO);
                nuevo.setTarjetaAlumno(new TarjetaAlumnoDTO());
                usuarioService.saveUser(nuevo);
                UsuarioDTO guardado = usuarioService.getUserByEmail(email);
                if (guardado == null) {
                    fallos.add(nombre);
                    continue;
                }
                creados++;
                filas.add(new String[] {guardado.getNombre(), guardado.getEmail(), clave});
                continue;
            }

            Curso actual = alumno.getCurso();
            if (actual != null && curso.getId().equals(actual.getId())) {
                yaEstaban++;
            } else {
                alumno.setCurso(curso);
                usuarioService.saveUser(alumno);
                anadidos++;
            }
            String clave = alumno.getPassword() == null || alumno.getPassword().isBlank()
                    ? "-" : alumno.getPassword();
            filas.add(new String[] {alumno.getNombre(), alumno.getEmail(), clave});
        }

        boolean hayCsv = filas.size() > 1;
        StringBuilder mensaje = new StringBuilder();
        if (creados > 0) {
            mensaje.append("He creado ").append(creados).append(" alumno(s) nuevos en ")
                    .append(curso.getCodigo()).append(". ");
        }
        if (anadidos > 0) {
            mensaje.append("He matriculado en el curso a ").append(anadidos)
                    .append(" que ya existían. ");
        }
        if (yaEstaban > 0) {
            mensaje.append(yaEstaban).append(" ya estaban en ").append(curso.getCodigo()).append(". ");
        }
        if (creados + anadidos + yaEstaban == 0) {
            mensaje.append("No he podido procesar a nadie de la lista. ");
        }
        if (!fallos.isEmpty()) {
            mensaje.append("No he podido con: ").append(String.join("; ", fallos)).append(". ");
        }
        if (hayCsv) {
            mensaje.append("Te dejo un CSV para descargar con los nombres completos, los usuarios "
                    + "y las contraseñas de todos ellos.");
        }

        ObjectNode res = resultado(creados + anadidos + yaEstaban > 0, mensaje.toString().trim());
        if (hayCsv) {
            res.put("csv", csvAlumnos(filas));
        }
        return res;
    }

    /** Alumno ya dado de alta por email, por id o por nombre (sin tildes ni mayúsculas). */
    private UsuarioDTO alumnoExistente(String referencia) {
        String limpio = referencia.trim();
        UsuarioDTO porReferencia = resolverAlumno(mapper.getNodeFactory().textNode(limpio));
        if (porReferencia != null) {
            return porReferencia;
        }
        String clave = normalizarNombre(limpio);
        for (UsuarioDTO alumno : usuarioService.getAllAlumnos()) {
            if (alumno.getNombre() != null && normalizarNombre(alumno.getNombre()).equals(clave)) {
                return alumno;
            }
        }
        return null;
    }

    private String normalizarNombre(String valor) {
        String sinTildes = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private String csvAlumnos(List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        for (String[] fila : filas) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(campoCsv(fila[i]));
            }
        }
        return sb.append("\n").toString();
    }

    private String campoCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    /** Guarda el CSV en la sesión del profe y devuelve su URL de descarga (null si no lo hay). */
    private String guardarCsv(JsonNode resultado, HttpSession sesionHttp) {
        JsonNode csv = resultado == null ? null : resultado.get("csv");
        if (csv == null || !csv.isTextual() || csv.asText().isBlank()) {
            return null;
        }
        sesionHttp.setAttribute(ATRIBUTO_CSV, csv.asText());
        sesionHttp.setAttribute(ATRIBUTO_CSV_NOMBRE, "alumnos-"
                + new java.text.SimpleDateFormat("yyyyMMdd-HHmm").format(new java.util.Date()) + ".csv");
        return "/api/chatbot/csv";
    }

    private void copiarCsv(JsonNode origen, ObjectNode destino) {
        JsonNode csv = origen == null ? null : origen.get("csv");
        if (csv != null && csv.isTextual()) {
            destino.put("csv", csv.asText());
        }
    }

    private JsonNode modificarAlumno(JsonNode params, Long profesorId) {
        UsuarioDTO alumno = buscarAlumno(params);
        if (alumno == null) {
            return resultado(false, "No encuentro ningún alumno con ese email o id.");
        }
        if (alumno.isAdmin()) {
            return resultado(false, "Solo puedo modificar alumnos de clase, no administradores.");
        }

        String nombre = texto(params, "nombre");
        String email = texto(params, "email");
        String cursoTexto = textoCurso(params);
        String password = texto(params, "password");
        Integer exp = entero(params, "exp");
        Integer electronios = entero(params, "electronios");

        if (email != null && email.contains("@")) {
            UsuarioDTO otro = usuarioService.getUserByEmail(email);
            if (otro != null && otro.getId() != null && !otro.getId().equals(alumno.getId())) {
                return resultado(false, "El email " + email + " ya lo usa otro usuario.");
            }
            alumno.setEmail(email);
        } else if (email != null) {
            return resultado(false, "El email '" + email + "' no es válido.");
        }

        Curso curso = curso(cursoTexto, profesorId);
        if (cursoTexto != null && curso == null) {
            return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + codigosCursos(profesorId));
        }

        if (nombre != null) {
            alumno.setNombre(nombre.length() > 25 ? nombre.substring(0, 25).trim() : nombre);
        }
        if (curso != null) {
            alumno.setCurso(curso);
        }
        if (password != null) {
            alumno.setPassword(password);
        }

        int expAntes = exp(alumno);
        int electroniosAntes = electronios(alumno);

        if (exp != null || electronios != null) {
            if (alumno.getTarjetaAlumno() == null) {
                alumno.setTarjetaAlumno(new TarjetaAlumnoDTO());
            }
            if (exp != null) {
                alumno.getTarjetaAlumno().setExp(exp);
            }
            if (electronios != null) {
                if (electronios < -100 || electronios > 100) {
                    return resultado(false, "Los electronios deben estar entre -100 y 100.");
                }
                alumno.getTarjetaAlumno().setElectronios(electronios.byteValue());
            }
        }

        int expDespues = exp(alumno);
        if (expDespues > expAntes) {
            historialService.addMoreExpToHistorial(alumno.getId(), expDespues - expAntes);
        } else if (expDespues < expAntes) {
            historialService.addLessExpToHistorial(alumno.getId(), expAntes - expDespues);
        }

        usuarioService.saveUser(alumno);

        if (electronios(alumno) != electroniosAntes) {
            log.debug("Electronios de {} modificados por el chatbot", alumno.getEmail());
        }

        ObjectNode res = resultado(true, "Datos de " + alumno.getNombre() + " actualizados.");
        res.set("alumno", alumnoJson(alumno));
        if (cursoTexto != null) {
            String clave = alumno.getPassword() == null || alumno.getPassword().isBlank()
                    ? "-" : alumno.getPassword();
            res.put("csv", csvAlumnos(List.of(
                    new String[] {"nombre", "usuario", "contrasena"},
                    new String[] {alumno.getNombre(), alumno.getEmail(), clave})));
        }
        return res;
    }

    private JsonNode borrarAlumno(JsonNode params) {
        UsuarioDTO alumno = buscarAlumno(params);
        if (alumno == null) {
            return resultado(false, "No encuentro ningún alumno con ese email o id.");
        }
        if (alumno.isAdmin()) {
            return resultado(false, "No se puede borrar un administrador desde el chatbot.");
        }
        String nombre = alumno.getNombre();
        Long id = alumno.getId();
        usuarioService.deleteUser(id);
        if (usuarioService.getUserById(id) != null) {
            return resultado(false, "No se ha podido borrar a " + nombre + ".");
        }
        return resultado(true, "Alumno " + nombre + " (" + alumno.getEmail() + ") borrado junto a su tarjeta.");
    }

    private JsonNode darExperiencia(JsonNode params, Long profesorId) {
        Integer cantidad = entero(params, "cantidad");
        if (cantidad == null) {
            cantidad = entero(params, "exp");
        }
        if (cantidad == null) {
            cantidad = entero(params, "experiencia");
        }
        if (cantidad == null) {
            return resultado(false, "Falta la cantidad de experiencia a dar o quitar.");
        }
        String cursoTexto = textoCurso(params);
        JsonNode alumnos = params != null && params.has("alumnos") ? params.get("alumnos") : null;

        if (cursoTexto != null) {
            Curso curso = curso(cursoTexto, profesorId);
            if (curso == null) {
                return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + codigosCursos(profesorId));
            }
            usuarioService.darExpCurso(curso.getId(), cantidad);
            return resultado(true, (cantidad >= 0 ? " + " : " ") + cantidad
                    + " de experiencia aplicada a todo " + curso.getNombre() + " (" + curso.getCodigo() + ").");
        }

        if (alumnos != null && alumnos.isArray() && alumnos.size() > 0) {
            List<Long> ids = new ArrayList<>();
            Set<String> nombres = new LinkedHashSet<>();
            for (JsonNode referencia : alumnos) {
                UsuarioDTO alumno = resolverAlumno(referencia);
                if (alumno == null) {
                    return resultado(false, "No encuentro al alumno " + referencia.asText() + ".");
                }
                if (alumno.isAdmin()) {
                    return resultado(false, "No se puede dar experiencia a un administrador.");
                }
                ids.add(alumno.getId());
                nombres.add(alumno.getNombre());
            }
            usuarioService.darExpAlumnos(ids, cantidad);
            return resultado(true, (cantidad >= 0 ? " + " : " ") + cantidad
                    + " de experiencia aplicada a " + String.join(", ", nombres) + ".");
        }

        if (alcanceTodos(params, alumnos)) {
            List<Long> ids = new ArrayList<>();
            for (UsuarioDTO alumno : usuarioService.getAllAlumnos()) {
                if (!alumno.isAdmin()) {
                    ids.add(alumno.getId());
                }
            }
            if (ids.isEmpty()) {
                return resultado(false, "No hay alumnos dados de alta.");
            }
            usuarioService.darExpAlumnos(ids, cantidad);
            return resultado(true, (cantidad >= 0 ? " + " : " ") + cantidad
                    + " de experiencia aplicada a los " + ids.size() + " alumnos.");
        }

        return resultado(false, "Indica el curso, la lista de alumnos o \"todos\" a los que quieres dar experiencia.");
    }

    /** true si el alcance pedido es "todo el alumnado". */
    private boolean alcanceTodos(JsonNode params, JsonNode alumnos) {
        if (alumnos != null && alumnos.isTextual()) {
            return "todos".equalsIgnoreCase(alumnos.asText().trim());
        }
        if (params != null && params.path("todos").asBoolean(false)) {
            return true;
        }
        String alcance = texto(params, "alcance");
        if (alcance != null) {
            return "todos".equalsIgnoreCase(alcance);
        }
        return alumnos == null && textoCurso(params) == null;
    }

    /**
     * Cambios masivos: pone la experiencia, el nivel o los electronios de todos
     * los alumnos, de un curso entero o de una lista concreta.
     */
    private JsonNode modificarEnMasa(JsonNode params, Long profesorId) {
        Integer exp = entero(params, "exp");
        Integer nivel = entero(params, "nivel");
        Integer electronios = entero(params, "electronios");

        if (exp == null && nivel == null && electronios == null) {
            return resultado(false, "Indica qué hay que cambiar: exp, nivel o electronios. "
                    + "Parámetros recibidos: " + paramsTexto(params));
        }
        if (exp != null && exp < 0) {
            return resultado(false, "La experiencia no puede ser negativa; para quitar experiencia usa \"dar_experiencia\" con cantidad negativa.");
        }
        if (nivel != null && (nivel < 0 || nivel > 6)) {
            return resultado(false, "El nivel debe estar entre 0 y 6.");
        }
        if (electronios != null && (electronios < -100 || electronios > 100)) {
            return resultado(false, "Los electronios deben estar entre -100 y 100.");
        }

        String cursoTexto = textoCurso(params);
        JsonNode alumnosRef = params != null ? params.get("alumnos") : null;
        List<UsuarioDTO> objetivos;
        String alcance;

        if (cursoTexto != null) {
            Curso curso = curso(cursoTexto, profesorId);
            if (curso == null) {
                return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + codigosCursos(profesorId));
            }
            objetivos = usuarioService.getAlumnosFromCurso(curso.getId());
            alcance = "todo " + curso.getNombre() + " (" + curso.getCodigo() + ")";
        } else if (alumnosRef != null && alumnosRef.isArray() && alumnosRef.size() > 0) {
            objetivos = new ArrayList<>();
            for (JsonNode referencia : alumnosRef) {
                UsuarioDTO alumno = resolverAlumno(referencia);
                if (alumno == null) {
                    return resultado(false, "No encuentro al alumno " + referencia.asText() + ".");
                }
                if (alumno.isAdmin()) {
                    return resultado(false, "No se puede modificar a un administrador desde el chatbot.");
                }
                objetivos.add(alumno);
            }
            alcance = "los alumnos indicados";
        } else {
            objetivos = usuarioService.getAllAlumnos();
            alcance = "todo el alumnado";
        }

        List<UsuarioDTO> alumnos = objetivos.stream().filter(a -> !a.isAdmin()).toList();
        if (alumnos.isEmpty()) {
            return resultado(false, "No hay alumnos en ese alcance.");
        }

        int expNueva = exp != null ? exp : (nivel != null ? expMinimoNivel(nivel) : -1);
        int tocados = 0;
        StringBuilder cambios = new StringBuilder();

        for (UsuarioDTO alumno : alumnos) {
            if (alumno.getTarjetaAlumno() == null) {
                alumno.setTarjetaAlumno(new TarjetaAlumnoDTO());
            }
            boolean cambia = false;

            if (expNueva >= 0) {
                int antes = exp(alumno);
                if (antes != expNueva) {
                    alumno.getTarjetaAlumno().setExp(expNueva);
                    if (expNueva > antes) {
                        historialService.addMoreExpToHistorial(alumno.getId(), expNueva - antes);
                    } else {
                        historialService.addLessExpToHistorial(alumno.getId(), antes - expNueva);
                    }
                    cambia = true;
                }
            }
            if (electronios != null && electronios(alumno) != electronios) {
                alumno.getTarjetaAlumno().setElectronios(electronios.byteValue());
                cambia = true;
            }

            if (cambia) {
                usuarioService.saveUser(alumno);
                tocados++;
                if (cambios.length() < 180) {
                    if (cambios.length() > 0) {
                        cambios.append(", ");
                    }
                    cambios.append(alumno.getNombre());
                }
            }
        }

        if (tocados == 0) {
            return resultado(true, "No hacía falta cambiar nada: " + alcance + " ya tenía esos datos.");
        }

        StringBuilder msg = new StringBuilder("Hecho: ");
        boolean algoEscrito = false;
        if (expNueva >= 0) {
            msg.append("exp = ").append(expNueva)
                    .append(" (nivel ").append(nivelDesdeExp(expNueva)).append(")");
            algoEscrito = true;
        }
        if (electronios != null) {
            if (algoEscrito) {
                msg.append(" y ");
            }
            msg.append("electronios = ").append(electronios);
        }
        msg.append(" en ").append(tocados).append(" alumno").append(tocados == 1 ? "" : "s")
                .append(" (").append(alcance).append(").");
        if (cambios.length() > 0) {
            msg.append(" Ejemplos: ").append(cambios).append(".");
        }
        return resultado(true, msg.toString());
    }

    /** Experiencia mínima que corresponde a cada nivel (la BBDD recalcula el nivel solo). */
    private int expMinimoNivel(int nivel) {
        return switch (nivel) {
            case 1 -> 100;
            case 2 -> 250;
            case 3 -> 500;
            case 4 -> 900;
            case 5 -> 1200;
            case 6 -> 1700;
            default -> 0;
        };
    }

    private int nivelDesdeExp(int exp) {
        if (exp >= 1700) {
            return 6;
        }
        if (exp >= 1200) {
            return 5;
        }
        if (exp >= 900) {
            return 4;
        }
        if (exp >= 500) {
            return 3;
        }
        if (exp >= 250) {
            return 2;
        }
        return exp >= 100 ? 1 : 0;
    }

    /** Email de reserva cuando el profe solo da el nombre: ana.garcia@alumno.com */
    private String generarEmail(String nombre) {
        String base = normalizarEmail(nombre);
        if (base.isEmpty()) {
            base = "alumno";
        }
        String candidato = base + "@alumno.com";
        int intento = 1;
        while (usuarioService.getUserByEmail(candidato) != null) {
            intento++;
            candidato = base + intento + "@alumno.com";
        }
        return candidato;
    }

    private String normalizarEmail(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^[.]+", "").replaceAll("[.]+$", "");
        if (limpio.length() > 25) {
            limpio = limpio.substring(0, 25).replaceAll("[.]+$", "");
        }
        return limpio;
    }

    private void aplicarTarjeta(Long idAlumno, Integer exp, Integer electronios) {
        UsuarioDTO alumno = usuarioService.getUserById(idAlumno);
        if (alumno == null) {
            return;
        }
        if (alumno.getTarjetaAlumno() == null) {
            alumno.setTarjetaAlumno(new TarjetaAlumnoDTO());
        }
        if (exp != null) {
            alumno.getTarjetaAlumno().setExp(exp);
        }
        if (electronios != null) {
            alumno.getTarjetaAlumno().setElectronios(electronios.byteValue());
        }
        usuarioService.saveUser(alumno);
    }

    // ------------------------------------------------------------- utilidades

    private UsuarioDTO buscarAlumno(JsonNode params) {
        if (params == null) {
            return null;
        }
        JsonNode referencia = params.has("alumno") ? params.get("alumno")
                : params.has("id") ? params.get("id")
                : params.has("email") ? params.get("email") : null;
        return resolverAlumno(referencia);
    }

    private UsuarioDTO resolverAlumno(JsonNode referencia) {
        if (referencia == null || referencia.isNull()) {
            return null;
        }
        if (referencia.isNumber()) {
            return usuarioService.getUserById(referencia.asLong());
        }
        String valor = referencia.asText("");
        if (valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.matches("\\d+")) {
            UsuarioDTO porId = usuarioService.getUserById(Long.parseLong(limpio));
            if (porId != null) {
                return porId;
            }
        }
        return usuarioService.getUserByEmail(limpio);
    }

    private JsonNode alumnoJson(UsuarioDTO alumno) {
        ObjectNode nodo = mapper.createObjectNode();
        if (alumno == null) {
            return nodo;
        }
        if (alumno.getId() != null) {
            nodo.put("id", alumno.getId());
        }
        nodo.put("nombre", alumno.getNombre() == null ? "" : alumno.getNombre());
        nodo.put("email", alumno.getEmail() == null ? "" : alumno.getEmail());
        nodo.put("curso", alumno.getCurso() == null ? "" : alumno.getCurso().getCodigo());
        nodo.put("nivel", nivel(alumno));
        nodo.put("exp", exp(alumno));
        nodo.put("electronios", electronios(alumno));
        return nodo;
    }

    private ObjectNode resultado(boolean ok, String mensaje) {
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("ok", ok);
        nodo.put("mensaje", mensaje);
        return nodo;
    }

    /** El resultado sin el CSV entero: al modelo solo le interesa que exista la descarga. */
    private String resultadoSinCsv(JsonNode resultado) {
        if (resultado == null) {
            return "{}";
        }
        if (!resultado.has("csv")) {
            return resultado.toString();
        }
        ObjectNode copia = resultado.deepCopy();
        copia.put("csv", "[CSV generado: se ofrece al usuario para descargarlo]");
        return copia.toString();
    }

    private int nivel(UsuarioDTO alumno) {
        if (alumno.getTarjetaAlumno() == null || alumno.getTarjetaAlumno().getNivel() == null) {
            return 0;
        }
        return alumno.getTarjetaAlumno().getNivel().intValue();
    }

    private int exp(UsuarioDTO alumno) {
        if (alumno.getTarjetaAlumno() == null || alumno.getTarjetaAlumno().getExp() == null) {
            return 0;
        }
        return alumno.getTarjetaAlumno().getExp().intValue();
    }

    private int electronios(UsuarioDTO alumno) {
        if (alumno.getTarjetaAlumno() == null || alumno.getTarjetaAlumno().getElectronios() == null) {
            return 0;
        }
        return alumno.getTarjetaAlumno().getElectronios().intValue();
    }

    private String paramsTexto(JsonNode params) {
        return params == null ? "{}" : params.toString();
    }

    private String texto(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        String texto = valor.asText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private Integer entero(JsonNode nodo, String campo) {
        JsonNode valor = nodo == null ? null : nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        try {
            if (valor.isNumber()) {
                return valor.asInt();
            }
            String texto = valor.asText("").trim();
            return texto.isEmpty() ? null : Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private ArrayNode array() {
        return mapper.createArrayNode();
    }
}
