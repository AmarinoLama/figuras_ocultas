package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.entity.Curso;
import edu.badpals.FigurasOcultas.model.entity.EtapaCurso;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.CursoRepository;
import edu.badpals.FigurasOcultas.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arranque del módulo de cursos:
 * 1) si la tabla cursos está vacía, crea los 12 cursos por defecto a nombre del primer admin;
 * 2) si queda la columna antigua "usuarios.curso" (enum), rellena el nuevo curso_id de cada alumno.
 */
@Component
public class CursoSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CursoSeedService.class);

    /** nombre visible | código corto | valor antiguo de la columna usuarios.curso */
    private record DatoCurso(String nombre, String codigo, String legacy) {
    }

    private static final List<DatoCurso> POR_DEFECTO = List.of(
            new DatoCurso("1º ESO A", "1ESOA", "PRIMERO_ESO_A"),
            new DatoCurso("1º ESO B", "1ESOB", "PRIMERO_ESO_B"),
            new DatoCurso("1º ESO C", "1ESOC", "PRIMERO_ESO_C"),
            new DatoCurso("2º ESO A", "2ESOA", "SEGUNDO_ESO_A"),
            new DatoCurso("2º ESO B", "2ESOB", "SEGUNDO_ESO_B"),
            new DatoCurso("2º ESO C", "2ESOC", "SEGUNDO_ESO_C"),
            new DatoCurso("3º ESO A", "3ESOA", "TERCERO_ESO_A"),
            new DatoCurso("3º ESO B", "3ESOB", "TERCERO_ESO_B"),
            new DatoCurso("3º ESO C", "3ESOC", "TERCERO_ESO_C"),
            new DatoCurso("4º ESO A", "4ESOA", "CUARTO_ESO_A"),
            new DatoCurso("4º ESO B", "4ESOB", "CUARTO_ESO_B"),
            new DatoCurso("4º ESO C", "4ESOC", "CUARTO_ESO_C"));

    @Autowired
    private EntityManager em;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            sembrarCursos();
            migrarCursoLegacy();
        } catch (Exception e) {
            log.warn("No se pudo inicializar el módulo de cursos: {}", e.getMessage());
        }
    }

    private void sembrarCursos() {
        if (cursoRepository.count() > 0) {
            return;
        }
        Usuario duenio = usuarioRepository.findByRol(RolUsuario.ADMIN).stream()
                .min((a, b) -> Long.compare(a.getId(), b.getId()))
                .orElse(null);
        if (duenio == null) {
            log.info("No hay ningún profesor todavía: los cursos por defecto se crearán después.");
            return;
        }
        for (DatoCurso dato : POR_DEFECTO) {
            Curso curso = new Curso();
            curso.setNombre(dato.nombre());
            curso.setCodigo(dato.codigo());
            curso.setEtapa(EtapaCurso.ESO);
            curso.setPropietario(duenio);
            curso.setCompartidoConTodos(false);
            cursoRepository.save(curso);
        }
        log.info("Creados los {} cursos por defecto a nombre de {}", POR_DEFECTO.size(), duenio.getEmail());
    }

    /** Vuelca los cursos antiguos (enum de la columna usuarios.curso) al nuevo curso_id. */
    private void migrarCursoLegacy() {
        if (!existeColumnaLegacy()) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(
                        "SELECT id, curso FROM usuarios WHERE curso IS NOT NULL AND curso_id IS NULL")
                .getResultList();
        if (filas.isEmpty()) {
            return;
        }

        Map<String, String> legacyPorCodigo = new HashMap<>();
        for (DatoCurso dato : POR_DEFECTO) {
            legacyPorCodigo.put(dato.legacy(), dato.codigo());
        }

        int migrados = 0;
        for (Object[] fila : filas) {
            Long usuarioId = ((Number) fila[0]).longValue();
            String legacy = fila[1] == null ? null : fila[1].toString();
            String codigo = legacy == null ? null : legacyPorCodigo.get(legacy);
            if (codigo == null) {
                continue;
            }
            Curso curso = cursoRepository.findByCodigoIgnoreCase(codigo).orElse(null);
            if (curso == null) {
                continue;
            }
            Usuario alumno = em.find(Usuario.class, usuarioId);
            if (alumno != null) {
                alumno.setCurso(curso);
                em.merge(alumno);
                migrados++;
            }
        }
        if (migrados > 0) {
            log.info("Asignados {} alumnos a sus cursos (migración de la columna curso antigua)", migrados);
        }
    }

    private boolean existeColumnaLegacy() {
        Number cuenta = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'curso'")
                .getSingleResult();
        return cuenta.intValue() > 0;
    }
}
