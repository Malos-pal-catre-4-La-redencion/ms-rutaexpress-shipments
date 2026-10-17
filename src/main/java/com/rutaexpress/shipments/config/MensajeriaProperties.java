package com.rutaexpress.shipments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres y políticas de la mensajería, leídos de application.yml
 * (rutaexpress.mensajeria.*). Es la única fuente de estos valores.
 */
@ConfigurationProperties(prefix = "rutaexpress.mensajeria")
public record MensajeriaProperties(
        Exchanges exchanges,
        Cola comandos,
        Cola notificaciones,
        Cola auditoria,
        Eventos eventos,
        Reintentos reintentos,
        Dlq dlq
) {

    public record Exchanges(String comandos, String eventos, String dlx) {
    }

    /** Una cola funcional con su binding y su DLQ. */
    public record Cola(String cola, String routingKey, String dlq, String dlqRoutingKey,
                       int ttlMs, int maxLength) {
    }

    public record Eventos(String envioCreado, String envioEstadoCambiado) {
    }

    public record Reintentos(int maxIntentos, long esperaMs) {
    }

    public record Dlq(int ttlMs, int maxLength) {
    }
}
