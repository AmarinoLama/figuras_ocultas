package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Webconfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebConfigRepository extends JpaRepository<Webconfig, Long> {
    Optional<Webconfig> findFirstBy();
}