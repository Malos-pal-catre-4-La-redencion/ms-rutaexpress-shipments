package com.rutaexpress.shipments.messaging;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeadLetterListenerTest {

    @Mock
    private Channel channel;

    private Message muerto(long tag) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(tag);
        propiedades.setHeader("x-first-death-queue", "q.auditoria");
        propiedades.setHeader("x-first-death-reason", "rejected");
        return new Message("{\"x\":1}".getBytes(StandardCharsets.UTF_8), propiedades);
    }

    @Test
    void registraYConfirmaLosMensajesMuertosDeNotificaciones() throws Exception {
        new DeadLetterListener().onNotificacionMuerta(muerto(21L), channel);

        verify(channel).basicAck(21L, false);
    }

    @Test
    void registraYConfirmaLosMensajesMuertosDeAuditoria() throws Exception {
        new DeadLetterListener().onAuditoriaMuerta(muerto(22L), channel);

        verify(channel).basicAck(22L, false);
    }
}
