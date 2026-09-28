package edu.badpals.FigurasOcultas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Un curso pertenece a un profesor (propietario) y puede estar compartido
 * con otros profesores (cursos_compartidos) o con todo el claustro.
 */
@Entity
@Table(name = "cursos", indexes = {
        @Index(name = "idx_curso_codigo", columnList = "codigo"),
        @Index(name = "idx_curso_propietario", columnList = "propietario_id")
})
@Getter
@Setter
public class Curso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, length = 20)
    private EtapaCurso etapa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    @Column(name = "compartido_con_todos", nullable = false)
    private boolean compartidoConTodos;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CursoCompartido> compartidos = new ArrayList<>();

    /** Número de alumnos matriculados en el curso. */
    @Transient
    private int alumnos;

    public boolean esDueno(Long usuarioId) {
        return propietario != null && propietario.getId() != null && propietario.getId().equals(usuarioId);
    }

    @Override
    public String toString() {
        return codigo;
    }
}
