package edu.badpals.FigurasOcultas.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

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
}
