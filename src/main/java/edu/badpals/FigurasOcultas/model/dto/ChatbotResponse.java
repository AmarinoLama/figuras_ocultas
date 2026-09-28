package edu.badpals.FigurasOcultas.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotResponse {

    /** Texto que se muestra en el chat. */
    private String respuesta;

    /** Acción propuesta (solo si hace falta confirmación del usuario). */
    private String accion;

    /** Parámetros de la acción propuesta. */
    private JsonNode params;

    /** true si el usuario debe confirmar antes de ejecutar la acción. */
    private boolean confirmacionRequerida;

    /** true si la petición acaba de ejecutar una acción sobre la base de datos. */
    private boolean accionEjecutada;

    public ChatbotResponse() {
    }

    public ChatbotResponse(String respuesta) {
        this.respuesta = respuesta;
    }
}
