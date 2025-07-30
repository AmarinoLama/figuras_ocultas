package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import edu.badpals.FigurasOcultas.service.WebConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/// TODO: hashear las contraseñas de los usuarios
/// TODO: mejorar la interfaz
/// TODO: poner un botón para desloguearse

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private WebConfigService webConfigService;

    @GetMapping({"/login", "/"})
    public String loginForm(Model model) {
        model.addAttribute("loginData", new UsuarioDTO());
        model.addAttribute("nombreWeb", webConfigService.getWebConfig());
        return "formLogin";
    }

    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute("loginData") UsuarioDTO userdto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loginData", userdto);
            return "formLogin";
        }

        UsuarioDTO usuario = usuarioService.getUserByEmail(userdto.getEmail());

        if (usuario != null && usuario.getPassword().equals(userdto.getPassword())) {
            managerUserSession.logearUsuario(usuario.getId());
            if (usuario.isAdmin()) {
                return "redirect:/alumnos";
            } else {
                return "redirect:/cartas";
            }
        } else {
            model.addAttribute("error", "Contraseña o usuario incorrectos");
            return "formLogin";
        }
    }
}