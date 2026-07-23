package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Insignia;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InsigniaRepository extends JpaRepository<Insignia, Long> {
    @Query("SELECT i FROM Insignia i WHERE i.alumno.id = :alumnoId")
    List<Insignia> findByAlumnoId(@Param("alumnoId") Long alumnoId);
    
    List<Insignia> findByAlumno(Usuario alumno);
}