package com.rutaexpress.shipments.web.dto;

import com.rutaexpress.shipments.model.Envio;
import com.rutaexpress.shipments.model.EstadoEnvio;

import java.math.BigDecimal;
import java.time.Instant;

public record EnvioResponse(
        Long id,
        String trackingCode,
        String remitente,
        String destinatario,
        String direccionDestino,
        BigDecimal pesoKg,
        String creadoPor,
        EstadoEnvio estado,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static EnvioResponse desde(Envio envio) {
        return new EnvioResponse(
                envio.getId(),
                envio.getTrackingCode(),
                envio.getRemitente(),
                envio.getDestinatario(),
                envio.getDireccionDestino(),
                envio.getPesoKg(),
                envio.getCreadoPor(),
                envio.getEstado(),
                envio.getCreadoEn(),
                envio.getActualizadoEn()
        );
    }
}
