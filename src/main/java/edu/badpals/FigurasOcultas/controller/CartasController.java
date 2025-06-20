package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.service.CartaService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/// TODO: hacer DTO de las cartas

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
            model.addAttribute("nuevaCarta", new Carta());
            model.addAttribute("cartas", cartaService.getAllCartas());
            System.out.println("Cartas: " + cartaService.getAllCartas());
        } else {
            return "redirect:/login";
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

    @PostMapping("/cartas/guardar")
    public String guardarCarta(@RequestParam("titulo") String titulo,
                               @RequestParam("precio") Integer precio,
                               @RequestParam("descripcion") String descripcion,
                               @RequestParam(value = "activa", required = false) Boolean activa,
                               @RequestParam("imagen") MultipartFile imagenFile) {
        cartaService.guardarCarta(titulo, precio, descripcion, activa, imagenFile);
        return "redirect:/cartas";
    }

    @GetMapping("/cartas/imagen/{id}")
    public ResponseEntity<byte[]> mostrarImagen(@PathVariable Long id) {
        try {
            byte[] imagen = cartaService.obtenerImagenCarta(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(imagen, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}