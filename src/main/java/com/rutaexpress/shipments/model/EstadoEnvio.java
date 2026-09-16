package com.rutaexpress.shipments.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EstadoEnvio {
    CREADO,
    ACEPTADO,
    EN_BODEGA,
    EN_RUTA,
    ENTREGADO,
    CANCELADO;

    private static final Map<EstadoEnvio, Set<EstadoEnvio>> TRANSICIONES_VALIDAS = Map.of(
            CREADO, EnumSet.of(ACEPTADO, CANCELADO),
            ACEPTADO, EnumSet.of(EN_BODEGA, CANCELADO),
            EN_BODEGA, EnumSet.of(EN_RUTA, CANCELADO),
            EN_RUTA, EnumSet.of(ENTREGADO, CANCELADO),
            ENTREGADO, EnumSet.noneOf(EstadoEnvio.class),
            CANCELADO, EnumSet.noneOf(EstadoEnvio.class)
    );

    public boolean puedeTransicionarA(EstadoEnvio siguiente) {
        return TRANSICIONES_VALIDAS.get(this).contains(siguiente);
    }
}
