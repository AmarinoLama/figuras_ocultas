package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    Optional<Curso> findByCodigoIgnoreCase(String codigo);

    List<Curso> findByNombreIgnoreCase(String nombre);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Curso> findByPropietarioId(Long propietarioId);

    /** Cursos que un profesor puede usar: los suyos, los compartidos con todos y los que le han pasado. */
    @Query("SELECT c FROM Curso c WHERE c.propietario.id = :profesorId "
            + "OR c.compartidoConTodos = true "
            + "OR EXISTS (SELECT cc FROM CursoCompartido cc WHERE cc.curso = c AND cc.usuario.id = :profesorId)")
    List<Curso> findVisiblesPara(@Param("profesorId") Long profesorId);
}
