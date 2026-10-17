package com.rutaexpress.shipments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Avisa al usuario de lo ocurrido con su envío. Por ahora la "notificación"
 * es una línea de log; aquí se conectaría un correo o un SMS.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public void notificar(String routingKey, JsonNode evento) {
        JsonNode tracking = evento == null ? null : evento.get("trackingCode");
        if (tracking == null || tracking.asText().isBlank()) {
            throw new MensajeInvalidoException("El evento '" + routingKey + "' no trae trackingCode, no se sabe a quién notificar");
        }
        log.info("[NOTIFICACIÓN] Envío {}: {} -> {}", tracking.asText(), routingKey, evento);
    }
}
