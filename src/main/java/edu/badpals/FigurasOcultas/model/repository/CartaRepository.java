package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Carta;
import org.springframework.data.repository.CrudRepository;

public interface CartaRepository extends CrudRepository<Carta, Long> {
}