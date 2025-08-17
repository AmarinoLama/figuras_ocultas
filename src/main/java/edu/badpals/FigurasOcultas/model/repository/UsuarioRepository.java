package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {
    Collection<Object> findByEmail(String email);
}