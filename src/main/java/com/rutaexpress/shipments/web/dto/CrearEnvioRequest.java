package com.rutaexpress.shipments.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CrearEnvioRequest(
        @NotBlank(message = "El remitente es obligatorio") String remitente,
        @NotBlank(message = "El destinatario es obligatorio") String destinatario,
        @NotBlank(message = "La dirección de destino es obligatoria") String direccionDestino,
        @NotNull(message = "El peso es obligatorio")
        @Positive(message = "El peso debe ser mayor a 0") BigDecimal pesoKg
) {
}
