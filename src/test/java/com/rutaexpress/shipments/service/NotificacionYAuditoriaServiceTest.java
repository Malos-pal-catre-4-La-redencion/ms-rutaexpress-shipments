package com.rutaexpress.shipments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import com.rutaexpress.shipments.model.AuditoriaEvento;
import com.rutaexpress.shipments.repository.AuditoriaEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificacionYAuditoriaServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AuditoriaEventoRepository repository;

    private JsonNode json(String texto) throws Exception {
        return mapper.readTree(texto);
    }

    @Test
    void notificaSiElEventoTraeTrackingCode() throws Exception {
        assertDoesNotThrow(() -> new NotificacionService()
                .notificar("envio.creado", json("{\"trackingCode\":\"RE-1\"}")));
    }

    @Test
    void noNotificaSiFaltaElTrackingCode() throws Exception {
        assertThrows(MensajeInvalidoException.class, () -> new NotificacionService()
                .notificar("envio.prueba", json("{\"x\":1}")));
        assertThrows(MensajeInvalidoException.class, () -> new NotificacionService()
                .notificar("envio.prueba", json("{\"trackingCode\":\"  \"}")));
    }

    @Test
    void auditoriaGuardaLaRoutingKeyYElContenido() throws Exception {
        new AuditoriaService(repository).registrar("envio.creado", json("{\"trackingCode\":\"RE-1\"}"));

        ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(repository).save(captor.capture());
        assertEquals("envio.creado", captor.getValue().getRoutingKey());
        assertEquals("{\"trackingCode\":\"RE-1\"}", captor.getValue().getPayload());
    }

    @Test
    void auditoriaRechazaEventosVaciosOSinRoutingKey() throws Exception {
        AuditoriaService service = new AuditoriaService(repository);

        assertThrows(MensajeInvalidoException.class, () -> service.registrar("envio.creado", json("{}")));
        assertThrows(MensajeInvalidoException.class, () -> service.registrar(" ", json("{\"a\":1}")));

        verifyNoInteractions(repository);
    }
}
