package edu.badpals.FigurasOcultas.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

@Getter
@Setter
public class CartaDTO {

    private Long id;

    private byte[] imagen;

    private Integer precio;

    private String titulo;

    private String descripcion;

    private Boolean activa = false;

    @Override
    public String toString() {
        return "CartaDTO{" +
                "activa=" + activa +
                ", descripcion='" + descripcion + '\'' +
                ", titulo='" + titulo + '\'' +
                ", precio=" + precio +
                ", imagen=" + Arrays.toString(imagen) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartaDTO cartaDTO = (CartaDTO) o;
        return Objects.equals(id, cartaDTO.id) && Objects.deepEquals(imagen, cartaDTO.imagen) && Objects.equals(precio, cartaDTO.precio) && Objects.equals(titulo, cartaDTO.titulo) && Objects.equals(descripcion, cartaDTO.descripcion) && Objects.equals(activa, cartaDTO.activa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, Arrays.hashCode(imagen), precio, titulo, descripcion, activa);
    }
}
