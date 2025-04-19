package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AlumnosController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/alumnos")
    public String loadIndex(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("alumnos", usuarioService.getAllAlumnos());
            model.addAttribute("usuario", usuario);
            model.addAttribute("nuevoAlumno", new UsuarioDTO());
        }
        return "alumnos";
    }

    @PostMapping("/alumnos/nuevo")
    public String createAlumno(UsuarioDTO nuevoAlumno) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            System.out.println("Creando nuevo alumno: " + nuevoAlumno.toString());
            //usuarioService.createAlumno(newAlumno);
        }
        return "redirect:/alumnos";
    }

}