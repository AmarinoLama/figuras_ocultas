package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    private TarjetaAlumno tarjetaAlumno;

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Insignia> insignias = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.rol == RolUsuario.ALUMNO) {
            if (this.tarjetaAlumno == null) {
                this.tarjetaAlumno = new TarjetaAlumno();
            }
            this.tarjetaAlumno.setUsuario(this);
            this.tarjetaAlumno.setNivel((byte) 0);
            this.tarjetaAlumno.setExp(0);
            this.tarjetaAlumno.setElectronios((byte) 0);
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
