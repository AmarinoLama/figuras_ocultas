package edu.badpals.FigurasOcultas.model.entity;

public enum EtapaCurso {
    ESO, BACHILLERATO, FP, OTRO;

    @Override
    public String toString() {
        return switch (this) {
            case ESO -> "Educación Secundaria";
            case BACHILLERATO -> "Bachillerato";
            case FP -> "Formación Profesional";
            case OTRO -> "Otro";
        };
    }
}
