package com.rutaexpress.shipments.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Convierte el cuerpo de un mensaje en un objeto JSON; si no lo es, el mensaje es inválido. */
@Component
public class EventoLector {

    private final ObjectMapper objectMapper;

    public EventoLector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode leer(Message message) {
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(message.getBody());
        } catch (IOException ex) {
            throw new MensajeInvalidoException("El cuerpo del mensaje no es JSON válido");
        }
        if (nodo == null || !nodo.isObject()) {
            throw new MensajeInvalidoException("El cuerpo del mensaje debe ser un objeto JSON");
        }
        return nodo;
    }
}
