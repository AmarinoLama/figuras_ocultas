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
@Table(name = "historial_transacciones")
public class HistorialTransacciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Usuario alumno;

    @Enumerated(EnumType.STRING)
    private TipoHistorial tipo;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @NotNull
    @Column(name = "electronios_en_momento", nullable = false)
    private Integer electroniosEnMomento;

    @CreationTimestamp
    @Column(name = "fecha", updatable = false, nullable = false)
    private Instant fecha;

}