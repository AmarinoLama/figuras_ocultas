package edu.badpals.FigurasOcultas.model.entity;

public enum CursoAlumno {
    PRIMERO_ESO_A, PRIMERO_ESO_B, PRIMERO_ESO_C,
    SEGUNDO_ESO_A, SEGUNDO_ESO_B, SEGUNDO_ESO_C,
    TERCERO_ESO_A, TERCERO_ESO_B, TERCERO_ESO_C,
    CUARTO_ESO_A, CUARTO_ESO_B, CUARTO_ESO_C;

    @Override
    public String toString() {
        return switch (this) {
            case PRIMERO_ESO_A -> "1º ESO A";
            case PRIMERO_ESO_B -> "1º ESO B";
            case PRIMERO_ESO_C -> "1º ESO C";

            case SEGUNDO_ESO_A -> "2º ESO A";
            case SEGUNDO_ESO_B -> "2º ESO B";
            case SEGUNDO_ESO_C -> "2º ESO C";

            case TERCERO_ESO_A -> "3º ESO A";
            case TERCERO_ESO_B -> "3º ESO B";
            case TERCERO_ESO_C -> "3º ESO C";

            case CUARTO_ESO_A -> "4º ESO A";
            case CUARTO_ESO_B -> "4º ESO B";
            case CUARTO_ESO_C -> "4º ESO C";
        };
    }
}