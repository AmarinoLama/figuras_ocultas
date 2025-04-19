package edu.badpals.FigurasOcultas.model.dto;

import edu.badpals.FigurasOcultas.model.entity.CursoAlumno;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.TarjetaAlumno;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private CursoAlumno curso;
    private RolUsuario rol;
    private TarjetaAlumno tarjetaAlumno;

    public boolean isAdmin() {
        return this.rol == RolUsuario.ADMIN;
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", curso=" + curso +
                ", rol=" + rol +
                '}';
    }
}