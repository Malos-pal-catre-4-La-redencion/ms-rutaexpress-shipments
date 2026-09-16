package com.rutaexpress.shipments.web.dto;

import com.rutaexpress.shipments.model.Envio;
import com.rutaexpress.shipments.model.EstadoEnvio;

import java.time.Instant;

public record EnvioResponse(
        Long id,
        String trackingCode,
        String remitente,
        String destinatario,
        String direccionDestino,
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
                envio.getEstado(),
                envio.getCreadoEn(),
                envio.getActualizadoEn()
        );
    }
}
