package com.rutaexpress.shipments.messaging.kafka.dto;

import java.time.Instant;

/**
 * Evento de analítica que shipments publica en rutaexpress.eventos.envios.
 * No reemplaza EnvioEvento (el de RabbitMQ, que alimenta notificaciones y
 * auditoría): este es solo para streaming/analítica, lo consume catalog.
 */
public record EnvioEventoKafka(Long envioId, String trackingCode, String estado, Instant ocurridoEn) {
}
