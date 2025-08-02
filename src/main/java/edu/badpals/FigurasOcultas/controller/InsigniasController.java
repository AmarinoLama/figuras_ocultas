package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Insignia;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.entity.Webconfig;
import edu.badpals.FigurasOcultas.service.InsigniaService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import edu.badpals.FigurasOcultas.service.WebConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class InsigniasController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private WebConfigService webConfigService;

    @Autowired
    private InsigniaService insigniaService;

    @GetMapping("/alumnos/insignias/{id}")
    public String insigniasAlumnos(@PathVariable(value = "id") Long idAlumno, Model model) {

        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            UsuarioDTO alumno = usuarioService.getUserById(idAlumno);
            model.addAttribute("alumno", alumno);
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());
            return "insignias";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/alumnos/insignias/asignar/{id}")
    public String asignarInsignia(
            @PathVariable(value = "id") Long idAlumno,
            @RequestParam("nombre") String nombreInsignia,
            @RequestParam("imagen") MultipartFile imagenFile) {

        try {

            System.out.println("Asignando insignia a alumno con ID: " + idAlumno + ", Nombre: " + nombreInsignia +
                    ", Imagen: " + imagenFile.getOriginalFilename());

            Insignia insignia = new Insignia();
            insignia.setNombre(nombreInsignia);
            insignia.setImagen(imagenFile.getBytes());

            insigniaService.saveInsignia(insignia, idAlumno);

        } catch (IOException e) {
            return "redirect:/alumnos?error=Error al procesar la imagen.";
        }

        return "redirect:/alumnos/insignias/" + idAlumno;
    }
}