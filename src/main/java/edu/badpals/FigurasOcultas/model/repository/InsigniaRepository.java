package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Insignia;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface InsigniaRepository extends CrudRepository<Insignia, Long> {
    List<Insignia> findByAlumno(Usuario map);
}