package com.rutaexpress.shipments.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.Channel;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import com.rutaexpress.shipments.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de q.auditoria: guarda en la base de datos cada evento recibido.
 * Si la base falla de forma transitoria se reintenta de forma acotada; si persiste,
 * el mensaje va a la DLQ.
 */
@Component
public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    private final EventoLector lector;
    private final ReintentoAcotado reintentos;
    private final AuditoriaService auditoriaService;

    public AuditoriaListener(EventoLector lector, ReintentoAcotado reintentos, AuditoriaService auditoriaService) {
        this.lector = lector;
        this.reintentos = reintentos;
        this.auditoriaService = auditoriaService;
    }

    @RabbitListener(queues = "${rutaexpress.mensajeria.auditoria.cola}")
    public void onEvento(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            JsonNode evento = lector.leer(message);
            reintentos.ejecutar("auditoría " + routingKey, () -> auditoriaService.registrar(routingKey, evento));
            channel.basicAck(tag, false);
        } catch (MensajeInvalidoException ex) {
            log.error("Evento de auditoría inválido (no recuperable), se envía a la DLQ: {}", ex.getMessage());
            channel.basicNack(tag, false, false);
        } catch (RuntimeException ex) {
            log.error("Se agotaron los reintentos de auditoría de {}, se envía a la DLQ: {}", routingKey, ex.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
