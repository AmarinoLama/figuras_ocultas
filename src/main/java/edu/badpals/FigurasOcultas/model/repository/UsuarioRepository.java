package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByNombreIgnoreCase(String nombre);
    List<Usuario> findByRol(RolUsuario rol);
    Page<Usuario> findByRol(RolUsuario rol, Pageable pageable);
    List<Usuario> findByRolAndCursoId(RolUsuario rol, Long cursoId);
    Page<Usuario> findByRolAndCursoId(RolUsuario rol, Long cursoId, Pageable pageable);

    /** Alumnos que hay en cada curso (para pintar los contadores de la lista de cursos). */
    @Query("SELECT u.curso.id, COUNT(u) FROM Usuario u WHERE u.rol = :rol AND u.curso IS NOT NULL GROUP BY u.curso.id")
    List<Object[]> contarAlumnosPorCurso(@Param("rol") RolUsuario rol);

    /** Deja sin curso a los alumnos de un curso que se va a borrar. */
    @Modifying
    @Query("UPDATE Usuario u SET u.curso = null WHERE u.curso.id = :cursoId")
    void desmatricularDeCurso(@Param("cursoId") Long cursoId);
}
