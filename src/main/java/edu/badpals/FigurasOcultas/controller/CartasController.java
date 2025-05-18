package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.service.CartaService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CartasController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CartaService cartaService;

    @GetMapping("/cartas")
    public String loadCartas(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("nuevaCarta", new Carta()); // Añadir DTO para la nueva carta

            model.addAttribute("cartas", cartaService.getAllCartas());
            System.out.println("Cartas: " + cartaService.getAllCartas());
        } else {
            return "redirect:/login"; // O la página que desees
        }

        return "cartas";
    }

    @PostMapping("/cartas/alternarVisibilidad/{id}")
    public String alternarVisibilidad(@PathVariable Long id) {
        System.out.println("Alternando visibilidad de la carta con ID: " + id);
        /*Carta carta = cartaRepository.findById(id).orElseThrow();
        carta.setVisible(!carta.isVisible());
        cartaRepository.save(carta);*/
        return "redirect:/cartas";
    }

}