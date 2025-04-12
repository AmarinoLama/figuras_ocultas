package edu.badpals.FigurasOcultas.model.dto;

import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private String curso;
    private RolUsuario rol;

    public boolean isAdmin() {
        return this.rol == RolUsuario.ADMIN;
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}