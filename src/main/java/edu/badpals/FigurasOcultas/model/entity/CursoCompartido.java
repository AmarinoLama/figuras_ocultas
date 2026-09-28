package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Filas de la tabla intermedia cursos_compartidos: el dueño de un curso
 * lo comparte con otros profesores (usuarios con rol ADMIN).
 */
@Entity
@Table(name = "cursos_compartidos",
        uniqueConstraints = @UniqueConstraint(name = "uk_curso_profesor", columnNames = {"curso_id", "usuario_id"}),
        indexes = {
                @Index(name = "idx_compartido_usuario", columnList = "usuario_id"),
                @Index(name = "idx_compartido_curso", columnList = "curso_id")
        })
@Getter
@Setter
public class CursoCompartido implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
