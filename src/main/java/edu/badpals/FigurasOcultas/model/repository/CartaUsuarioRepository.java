package edu.badpals.FigurasOcultas.model.repository;
import edu.badpals.FigurasOcultas.model.entity.CartasUsuario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaUsuarioRepository extends CrudRepository<CartasUsuario, Long> {

    @Query("SELECT cu.carta.id, COUNT(cu) FROM CartasUsuario cu WHERE cu.alumno.id = :usuarioId AND (cu.usada = false OR cu.usada IS NULL) GROUP BY cu.carta.id")
    List<Object[]> countByCartaIdAndUsuarioId(@Param("usuarioId") Long usuarioId);

    List<CartasUsuario> findByAlumnoIdAndUsadaFalse(Long usuarioId);

    List<CartasUsuario> findByCartaIdAndAlumnoId(Long idCarta, Long idUsuario);

    List<CartasUsuario> findByAlumnoId(Long usuarioId);
}