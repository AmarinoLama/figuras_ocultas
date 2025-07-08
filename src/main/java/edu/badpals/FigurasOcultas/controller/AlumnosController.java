package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.service.TarjetaAlumnoService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/// TODO: Añadir una barra de búsqueda para buscar alumnos
/// TODO: Añadir paginación para la lista de alumnos
/// TODO: Mejorar los iconos de la tabla de ordenar
/// TODO: La contraseña del menú de editar ocultarla
/// TODO: mejorar los mensajes de confirmación

@Controller
public class AlumnosController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TarjetaAlumnoService tarjetaAlumnoService;

    @GetMapping("/alumnos")
    public String loadIndex(Model model, @RequestParam(required = false) String curso) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("nuevoAlumno", new UsuarioDTO());

            if (curso != null && !curso.isEmpty()) {
                model.addAttribute("alumnos", usuarioService.getAlumnosFromCurso(curso));
            } else {
                model.addAttribute("alumnos", usuarioService.getAllAlumnos());
            }
            model.addAttribute("cursoSeleccionado", curso);
        }

        return "alumnos";
    }

    @GetMapping("/alumnos/info")
    public String cargarDatosAlumno(@RequestParam String email, Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            UsuarioDTO alumno = usuarioService.getUserByEmail(email);
            model.addAttribute("alumno", alumno);
            return "fragments/editAlumno :: editAlumno";
        }
        return "alumnos";
    }

    @PostMapping("/alumnos/nuevo")
    public String createAlumno(@Valid @ModelAttribute("nuevoAlumno") UsuarioDTO nuevoAlumno, RedirectAttributes redirectAttributes) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            if (usuarioService.getUserByEmail(nuevoAlumno.getEmail()) == null) {
                nuevoAlumno.setRol(RolUsuario.ALUMNO);
                usuarioService.saveUser(nuevoAlumno);
                UsuarioDTO usuarioCreado = usuarioService.getUserByEmail(nuevoAlumno.getEmail());
                TarjetaAlumnoDTO tarjetaAlumnoDTO = usuarioCreado.getTarjetaAlumno();
                tarjetaAlumnoDTO.setUsuarioId(usuarioCreado.getId());
                tarjetaAlumnoService.addTarjetaAlumno(tarjetaAlumnoDTO);
            } else {
                redirectAttributes.addFlashAttribute("showAlert", true);
            }
        }
        return "redirect:/alumnos";
    }

    @PostMapping("/alumnos/editar/{id}")
    public String editarAlumno(@PathVariable("id") Long idAlumno,
                               @ModelAttribute UsuarioDTO alumnoEditado) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {
                // Obtén el alumno original
                UsuarioDTO alumnoOriginal = usuarioService.getUserById(idAlumno);

                // Actualiza los campos
                alumnoOriginal.setNombre(alumnoEditado.getNombre());
                alumnoOriginal.setPassword(alumnoEditado.getPassword());
                alumnoOriginal.setCurso(alumnoEditado.getCurso());

                alumnoOriginal.getTarjetaAlumno().setExp(alumnoEditado.getTarjetaAlumno().getExp());
                alumnoOriginal.getTarjetaAlumno().setElectronios(alumnoEditado.getTarjetaAlumno().getElectronios());

                // Guarda
                System.out.println("Actualizando alumno: " + alumnoOriginal);
                usuarioService.saveUser(alumnoOriginal);
            }
        }
        return "redirect:/alumnos";
    }


    @PostMapping("/alumnos/borrar/{id}")
    public String borrarAlumno(@PathVariable(value = "id") Long idAlumno) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {
                usuarioService.deleteUser(idAlumno);
                tarjetaAlumnoService.borrarTarjetaAlumno(idAlumno);
            }
        }
        return "redirect:/alumnos";
    }
}