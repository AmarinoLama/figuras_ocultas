package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cartas_usuario", indexes = {
    @Index(name = "idx_cartas_usuario_alumno", columnList = "alumno_id"),
    @Index(name = "idx_cartas_usuario_carta", columnList = "carta_id"),
    @Index(name = "idx_cartas_usuario_usada", columnList = "usada")
})
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

    @CreationTimestamp
    @Column(name = "fecha_adquisicion", updatable = false, nullable = false)
    private Instant fechaAdquisicion;

    @ColumnDefault("0")
    @Column(name = "usada")
    private Boolean usada;

    @Column(name = "fecha_usada")
    private Instant fechaUsada;

}