package com.rutaexpress.shipments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de tópicos y del consumer group propio de este servicio, leídos de
 * application.yml (rutaexpress.kafka.*). Deben coincidir exactamente con los
 * de catalog: ambos servicios hablan de los mismos dos tópicos.
 */
@ConfigurationProperties(prefix = "rutaexpress.kafka")
public record KafkaTopicsProperties(
        Topics topics,
        String consumerGroup
) {
    public record Topics(
            String eventosVehiculos, // productor: catalog · consumidor: shipments
            String eventosEnvios     // productor: shipments · consumidor: catalog
    ) {
    }
}
