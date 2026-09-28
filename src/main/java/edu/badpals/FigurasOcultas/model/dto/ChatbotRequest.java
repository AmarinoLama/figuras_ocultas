package edu.badpals.FigurasOcultas.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotRequest {

    /** Texto escrito por el usuario. */
    private String mensaje;

    /** true cuando el usuario acepta una acción destructiva propuesta antes. */
    private boolean confirmado;

    /** Acción propuesta que el usuario acaba de confirmar. */
    private String accion;

    /** Parámetros de dicha acción. */
    private JsonNode params;
}
