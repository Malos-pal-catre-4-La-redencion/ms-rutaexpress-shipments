package com.rutaexpress.shipments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import com.rutaexpress.shipments.model.AuditoriaEvento;
import com.rutaexpress.shipments.repository.AuditoriaEventoRepository;
import org.springframework.stereotype.Service;

/** Deja en la base de datos un registro de cada evento recibido. No sabe nada de RabbitMQ. */
@Service
public class AuditoriaService {

    private static final int MAX_PAYLOAD = 4000;

    private final AuditoriaEventoRepository repository;

    public AuditoriaService(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    public AuditoriaEvento registrar(String routingKey, JsonNode evento) {
        if (routingKey == null || routingKey.isBlank()) {
            throw new MensajeInvalidoException("El evento no trae routing key");
        }
        if (evento == null || evento.isEmpty()) {
            throw new MensajeInvalidoException("El evento está vacío");
        }
        String payload = evento.toString();
        if (payload.length() > MAX_PAYLOAD) {
            payload = payload.substring(0, MAX_PAYLOAD);
        }
        return repository.save(new AuditoriaEvento(routingKey, payload));
    }
}
