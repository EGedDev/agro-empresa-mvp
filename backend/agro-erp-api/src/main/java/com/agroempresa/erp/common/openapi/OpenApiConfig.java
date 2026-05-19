package com.agroempresa.erp.common.openapi;

import com.agroempresa.erp.common.tracing.RequestTraceContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final Set<String> RUTAS_IDEMPOTENTES = Set.of(
            "/api/v1/ventas",
            "/api/v1/compras",
            "/api/v1/ventas/{ventaId}/pagos",
            "/api/v1/compras/{compraId}/pagos",
            "/api/v1/ventas/{ventaId}/pagos/{pagoId}/anular",
            "/api/v1/compras/{compraId}/pagos/{pagoId}/anular",
            "/api/v1/ventas/{ventaId}/devoluciones",
            "/api/v1/compras/{compraId}/devoluciones",
            "/api/v1/inventario/movimientos",
            "/api/v1/finanzas/caja/cierres"
    );

    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            "/api/v1/health",
            "/api/v1/auth/login",
            "/api/v1/auth/bootstrap-admin"
    );

    @Bean
    public OpenAPI agroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agro ERP API")
                        .version("v1")
                        .description("API REST del nucleo ERP agricola para catalogo, ventas, compras, pagos, inventario, seguridad y auditoria."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer operacionesCriticasOpenApiCustomizer() {
        return openApi -> openApi.getPaths().forEach((ruta, pathItem) -> {
            documentarTrazabilidad(pathItem);
            marcarOperacionPublicaSiAplica(ruta, pathItem);
            documentarIdempotenciaSiAplica(ruta, pathItem);
        });
    }

    private void documentarTrazabilidad(PathItem pathItem) {
        pathItem.readOperations().forEach(operation -> {
            if (operation.getParameters() == null
                    || operation.getParameters().stream().noneMatch(this::esHeaderCorrelationId)) {
                operation.addParametersItem(new Parameter()
                        .name(RequestTraceContext.CORRELATION_ID_HEADER)
                        .in("header")
                        .required(false)
                        .description("Identificador opcional para correlacionar logs, errores y auditoria.")
                        .schema(new StringSchema().maxLength(120)));
            }
        });
    }

    private void marcarOperacionPublicaSiAplica(String ruta, PathItem pathItem) {
        if (!RUTAS_PUBLICAS.contains(ruta)) {
            return;
        }

        pathItem.readOperations().forEach(operation -> operation.setSecurity(List.of()));
    }

    private void documentarIdempotenciaSiAplica(String ruta, PathItem pathItem) {
        if (!RUTAS_IDEMPOTENTES.contains(ruta) || pathItem.getPost() == null) {
            return;
        }

        Operation post = pathItem.getPost();
        if (post.getParameters() == null || post.getParameters().stream().noneMatch(this::esHeaderIdempotencyKey)) {
            post.addParametersItem(new Parameter()
                    .name(IDEMPOTENCY_KEY)
                    .in("header")
                    .required(true)
                    .description("Clave unica por usuario, metodo y ruta. Permite reintentos seguros sin duplicar operaciones.")
                    .schema(new StringSchema().maxLength(120)));
        }

        ApiResponses responses = post.getResponses() == null ? new ApiResponses() : post.getResponses();
        responses.addApiResponse("409", new ApiResponse()
                .description("La clave de idempotencia ya fue usada con otro body o la solicitud sigue en proceso."));
        responses.addApiResponse("428", new ApiResponse()
                .description("El header Idempotency-Key es obligatorio para esta operacion."));
        post.setResponses(responses);
    }

    private boolean esHeaderIdempotencyKey(Parameter parameter) {
        return IDEMPOTENCY_KEY.equalsIgnoreCase(parameter.getName())
                && "header".equalsIgnoreCase(parameter.getIn());
    }

    private boolean esHeaderCorrelationId(Parameter parameter) {
        return RequestTraceContext.CORRELATION_ID_HEADER.equalsIgnoreCase(parameter.getName())
                && "header".equalsIgnoreCase(parameter.getIn());
    }
}
