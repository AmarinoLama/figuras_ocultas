package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.CursoCompartido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoCompartidoRepository extends JpaRepository<CursoCompartido, Long> {

    List<CursoCompartido> findByUsuarioId(Long usuarioId);

    List<CursoCompartido> findByCursoId(Long cursoId);

    Optional<CursoCompartido> findByCursoIdAndUsuarioId(Long cursoId, Long usuarioId);

    void deleteByCursoId(Long cursoId);
}
