package com.agroempresa.erp.integration;

import com.agroempresa.erp.common.tracing.RequestTraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicaContratoOpenApiConJwtEIdempotencia() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode api = objectMapper.readTree(result.getResponse().getContentAsString());

        JsonNode bearerAuth = api.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearerAuth.path("type").asText()).isEqualTo("http");
        assertThat(bearerAuth.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearerAuth.path("bearerFormat").asText()).isEqualTo("JWT");

        JsonNode ventaPost = api.path("paths").path("/api/v1/ventas").path("post");
        assertThat(tieneHeader(ventaPost, RequestTraceContext.CORRELATION_ID_HEADER, false)).isTrue();
        assertThat(tieneHeaderIdempotencyKey(ventaPost)).isTrue();
        assertThat(ventaPost.path("responses").has("409")).isTrue();
        assertThat(ventaPost.path("responses").has("428")).isTrue();

        JsonNode pagoVentaPost = api.path("paths").path("/api/v1/ventas/{ventaId}/pagos").path("post");
        assertThat(tieneHeaderIdempotencyKey(pagoVentaPost)).isTrue();

        JsonNode cierreCajaPost = api.path("paths").path("/api/v1/finanzas/caja/cierres").path("post");
        assertThat(tieneHeaderIdempotencyKey(cierreCajaPost)).isTrue();

        JsonNode loginPost = api.path("paths").path("/api/v1/auth/login").path("post");
        assertThat(loginPost.path("security").isArray()).isTrue();
        assertThat(loginPost.path("security").size()).isZero();
        assertThat(tieneHeader(loginPost, RequestTraceContext.CORRELATION_ID_HEADER, false)).isTrue();
    }

    @Test
    void publicaSwaggerUiSinAutenticacion() throws Exception {
        MvcResult result = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isBetween(200, 399);
    }

    private boolean tieneHeaderIdempotencyKey(JsonNode operation) {
        return tieneHeader(operation, "Idempotency-Key", true);
    }

    private boolean tieneHeader(JsonNode operation, String nombre, boolean requerido) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (nombre.equals(parameter.path("name").asText())
                    && "header".equals(parameter.path("in").asText())
                    && parameter.path("required").asBoolean() == requerido) {
                return true;
            }
        }

        return false;
    }
}
