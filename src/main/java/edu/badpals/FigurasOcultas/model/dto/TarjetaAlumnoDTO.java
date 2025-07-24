package edu.badpals.FigurasOcultas.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarjetaAlumnoDTO {

    private Long id;
    private Byte nivel;
    private Integer exp = 0;
    private Byte electronios = 0;
    private Long usuarioId; // Aquí guardamos la id del usuario asociado

    @Override
    public String toString() {
        return "TarjetaAlumnoDTO{" +
                "id=" + id +
                ", nivel=" + nivel +
                ", exp=" + exp +
                ", electronios=" + electronios +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
