package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private CursoAlumno curso;

    @Enumerated(EnumType.STRING)
    private RolUsuario rol;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TarjetaAlumno tarjetaAlumno = new TarjetaAlumno();

    // Crear tarjetaAlumno automáticamente antes de persistir el usuario
    @PrePersist
    public void prePersist() {
        if (this.tarjetaAlumno == null) {
            this.tarjetaAlumno = new TarjetaAlumno();
            this.tarjetaAlumno.setUsuario(this); // Establecer la relación inversa
        }
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", rol=" + rol +
                '}';
    }
}
