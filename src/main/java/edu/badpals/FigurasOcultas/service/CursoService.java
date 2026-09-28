package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Curso;
import edu.badpals.FigurasOcultas.model.entity.CursoCompartido;
import edu.badpals.FigurasOcultas.model.entity.EtapaCurso;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.CursoCompartidoRepository;
import edu.badpals.FigurasOcultas.model.repository.CursoRepository;
import edu.badpals.FigurasOcultas.model.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * CRUD de cursos: cada profesor tiene sus cursos y puede compartirlos
 * con otros profesores (concretos) o con todo el claustro.
 */
@Service
public class CursoService {

    /** Error de negocio de los cursos (mensaje pensado para el profe). */
    public static class CursoException extends RuntimeException {
        public CursoException(String mensaje) {
            super(mensaje);
        }
    }

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private CursoCompartidoRepository compartidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    // ------------------------------------------------------------ consultas

    /** Cursos que el profesor puede ver y usar (los suyos + los compartidos con él). */
    @Transactional(readOnly = true)
    public List<Curso> visiblesPara(Long profesorId) {
        if (profesorId == null) {
            return List.of();
        }
        List<Curso> cursos = new ArrayList<>(cursoRepository.findVisiblesPara(profesorId));
        rellenarAlumnos(cursos);
        return cursos;
    }

    @Transactional(readOnly = true)
    public Curso buscarPorId(Long id) {
        if (id == null) {
            return null;
        }
        return cursoRepository.findById(id).orElse(null);
    }

