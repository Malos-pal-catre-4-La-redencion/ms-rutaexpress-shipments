package com.rutaexpress.shipments.messaging;

import com.rutaexpress.shipments.config.MensajeriaProperties;
import com.rutaexpress.shipments.messaging.mensajes.AsignarVehiculoComando;
import com.rutaexpress.shipments.messaging.mensajes.EnvioEvento;
import com.rutaexpress.shipments.model.Envio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Productor de shipments.
 *  - Comandos al exchange direct (routing key exacta).
 *  - Eventos al exchange topic (cada cola se suscribe con un patrón).
 *
 * Si RabbitMQ no responde, el error se registra y no interrumpe la petición HTTP:
 * el envío ya está guardado. (La solución completa sería un patrón outbox.)
 */
@Component
public class EnvioMensajesPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnvioMensajesPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public EnvioMensajesPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarComandoAsignarVehiculo(Envio envio) {
        AsignarVehiculoComando comando =
                new AsignarVehiculoComando(envio.getId(), envio.getTrackingCode(), envio.getPesoKg());
        publicar(props.exchanges().comandos(), props.comandos().routingKey(), comando);
    }

    public void publicarEnvioCreado(Envio envio) {
        publicar(props.exchanges().eventos(), props.eventos().envioCreado(), aEvento(envio));
    }

    public void publicarEstadoCambiado(Envio envio) {
        publicar(props.exchanges().eventos(), props.eventos().envioEstadoCambiado(), aEvento(envio));
    }

    private EnvioEvento aEvento(Envio envio) {
        return new EnvioEvento(envio.getId(), envio.getTrackingCode(), envio.getEstado().name(), Instant.now());
    }

    private void publicar(String exchange, String routingKey, Object mensaje) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, mensaje);
            log.info("Mensaje publicado: {} -> {}", exchange, routingKey);
        } catch (AmqpException ex) {
            log.error("No se pudo publicar en {} con routing key {}: {}", exchange, routingKey, ex.getMessage());
        }
    }
}
