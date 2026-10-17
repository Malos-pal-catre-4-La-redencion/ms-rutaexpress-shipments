package com.rutaexpress.shipments.messaging.mensajes;

import java.time.Instant;

/** Evento que shipments publica cuando un envío se crea o cambia de estado. */
public record EnvioEvento(Long envioId, String trackingCode, String estado, Instant ocurridoEn) {
}
