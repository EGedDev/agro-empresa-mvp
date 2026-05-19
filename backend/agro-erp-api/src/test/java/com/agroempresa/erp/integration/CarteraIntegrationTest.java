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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarteraIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.cartera";
    private static final String ADMIN_PASSWORD = "Password123!";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudIdempotenteRepository solicitudIdempotenteRepository;

    @Autowired
    private CierreCajaRepository cierreCajaRepository;

    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;

    @Autowired
    private PagoVentaRepository pagoVentaRepository;

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
    void carteraListaFiltraYResumeCuentasAbiertas() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Cartera");
        Long productoId = crearProducto(token, categoriaId, "Producto Cartera", new BigDecimal("10.00"), 80, 5);
        Long clienteParcialId = crearCliente(token, "Cliente Cartera Parcial");
        Long clientePendienteId = crearCliente(token, "Cliente Cartera Pendiente");
        Long proveedorId = crearProveedor(token, "Proveedor Cartera");
        LocalDate hoy = LocalDate.now();
        LocalDate vencimientoPasado = hoy.minusDays(5);
        LocalDate vencimientoFuturo = hoy.plusDays(3);

        Long ventaParcialId = crearVenta(token, clienteParcialId, productoId, 3, vencimientoPasado);
        registrarPagoVenta(token, ventaParcialId, new BigDecimal("10.00"));
        crearVenta(token, clientePendienteId, productoId, 4, vencimientoFuturo);

        Long ventaPagadaId = crearVenta(token, clienteParcialId, productoId, 1, vencimientoPasado);
        registrarPagoVenta(token, ventaPagadaId, new BigDecimal("10.00"));

        Long compraParcialId = crearCompra(token, proveedorId, productoId, 4, new BigDecimal("5.00"), vencimientoPasado);
        registrarPagoCompra(token, compraParcialId, new BigDecimal("7.00"));
        crearCompra(token, proveedorId, productoId, 1, new BigDecimal("17.00"), vencimientoFuturo);

        Long compraPagadaId = crearCompra(token, proveedorId, productoId, 1, new BigDecimal("9.00"), vencimientoPasado);
        registrarPagoCompra(token, compraPagadaId, new BigDecimal("9.00"));

        JsonNode cuentasPorCobrar = getJson(
                "/api/v1/finanzas/cartera/cuentas-por-cobrar?sort=saldoPendiente,desc",
                token
        );
        assertThat(cuentasPorCobrar.path("totalElementos").asLong()).isEqualTo(2L);
        assertThat(cuentasPorCobrar.path("contenido").get(0).path("clienteId").asLong())
                .isEqualTo(clientePendienteId);
        assertThat(cuentasPorCobrar.path("contenido").get(0).path("saldoPendiente").decimalValue())
                .isEqualByComparingTo("40.00");

        JsonNode cuentasParciales = getJson(
                "/api/v1/finanzas/cartera/cuentas-por-cobrar?estadoPago=PARCIAL",
                token
        );
        assertThat(cuentasParciales.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(cuentasParciales.path("contenido").get(0).path("ventaId").asLong()).isEqualTo(ventaParcialId);
        assertThat(cuentasParciales.path("contenido").get(0).path("fechaVencimiento").asText())
                .isEqualTo(vencimientoPasado.toString());
        assertThat(cuentasParciales.path("contenido").get(0).path("vencida").asBoolean()).isTrue();
        assertThat(cuentasParciales.path("contenido").get(0).path("diasVencida").asLong()).isEqualTo(5L);
        assertThat(cuentasParciales.path("contenido").get(0).path("saldoPendiente").decimalValue())
                .isEqualByComparingTo("20.00");

        JsonNode cuentasPorVencer = getJson(
                "/api/v1/finanzas/cartera/cuentas-por-cobrar?vencida=false&venceDesde="
                        + hoy
                        + "&venceHasta="
                        + hoy.plusDays(10),
                token
        );
        assertThat(cuentasPorVencer.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(cuentasPorVencer.path("contenido").get(0).path("clienteId").asLong())
                .isEqualTo(clientePendienteId);
        assertThat(cuentasPorVencer.path("contenido").get(0).path("vencida").asBoolean()).isFalse();
        assertThat(cuentasPorVencer.path("contenido").get(0).path("diasVencida").asLong()).isZero();

        JsonNode cuentasPorPagar = getJson(
                "/api/v1/finanzas/cartera/cuentas-por-pagar?proveedorId=" + proveedorId,
                token
        );
        assertThat(cuentasPorPagar.path("totalElementos").asLong()).isEqualTo(2L);

        JsonNode resumen = getJson("/api/v1/finanzas/cartera/resumen", token);
        assertThat(resumen.path("cuentasPorCobrar").path("cantidadDocumentos").asLong()).isEqualTo(2L);
        assertThat(resumen.path("cuentasPorCobrar").path("saldoPendiente").decimalValue())
                .isEqualByComparingTo("60.00");
        assertThat(resumen.path("cuentasPorCobrar").path("cantidadVencida").asLong()).isEqualTo(1L);
        assertThat(resumen.path("cuentasPorCobrar").path("saldoVencido").decimalValue())
                .isEqualByComparingTo("20.00");
        assertThat(resumen.path("cuentasPorCobrar").path("cantidadPorVencer").asLong()).isEqualTo(1L);
        assertThat(resumen.path("cuentasPorCobrar").path("saldoPorVencer").decimalValue())
                .isEqualByComparingTo("40.00");
        assertThat(resumen.path("cuentasPorPagar").path("cantidadDocumentos").asLong()).isEqualTo(2L);
        assertThat(resumen.path("cuentasPorPagar").path("saldoPendiente").decimalValue())
                .isEqualByComparingTo("30.00");
        assertThat(resumen.path("cuentasPorPagar").path("cantidadVencida").asLong()).isEqualTo(1L);
        assertThat(resumen.path("cuentasPorPagar").path("saldoVencido").decimalValue())
                .isEqualByComparingTo("13.00");
        assertThat(resumen.path("cuentasPorPagar").path("cantidadPorVencer").asLong()).isEqualTo(1L);
        assertThat(resumen.path("cuentasPorPagar").path("saldoPorVencer").decimalValue())
                .isEqualByComparingTo("17.00");
        assertThat(resumen.path("saldoNeto").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(resumen.path("generadoEn").asText()).isNotBlank();
    }

    @Test
    void carteraRechazaEstadoDePagoCerrado() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/v1/finanzas/cartera/cuentas-por-cobrar?estadoPago=PAGADA")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("El estado de pago para cartera debe ser PENDIENTE o PARCIAL"));
    }

    @Test
    void carteraRechazaRangoDeVencimientoInvalido() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get("/api/v1/finanzas/cartera/cuentas-por-pagar?venceDesde="
                        + hoy
                        + "&venceHasta="
                        + hoy.minusDays(1))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("La fecha final de vencimiento no puede ser anterior a la fecha inicial"));
    }

    @Test
    void usuarioVentasNoPuedeConsultarCarteraFinanciera() throws Exception {
        String adminToken = obtenerTokenAdmin();
        crearUsuario(adminToken, "ventas.cartera", "Usuario Ventas Cartera", "VENTAS");
        String ventasToken = login("ventas.cartera", ADMIN_PASSWORD);
        String correlationId = "cartera-forbidden-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/finanzas/cartera/resumen")
                        .header("Authorization", bearer(ventasToken))
                        .header(RequestTraceContext.CORRELATION_ID_HEADER, correlationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.correlationId").value(correlationId));
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Cartera"
                        ))))
                .andExpect(status().isCreated());

        return login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private String login(String username, String password) throws Exception {
        JsonNode login = jsonNode(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        return login.path("accessToken").asText();
    }

    private void crearUsuario(String token, String username, String nombre, String rol) throws Exception {
        postJsonSinKey(
                "/api/v1/usuarios",
                Map.of(
                        "username", username,
                        "password", ADMIN_PASSWORD,
                        "nombre", nombre,
                        "rol", rol
                ),
                token
        );
    }

    private Long crearCategoria(String token, String nombre) throws Exception {
        return postJsonSinKey(
                "/api/v1/categorias",
                Map.of(
                        "nombre", nombre,
                        "descripcion", "Categoria de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProducto(
            String token,
            Long categoriaId,
            String nombre,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo
    ) throws Exception {
        return postJsonSinKey(
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
        ).path("id").asLong();
    }

    private Long crearCliente(String token, String nombre) throws Exception {
        return postJsonSinKey(
                "/api/v1/clientes",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", "CLI-" + Integer.toUnsignedString(nombre.hashCode()),
                        "telefono", "999000111",
                        "email", "cliente.cartera@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProveedor(String token, String nombre) throws Exception {
        return postJsonSinKey(
                "/api/v1/proveedores",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", "PRO-" + Integer.toUnsignedString(nombre.hashCode()),
                        "telefono", "999000222",
                        "email", "proveedor.cartera@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearVenta(
            String token,
            Long clienteId,
            Long productoId,
            int cantidad,
            LocalDate fechaVencimiento
    ) throws Exception {
        return postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "fechaVencimiento", fechaVencimiento.toString(),
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", cantidad
                        ))
                ),
                token
        ).path("id").asLong();
    }

    private Long crearCompra(
            String token,
            Long proveedorId,
            Long productoId,
            int cantidad,
            BigDecimal costoUnitario,
            LocalDate fechaVencimiento
    ) throws Exception {
        return postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "fechaVencimiento", fechaVencimiento.toString(),
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", cantidad,
                                "costoUnitario", costoUnitario
                        ))
                ),
                token
        ).path("id").asLong();
    }

    private void registrarPagoVenta(String token, Long ventaId, BigDecimal monto) throws Exception {
        postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", monto,
                        "metodoPago", "EFECTIVO",
                        "referencia", "Pago cartera"
                ),
                token
        );
    }

    private void registrarPagoCompra(String token, Long compraId, BigDecimal monto) throws Exception {
        postJson(
                "/api/v1/compras/" + compraId + "/pagos",
                Map.of(
                        "monto", monto,
                        "metodoPago", "TRANSFERENCIA",
                        "referencia", "Pago cartera"
                ),
                token
        );
    }

    private JsonNode postJson(String url, Object body, String token) throws Exception {
        return jsonNode(mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private JsonNode postJsonSinKey(String url, Object body, String token) throws Exception {
        return jsonNode(mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn());
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
