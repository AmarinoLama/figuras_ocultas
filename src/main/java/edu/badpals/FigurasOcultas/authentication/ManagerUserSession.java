package edu.badpals.FigurasOcultas.authentication;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManagerUserSession {

    @Autowired
    HttpSession session;

    public void logearUsuario(Long idUsuario) {
        session.setAttribute("idUsuarioLogeado", idUsuario);
    }

    public Long usuarioLogeado() {
        return (Long) session.getAttribute("idUsuarioLogeado");
    }

    public void logout() {
        session.setAttribute("idUsuarioLogeado", null);
    }
}