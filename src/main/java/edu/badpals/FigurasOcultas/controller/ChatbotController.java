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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(chatbotService.chat(datos, sesionHttp, usuarioId));
    }

    private ResponseEntity<ChatbotResponse> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ChatbotResponse(mensaje));
    }
}
