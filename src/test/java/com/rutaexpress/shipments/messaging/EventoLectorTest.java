package com.rutaexpress.shipments.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoLectorTest {

    private final EventoLector lector = new EventoLector(new ObjectMapper());

    private Message mensaje(String cuerpo) {
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void leeUnObjetoJson() {
        assertEquals("RE-1", lector.leer(mensaje("{\"trackingCode\":\"RE-1\"}")).get("trackingCode").asText());
    }

    @Test
    void rechazaTextoQueNoEsJson() {
        assertThrows(MensajeInvalidoException.class, () -> lector.leer(mensaje("esto no es json")));
    }

    @Test
    void rechazaJsonQueNoEsUnObjeto() {
        assertThrows(MensajeInvalidoException.class, () -> lector.leer(mensaje("[1,2,3]")));
        assertThrows(MensajeInvalidoException.class, () -> lector.leer(mensaje("\"hola\"")));
    }

    @Test
    void rechazaUnCuerpoVacio() {
        assertThrows(MensajeInvalidoException.class, () -> lector.leer(mensaje("")));
    }
}
