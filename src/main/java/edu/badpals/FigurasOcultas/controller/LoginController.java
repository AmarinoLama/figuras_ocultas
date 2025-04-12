package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginData", new UsuarioDTO());
        return "formLogin";
    }

    @PostMapping("/login")
    public String loginSubmit(@ModelAttribute UsuarioDTO userdto) {
        System.out.println(userdto.toString());
        UsuarioDTO usuario = usuarioService.getUserByEmail(userdto.getEmail());
        if (usuario != null && usuario.getPassword().equals(userdto.getPassword())) {
            return "/index";
        } else {
            return "redirect:/login";
        }
    }
}
