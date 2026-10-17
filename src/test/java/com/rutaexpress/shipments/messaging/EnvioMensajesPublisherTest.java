package com.rutaexpress.shipments.messaging;

import com.rutaexpress.shipments.messaging.mensajes.AsignarVehiculoComando;
import com.rutaexpress.shipments.messaging.mensajes.EnvioEvento;
import com.rutaexpress.shipments.model.Envio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnvioMensajesPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EnvioMensajesPublisher publisher;

    private final Envio envio = new Envio("RE-TEST0001", "Tienda", "Juan", "Calle 1", new BigDecimal("40"));

    @BeforeEach
    void setUp() {
        publisher = new EnvioMensajesPublisher(rabbitTemplate, PropiedadesDePrueba.crear());
    }

    @Test
    void elComandoVaAlExchangeDirectConSuRoutingKey() {
        publisher.publicarComandoAsignarVehiculo(envio);

        ArgumentCaptor<Object> mensaje = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rutaexpress.commands"), eq("envio.asignar-vehiculo"), mensaje.capture());
        AsignarVehiculoComando comando = (AsignarVehiculoComando) mensaje.getValue();
        assertEquals("RE-TEST0001", comando.trackingCode());
        assertEquals(new BigDecimal("40"), comando.pesoKg());
    }

    @Test
    void elEventoDeCreacionVaAlExchangeTopic() {
        publisher.publicarEnvioCreado(envio);

        ArgumentCaptor<Object> mensaje = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rutaexpress.events"), eq("envio.creado"), mensaje.capture());
        EnvioEvento evento = (EnvioEvento) mensaje.getValue();
        assertEquals("RE-TEST0001", evento.trackingCode());
        assertEquals("CREADO", evento.estado());
    }

    @Test
    void elEventoDeCambioDeEstadoUsaSuPropiaRoutingKey() {
        publisher.publicarEstadoCambiado(envio);

        verify(rabbitTemplate).convertAndSend(eq("rutaexpress.events"), eq("envio.estado.cambiado"), any(Object.class));
    }

    @Test
    void siRabbitNoResponderElErrorSeRegistraYNoInterrumpeLaPeticion() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("rechazada")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publicarComandoAsignarVehiculo(envio));
    }
}
