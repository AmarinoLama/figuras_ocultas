package edu.badpals.FigurasOcultas.controller;

import edu.badpals.FigurasOcultas.authentication.ManagerUserSession;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.service.CursoService;
import edu.badpals.FigurasOcultas.service.UsuarioService;
import edu.badpals.FigurasOcultas.service.WebConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CRUD de cursos: cada profesor gestiona los suyos y decide con quién los comparte.
 */
@Controller
@RequestMapping("/cursos")
public class CursosController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CursoService cursoService;

    @Autowired
    private WebConfigService webConfigService;

    @GetMapping
    public String listar(Model model) {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        UsuarioDTO usuario = usuarioLogeadoId == null ? null : usuarioService.getUserById(usuarioLogeadoId);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!usuario.isAdmin()) {
            return "redirect:/cartas";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("nombreWeb", webConfigService.getWebConfig());
        model.addAttribute("cursos", cursoService.visiblesPara(usuarioLogeadoId));
        model.addAttribute("profesores", cursoService.profesoresDisponibles().stream()
                .filter(p -> !usuarioLogeadoId.equals(p.getId()))
                .toList());
        return "cursos";
    }

    @PostMapping("/nuevo")
    public String crear(@RequestParam String nombre,
                        @RequestParam(required = false) String codigo,
                        @RequestParam(required = false) String etapa,
                        RedirectAttributes flash) {
        Long usuarioLogeadoId = docente();
        if (usuarioLogeadoId == null) {
            return "redirect:/login";
        }
        try {
            cursoService.crear(nombre, codigo, etapa, usuarioLogeadoId);
            flash.addFlashAttribute("avisoCurso", "Curso creado correctamente.");
        } catch (CursoService.CursoException e) {
            flash.addFlashAttribute("errorCurso", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("errorCurso", "No se ha podido crear el curso.");
        }
        return "redirect:/cursos";
    }

    @PostMapping("/editar/{id}")
    public String modificar(@PathVariable("id") Long idCurso,
                            @RequestParam String nombre,
                            @RequestParam(required = false) String codigo,
                            @RequestParam(required = false) String etapa,
                            RedirectAttributes flash) {
        Long usuarioLogeadoId = docente();
        if (usuarioLogeadoId == null) {
            return "redirect:/login";
        }
        try {
            cursoService.modificar(idCurso, nombre, codigo, etapa, usuarioLogeadoId);
            flash.addFlashAttribute("avisoCurso", "Curso actualizado.");
        } catch (CursoService.CursoException e) {
            flash.addFlashAttribute("errorCurso", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("errorCurso", "No se ha podido actualizar el curso.");
        }
        return "redirect:/cursos";
    }

    @PostMapping("/borrar/{id}")
    public String borrar(@PathVariable("id") Long idCurso, RedirectAttributes flash) {
        Long usuarioLogeadoId = docente();
        if (usuarioLogeadoId == null) {
            return "redirect:/login";
        }
        try {
            cursoService.borrar(idCurso, usuarioLogeadoId);
            flash.addFlashAttribute("avisoCurso", "Curso borrado. Sus alumnos se han quedado sin curso.");
        } catch (CursoService.CursoException e) {
            flash.addFlashAttribute("errorCurso", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("errorCurso", "No se ha podido borrar el curso.");
        }
        return "redirect:/cursos";
    }

    @PostMapping("/compartir/{id}")
    public String compartir(@PathVariable("id") Long idCurso,
                            @RequestParam String destino,
                            @RequestParam(required = false, defaultValue = "false") boolean quitar,
                            RedirectAttributes flash) {
        Long usuarioLogeadoId = docente();
        if (usuarioLogeadoId == null) {
            return "redirect:/login";
        }
        try {
            cursoService.compartir(idCurso, destino, usuarioLogeadoId, quitar);
            flash.addFlashAttribute("avisoCurso", quitar ? "Se ha dejado de compartir el curso."
                    : "Curso compartido correctamente.");
        } catch (CursoService.CursoException e) {
            flash.addFlashAttribute("errorCurso", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("errorCurso", "No se ha podido compartir el curso.");
        }
        return "redirect:/cursos";
    }

    /** Devuelve el id del docente logueado solo si es administrador. */
    private Long docente() {
        Long usuarioLogeadoId = managerUserSession.usuarioLogeado();
        if (usuarioLogeadoId == null) {
            return null;
        }
        UsuarioDTO usuario = usuarioService.getUserById(usuarioLogeadoId);
        return usuario != null && usuario.isAdmin() ? usuarioLogeadoId : null;
    }
}
