package com.rutaexpress.shipments.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.Channel;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import com.rutaexpress.shipments.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de q.notificaciones. Solo orquesta: lee el evento, llama al servicio
 * de negocio y decide entre ACK y NACK sin requeue (que lo envía a la DLQ).
 */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final EventoLector lector;
    private final ReintentoAcotado reintentos;
    private final NotificacionService notificacionService;

    public NotificacionListener(EventoLector lector, ReintentoAcotado reintentos,
                                NotificacionService notificacionService) {
        this.lector = lector;
        this.reintentos = reintentos;
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = "${rutaexpress.mensajeria.notificaciones.cola}")
    public void onEvento(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            JsonNode evento = lector.leer(message);
            reintentos.ejecutar("notificación " + routingKey, () -> notificacionService.notificar(routingKey, evento));
            channel.basicAck(tag, false);
        } catch (MensajeInvalidoException ex) {
            log.error("Notificación inválida (no recuperable), se envía a la DLQ: {}", ex.getMessage());
            channel.basicNack(tag, false, false);
        } catch (RuntimeException ex) {
            log.error("Se agotaron los reintentos de la notificación {}, se envía a la DLQ: {}", routingKey, ex.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
