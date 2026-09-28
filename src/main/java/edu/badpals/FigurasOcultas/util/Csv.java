package edu.badpals.FigurasOcultas.util;

import java.util.List;

/**
 * Escapado de CSV al estilo RFC 4180: envuelve entre comillas los campos que
 * llevan comas, comillas o saltos de línea. Se usa en las exportaciones de
 * alumnos (tanto del chatbot como del botón de la web).
 */
public final class Csv {

    private Csv() {
    }

    /** Convierte una lista de filas en texto CSV (con salto de línea final). */
    public static String filas(List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        for (String[] fila : filas) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(campo(fila[i]));
            }
        }
        return sb.append("\n").toString();
    }

    /** Escapa un único campo. */
    public static String campo(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
