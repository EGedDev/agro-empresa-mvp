package com.agroempresa.erp.integration;

import com.agroempresa.erp.auditoria.AuditoriaEventoRepository;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.venta.VentaRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CierreCajaIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.cierre.caja";
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
    void registraCierreCalculandoTotalesDiferenciaYAuditoria() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);

        JsonNode cierre = postJson(
                "/api/v1/finanzas/caja/cierres",
                Map.of(
                        "desde", hoy.toString(),
                        "hasta", hoy.toString(),
                        "saldoReportado", new BigDecimal("4.00"),
                        "observaciones", "Cierre diario",
                        "metodos", List.of(
                                Map.of(
                                        "metodoPago", "EFECTIVO",
                                        "saldoReportado", new BigDecimal("16.00")
                                ),
                                Map.of(
                                        "metodoPago", "TRANSFERENCIA",
                                        "saldoReportado", new BigDecimal("-12.00")
                                )
                        )
                ),
                token
        );

        Long cierreId = cierre.path("id").asLong();
        assertThat(cierre.path("desde").asText()).isEqualTo(hoy.toString());
        assertThat(cierre.path("hasta").asText()).isEqualTo(hoy.toString());
        assertThat(cierre.path("cantidadIngresos").asLong()).isEqualTo(1L);
        assertThat(cierre.path("cantidadEgresos").asLong()).isEqualTo(1L);
        assertThat(cierre.path("totalIngresos").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(cierre.path("totalEgresos").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(cierre.path("saldoCalculado").decimalValue()).isEqualByComparingTo("3.00");
        assertThat(cierre.path("saldoReportado").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(cierre.path("diferencia").decimalValue()).isEqualByComparingTo("1.00");
        assertThat(cierre.path("cerradoPor").asText()).isEqualTo(ADMIN_USERNAME);
        assertThat(cierre.path("metodos").size()).isEqualTo(2);

        JsonNode efectivo = buscarMetodo(cierre, "EFECTIVO");
        assertThat(efectivo.path("cantidadIngresos").asLong()).isEqualTo(1L);
        assertThat(efectivo.path("cantidadEgresos").asLong()).isZero();
        assertThat(efectivo.path("totalIngresos").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(efectivo.path("totalEgresos").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(efectivo.path("saldoCalculado").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(efectivo.path("saldoReportado").decimalValue()).isEqualByComparingTo("16.00");
        assertThat(efectivo.path("diferencia").decimalValue()).isEqualByComparingTo("1.00");

        JsonNode transferencia = buscarMetodo(cierre, "TRANSFERENCIA");
        assertThat(transferencia.path("cantidadIngresos").asLong()).isZero();
        assertThat(transferencia.path("cantidadEgresos").asLong()).isEqualTo(1L);
        assertThat(transferencia.path("totalIngresos").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(transferencia.path("totalEgresos").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(transferencia.path("saldoCalculado").decimalValue()).isEqualByComparingTo("-12.00");
        assertThat(transferencia.path("saldoReportado").decimalValue()).isEqualByComparingTo("-12.00");
        assertThat(transferencia.path("diferencia").decimalValue()).isEqualByComparingTo("0.00");

        JsonNode cierreObtenido = getJson("/api/v1/finanzas/caja/cierres/" + cierreId, token);
        assertThat(cierreObtenido.path("id").asLong()).isEqualTo(cierreId);
        assertThat(cierreObtenido.path("metodos").size()).isEqualTo(2);

        JsonNode cierres = getJson("/api/v1/finanzas/caja/cierres", token);
        assertThat(cierres.path("totalElementos").asLong()).isEqualTo(1L);

        JsonNode auditoria = getJson("/api/v1/auditoria/eventos?accion=cierre_caja_registrado", token);
        assertThat(auditoria.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(auditoria.path("contenido").get(0).path("recursoId").asLong()).isEqualTo(cierreId);
    }

    @Test
    void reintentoDeCierreConMismaClaveNoDuplicaRegistro() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);
        String idempotencyKey = "cierre-caja-" + UUID.randomUUID();
        Map<String, Object> request = Map.of(
                "desde", hoy.toString(),
                "hasta", hoy.toString(),
                "saldoReportado", new BigDecimal("3.00"),
                "observaciones", "Reintento controlado"
        );

        JsonNode primerCierre = postJsonConKey(
                "/api/v1/finanzas/caja/cierres",
                request,
                token,
                idempotencyKey
        );
        MvcResult segundoResultado = postJsonResult(
                "/api/v1/finanzas/caja/cierres",
                request,
                token,
                idempotencyKey
        );
        JsonNode segundoCierre = jsonNode(segundoResultado);

        assertThat(segundoResultado.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(segundoCierre.path("id").asLong()).isEqualTo(primerCierre.path("id").asLong());
        assertThat(cierreCajaRepository.count()).isEqualTo(1L);
    }

    @Test
    void rechazaCierreSolapadoConPeriodoYaCerrado() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);
        postJson(
                "/api/v1/finanzas/caja/cierres",
                Map.of(
                        "desde", hoy.toString(),
                        "hasta", hoy.toString(),
                        "saldoReportado", new BigDecimal("3.00")
                ),
                token
        );

        mockMvc.perform(post("/api/v1/finanzas/caja/cierres")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "desde", hoy.toString(),
                                "hasta", hoy.plusDays(1).toString(),
                                "saldoReportado", new BigDecimal("3.00")
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("Ya existe un cierre de caja que se solapa con el periodo informado"));
    }

    @Test
    void bloqueaPagoDeVentaEnPeriodoDeCajaCerrado() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        DatosCaja datosCaja = prepararMovimientosCaja(token);
        registrarCierreBasico(token, hoy);

        long pagosAntes = pagoVentaRepository.count();
        long movimientosAntes = movimientoCajaRepository.count();

        mockMvc.perform(post("/api/v1/ventas/" + datosCaja.ventaId() + "/pagos")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "monto", new BigDecimal("5.00"),
                                "metodoPago", "EFECTIVO",
                                "referencia", "Pago despues del cierre"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("No se pueden registrar movimientos en un periodo de caja cerrado"));

        assertThat(pagoVentaRepository.count()).isEqualTo(pagosAntes);
        assertThat(movimientoCajaRepository.count()).isEqualTo(movimientosAntes);
        assertThat(ventaRepository.findById(datosCaja.ventaId()).orElseThrow().getTotalPagado())
                .isEqualByComparingTo("15.00");
    }

    @Test
    void bloqueaPagoDeCompraEnPeriodoDeCajaCerrado() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        DatosCaja datosCaja = prepararMovimientosCaja(token);
        registrarCierreBasico(token, hoy);

        long pagosAntes = pagoCompraRepository.count();
        long movimientosAntes = movimientoCajaRepository.count();

        mockMvc.perform(post("/api/v1/compras/" + datosCaja.compraId() + "/pagos")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "monto", new BigDecimal("3.00"),
                                "metodoPago", "TRANSFERENCIA",
                                "referencia", "Pago despues del cierre"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("No se pueden registrar movimientos en un periodo de caja cerrado"));

        assertThat(pagoCompraRepository.count()).isEqualTo(pagosAntes);
        assertThat(movimientoCajaRepository.count()).isEqualTo(movimientosAntes);
        assertThat(compraRepository.findById(datosCaja.compraId()).orElseThrow().getTotalPagado())
                .isEqualByComparingTo("12.00");
    }

    @Test
    void reporteDiferenciasFiltraPorMetodoYSoloConDiferencia() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);
        JsonNode cierre = postJson(
                "/api/v1/finanzas/caja/cierres",
                Map.of(
                        "desde", hoy.toString(),
                        "hasta", hoy.toString(),
                        "saldoReportado", new BigDecimal("4.00"),
                        "metodos", List.of(
                                Map.of(
                                        "metodoPago", "EFECTIVO",
                                        "saldoReportado", new BigDecimal("16.00")
                                ),
                                Map.of(
                                        "metodoPago", "TRANSFERENCIA",
                                        "saldoReportado", new BigDecimal("-12.00")
                                )
                        )
                ),
                token
        );

        JsonNode diferencias = getJson(
                "/api/v1/finanzas/caja/cierres/diferencias?desde=" + hoy + "&hasta=" + hoy,
                token
        );
        assertThat(diferencias.path("totalElementos").asLong()).isEqualTo(1L);
        JsonNode diferencia = diferencias.path("contenido").get(0);
        assertThat(diferencia.path("cierreId").asLong()).isEqualTo(cierre.path("id").asLong());
        assertThat(diferencia.path("diferencia").decimalValue()).isEqualByComparingTo("1.00");
        assertThat(diferencia.path("tieneDiferencia").asBoolean()).isTrue();
        assertThat(diferencia.path("metodos").size()).isEqualTo(1);
        assertThat(diferencia.path("metodos").get(0).path("metodoPago").asText()).isEqualTo("EFECTIVO");
        assertThat(diferencia.path("metodos").get(0).path("diferencia").decimalValue()).isEqualByComparingTo("1.00");

        JsonNode transferenciaConDiferencia = getJson(
                "/api/v1/finanzas/caja/cierres/diferencias?metodoPago=TRANSFERENCIA",
                token
        );
        assertThat(transferenciaConDiferencia.path("totalElementos").asLong()).isZero();

        JsonNode transferenciaSinFiltroDiferencia = getJson(
                "/api/v1/finanzas/caja/cierres/diferencias?metodoPago=TRANSFERENCIA&soloConDiferencia=false",
                token
        );
        assertThat(transferenciaSinFiltroDiferencia.path("totalElementos").asLong()).isEqualTo(1L);
        JsonNode cierreTransferencia = transferenciaSinFiltroDiferencia.path("contenido").get(0);
        assertThat(cierreTransferencia.path("metodos").size()).isEqualTo(1);
        assertThat(cierreTransferencia.path("metodos").get(0).path("metodoPago").asText())
                .isEqualTo("TRANSFERENCIA");
        assertThat(cierreTransferencia.path("metodos").get(0).path("tieneDiferencia").asBoolean()).isFalse();
    }

    @Test
    void reporteDiferenciasRechazaRangoInvalido() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/v1/finanzas/caja/cierres/diferencias?desde=2026-05-18&hasta=2026-05-17")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("La fecha final no puede ser anterior a la fecha inicial"));
    }

    @Test
    void rechazaCierreConDetalleIncompletoPorMetodo() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);

        mockMvc.perform(post("/api/v1/finanzas/caja/cierres")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "desde", hoy.toString(),
                                "hasta", hoy.toString(),
                                "saldoReportado", new BigDecimal("15.00"),
                                "metodos", List.of(Map.of(
                                        "metodoPago", "EFECTIVO",
                                        "saldoReportado", new BigDecimal("15.00")
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("Debe reportar saldo para todos los metodos de pago con movimientos en el periodo"));
    }

    @Test
    void rechazaCierreCuandoSumaPorMetodoNoCoincideConSaldoReportado() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        prepararMovimientosCaja(token);

        mockMvc.perform(post("/api/v1/finanzas/caja/cierres")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "desde", hoy.toString(),
                                "hasta", hoy.toString(),
                                "saldoReportado", new BigDecimal("4.00"),
                                "metodos", List.of(
                                        Map.of(
                                                "metodoPago", "EFECTIVO",
                                                "saldoReportado", new BigDecimal("15.00")
                                        ),
                                        Map.of(
                                                "metodoPago", "TRANSFERENCIA",
                                                "saldoReportado", new BigDecimal("-12.00")
                                        )
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message")
                        .value("La suma de saldos reportados por metodo debe coincidir con el saldo reportado del cierre"));
    }

    @Test
    void cierreCajaSinIdempotencyKeyEsRechazado() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        String correlationId = "cierre-sin-key-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/finanzas/caja/cierres")
                        .header("Authorization", bearer(token))
                        .header("X-Correlation-Id", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "desde", hoy.toString(),
                                "hasta", hoy.toString(),
                                "saldoReportado", new BigDecimal("0.00")
                        ))))
                .andExpect(status().is(428))
                .andExpect(header().string("X-Correlation-Id", correlationId))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    private DatosCaja prepararMovimientosCaja(String token) throws Exception {
        Long categoriaId = crearCategoria(token, "Caja Cierre");
        Long productoId = crearProducto(token, categoriaId, "Producto Cierre", new BigDecimal("10.00"), 20, 2);
        Long clienteId = crearCliente(token, "Cliente Cierre");
        Long proveedorId = crearProveedor(token, "Proveedor Cierre");

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
                        "monto", new BigDecimal("15.00"),
                        "metodoPago", "EFECTIVO",
                        "referencia", "Ingreso cierre"
                ),
                token
        );

        Long compraId = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 3,
                                "costoUnitario", new BigDecimal("6.00")
                        ))
                ),
                token
        ).path("id").asLong();

        postJson(
                "/api/v1/compras/" + compraId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("12.00"),
                        "metodoPago", "TRANSFERENCIA",
                        "referencia", "Egreso cierre"
                ),
                token
        );

        return new DatosCaja(ventaId, compraId);
    }

    private void registrarCierreBasico(String token, LocalDate fecha) throws Exception {
        postJson(
                "/api/v1/finanzas/caja/cierres",
                Map.of(
                        "desde", fecha.toString(),
                        "hasta", fecha.toString(),
                        "saldoReportado", new BigDecimal("3.00")
                ),
                token
        );
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Cierre Caja"
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
                        "email", "cliente.cierre@test.local",
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
                        "email", "proveedor.cierre@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
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

    private JsonNode buscarMetodo(JsonNode cierre, String metodoPago) {
        for (JsonNode metodo : cierre.path("metodos")) {
            if (metodoPago.equals(metodo.path("metodoPago").asText())) {
                return metodo;
            }
        }

        throw new AssertionError("No se encontro el metodo de pago " + metodoPago);
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

    private record DatosCaja(Long ventaId, Long compraId) {
    }
}
