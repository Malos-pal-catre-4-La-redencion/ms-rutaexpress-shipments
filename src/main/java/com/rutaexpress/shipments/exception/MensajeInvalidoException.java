package com.rutaexpress.shipments.exception;

/** Error NO recuperable: el mensaje recibido está mal formado. Reintentar no lo arregla. */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
