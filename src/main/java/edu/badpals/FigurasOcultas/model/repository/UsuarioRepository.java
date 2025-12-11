package edu.badpals.FigurasOcultas.model.repository;

import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.CursoAlumno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRol(RolUsuario rol);
    Page<Usuario> findByRol(RolUsuario rol, Pageable pageable);
    List<Usuario> findByRolAndCurso(RolUsuario rol, CursoAlumno curso);
    Page<Usuario> findByRolAndCurso(RolUsuario rol, CursoAlumno curso, Pageable pageable);
}