package com.agroempresa.erp.integration;

import com.agroempresa.erp.auditoria.AuditoriaEventoRepository;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraRepository;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaRepository;
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
import java.util.Locale;
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
class ReportesFinancierosIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.reportes";
    private static final String ADMIN_PASSWORD = "Password123!";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudIdempotenteRepository solicitudIdempotenteRepository;

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
    private DevolucionVentaRepository devolucionVentaRepository;

    @Autowired
    private DevolucionCompraRepository devolucionCompraRepository;

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
        devolucionVentaRepository.deleteAll();
        devolucionCompraRepository.deleteAll();
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
    void resumenFinancieroCalculaVentasComprasCobrosPagosYFlujoNetoDelPeriodo() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Reportes");
        Long productoId = crearProducto(token, categoriaId, "Producto Reporte", new BigDecimal("10.00"), 20, 3);
        Long clienteId = crearCliente(token, "Cliente Reporte");
        Long proveedorId = crearProveedor(token, "Proveedor Reporte");

        Long ventaId = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 3
                        ))
                ),
                token
        ).path("id").asLong();

        postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("12.00"),
                        "metodoPago", "EFECTIVO",
                        "referencia", "Caja reporte"
                ),
                token
        );

        Long compraId = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 4,
                                "costoUnitario", new BigDecimal("5.00")
                        ))
                ),
                token
        ).path("id").asLong();

        postJson(
                "/api/v1/compras/" + compraId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("7.00"),
                        "metodoPago", "TRANSFERENCIA",
                        "referencia", "Pago reporte"
                ),
                token
        );

        LocalDate hoy = LocalDate.now();
        JsonNode resumen = getJson(
                "/api/v1/reportes/finanzas/resumen?desde=" + hoy + "&hasta=" + hoy,
                token
        );

        assertThat(resumen.path("desde").asText()).isEqualTo(hoy.toString());
        assertThat(resumen.path("hasta").asText()).isEqualTo(hoy.toString());
        assertThat(resumen.path("ventas").path("cantidad").asLong()).isEqualTo(1L);
        assertThat(resumen.path("ventas").path("total").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(resumen.path("ventas").path("saldoPendiente").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(resumen.path("cobrosRecibidos").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(resumen.path("compras").path("cantidad").asLong()).isEqualTo(1L);
        assertThat(resumen.path("compras").path("total").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(resumen.path("compras").path("saldoPendiente").decimalValue()).isEqualByComparingTo("13.00");
        assertThat(resumen.path("pagosRealizados").decimalValue()).isEqualByComparingTo("7.00");
        assertThat(resumen.path("flujoCajaNeto").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(resumen.path("generadoEn").asText()).isNotBlank();
    }

    @Test
    void rentabilidadCalculaIngresosCostosDevolucionesYMargenBruto() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Rentabilidad");
        Long productoId = crearProductoConCosto(
                token,
                categoriaId,
                "Fertilizante Rentable",
                new BigDecimal("10.00"),
                20,
                new BigDecimal("4.00"),
                3
        );
        Long clienteId = crearCliente(token, "Cliente Rentabilidad");

        JsonNode venta = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 5
                        ))
                ),
                token
        );

        Long ventaId = venta.path("id").asLong();
        Long ventaDetalleId = venta.path("detalles").get(0).path("id").asLong();

        postJson(
                "/api/v1/ventas/" + ventaId + "/devoluciones",
                Map.of(
                        "motivo", "Devolucion para rentabilidad",
                        "detalles", List.of(Map.of(
                                "ventaDetalleId", ventaDetalleId,
                                "cantidad", 2
                        ))
                ),
                token
        );

        LocalDate hoy = LocalDate.now();
        JsonNode rentabilidad = getJson(
                "/api/v1/reportes/finanzas/rentabilidad?desde=" + hoy + "&hasta=" + hoy,
                token
        );

        assertThat(rentabilidad.path("ventas").asLong()).isEqualTo(1L);
        assertThat(rentabilidad.path("ingresosBrutos").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(rentabilidad.path("costoVentasBruto").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(rentabilidad.path("devolucionesVenta").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(rentabilidad.path("costoDevuelto").decimalValue()).isEqualByComparingTo("8.00");
        assertThat(rentabilidad.path("ingresosNetos").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(rentabilidad.path("costoVentasNeto").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(rentabilidad.path("utilidadBruta").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(rentabilidad.path("margenBrutoPorcentaje").decimalValue()).isEqualByComparingTo("60.00");

        JsonNode productos = getJson(
                "/api/v1/reportes/finanzas/rentabilidad/productos?desde=" + hoy + "&hasta=" + hoy + "&limite=5",
                token
        );

        assertThat(productos).hasSize(1);
        JsonNode producto = productos.get(0);
        assertThat(producto.path("productoId").asLong()).isEqualTo(productoId);
        assertThat(producto.path("unidadesVendidas").asLong()).isEqualTo(5L);
        assertThat(producto.path("unidadesDevueltas").asLong()).isEqualTo(2L);
        assertThat(producto.path("unidadesNetas").asLong()).isEqualTo(3L);
        assertThat(producto.path("ingresosNetos").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(producto.path("costoVentasNeto").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(producto.path("utilidadBruta").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(producto.path("margenBrutoPorcentaje").decimalValue()).isEqualByComparingTo("60.00");
    }

    @Test
    void resumenFinancieroRechazaRangoInvalido() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/v1/reportes/finanzas/resumen?desde=2026-05-18&hasta=2026-05-17")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("La fecha final no puede ser anterior a la fecha inicial"));
    }

    @Test
    void usuarioVentasNoPuedeConsultarReportesFinancieros() throws Exception {
        String adminToken = obtenerTokenAdmin();
        crearUsuario(adminToken, "ventas.reportes", "Usuario Ventas", "VENTAS");
        String ventasToken = login("ventas.reportes", ADMIN_PASSWORD);
        LocalDate hoy = LocalDate.now();
        String correlationId = "reporte-forbidden-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/reportes/finanzas/resumen?desde=" + hoy + "&hasta=" + hoy)
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
                                "nombre", "Administrador Reportes"
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

    private Long crearProductoConCosto(
            String token,
            Long categoriaId,
            String nombre,
            BigDecimal precioVenta,
            Integer stockActual,
            BigDecimal costoInicial,
            Integer stockMinimo
    ) throws Exception {
        return postJsonSinKey(
                "/api/v1/productos",
                Map.of(
                        "nombre", nombre,
                        "descripcion", "Producto de prueba",
                        "precioVenta", precioVenta,
                        "stockActual", stockActual,
                        "costoInicial", costoInicial,
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
                        "documentoIdentidad", nombre.replace(" ", "-").toUpperCase(Locale.ROOT),
                        "telefono", "999000111",
                        "email", "cliente.reportes@test.local",
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
                        "documentoIdentidad", nombre.replace(" ", "-").toUpperCase(Locale.ROOT),
                        "telefono", "999000222",
                        "email", "proveedor.reportes@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
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
