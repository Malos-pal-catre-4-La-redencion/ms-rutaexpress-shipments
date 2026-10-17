package com.rutaexpress.shipments.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Consume las DLQ de notificaciones y auditoría y registra en el log cada mensaje
 * no entregado: cola de origen, motivo (rejected, expired o maxlen) y contenido.
 * (La DLQ de comandos la consume catalog, que es quien procesa esos comandos.)
 */
@Component
public class DeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    @RabbitListener(queues = "${rutaexpress.mensajeria.notificaciones.dlq}")
    public void onNotificacionMuerta(Message message, Channel channel) throws IOException {
        registrar(message, channel);
    }

    @RabbitListener(queues = "${rutaexpress.mensajeria.auditoria.dlq}")
    public void onAuditoriaMuerta(Message message, Channel channel) throws IOException {
        registrar(message, channel);
    }

    private void registrar(Message message, Channel channel) throws IOException {
        MessageProperties propiedades = message.getMessageProperties();

        log.error("[DLQ] Mensaje no entregado. colaOrigen={} motivo={} contenido={}",
                propiedades.getHeaders().get("x-first-death-queue"),
                propiedades.getHeaders().get("x-first-death-reason"),
                new String(message.getBody(), StandardCharsets.UTF_8));

        channel.basicAck(propiedades.getDeliveryTag(), false);
    }
}
