package com.rutaexpress.shipments.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ que necesita shipments. Solo declara beans; no tiene
 * lógica de negocio. RabbitAdmin los crea en el broker al conectarse.
 *
 * Exchanges
 *  - rutaexpress.commands (direct): shipments publica comandos con una routing key exacta.
 *  - rutaexpress.events   (topic):  shipments y catalog publican eventos; cada cola se
 *                                   suscribe con un patrón (envio.# o #).
 *  - rutaexpress.dlx      (direct): recibe los mensajes rechazados, vencidos o desbordados.
 *
 * Tres colas funcionales, cada una con su DLQ
 *  1. q.envio.comandos  (direct)  comandos para catalog.
 *  2. q.notificaciones  (topic)   eventos "envio.#" para avisar al usuario.
 *  3. q.auditoria       (topic)   todos los eventos ("#") para dejar registro.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitTopologyConfig {

    private final MensajeriaProperties props;

    public RabbitTopologyConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ------------------------------------------------------------- exchanges

    @Bean
    public DirectExchange comandosExchange() {
        return new DirectExchange(props.exchanges().comandos(), true, false);
    }

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(props.exchanges().eventos(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }

    // --------------------------------------------- cola 1: comandos (direct)

    @Bean
    public Queue comandosQueue() {
        return colaConDlq(props.comandos());
    }

    @Bean
    public Queue comandosDlq() {
        return dlq(props.comandos());
    }

    @Bean
    public Binding comandosBinding() {
        return BindingBuilder.bind(comandosQueue()).to(comandosExchange()).with(props.comandos().routingKey());
    }

    @Bean
    public Binding comandosDlqBinding() {
        return BindingBuilder.bind(comandosDlq()).to(deadLetterExchange()).with(props.comandos().dlqRoutingKey());
    }

    // ---------------------------------------- cola 2: notificaciones (topic)

    @Bean
    public Queue notificacionesQueue() {
        return colaConDlq(props.notificaciones());
    }

    @Bean
    public Queue notificacionesDlq() {
        return dlq(props.notificaciones());
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange()).with(props.notificaciones().routingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(deadLetterExchange()).with(props.notificaciones().dlqRoutingKey());
    }

    // ------------------------------------------------ cola 3: auditoría (topic)

    @Bean
    public Queue auditoriaQueue() {
        return colaConDlq(props.auditoria());
    }

    @Bean
    public Queue auditoriaDlq() {
        return dlq(props.auditoria());
    }

    @Bean
    public Binding auditoriaBinding() {
        return BindingBuilder.bind(auditoriaQueue()).to(eventosExchange()).with(props.auditoria().routingKey());
    }

    @Bean
    public Binding auditoriaDlqBinding() {
        return BindingBuilder.bind(auditoriaDlq()).to(deadLetterExchange()).with(props.auditoria().dlqRoutingKey());
    }

    // --------------------------------------------------------------- helpers

    /** Cola con retención (TTL y tamaño máximo); lo que expira o se desborda va a la DLQ. */
    private Queue colaConDlq(MensajeriaProperties.Cola cola) {
        return QueueBuilder.durable(cola.cola())
                .ttl(cola.ttlMs())
                .maxLength(cola.maxLength())
                .overflow(QueueBuilder.Overflow.dropHead)
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(cola.dlqRoutingKey())
                .build();
    }

    /** DLQ con límites propios para que tampoco crezca sin control. */
    private Queue dlq(MensajeriaProperties.Cola cola) {
        return QueueBuilder.durable(cola.dlq())
                .ttl(props.dlq().ttlMs())
                .maxLength(props.dlq().maxLength())
                .build();
    }
}
