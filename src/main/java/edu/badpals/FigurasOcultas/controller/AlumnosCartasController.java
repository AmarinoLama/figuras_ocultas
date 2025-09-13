package edu.badpals.FigurasOcultas.controller;
import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.HistorialTransacciones;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.service.CartaUsuarioService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import edu.badpals.FigurasOcultas.service.WebConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/// TODO: Hacer DTO de alumnosCartas
/// TODO: Eliminar atributos de fechas porque ya están en el historial de transacciones

@Controller
public class AlumnosCartasController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CartaUsuarioService cartaUsuarioService;

    @Autowired
    private WebConfigService webConfigService;

    @GetMapping("/inventario")
    public String loadIndex(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            if (usuario.getRol() == RolUsuario.ALUMNO) {
                model.addAttribute("cartas", cartaUsuarioService.getCartasDisponiblesByUsuario(usuarioLogeadoId));
                Map<Long, Integer> inventario = cartaUsuarioService.getCartasByAlumno(usuario.getId());
                model.addAttribute("inventario", inventario);
            }

            return "inventario";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/cartas/comprar/{id}")
    public String comprarCarta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null && !usuario.isAdmin()) {
                boolean operacionExitosa = cartaUsuarioService.comprarCarta(usuario.getId(), id);
                if (!operacionExitosa) {
                    redirectAttributes.addFlashAttribute("mensajeCompra", "❌ No se pudo completar la compra debido a la insuficiencia de electronios.");
                    redirectAttributes.addFlashAttribute("tipoAlerta", "danger");
                } else {
                    redirectAttributes.addFlashAttribute("mensajeCompra", "✅ ¡Compra realizada con éxito!");
                    redirectAttributes.addFlashAttribute("tipoAlerta", "success");
                }
            }
        }
        return "redirect:/cartas";
    }

    @PostMapping("/inventario/usar/{idCarta}")
    public String usarCarta(@PathVariable Long idCarta, RedirectAttributes redirectAttributes) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null && !usuario.isAdmin()) {
                boolean operacionExitosa = cartaUsuarioService.usarCarta(idCarta, usuarioLogeadoId);
                if (!operacionExitosa) {
                    redirectAttributes.addFlashAttribute("mensajeUso", "❌ No se ha podido usar la carta");
                    redirectAttributes.addFlashAttribute("tipoAlerta", "danger");
                } else {
                    redirectAttributes.addFlashAttribute("mensajeUso", "✅ ¡Carta usada correctamente");
                    redirectAttributes.addFlashAttribute("tipoAlerta", "success");
                }
            }
        }
        return "redirect:/inventario";
    }

    @GetMapping("/historial")
    public String historialCartas(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);

            model.addAttribute("usuario", usuario);

            List<HistorialTransacciones> historial = cartaUsuarioService.getHistorial(usuarioLogeadoId);
            Collections.reverse(historial);
            model.addAttribute("historial", historial);

            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            return "historial";
        }
        return "redirect:/login";
    }
}