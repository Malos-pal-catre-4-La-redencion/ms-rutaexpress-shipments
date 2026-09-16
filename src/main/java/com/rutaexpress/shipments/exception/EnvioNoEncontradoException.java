package com.rutaexpress.shipments.exception;

public class EnvioNoEncontradoException extends RuntimeException {
    public EnvioNoEncontradoException(Long id) {
        super("No existe un envío con id " + id);
    }
}
