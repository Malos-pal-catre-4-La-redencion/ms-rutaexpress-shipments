package com.rutaexpress.shipments;

import com.rutaexpress.shipments.model.EstadoEnvio;
import com.rutaexpress.shipments.web.dto.CambiarEstadoRequest;
import com.rutaexpress.shipments.web.dto.CrearEnvioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void creaUnEnvioYQuedaEnEstadoCreado() throws Exception {
        CrearEnvioRequest request = new CrearEnvioRequest("Tienda RutaExpress", "Juan Pérez", "Av. Siempre Viva 123");

        mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.trackingCode").value(org.hamcrest.Matchers.startsWith("RE-")));
    }

    @Test
    void noPermiteSaltarDeCreadoAEnRuta() throws Exception {
        CrearEnvioRequest crear = new CrearEnvioRequest("Tienda RutaExpress", "Ana Soto", "Calle Falsa 456");

        String respuesta = mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crear)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(respuesta).get("id").asLong();

        CambiarEstadoRequest saltoInvalido = new CambiarEstadoRequest(EstadoEnvio.EN_RUTA);

        mockMvc.perform(patch("/envios/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saltoInvalido)))
                .andExpect(status().isConflict());
    }

    @Test
    void permiteLaTransicionCorrectaPasoAPaso() throws Exception {
        CrearEnvioRequest crear = new CrearEnvioRequest("Tienda RutaExpress", "Luis Rojas", "Pasaje Los Aromos 789");

        String respuesta = mockMvc.perform(post("/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crear)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(respuesta).get("id").asLong();

        mockMvc.perform(patch("/envios/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambiarEstadoRequest(EstadoEnvio.ACEPTADO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));
    }

    @Test
    void envioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/envios/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}
