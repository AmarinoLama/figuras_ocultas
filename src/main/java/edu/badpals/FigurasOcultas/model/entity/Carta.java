package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "cartas")
public class Carta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "imagen", columnDefinition = "MEDIUMBLOB")
    private byte[] imagen;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "precio", nullable = false)
    private Integer precio;

    @Size(max = 50)
    @NotNull
    @Column(name = "titulo", nullable = false, length = 50)
    private String titulo;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "activa", nullable = false)
    private Boolean activa = false;
}