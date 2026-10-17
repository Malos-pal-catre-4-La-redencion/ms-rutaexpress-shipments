package com.rutaexpress.shipments.messaging;

import com.rutaexpress.shipments.config.MensajeriaProperties;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ejecuta una acción con reintentos acotados. Solo se reintentan los errores
 * recuperables; un {@link MensajeInvalidoException} se propaga de inmediato
 * porque repetirlo no lo arregla. Si se agotan los intentos, relanza el último error.
 */
@Component
public class ReintentoAcotado {

    private static final Logger log = LoggerFactory.getLogger(ReintentoAcotado.class);

    private final MensajeriaProperties props;

    public ReintentoAcotado(MensajeriaProperties props) {
        this.props = props;
    }

    public void ejecutar(String descripcion, Runnable accion) {
        int maxIntentos = Math.max(1, props.reintentos().maxIntentos());
        RuntimeException ultimoError = null;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                accion.run();
                return;
            } catch (MensajeInvalidoException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                ultimoError = ex;
                log.warn("Intento {}/{} fallido ({}): {}", intento, maxIntentos, descripcion, ex.getMessage());
                if (intento < maxIntentos && !esperar(props.reintentos().esperaMs() * intento)) {
                    break;
                }
            }
        }
        throw ultimoError;
    }

    /** @return false si el hilo fue interrumpido y no conviene seguir reintentando. */
    private boolean esperar(long milisegundos) {
        if (milisegundos <= 0) {
            return true;
        }
        try {
            Thread.sleep(milisegundos);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
