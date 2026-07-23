package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.CartaDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.service.CartaService;
import edu.badpals.FigurasOcultas.service.CartaUsuarioService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import edu.badpals.FigurasOcultas.service.WebConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

/// TODO: hacer DTO de las cartas
/// TODO: mejorar la vista en general
/// TODO: mejorar los mensajes de confirmación
/// TODO: hacer que las fotos tengan el mismo tamaño
/// TODO: hacer DTO de las cartas en el método getAllCardsDTO()
/// TODO: dividir la parte de la tienda de lo de administradores

@Controller
public class CartasController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CartaService cartaService;

    @Autowired
    private CartaUsuarioService cartaUsuarioService;

    @Autowired
    private WebConfigService webConfigService;

    @GetMapping("/cartas")
    public String loadCartas(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (!usuarioLogeado) {
            return "redirect:/login";
        }

        UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
        model.addAttribute("usuario", usuario);
        Carta cartanew = new Carta();
        cartanew.setPrecio(0);
        model.addAttribute("nuevaCarta", cartanew);
        model.addAttribute("nombreWeb", webConfigService.getWebConfig());

        if (usuario.isAdmin()) {
            var cartasPage = cartaService.getAllCartasDTO(org.springframework.data.domain.PageRequest.of(page, size));
            model.addAttribute("cartas", cartasPage.getContent());
            model.addAttribute("cartasPageNumber", cartasPage.getNumber());
            model.addAttribute("cartasTotalPages", cartasPage.getTotalPages());
            model.addAttribute("cartasTotalElements", cartasPage.getTotalElements());
            model.addAttribute("cartasEmpty", cartasPage.getTotalElements() == 0);
        } else {
            var cartasPage = cartaService.getCartasDisponibles(org.springframework.data.domain.PageRequest.of(page, size));
            model.addAttribute("cartas", cartasPage.getContent());
            model.addAttribute("cartasPageNumber", cartasPage.getNumber());
            model.addAttribute("cartasTotalPages", cartasPage.getTotalPages());
            model.addAttribute("cartasTotalElements", cartasPage.getTotalElements());

            Map<Long, Integer> inventario = cartaUsuarioService.getCartasByAlumno(usuario.getId());
            model.addAttribute("electroniosTotales", usuario.getTarjetaAlumno().getElectronios());
            model.addAttribute("inventario", inventario);
            // show empty message if there are no cartas
            model.addAttribute("cartasEmpty", cartasPage.getTotalElements() == 0);
        }

        return "cartas";
    }

    @PostMapping("/cartas/alternarVisibilidad/{id}")
    public String alternarVisibilidad(@PathVariable Long id) throws IOException {
        CartaDTO carta = cartaService.getCartaById(id);
        carta.setActiva(!carta.getActiva());
        cartaService.actualizarCarta(carta, id, null);
        return "redirect:/cartas";
    }

    @PostMapping("/cartas/nueva")
    public String guardarCarta(@RequestParam("titulo") String titulo,
                               @RequestParam("precio") Integer precio,
                               @RequestParam("descripcion") String descripcion,
                               @RequestParam(value = "activa", required = false) Boolean activa,
                               @RequestParam("imagenFile") MultipartFile imagenFile) {
        cartaService.guardarCarta(titulo, precio, descripcion, activa, imagenFile);
        return "redirect:/cartas";
    }

    @PostMapping("/cartas/borrar/{id}")
    public String borrarCarta(@PathVariable(value = "id") Long idCarta) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {
                cartaService.borrarCarta(idCarta);
            }
        }
        return "redirect:/cartas";
    }

    @GetMapping("/cartas/imagen/{id}")
    public ResponseEntity<byte[]> mostrarImagen(@PathVariable Long id) {
        try {
            byte[] imagen = cartaService.obtenerImagenCarta(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("max-age=86400, public");
            return new ResponseEntity<>(imagen, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cartas/info")
    public String cargarDatosCarta(@RequestParam Long id, Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);

            CartaDTO carta = cartaService.getCartaById(id);
            model.addAttribute("carta", carta);

            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            if (usuario.isAdmin()) {
                return "fragments/editCarta :: editCarta";
            } else {
                return "fragments/infoCarta :: infoCarta";
            }
        }
        return "redirect:/cartas";
    }

    @PostMapping("/cartas/editar/{id}")
    public String editarCarta(@PathVariable("id") Long idCarta,
                              @ModelAttribute CartaDTO cartaEditada,
                              @RequestParam("imagenFile") MultipartFile imagenFile) throws IOException {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {
                cartaService.actualizarCarta(cartaEditada, idCarta, imagenFile);
            }
        }
        return "redirect:/cartas";
    }
}