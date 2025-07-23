package edu.badpals.FigurasOcultas.model.dto;

import edu.badpals.FigurasOcultas.model.entity.CursoAlumno;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {

    private Long id;
    private String nombre;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "Debe ser un email válido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    private CursoAlumno curso;
    private RolUsuario rol;

    // Aquí agregamos la propiedad tarjetaAlumno
    private TarjetaAlumnoDTO tarjetaAlumno = new TarjetaAlumnoDTO();

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
                ", tarjetaAlumno=" + tarjetaAlumno + // Asegúrate de incluirla también en el toString
                '}';
    }
}
