package com.rutaexpress.shipments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rutaexpress.shipments.messaging.EnvioMensajesPublisher;
import com.rutaexpress.shipments.model.Envio;
import com.rutaexpress.shipments.model.EstadoEnvio;
import com.rutaexpress.shipments.web.dto.CambiarEstadoRequest;
import com.rutaexpress.shipments.web.dto.CrearEnvioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // El publicador se reemplaza por un mock: las pruebas no necesitan RabbitMQ.
    @MockBean
    private EnvioMensajesPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long crearEnvio(String destinatario) throws Exception {
        CrearEnvioRequest crear = new CrearEnvioRequest("Tienda RutaExpress", destinatario,
                "Calle Falsa 456", new BigDecimal("12.5"));

        String respuesta = mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crear)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asLong();
    }

    @Test
    void creaUnEnvioYQuedaEnEstadoCreado() throws Exception {
        CrearEnvioRequest request = new CrearEnvioRequest("Tienda RutaExpress", "Juan Pérez",
                "Av. Siempre Viva 123", new BigDecimal("12.5"));

        mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.pesoKg").value(12.5))
                .andExpect(jsonPath("$.trackingCode").value(org.hamcrest.Matchers.startsWith("RE-")));
    }

    @Test
    void alCrearUnEnvioValidoPublicaElComandoYElEvento() throws Exception {
        CrearEnvioRequest request = new CrearEnvioRequest("Tienda RutaExpress", "Ana Soto",
                "Calle Falsa 456", new BigDecimal("30"));

        mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(publisher).publicarComandoAsignarVehiculo(any(Envio.class));
        verify(publisher).publicarEnvioCreado(any(Envio.class));
    }

    @Test
    void conPesoInvalidoDevuelve400YNoPublicaNingunMensaje() throws Exception {
        Map<String, Object> request = Map.of(
                "remitente", "Tienda RutaExpress",
                "destinatario", "Ana Soto",
                "direccionDestino", "Calle Falsa 456",
                "pesoKg", -3);

        mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pesoKg").value("El peso debe ser mayor a 0"));

        verifyNoInteractions(publisher);
    }

    @Test
    void sinPesoDevuelve400YNoPublicaNingunMensaje() throws Exception {
        Map<String, Object> request = Map.of(
                "remitente", "Tienda RutaExpress",
                "destinatario", "Ana Soto",
                "direccionDestino", "Calle Falsa 456");

        mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pesoKg").exists());

        verifyNoInteractions(publisher);
    }

    @Test
    void noPermiteSaltarDeCreadoAEnRuta() throws Exception {
        Long id = crearEnvio("Ana Soto");

        mockMvc.perform(patch("/envios/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoRequest(EstadoEnvio.EN_RUTA))))
                .andExpect(status().isConflict());

        verify(publisher, never()).publicarEstadoCambiado(any(Envio.class));
    }

    @Test
    void permiteLaTransicionCorrectaYPublicaElEventoDeEstado() throws Exception {
        Long id = crearEnvio("Luis Rojas");

        mockMvc.perform(patch("/envios/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoRequest(EstadoEnvio.ACEPTADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));

        verify(publisher).publicarEstadoCambiado(any(Envio.class));
    }

    @Test
    void envioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/envios/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}
