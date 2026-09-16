package com.rutaexpress.shipments.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearEnvioRequest(
        @NotBlank(message = "El remitente es obligatorio") String remitente,
        @NotBlank(message = "El destinatario es obligatorio") String destinatario,
        @NotBlank(message = "La dirección de destino es obligatoria") String direccionDestino
) {
}
