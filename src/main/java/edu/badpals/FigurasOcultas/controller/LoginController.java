package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    /// TODO: hashing user passwords

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @GetMapping({"/login", "/"})
    public String loginForm(Model model) {
        model.addAttribute("loginData", new UsuarioDTO());
        return "formLogin";
    }

    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute("loginData") UsuarioDTO userdto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loginData", new UsuarioDTO());
            return "formLogin";
        }

        UsuarioDTO usuario = usuarioService.getUserByEmail(userdto.getEmail());

        if (usuario != null && usuario.getPassword().equals(userdto.getPassword())) {
            managerUserSession.logearUsuario(usuario.getId());
            return "redirect:/alumnos";
        } else {
            model.addAttribute("error", "Contraseña o usuario incorrectos");
            return "formLogin";
        }
    }
}