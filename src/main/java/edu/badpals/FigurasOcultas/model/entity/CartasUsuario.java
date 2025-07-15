package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cartas_usuario")
public class CartasUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "carta_id", nullable = false)
    private Carta carta;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_adquisicion")
    private Instant fechaAdquisicion;

    @ColumnDefault("0")
    @Column(name = "usada")
    private Boolean usada;

    @Column(name = "fecha_usada")
    private Instant fechaUsada;

}