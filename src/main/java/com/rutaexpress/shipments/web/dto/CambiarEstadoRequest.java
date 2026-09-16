package com.rutaexpress.shipments.web.dto;

import com.rutaexpress.shipments.model.EstadoEnvio;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El nuevo estado es obligatorio") EstadoEnvio nuevoEstado
) {
}
