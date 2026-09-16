package com.rutaexpress.shipments.exception;

import com.rutaexpress.shipments.model.EstadoEnvio;

public class TransicionInvalidaException extends RuntimeException {
    public TransicionInvalidaException(EstadoEnvio actual, EstadoEnvio solicitado) {
        super("No se puede pasar de " + actual + " a " + solicitado);
    }
}
