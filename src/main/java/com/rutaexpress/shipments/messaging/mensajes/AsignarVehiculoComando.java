package com.rutaexpress.shipments.messaging.mensajes;

import java.math.BigDecimal;

/** Comando para catalog: reservar un vehículo con capacidad para este envío. */
public record AsignarVehiculoComando(Long envioId, String trackingCode, BigDecimal pesoKg) {
}
