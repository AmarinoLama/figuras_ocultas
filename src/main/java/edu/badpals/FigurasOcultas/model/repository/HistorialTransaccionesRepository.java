package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.CartasUsuario;
import edu.badpals.FigurasOcultas.model.entity.HistorialTransacciones;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HistorialTransaccionesRepository extends CrudRepository<HistorialTransacciones, Long> {
    List<HistorialTransacciones> findByAlumnoId(Long usuarioId);
}