    /** Busca un curso por código o por nombre, solo entre los visibles para el profesor. */
    @Transactional(readOnly = true)
    public Curso buscar(String valor, Long profesorId) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = normalizar(valor);
        for (Curso curso : visiblesPara(profesorId)) {
            if (normalizar(curso.getCodigo()).equals(limpio) || normalizar(curso.getNombre()).equals(limpio)) {
                return curso;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public boolean puedeUsar(Curso curso, Long profesorId) {
        if (curso == null || profesorId == null) {
            return false;
        }
        if (curso.esDueno(profesorId) || curso.isCompartidoConTodos()) {
            return true;
        }
        return compartidoRepository.findByCursoIdAndUsuarioId(curso.getId(), profesorId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean esDueno(Curso curso, Long profesorId) {
        return curso != null && curso.esDueno(profesorId);
    }

    /** Profesores con los que se puede compartir (el resto de administradores). */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> profesoresDisponibles() {
        return usuarioRepository.findByRol(RolUsuario.ADMIN).stream()
                .map(u -> modelMapper.map(u, UsuarioDTO.class))
                .toList();
    }

    /** Cuentas docentes con las que se puede compartir un curso, sin repetir ni incluir al dueño. */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> profesoresParaCompartir(Long cursoId) {
        Curso curso = buscarPorId(cursoId);
        return profesoresDisponibles().stream()
                .filter(p -> curso == null || !curso.esDueno(p.getId()))
                .toList();
    }

    // -------------------------------------------------------------- altas

    @Transactional
    public Curso crear(String nombre, String codigo, String etapaTexto, Long propietarioId) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty()) {
            throw new CursoException("El curso necesita un nombre.");
        }
        if (nombreLimpio.length() > 60) {
            nombreLimpio = nombreLimpio.substring(0, 60);
        }
        EtapaCurso etapa = etapa(etapaTexto);
        Usuario propietario = usuarioRepository.findById(propietarioId)
                .orElseThrow(() -> new CursoException("No encuentro al profesor propietario."));

        String codigoLimpio = codigoSugerido(codigo, nombreLimpio);
        Curso curso = new Curso();
        curso.setNombre(nombreLimpio);
        curso.setCodigo(codigoLimpio);
        curso.setEtapa(etapa);
        curso.setPropietario(propietario);
        curso.setCompartidoConTodos(false);
        return cursoRepository.save(curso);
    }

    // ---------------------------------------------------------- modificación

    @Transactional
    public Curso modificar(Long cursoId, String nombre, String codigo, String etapaTexto, Long solicitanteId) {
        Curso curso = exigirCurso(cursoId);
        exigirDueno(curso, solicitanteId);

        if (nombre != null && !nombre.trim().isEmpty()) {
            String nombreLimpio = nombre.trim();
            curso.setNombre(nombreLimpio.length() > 60 ? nombreLimpio.substring(0, 60) : nombreLimpio);
        }
        if (codigo != null && !codigo.trim().isEmpty()) {
            String codigoLimpio = normalizarCodigo(codigo);
            if (cursoRepository.existsByCodigoIgnoreCaseAndIdNot(codigoLimpio, curso.getId())) {
                throw new CursoException("Ya existe otro curso con el código " + codigoLimpio + ".");
            }
            curso.setCodigo(codigoLimpio);
        }
        if (etapaTexto != null && !etapaTexto.trim().isEmpty()) {
            curso.setEtapa(etapa(etapaTexto));
        }
        return cursoRepository.save(curso);
    }

    /** Borra el curso y deja a sus alumnos sin curso. Solo el dueño. */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "alumnos", allEntries = true)
    public void borrar(Long cursoId, Long solicitanteId) {
        Curso curso = exigirCurso(cursoId);
        exigirDueno(curso, solicitanteId);
        usuarioRepository.desmatricularDeCurso(curso.getId());
        compartidoRepository.deleteByCursoId(curso.getId());
        cursoRepository.delete(curso);
    }

    /**
     * Comparte o deja de compartir un curso.
     * destino: email de un profesor o la palabra "todos".
     */
    @Transactional
    public void compartir(Long cursoId, String destino, Long solicitanteId, boolean quitar) {
        Curso curso = exigirCurso(cursoId);
        exigirDueno(curso, solicitanteId);

        if (destino == null || destino.trim().isEmpty()) {
            throw new CursoException("Indica con quién quieres compartir el curso (email del profesor o \"todos\").");
        }
        String valor = destino.trim();

        if ("todos".equalsIgnoreCase(valor)) {
            curso.setCompartidoConTodos(!quitar);
            cursoRepository.save(curso);
            return;
        }

        Usuario profesor = usuarioRepository.findByEmail(valor)
                .or(() -> usuarioRepository.findByNombreIgnoreCase(valor.trim()))
                .orElseThrow(() -> new CursoException("No encuentro al profesor " + valor + "."));
        if (profesor.getRol() != RolUsuario.ADMIN) {
            throw new CursoException(profesor.getNombre() + " no es un profesor, no se le puede compartir el curso.");
        }
        if (curso.esDueno(profesor.getId())) {
            throw new CursoException("Ese curso ya es tuyo.");
        }

        Optional<CursoCompartido> existente = compartidoRepository
                .findByCursoIdAndUsuarioId(curso.getId(), profesor.getId());
        if (quitar) {
            existente.ifPresent(compartidoRepository::delete);
        } else {
            if (existente.isEmpty()) {
                CursoCompartido compartido = new CursoCompartido();
                compartido.setCurso(curso);
                compartido.setUsuario(profesor);
                compartidoRepository.save(compartido);
            }
        }
    }

    /** Profesores con los que está compartido ahora mismo un curso. */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> compartidosCon(Long cursoId) {
        List<UsuarioDTO> resultado = new ArrayList<>();
        for (CursoCompartido compartido : compartidoRepository.findByCursoId(cursoId)) {
            resultado.add(modelMapper.map(compartido.getUsuario(), UsuarioDTO.class));
        }
        return resultado;
    }

    /** true si el curso está compartido con alguien (persona concreta o todo el claustro). */
    public boolean estaCompartido(Curso curso) {
        return curso != null && (curso.isCompartidoConTodos() || !curso.getCompartidos().isEmpty());
    }

    public List<String> compartidosTexto(Curso curso) {
        Set<String> textos = new LinkedHashSet<>();
        if (curso == null) {
            return new ArrayList<>(textos);
        }
        if (curso.isCompartidoConTodos()) {
            textos.add("Todo el claustro");
        }
        for (CursoCompartido compartido : curso.getCompartidos()) {
            textos.add(compartido.getUsuario() != null ? compartido.getUsuario().getEmail() : "?");
        }
        return new ArrayList<>(textos);
    }

    // ------------------------------------------------------------ utilidades

    /** Codigo corto apto para el chatbot: sin tildes, sin espacios y en mayúsculas. */
    public String normalizarCodigo(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /** Si no viene código, se inventa a partir del nombre; si choca, se le añade un número. */
    public String codigoSugerido(String codigo, String nombre) {
        String base = normalizarCodigo(codigo);
        if (base.isEmpty()) {
            base = normalizarCodigo(nombre);
        }
        if (base.isEmpty()) {
            base = "CURSO";
        }
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        String candidato = base;
        int intento = 1;
        while (cursoRepository.existsByCodigoIgnoreCase(candidato)) {
            intento++;
            candidato = base + intento;
        }
        return candidato;
    }

    public EtapaCurso etapa(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return EtapaCurso.ESO;
        }
        String limpio = normalizar(texto);
        for (EtapaCurso etapa : EtapaCurso.values()) {
            if (normalizar(etapa.name()).equals(limpio) || normalizar(etapa.toString()).equals(limpio)) {
                return etapa;
            }
        }
        throw new CursoException("La etapa '" + texto + "' no existe. Usa una de: ESO, BACHILLERATO, FP, OTRO.");
    }

    public String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private Curso exigirCurso(Long cursoId) {
        Curso curso = buscarPorId(cursoId);
        if (curso == null) {
            throw new CursoException("No encuentro ese curso.");
        }
        return curso;
    }

    private void exigirDueno(Curso curso, Long solicitanteId) {
        if (!curso.esDueno(solicitanteId)) {
            throw new CursoException("Solo el profesor que creó el curso puede modificarlo o borrarlo.");
        }
    }

    private void rellenarAlumnos(List<Curso> cursos) {
        if (cursos.isEmpty()) {
            return;
        }
        for (Object[] fila : usuarioRepository.contarAlumnosPorCurso(RolUsuario.ALUMNO)) {
            Long cursoId = (Long) fila[0];
            long total = (Long) fila[1];
            for (Curso curso : cursos) {
                if (curso.getId().equals(cursoId)) {
                    curso.setAlumnos((int) total);
                }
            }
        }
    }
}
