package com.rutaexpress.shipments.config;

import com.rutaexpress.shipments.messaging.PropiedadesDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitTopologyConfigTest {

    private RabbitTopologyConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitTopologyConfig(PropiedadesDePrueba.crear());
    }

    @Test
    void hayUnExchangeDirectUnoTopicYElDeDeadLetter() {
        assertEquals("direct", config.comandosExchange().getType());
        assertEquals("topic", config.eventosExchange().getType());
        assertEquals("direct", config.deadLetterExchange().getType());
    }

    @Test
    void lasTresColasFuncionalesTienenRetencionYApuntanASuDlq() {
        verificarCola(config.comandosQueue(), "q.envio.comandos", 300000, 10000, "envio.comandos.dlq");
        verificarCola(config.notificacionesQueue(), "q.notificaciones", 3600000, 10000, "notificaciones.dlq");
        verificarCola(config.auditoriaQueue(), "q.auditoria", 86400000, 50000, "auditoria.dlq");
    }

    private void verificarCola(Queue cola, String nombre, int ttl, int maxLength, String dlqRoutingKey) {
        assertEquals(nombre, cola.getName());
        assertTrue(cola.isDurable());
        assertEquals(ttl, cola.getArguments().get("x-message-ttl"));
        assertEquals(maxLength, cola.getArguments().get("x-max-length"));
        assertEquals("drop-head", cola.getArguments().get("x-overflow"));
        assertEquals("rutaexpress.dlx", cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals(dlqRoutingKey, cola.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void cadaDlqTieneSusPropiosLimitesYNoTieneOtraDlq() {
        for (Queue dlq : new Queue[]{config.comandosDlq(), config.notificacionesDlq(), config.auditoriaDlq()}) {
            assertEquals(604800000, dlq.getArguments().get("x-message-ttl"));
            assertEquals(10000, dlq.getArguments().get("x-max-length"));
            assertNull(dlq.getArguments().get("x-dead-letter-exchange"));
        }
        assertEquals("q.notificaciones.dlq", config.notificacionesDlq().getName());
        assertEquals("q.auditoria.dlq", config.auditoriaDlq().getName());
    }

    @Test
    void elComandoUsaRoutingKeyExactaEnElExchangeDirect() {
        Binding binding = config.comandosBinding();

        assertEquals("rutaexpress.commands", binding.getExchange());
        assertEquals("q.envio.comandos", binding.getDestination());
        assertEquals("envio.asignar-vehiculo", binding.getRoutingKey());
    }

    @Test
    void notificacionesYAuditoriaSeSuscribenConPatronesEnElExchangeTopic() {
        Binding notificaciones = config.notificacionesBinding();
        assertEquals("rutaexpress.events", notificaciones.getExchange());
        assertEquals("q.notificaciones", notificaciones.getDestination());
        assertEquals("envio.#", notificaciones.getRoutingKey());

        Binding auditoria = config.auditoriaBinding();
        assertEquals("rutaexpress.events", auditoria.getExchange());
        assertEquals("q.auditoria", auditoria.getDestination());
        assertEquals("#", auditoria.getRoutingKey());
    }

    @Test
    void cadaDlqEstaEnlazadaAlExchangeDeDeadLetter() {
        assertEquals("envio.comandos.dlq", config.comandosDlqBinding().getRoutingKey());
        assertEquals("notificaciones.dlq", config.notificacionesDlqBinding().getRoutingKey());
        assertEquals("auditoria.dlq", config.auditoriaDlqBinding().getRoutingKey());
        assertEquals("rutaexpress.dlx", config.auditoriaDlqBinding().getExchange());
        assertEquals("q.auditoria.dlq", config.auditoriaDlqBinding().getDestination());
    }
}
