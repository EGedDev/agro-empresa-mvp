package com.agroempresa.erp.integration;

import com.agroempresa.erp.cliente.ClienteRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebClienteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void registrarClienteWebNoRequiereAutenticacion() throws Exception {
        String documento = "WEB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        mockMvc.perform(post("/api/v1/web/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Cliente web demo",
                                "documentoIdentidad", documento,
                                "telefono", "999888777",
                                "email", "cliente.web@example.com",
                                "direccion", "Lima | Palto | Nutricion"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Cliente web demo"))
                .andExpect(jsonPath("$.documentoIdentidad").value(documento));

        assertThat(clienteRepository.findByDocumentoIdentidadIgnoreCase(documento)).isPresent();
    }

    @Test
    void crearClienteInternoSigueProtegido() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombre", "Cliente interno protegido"
                        ))))
                .andExpect(status().isUnauthorized());
    }
}
