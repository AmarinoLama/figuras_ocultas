package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "tarjeta_alumno")
public class TarjetaAlumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "nivel", nullable = false)
    private Byte nivel;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "exp", nullable = false)
    private Integer exp;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "electronios", nullable = false)
    private Byte electronios;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

}