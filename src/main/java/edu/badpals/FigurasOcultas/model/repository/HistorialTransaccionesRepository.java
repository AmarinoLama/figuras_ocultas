package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.CartasUsuario;
import edu.badpals.FigurasOcultas.model.entity.HistorialTransacciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialTransaccionesRepository extends JpaRepository<HistorialTransacciones, Long> {
    @Query("SELECT h FROM HistorialTransacciones h WHERE h.alumno.id = :alumnoId ORDER BY h.fecha DESC")
    List<HistorialTransacciones> findByAlumnoId(@Param("alumnoId") Long alumnoId);
}