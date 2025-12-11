package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.TarjetaAlumnoDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.HistorialTransacciones;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    @Autowired
    private InsigniaService insigniasService;

    @Autowired
    private WebConfigService webConfigService;

    @Autowired
    private CartaUsuarioService cartaUsuarioService;

    @GetMapping("/alumnos")
    public String loadIndex(Model model,
                            @RequestParam(required = false) String curso,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            UsuarioDTO nuevoAlumno = new UsuarioDTO();
            nuevoAlumno.setTarjetaAlumno(new TarjetaAlumnoDTO());
            model.addAttribute("nuevoAlumno", nuevoAlumno);
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            if (curso != null && !curso.isEmpty()) {
                var alumnosPage = usuarioService.getAlumnosFromCurso(curso, org.springframework.data.domain.PageRequest.of(page, size));
                model.addAttribute("alumnos", alumnosPage.getContent());
                model.addAttribute("alumnosPageNumber", alumnosPage.getNumber());
                model.addAttribute("alumnosTotalPages", alumnosPage.getTotalPages());
                model.addAttribute("alumnosTotalElements", alumnosPage.getTotalElements());
            } else {
                var alumnosPage = usuarioService.getAllAlumnos(org.springframework.data.domain.PageRequest.of(page, size));
                model.addAttribute("alumnos", alumnosPage.getContent());
                model.addAttribute("alumnosPageNumber", alumnosPage.getNumber());
                model.addAttribute("alumnosTotalPages", alumnosPage.getTotalPages());
                model.addAttribute("alumnosTotalElements", alumnosPage.getTotalElements());
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

            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

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
                               @ModelAttribute UsuarioDTO alumnoEditado, Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {

                UsuarioDTO alumnoOriginal = usuarioService.getUserById(idAlumno);

                int electroniosBefore = alumnoOriginal.getTarjetaAlumno().getElectronios();
                int electroniosAfter = alumnoEditado.getTarjetaAlumno().getElectronios();

                if (electroniosBefore > electroniosAfter) {
                    hts.addLessExpToHistorial(idAlumno, electroniosBefore - electroniosAfter);
                } else if (electroniosBefore < electroniosAfter) {
                    hts.addMoreExpToHistorial(idAlumno, electroniosAfter - electroniosBefore);
                }

                    if (alumnoEditado.getEmail() != null && !alumnoEditado.getEmail().isEmpty()) {
                        if (usuarioService.checkValidEmail(alumnoEditado.getEmail())) {
                            alumnoOriginal.setEmail(alumnoEditado.getEmail());
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
            @RequestParam(required = false) Set<Long> idsAlumnos,
            @RequestParam(required = false) String curso
    ) {

        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;
        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            if (usuario != null) {

                if (Objects.equals(tipoSeleccion, "curso")) {
                    usuarioService.darExpCurso(curso, cantidadElectronios);
                    return "redirect:/alumnos?curso=" + curso;

                } else {
                    usuarioService.darExpAlumnos(idsAlumnos.stream().toList(), cantidadElectronios);
                }
            }
        } else {
            return "redirect:/login";
        }

        return "redirect:/alumnos";
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {

        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            if (usuario.getRol() == RolUsuario.ALUMNO) {
                model.addAttribute("insigniasAlumno", insigniasService.getInsigniasAlumno(usuarioLogeadoId));
            }

            return "perfil";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute("usuario") UsuarioDTO usuarioForm, Model model) {

        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        boolean usuarioLogeado = usuarioLogeadoId != null;

        if (usuarioLogeado) {

            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);

            // Validación manual del nombre
            if (usuarioForm.getNombre() == null || usuarioForm.getNombre().trim().isEmpty()) {
                model.addAttribute("usuario", usuario);
                model.addAttribute("errorMessage", "El nombre no puede estar vacío");
                model.addAttribute("nombreWeb", webConfigService.getWebConfig());
                return "perfil";
            }

            usuario.setNombre(usuarioForm.getNombre());

            // Comprobamos si el email ha cambiado
            if (usuarioForm.getEmail() != null && !usuario.getEmail().equals(usuarioForm.getEmail())) {
                if (usuarioService.checkValidEmail(usuarioForm.getEmail())) {
                    usuario.setEmail(usuarioForm.getEmail());
                } else {
                    model.addAttribute("usuario", usuario);
                    model.addAttribute("errorMessage", "Email no disponible");
                    model.addAttribute("nombreWeb", webConfigService.getWebConfig());
                    return "perfil";
                }
            }

            // Solo actualizar contraseña si se proporcionó una nueva
            if (usuarioForm.getPassword() != null && !usuarioForm.getPassword().trim().isEmpty()) {
                usuario.setPassword(usuarioForm.getPassword());
            }

            usuarioService.saveUser(usuario);

            model.addAttribute("usuario", usuario);
            model.addAttribute("successMessage", "Perfil actualizado correctamente");
            model.addAttribute("nombreWeb", webConfigService.getWebConfig());
            return "perfil";

        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("/alumnos/historial/{id}")
    public String historialAlumno(@PathVariable(value = "id") Long idAlumno, Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId != null) {
            UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
            UsuarioDTO alumno = usuarioService.getUserById(idAlumno);

            model.addAttribute("usuario", usuario);
            model.addAttribute("nombre", alumno.getNombre());

            List<HistorialTransacciones> historial = cartaUsuarioService.getHistorial(idAlumno);
            Collections.reverse(historial);
            model.addAttribute("historial", historial);

            model.addAttribute("nombreWeb", webConfigService.getWebConfig());

            return "historial";
        }
        return "redirect:/login";
    }
}