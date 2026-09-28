package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.ChatbotRequest;
import edu.badpals.FigurasOcultas.model.dto.ChatbotResponse;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.service.ChatbotService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * API del chatbot de la web. Solo los administradores pueden usarla:
 * el widget ni siquiera se pinta en las páginas del alumnado.
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(@RequestBody(required = false) ChatbotRequest request,
                                                HttpSession sesionHttp) {
        Long usuarioId = managerUserSession.usuarioLogeado();
        if (usuarioId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Tu sesión ha caducado, vuelve a iniciar sesión.");
        }

        UsuarioDTO usuario = usuarioService.getUserById(usuarioId);
        if (usuario == null || !usuario.isAdmin()) {
            log.warn("Intento de usar el chatbot desde una cuenta no administradora (id {})", usuarioId);
            return error(HttpStatus.FORBIDDEN, "El chat está reservado al personal docente.");
        }

        ChatbotRequest datos = request != null ? request : new ChatbotRequest();
        long inicio = System.nanoTime();
        ChatbotResponse respuesta = chatbotService.chat(datos, sesionHttp, usuarioId);
        respuesta.setTiempoMs((System.nanoTime() - inicio) / 1_000_000L);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CSV generado por la última acción masiva del chatbot: nombres completos,
     * usuario (email) y contraseña de los alumnos creados o matriculados.
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> descargarCsv(HttpSession sesionHttp) {
        Long usuarioId = managerUserSession.usuarioLogeado();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UsuarioDTO usuario = usuarioService.getUserById(usuarioId);
        if (usuario == null || !usuario.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Object contenido = sesionHttp.getAttribute(ChatbotService.ATRIBUTO_CSV);
        if (!(contenido instanceof String csv) || csv.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Object nombre = sesionHttp.getAttribute(ChatbotService.ATRIBUTO_CSV_NOMBRE);
        String fichero = (nombre instanceof String n && !n.isBlank()) ? n : "alumnos.csv";

        byte[] cuerpo = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichero + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(cuerpo);
    }

    private ResponseEntity<ChatbotResponse> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ChatbotResponse(mensaje));
    }
}
