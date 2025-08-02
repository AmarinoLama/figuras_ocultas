package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.entity.Webconfig;
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

@Controller
public class WebController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private WebConfigService webConfigService;

    @GetMapping("/webConfig")
    public String loginForm(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());
            model.addAttribute("webConfigForm", new Webconfig());

            return "webConfig";

        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/webConfig/changeName")
    public String changeName(@ModelAttribute("webConfig") Webconfig webConfig) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {

            webConfigService.updateWebConfig(webConfig.getNombre());

            return "redirect:/webConfig";
        } else {
            return "redirect:/login";
        }
    }

}