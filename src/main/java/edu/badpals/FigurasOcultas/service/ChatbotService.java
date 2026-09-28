package edu.badpals.FigurasOcultas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.badpals.FigurasOcultas.model.dto.ChatbotRequest;
import edu.badpals.FigurasOcultas.model.dto.ChatbotResponse;
import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.CursoAlumno;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chatbot de la web: usa opencode serve (modelo ling-3.0-flash-fin-free)
 * para interpretar lo que pide el profe y ejecutar acciones sobre los alumnos.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    public static final String ACCION_NINGUNA = "ninguna";
    private static final String ATRIBUTO_SESION = "chatbotSesionOpencode";
    private static final String TITULO_SESION = "chatbot-web";
    private static final int MAX_ALUMNOS_PROMPT = 300;
    private static final int SESIONES_BORRADAS_POR_PETICION = 5;
    private static final long SESIONES_TTL_MS = 24L * 60L * 60L * 1000L;

    private static final Set<String> ACCIONES = Set.of(
            "crear_alumno", "modificar_alumno", "borrar_alumno", "dar_experiencia", "modificar_en_masa");
    private static final Set<String> ACCIONES_DESTRUCTIVAS = Set.of("borrar_alumno");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TarjetaAlumnoService tarjetaAlumnoService;

    @Autowired
    private HistorialTransaccionesService historialService;

    @Autowired
    private OpencodeClient opencode;

    // ------------------------------------------------------------------ API

    public ChatbotResponse chat(ChatbotRequest request, HttpSession sesionHttp) {
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
                JsonNode resultado = ejecutarAccion(request.getAccion(), request.getParams());
                ChatbotResponse respuesta = new ChatbotResponse(
                        resumir(sesion, mensajeConfirmacion(request.getAccion(), request.getParams(), resultado), resultado));
                respuesta.setAccionEjecutada(resultado.path("ok").asBoolean(false));
                return respuesta;
            }

            JsonNode estructurado = llamar(sesion, sistemaAccion(), formatoAccion(), mensaje);
            String accion = texto(estructurado, "accion");
            String propia = texto(estructurado, "respuesta");
            JsonNode params = estructurado.has("params") && estructurado.get("params").isObject()
                    ? estructurado.get("params") : null;

            if (!esAccionValida(accion)) {
                return new ChatbotResponse(propia != null ? propia
                        : "No he entendido bien lo que me pides, ¿me lo puedes reformular?");
            }

            if (requiereConfirmacion(accion, params)) {
                ChatbotResponse respuesta = new ChatbotResponse();
                respuesta.setRespuesta(propia != null ? propia
                        : "Esta acción cambia datos de muchos alumnos. ¿Quieres que continúe?");
                respuesta.setAccion(accion);
                respuesta.setParams(params);
                respuesta.setConfirmacionRequerida(true);
                respuesta.setTextoConfirmacion(textoConfirmacion(accion, params));
                return respuesta;
            }

            JsonNode resultado = ejecutarAccion(accion, params);
            ChatbotResponse respuesta = new ChatbotResponse(
                    resumir(sesion, mensajeAccion(accion, params, resultado), resultado));
            respuesta.setAccionEjecutada(resultado.path("ok").asBoolean(false));
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
            String respuesta = texto(estructurado, "respuesta");
            if (respuesta != null) {
                return respuesta;
            }
        } catch (OpencodeClient.OpencodeException e) {
            log.warn("No se pudo redactar el resumen de la acción: {}", e.getMessage());
        }
        String local = texto(resultado, "mensaje");
        return local != null ? local : "Hecho.";
    }

    private String mensajeAccion(String accion, JsonNode params, JsonNode resultado) {
        return "El usuario te ha pedido la acción \"" + accion + "\" con estos parámetros: "
                + paramsTexto(params) + ".\n"
                + "El resultado real de ejecutarla ha sido: " + resultado.toString() + "\n"
                + "Cuéntaselo al usuario en español, breve y natural, indicando si ha ido bien o qué ha fallado. "
                + "Solo texto plano (sin Markdown, sin listas, sin emojis) y sin mencionar avisos internos del sistema. "
                + "No propongas nuevas acciones ni uses herramientas.";
    }

    private String mensajeConfirmacion(String accion, JsonNode params, JsonNode resultado) {
        return "El usuario ha confirmado que quiere ejecutar la acción \"" + accion + "\" con estos parámetros: "
                + paramsTexto(params) + ".\n"
                + "El resultado real ha sido: " + resultado.toString() + "\n"
                + "Cuéntaselo en español, breve y natural. No propongas acciones ni uses herramientas.";
    }

    // ------------------------------------------------------ prompts y esquemas

    private String sistemaAccion() {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres \"ProfeBot\", el asistente de la web escolar \"Figuras Ocultas\". ");
        sb.append("Hablas SIEMPRE en español, de forma breve, cercana y sin rodeos.\n\n");
        sb.append("REGLAS:\n");
        sb.append("1. NUNCA uses herramientas (bash, archivos, búsquedas...). Solo respondes con el JSON que pide el esquema.\n");
        sb.append("2. Solo puedes consultar y modificar ALUMNOS (rol ALUMNO). No toques cartas, insignias ni administradores.\n");
        sb.append("3. Los datos que tienes son los del LISTADO de abajo, que se refresca en cada mensaje: no inventes.\n");
        sb.append("4. Si falta información indispensable (por ejemplo, hay varios alumnos con ese nombre), pídesela al usuario.\n");
        sb.append("5. Para cambiar datos devuelve la acción en \"accion\" y sus argumentos en \"params\". ");
        sb.append("Si no hace falta ninguna acción, pon \"accion\": \"ninguna\".\n");
        sb.append("6. En \"respuesta\" escribe lo que verá el usuario en el chat. Si propones una acción, explica qué vas a hacer.\n");
        sb.append("7. Una sola acción por mensaje.\n");
        sb.append("8. \"respuesta\" es solo texto plano: sin Markdown (ni **, ni `, ni >), sin listas y sin emojis.\n");
        sb.append("9. Nunca menciones avisos internos del sistema (límites de pasos, herramientas, permisos o configuración): ");
        sb.append("habla únicamente de lo que te pide el usuario.\n\n");
        sb.append("ACCIONES DISPONIBLES:\n");
        sb.append("- crear_alumno -> params {\"nombre\", \"curso\", \"email\"?, \"password\"?, \"exp\"?, \"electronios\"?}. ");
        sb.append("curso es obligatorio; si no pones email se genera uno automaticamente (por ejemplo ana.garcia@alumno.com) ");
        sb.append("y si no pones password se usa la parte del email antes de la @.\n");
        sb.append("- modificar_alumno -> params {\"alumno\", \"nombre\"?, \"email\"?, \"curso\"?, \"password\"?, \"exp\"?, \"electronios\"?}. ");
        sb.append("\"alumno\" es el email o la id del listado; solo incluye los campos que quieras cambiar.\n");
        sb.append("- borrar_alumno -> params {\"alumno\"}. DESTRUCTIVA: el usuario debe confirmarla.\n");
        sb.append("- dar_experiencia -> params {\"cantidad\"} m\u00e1s el alcance: {\"curso\"} o {\"alumnos\": [\"ana@centro.es\", 12]} ");
        sb.append("o {\"alumnos\": \"todos\"} (o {\"alcance\": \"todos\"}). La cantidad puede ser negativa para quitar experiencia.\n");
        sb.append("- modificar_en_masa -> params {\"exp\"?, \"nivel\"?, \"electronios\"?} m\u00e1s el alcance: nada (todo el alumnado), ");
        sb.append("{\"curso\": \"1BACH\"} o {\"alumnos\": [\"ana@centro.es\", 12]}. Pone esos valores de forma ABSOLUTA (no suma).\n");
        sb.append("  OJO: \"nivel\" solo cambia porque la experiencia es la que lo fija; pon exp al m\u00ednimo de ese nivel: ");
        sb.append("0->0, 1->100, 2->250, 3->500, 4->900, 5->1200, 6->1700. Si quieres subir experiencia en vez de fijarla, usa dar_experiencia.\n");
        sb.append("  Si \"modificar_en_masa\" afecta a TODOS los alumnos, el usuario tendr\u00e1 que confirmar; si es de un curso o de una lista, se ejecuta ya.\n\n");
        sb.append("CURSOS VÁLIDOS (usa siempre el código de la izquierda):\n");
        sb.append(cursosValidos()).append("\n\n");
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
                + "\",\"crear_alumno\",\"modificar_alumno\",\"borrar_alumno\",\"dar_experiencia\",\"modificar_en_masa\"]},"
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

    private String cursosValidos() {
        StringBuilder sb = new StringBuilder();
        for (CursoAlumno curso : CursoAlumno.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(curso.name()).append(" (").append(curso).append(")");
        }
        return sb.toString();
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
                    .append(alumno.getCurso()).append(" | ")
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
        if (texto(params, "curso") != null) {
            return false;
        }
        JsonNode alumnos = params.get("alumnos");
        if (alumnos != null && alumnos.isArray()) {
            return alumnos.size() == 0;
        }
        String alcance = texto(params, "alcance");
        return alcance == null || "todos".equalsIgnoreCase(alcance);
    }

    private String textoConfirmacion(String accion, JsonNode params) {
        if (ACCIONES_DESTRUCTIVAS.contains(accion)) {
            return "⚠️ Esta acción borra datos de forma definitiva. ¿Continúo?";
        }
        return "⚠️ Esta acción cambiará datos de TODO el alumnado a la vez. ¿Continúo?";
    }

    private JsonNode ejecutarAccion(String accion, JsonNode params) {
        try {
            return switch (accion) {
                case "crear_alumno" -> crearAlumno(params);
                case "modificar_alumno" -> modificarAlumno(params);
                case "borrar_alumno" -> borrarAlumno(params);
                case "dar_experiencia" -> darExperiencia(params);
                case "modificar_en_masa" -> modificarEnMasa(params);
                default -> resultado(false, "Acción desconocida: " + accion);
            };
        } catch (Exception e) {
            log.error("Error ejecutando la acción '{}' del chatbot", accion, e);
            return resultado(false, "Error interno al ejecutar la acción: " + e.getMessage());
        }
    }

    private JsonNode crearAlumno(JsonNode params) {
        String nombre = texto(params, "nombre");
        String email = texto(params, "email");
        String cursoTexto = texto(params, "curso");
        String password = texto(params, "password");
        Integer exp = entero(params, "exp");
        Integer electronios = entero(params, "electronios");

        if (nombre == null) {
            return resultado(false, "Falta el nombre del alumno.");
        }
        if (nombre.length() > 25) {
            nombre = nombre.substring(0, 25).trim();
        }
        CursoAlumno curso = curso(cursoTexto);
        if (curso == null) {
            return resultado(false, "El curso '" + (cursoTexto == null ? "" : cursoTexto)
                    + "' no existe. Usa uno de: " + cursosValidos());
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
        return res;
    }

    private JsonNode modificarAlumno(JsonNode params) {
        UsuarioDTO alumno = buscarAlumno(params);
        if (alumno == null) {
            return resultado(false, "No encuentro ningún alumno con ese email o id.");
        }
        if (alumno.isAdmin()) {
            return resultado(false, "Solo puedo modificar alumnos de clase, no administradores.");
        }

        String nombre = texto(params, "nombre");
        String email = texto(params, "email");
        String cursoTexto = texto(params, "curso");
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

        CursoAlumno curso = curso(cursoTexto);
        if (cursoTexto != null && curso == null) {
            return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + cursosValidos());
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

    private JsonNode darExperiencia(JsonNode params) {
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
        String cursoTexto = texto(params, "curso");
        JsonNode alumnos = params != null && params.has("alumnos") ? params.get("alumnos") : null;

        if (cursoTexto != null) {
            CursoAlumno curso = curso(cursoTexto);
            if (curso == null) {
                return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + cursosValidos());
            }
            usuarioService.darExpCurso(curso.name(), cantidad);
            return resultado(true, (cantidad >= 0 ? " + " : " ") + cantidad
                    + " de experiencia aplicada a todo " + curso + ".");
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
        return alumnos == null && texto(params, "curso") == null;
    }

    /**
     * Cambios masivos: pone la experiencia, el nivel o los electronios de todos
     * los alumnos, de un curso entero o de una lista concreta.
     */
    private JsonNode modificarEnMasa(JsonNode params) {
        Integer exp = entero(params, "exp");
        Integer nivel = entero(params, "nivel");
        Integer electronios = entero(params, "electronios");

        if (exp == null && nivel == null && electronios == null) {
            return resultado(false, "Indica qué hay que cambiar: exp, nivel o electronios.");
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

        String cursoTexto = texto(params, "curso");
        JsonNode alumnosRef = params != null ? params.get("alumnos") : null;
        List<UsuarioDTO> objetivos;
        String alcance;

        if (cursoTexto != null) {
            CursoAlumno curso = curso(cursoTexto);
            if (curso == null) {
                return resultado(false, "El curso '" + cursoTexto + "' no existe. Usa uno de: " + cursosValidos());
            }
            objetivos = usuarioService.getAlumnosFromCurso(curso.name());
            alcance = "todo " + curso;
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

    private CursoAlumno curso(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = normalizar(valor);
        for (CursoAlumno curso : CursoAlumno.values()) {
            if (normalizar(curso.name()).equals(limpio) || normalizar(curso.toString()).equals(limpio)) {
                return curso;
            }
        }
        return null;
    }

    private String normalizar(String valor) {
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().replaceAll("[^A-Z0-9]", "");
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
        nodo.put("curso", alumno.getCurso() == null ? "" : alumno.getCurso().name());
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
