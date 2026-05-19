package com.agroempresa.erp.integration;

import com.agroempresa.erp.auditoria.AuditoriaEventoRepository;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.common.tracing.RequestTraceContext;
import com.agroempresa.erp.finanzas.caja.CierreCajaRepository;
import com.agroempresa.erp.finanzas.caja.MovimientoCajaRepository;
import com.agroempresa.erp.finanzas.pago.compra.PagoCompraRepository;
import com.agroempresa.erp.finanzas.pago.venta.PagoVentaRepository;
import com.agroempresa.erp.idempotencia.SolicitudIdempotenteRepository;
import com.agroempresa.erp.inventario.MovimientoInventarioRepository;
import com.agroempresa.erp.proveedor.ProveedorRepository;
import com.agroempresa.erp.seguridad.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujosCriticosHttpIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.integration";
    private static final String ADMIN_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private PagoVentaRepository pagoVentaRepository;

    @Autowired
    private CierreCajaRepository cierreCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    @Autowired
    private PagoCompraRepository pagoCompraRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudIdempotenteRepository solicitudIdempotenteRepository;

    @BeforeEach
    void limpiarDatos() {
        solicitudIdempotenteRepository.deleteAll();
        cierreCajaRepository.deleteAll();
        movimientoCajaRepository.deleteAll();
        pagoVentaRepository.deleteAll();
        pagoCompraRepository.deleteAll();
        movimientoInventarioRepository.deleteAll();
        ventaRepository.deleteAll();
        compraRepository.deleteAll();
        productoRepository.deleteAll();
        clienteRepository.deleteAll();
        proveedorRepository.deleteAll();
        categoriaRepository.deleteAll();
        auditoriaEventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void flujoVentaDescuentaStockRegistraPagosYBloqueaCancelacionConPagos() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Fertilizantes");
        Long productoId = crearProducto(token, categoriaId, "Urea granulada", new BigDecimal("12.50"), 10, 2);
        Long clienteId = crearCliente(token, "Cliente Venta");

        JsonNode venta = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 3
                        ))
                ),
                token
        );

        Long ventaId = venta.path("id").asLong();
        assertThat(venta.path("estado").asText()).isEqualTo("REGISTRADA");
        assertThat(venta.path("estadoPago").asText()).isEqualTo("PENDIENTE");
        assertThat(venta.path("total").decimalValue()).isEqualByComparingTo("37.50");
        assertThat(venta.path("saldoPendiente").decimalValue()).isEqualByComparingTo("37.50");

        JsonNode productoLuegoDeVenta = getJson("/api/v1/productos/" + productoId, token);
        assertThat(productoLuegoDeVenta.path("stockActual").asInt()).isEqualTo(7);

        JsonNode movimientosSalida = getJson(
                "/api/v1/inventario/movimientos?productoId=" + productoId + "&tipo=SALIDA_POR_VENTA",
                token
        );
        assertThat(movimientosSalida.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(movimientosSalida.path("contenido").get(0).path("stockAnterior").asInt()).isEqualTo(10);
        assertThat(movimientosSalida.path("contenido").get(0).path("stockNuevo").asInt()).isEqualTo(7);

        postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("20.00"),
                        "metodoPago", "EFECTIVO",
                        "referencia", "Caja 1"
                ),
                token
        );

        JsonNode ventaParcial = getJson("/api/v1/ventas/" + ventaId, token);
        assertThat(ventaParcial.path("estadoPago").asText()).isEqualTo("PARCIAL");
        assertThat(ventaParcial.path("totalPagado").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(ventaParcial.path("saldoPendiente").decimalValue()).isEqualByComparingTo("17.50");

        postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("17.50"),
                        "metodoPago", "YAPE",
                        "referencia", "YAPE-001"
                ),
                token
        );

        JsonNode ventaPagada = getJson("/api/v1/ventas/" + ventaId, token);
        assertThat(ventaPagada.path("estadoPago").asText()).isEqualTo("PAGADA");
        assertThat(ventaPagada.path("saldoPendiente").decimalValue()).isEqualByComparingTo("0.00");

        mockMvc.perform(patch("/api/v1/ventas/{id}/cancelar", ventaId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void flujoCompraAumentaStockRegistraPagoYBloqueaCancelacionConPagos() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Semillas");
        Long productoId = crearProducto(token, categoriaId, "Semilla certificada", new BigDecimal("18.00"), 5, 2);
        Long proveedorId = crearProveedor(token, "Proveedor Compra");

        JsonNode compra = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 4,
                                "costoUnitario", new BigDecimal("7.30")
                        ))
                ),
                token
        );

        Long compraId = compra.path("id").asLong();
        assertThat(compra.path("estado").asText()).isEqualTo("REGISTRADA");
        assertThat(compra.path("estadoPago").asText()).isEqualTo("PENDIENTE");
        assertThat(compra.path("total").decimalValue()).isEqualByComparingTo("29.20");

        JsonNode productoLuegoDeCompra = getJson("/api/v1/productos/" + productoId, token);
        assertThat(productoLuegoDeCompra.path("stockActual").asInt()).isEqualTo(9);

        JsonNode movimientosEntrada = getJson(
                "/api/v1/inventario/movimientos?productoId=" + productoId + "&tipo=ENTRADA_POR_COMPRA",
                token
        );
        assertThat(movimientosEntrada.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(movimientosEntrada.path("contenido").get(0).path("stockAnterior").asInt()).isEqualTo(5);
        assertThat(movimientosEntrada.path("contenido").get(0).path("stockNuevo").asInt()).isEqualTo(9);

        postJson(
                "/api/v1/compras/" + compraId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("29.20"),
                        "metodoPago", "TRANSFERENCIA",
                        "referencia", "OP-001"
                ),
                token
        );

        JsonNode compraPagada = getJson("/api/v1/compras/" + compraId, token);
        assertThat(compraPagada.path("estadoPago").asText()).isEqualTo("PAGADA");
        assertThat(compraPagada.path("saldoPendiente").decimalValue()).isEqualByComparingTo("0.00");

        mockMvc.perform(patch("/api/v1/compras/{id}/cancelar", compraId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void ventaSinStockSuficienteNoRegistraMovimientoNiModificaStock() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Agroquimicos");
        Long productoId = crearProducto(token, categoriaId, "Fungicida", new BigDecimal("45.00"), 2, 1);
        Long clienteId = crearCliente(token, "Cliente Sin Stock");

        mockMvc.perform(post("/api/v1/ventas")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "venta-sin-stock-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clienteId", clienteId,
                                "detalles", List.of(Map.of(
                                        "productoId", productoId,
                                        "cantidad", 3
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        JsonNode producto = getJson("/api/v1/productos/" + productoId, token);
        assertThat(producto.path("stockActual").asInt()).isEqualTo(2);

        JsonNode movimientos = getJson("/api/v1/inventario/movimientos?productoId=" + productoId, token);
        assertThat(movimientos.path("totalElementos").asLong()).isZero();
    }

    @Test
    void listadoDeProductosPaginaYRechazaOrdenamientoInvalido() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Herramientas");
        crearProducto(token, categoriaId, "Bomba mochila", new BigDecimal("150.00"), 4, 1);
        crearProducto(token, categoriaId, "Atomizador", new BigDecimal("80.00"), 6, 2);

        JsonNode pagina = getJson("/api/v1/productos?page=0&size=1&sort=nombre,asc", token);

        assertThat(pagina.path("pagina").asInt()).isZero();
        assertThat(pagina.path("tamanio").asInt()).isEqualTo(1);
        assertThat(pagina.path("totalElementos").asLong()).isEqualTo(2L);
        assertThat(pagina.path("totalPaginas").asInt()).isEqualTo(2);
        assertThat(pagina.path("contenido").get(0).path("nombre").asText()).isEqualTo("Atomizador");

        mockMvc.perform(get("/api/v1/productos?sort=nombre,asc,extra")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Formato de ordenamiento invalido. Use campo,direccion"));
    }

    @Test
    void reintentoDeVentaConMismaClaveNoDuplicaStockNiMovimientos() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Riego");
        Long productoId = crearProducto(token, categoriaId, "Manguera", new BigDecimal("25.00"), 8, 2);
        Long clienteId = crearCliente(token, "Cliente Retry Venta");
        String idempotencyKey = "venta-retry-" + UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "clienteId", clienteId,
                "detalles", List.of(Map.of(
                        "productoId", productoId,
                        "cantidad", 3
                ))
        );

        JsonNode primeraVenta = postJsonConKey("/api/v1/ventas", request, token, idempotencyKey);
        MvcResult segundoResultado = postJsonResult("/api/v1/ventas", request, token, idempotencyKey);
        JsonNode segundaVenta = jsonNode(segundoResultado);

        assertThat(segundoResultado.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(segundaVenta.path("id").asLong()).isEqualTo(primeraVenta.path("id").asLong());

        JsonNode producto = getJson("/api/v1/productos/" + productoId, token);
        assertThat(producto.path("stockActual").asInt()).isEqualTo(5);

        JsonNode movimientos = getJson(
                "/api/v1/inventario/movimientos?productoId=" + productoId + "&tipo=SALIDA_POR_VENTA",
                token
        );
        assertThat(movimientos.path("totalElementos").asLong()).isEqualTo(1L);

        JsonNode ventas = getJson("/api/v1/ventas?clienteId=" + clienteId, token);
        assertThat(ventas.path("totalElementos").asLong()).isEqualTo(1L);
    }

    @Test
    void reintentoDePagoConMismaClaveNoDuplicaMontoNiRegistro() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Nutricion");
        Long productoId = crearProducto(token, categoriaId, "Bioestimulante", new BigDecimal("40.00"), 6, 2);
        Long clienteId = crearCliente(token, "Cliente Retry Pago");
        Long ventaId = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 2
                        ))
                ),
                token
        ).path("id").asLong();

        String idempotencyKey = "pago-venta-retry-" + UUID.randomUUID();
        Map<String, Object> pagoRequest = Map.of(
                "monto", new BigDecimal("30.00"),
                "metodoPago", "EFECTIVO",
                "referencia", "Caja retry"
        );

        postJsonConKey("/api/v1/ventas/" + ventaId + "/pagos", pagoRequest, token, idempotencyKey);
        MvcResult segundoResultado = postJsonResult(
                "/api/v1/ventas/" + ventaId + "/pagos",
                pagoRequest,
                token,
                idempotencyKey
        );

        assertThat(segundoResultado.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");

        JsonNode venta = getJson("/api/v1/ventas/" + ventaId, token);
        assertThat(venta.path("totalPagado").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(venta.path("saldoPendiente").decimalValue()).isEqualByComparingTo("50.00");

        JsonNode pagos = getJson("/api/v1/ventas/" + ventaId + "/pagos", token);
        assertThat(pagos.path("totalElementos").asLong()).isEqualTo(1L);
    }

    @Test
    void mismaClaveConOtroBodyRespondeConflicto() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Maquinaria");
        Long productoId = crearProducto(token, categoriaId, "Repuesto", new BigDecimal("15.00"), 10, 2);
        Long clienteId = crearCliente(token, "Cliente Conflicto");
        String idempotencyKey = "venta-conflicto-" + UUID.randomUUID();

        postJsonConKey(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 1
                        ))
                ),
                token,
                idempotencyKey
        );

        mockMvc.perform(post("/api/v1/ventas")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clienteId", clienteId,
                                "detalles", List.of(Map.of(
                                        "productoId", productoId,
                                        "cantidad", 2
                                ))
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void operacionCriticaSinIdempotencyKeyEsRechazada() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Proteccion");
        Long productoId = crearProducto(token, categoriaId, "Insecticida", new BigDecimal("32.00"), 5, 1);
        Long clienteId = crearCliente(token, "Cliente Sin Key");
        String correlationId = "missing-key-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ventas")
                        .header("Authorization", bearer(token))
                        .header(RequestTraceContext.CORRELATION_ID_HEADER, correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "clienteId", clienteId,
                                "detalles", List.of(Map.of(
                                        "productoId", productoId,
                                        "cantidad", 1
                                ))
                        ))))
                .andExpect(status().is(428))
                .andExpect(header().string(RequestTraceContext.CORRELATION_ID_HEADER, correlationId))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void auditoriaRegistraTrazabilidadYPermiteFiltrarPorCorrelationId() throws Exception {
        String token = obtenerTokenAdmin();
        String correlationId = "trace-" + UUID.randomUUID();
        String userAgent = "AgroERP-IntegrationTest/1.0";

        MvcResult result = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearer(token))
                        .header(RequestTraceContext.CORRELATION_ID_HEADER, correlationId)
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.20")
                        .header("User-Agent", userAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "usuario.trazabilidad",
                                "password", "Password123!",
                                "nombre", "Usuario Trazabilidad",
                                "rol", "VENTAS"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse().getHeader(RequestTraceContext.CORRELATION_ID_HEADER))
                .isEqualTo(correlationId);

        JsonNode eventos = getJson("/api/v1/auditoria/eventos?correlationId=" + correlationId, token);
        assertThat(eventos.path("totalElementos").asLong()).isEqualTo(1L);

        JsonNode evento = eventos.path("contenido").get(0);
        assertThat(evento.path("accion").asText()).isEqualTo("USUARIO_CREADO");
        assertThat(evento.path("username").asText()).isEqualTo(ADMIN_USERNAME);
        assertThat(evento.path("correlationId").asText()).isEqualTo(correlationId);
        assertThat(evento.path("ipAddress").asText()).isEqualTo("203.0.113.10");
        assertThat(evento.path("userAgent").asText()).isEqualTo(userAgent);
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Integracion"
                        ))))
                .andExpect(status().isCreated());

        JsonNode login = jsonNode(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        return login.path("accessToken").asText();
    }

    private Long crearCategoria(String token, String nombre) throws Exception {
        JsonNode categoria = postJson(
                "/api/v1/categorias",
                Map.of(
                        "nombre", nombre,
                        "descripcion", "Categoria de prueba"
                ),
                token
        );

        return categoria.path("id").asLong();
    }

    private Long crearProducto(
            String token,
            Long categoriaId,
            String nombre,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo
    ) throws Exception {
        JsonNode producto = postJson(
                "/api/v1/productos",
                Map.of(
                        "nombre", nombre,
                        "descripcion", "Producto de prueba",
                        "precioVenta", precioVenta,
                        "stockActual", stockActual,
                        "stockMinimo", stockMinimo,
                        "categoriaId", categoriaId
                ),
                token
        );

        return producto.path("id").asLong();
    }

    private Long crearCliente(String token, String nombre) throws Exception {
        JsonNode cliente = postJson(
                "/api/v1/clientes",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", nombre.replace(" ", "-").toUpperCase(Locale.ROOT),
                        "telefono", "999000111",
                        "email", "cliente.integration@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        );

        return cliente.path("id").asLong();
    }

    private Long crearProveedor(String token, String nombre) throws Exception {
        JsonNode proveedor = postJson(
                "/api/v1/proveedores",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", nombre.replace(" ", "-").toUpperCase(Locale.ROOT),
                        "telefono", "999000222",
                        "email", "proveedor.integration@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        );

        return proveedor.path("id").asLong();
    }

    private JsonNode postJson(String url, Object body, String token) throws Exception {
        return postJsonConKey(url, body, token, UUID.randomUUID().toString());
    }

    private JsonNode postJsonConKey(
            String url,
            Object body,
            String token,
            String idempotencyKey
    ) throws Exception {
        return jsonNode(postJsonResult(url, body, token, idempotencyKey));
    }

    private MvcResult postJsonResult(
            String url,
            Object body,
            String token,
            String idempotencyKey
    ) throws Exception {
        return mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        return jsonNode(mockMvc.perform(get(url)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode jsonNode(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
