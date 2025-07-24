package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.service.HistorialTransaccionesService;
import edu.badpals.FigurasOcultas.service.TarjetaAlumnoService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

/// TODO: Añadir una barra de búsqueda para buscar alumnos
/// TODO: Añadir paginación para la lista de alumnos
/// TODO: Poner un botón para desloguearse

/// mejorar la comunicación conforme los errores (en el email intentar arreglar eso)
/// extraer las cosas q sean del service al service

@Controller
public class AlumnosController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private HistorialTransaccionesService hts;

    @Autowired
    private TarjetaAlumnoService tarjetaAlumnoService;

    @GetMapping("/alumnos")
    public String loadIndex(Model model, @RequestParam(required = false) String curso) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            UsuarioDTO nuevoAlumno = new UsuarioDTO();
            nuevoAlumno.setTarjetaAlumno(new TarjetaAlumnoDTO());
            model.addAttribute("nuevoAlumno", nuevoAlumno);

            if (curso != null && !curso.isEmpty()) {
                model.addAttribute("alumnos", usuarioService.getAlumnosFromCurso(curso));
            } else {
                model.addAttribute("alumnos", usuarioService.getAllAlumnos());
            }
            model.addAttribute("cursoSeleccionado", curso);

            return "alumnos";
        } else {
            return "redirect:/login";
        }
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

        } else {
            return "redirect:/login";
        }
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
            return "redirect:/alumnos";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/alumnos/editar/{id}")
    public String editarAlumno(@PathVariable("id") Long idAlumno,
                               @ModelAttribute UsuarioDTO alumnoEditado) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {

                UsuarioDTO alumnoOriginal = usuarioService.getUserById(idAlumno);

                int electroniosBefore = alumnoOriginal.getTarjetaAlumno().getElectronios();
                int electroniosAfter = alumnoEditado.getTarjetaAlumno().getElectronios();

                if (electroniosBefore > electroniosAfter) {
                    hts.addLessElectroniosToHistorial(idAlumno, electroniosBefore - electroniosAfter);
                } else if (electroniosBefore < electroniosAfter) {
                    hts.addMoreElectroniosToHistorial(idAlumno, electroniosAfter - electroniosBefore);
                }

                // Actualiza los campos

                if (alumnoEditado.getEmail() != null && !alumnoEditado.getEmail().isEmpty()) {
                    alumnoOriginal.setEmail(alumnoEditado.getEmail());
                    if (usuarioService.getUserByEmail(alumnoEditado.getEmail()) != null &&
                            !usuarioService.getUserByEmail(alumnoEditado.getEmail()).getId().equals(idAlumno)) {
                        alumnoOriginal.setEmail(alumnoOriginal.getEmail());
                    }
                }

                alumnoOriginal.setNombre(alumnoEditado.getNombre());
                alumnoOriginal.setPassword(alumnoEditado.getPassword());
                alumnoOriginal.setCurso(alumnoEditado.getCurso());


                alumnoOriginal.getTarjetaAlumno().setExp(alumnoEditado.getTarjetaAlumno().getExp());
                alumnoOriginal.getTarjetaAlumno().setElectronios(alumnoEditado.getTarjetaAlumno().getElectronios());

                usuarioService.saveUser(alumnoOriginal);
            }
            return "redirect:/alumnos";
        } else {
            return "redirect:/login";
        }
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
            return "redirect:/alumnos";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/alumnos/darElectronios")
    public String darElectronios(
            @RequestParam String tipoSeleccion,
            @RequestParam int cantidadElectronios,
            @RequestParam(required = false) List<Long> idsAlumnos,
            @RequestParam(required = false) String curso
    ) {

        if (Objects.equals(tipoSeleccion, "curso")) {
            usuarioService.darElectroniosCurso(curso, cantidadElectronios);
            return "redirect:/alumnos?curso=" + curso;

        } else {
            usuarioService.darElectroniosAlumnos(idsAlumnos, cantidadElectronios);
        }

        return "redirect:/alumnos";
    }
}