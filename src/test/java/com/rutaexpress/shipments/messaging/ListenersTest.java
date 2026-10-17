package com.rutaexpress.shipments.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rutaexpress.shipments.exception.MensajeInvalidoException;
import com.rutaexpress.shipments.service.AuditoriaService;
import com.rutaexpress.shipments.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pruebas de los consumidores de notificaciones y auditoría (sin RabbitMQ: Channel y servicios son mocks). */
@ExtendWith(MockitoExtension.class)
class ListenersTest {

    private static final long TAG = 5L;
    private static final String EVENTO =
            "{\"envioId\":1,\"trackingCode\":\"RE-AAAA1111\",\"estado\":\"CREADO\"}";

    @Mock
    private Channel channel;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoriaService;

    private NotificacionListener notificacionListener;
    private AuditoriaListener auditoriaListener;

    @BeforeEach
    void setUp() {
        EventoLector lector = new EventoLector(new ObjectMapper());
        ReintentoAcotado reintentos = new ReintentoAcotado(PropiedadesDePrueba.crear());
        notificacionListener = new NotificacionListener(lector, reintentos, notificacionService);
        auditoriaListener = new AuditoriaListener(lector, reintentos, auditoriaService);
    }

    private Message mensaje(String cuerpo, String routingKey) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(TAG);
        propiedades.setReceivedRoutingKey(routingKey);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }

    // ----------------------------------------------------------- notificaciones

    @Test
    void notificacionValidaSeConfirmaConAck() throws Exception {
        notificacionListener.onEvento(mensaje(EVENTO, "envio.creado"), channel);

        verify(notificacionService).notificar(eq("envio.creado"), any(JsonNode.class));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void notificacionConCuerpoQueNoEsJsonVaALaDlq() throws Exception {
        notificacionListener.onEvento(mensaje("no es json", "envio.creado"), channel);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void notificacionInvalidaVaALaDlqSinReintentar() throws Exception {
        doThrow(new MensajeInvalidoException("sin trackingCode"))
                .when(notificacionService).notificar(any(), any());

        notificacionListener.onEvento(mensaje("{\"x\":1}", "envio.prueba"), channel);

        verify(notificacionService, times(1)).notificar(any(), any());
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // ---------------------------------------------------------------- auditoría

    @Test
    void auditoriaValidaSeGuardaYSeConfirmaConAck() throws Exception {
        auditoriaListener.onEvento(mensaje(EVENTO, "envio.creado"), channel);

        verify(auditoriaService).registrar(eq("envio.creado"), any(JsonNode.class));
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void auditoriaConMensajeIlegibleVaALaDlq() throws Exception {
        auditoriaListener.onEvento(mensaje("[1,2,3]", "envio.creado"), channel);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void falloTransitorioDeLaBaseSeReintentaYTerminaEnLaDlqSiPersiste() throws Exception {
        doThrow(new IllegalStateException("base de datos caída"))
                .when(auditoriaService).registrar(any(), any());

        auditoriaListener.onEvento(mensaje(EVENTO, "envio.creado"), channel);

        verify(auditoriaService, times(3)).registrar(any(), any());
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void falloTransitorioQueSeResuelveEnElSegundoIntentoSeConfirmaConAck() throws Exception {
        doThrow(new IllegalStateException("base de datos caída"))
                .doReturn(null)
                .when(auditoriaService).registrar(any(), any());

        auditoriaListener.onEvento(mensaje(EVENTO, "envio.creado"), channel);

        verify(auditoriaService, times(2)).registrar(any(), any());
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
