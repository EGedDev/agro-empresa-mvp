package com.agroempresa.erp.integration;

import com.agroempresa.erp.comercial.solicitud.SolicitudAtencionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSolicitudAtencionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudAtencionRepository solicitudAtencionRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void registrarSolicitudWebNoRequiereAutenticacion() throws Exception {
        long totalAntes = solicitudAtencionRepository.count();

        mockMvc.perform(post("/api/v1/web/solicitudes-atencion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Productor interesado",
                                "telefono", "999888777",
                                "email", "productor@example.com",
                                "cultivo", "Palto",
                                "interes", "Bioestimulantes",
                                "mensaje", "Necesito asesoria para recuperar raiz"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Productor interesado"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        assertThat(solicitudAtencionRepository.count()).isEqualTo(totalAntes + 1);
    }

    @Test
    void listarSolicitudesInternasSigueProtegido() throws Exception {
        mockMvc.perform(get("/api/v1/comercial/solicitudes-atencion"))
                .andExpect(status().isUnauthorized());
    }
}
