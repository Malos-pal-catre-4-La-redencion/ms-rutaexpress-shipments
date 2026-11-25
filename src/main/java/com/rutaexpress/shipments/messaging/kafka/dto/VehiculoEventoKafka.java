package com.rutaexpress.shipments.messaging.kafka.dto;

import java.time.Instant;

/**
 * Forma del evento que catalog publica en rutaexpress.eventos.vehiculos.
 * shipments solo lo lee (catalog es la fuente de verdad de ese DTO):
 * misma forma JSON, cada servicio con su propia clase.
 */
public record VehiculoEventoKafka(
        Long vehiculoId,
        String patente,
        String tipoEvento,
        boolean disponible,
        Instant ocurridoEn
) {
}
