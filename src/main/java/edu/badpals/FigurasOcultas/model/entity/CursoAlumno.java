package edu.badpals.FigurasOcultas.model.entity;

public enum CursoAlumno {
    PRIMERO_ESO, SEGUNDO_ESO, TERCERO_ESO, CUARTO_ESO,
    PRIMERO_BACH, SEGUNDO_BACH;

    @Override
    public String toString() {
        return switch (this) {
            case PRIMERO_ESO -> "1º ESO";
            case SEGUNDO_ESO -> "2º ESO";
            case TERCERO_ESO -> "3º ESO";
            case CUARTO_ESO -> "4º ESO";
            case PRIMERO_BACH -> "1º BACH";
            case SEGUNDO_BACH -> "2º BACH";
        };
    }
}