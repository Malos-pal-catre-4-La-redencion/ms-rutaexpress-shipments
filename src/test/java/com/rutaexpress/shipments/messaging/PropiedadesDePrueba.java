package com.rutaexpress.shipments.messaging;

import com.rutaexpress.shipments.config.MensajeriaProperties;

/** Mismos valores que application.yml, con reintentos sin espera para que las pruebas sean rápidas. */
public final class PropiedadesDePrueba {

    private PropiedadesDePrueba() {
    }

    public static MensajeriaProperties crear() {
        return new MensajeriaProperties(
                new MensajeriaProperties.Exchanges("rutaexpress.commands", "rutaexpress.events", "rutaexpress.dlx"),
                new MensajeriaProperties.Cola("q.envio.comandos", "envio.asignar-vehiculo",
                        "q.envio.comandos.dlq", "envio.comandos.dlq", 300000, 10000),
                new MensajeriaProperties.Cola("q.notificaciones", "envio.#",
                        "q.notificaciones.dlq", "notificaciones.dlq", 3600000, 10000),
                new MensajeriaProperties.Cola("q.auditoria", "#",
                        "q.auditoria.dlq", "auditoria.dlq", 86400000, 50000),
                new MensajeriaProperties.Eventos("envio.creado", "envio.estado.cambiado"),
                new MensajeriaProperties.Reintentos(3, 0),
                new MensajeriaProperties.Dlq(604800000, 10000));
    }
}
