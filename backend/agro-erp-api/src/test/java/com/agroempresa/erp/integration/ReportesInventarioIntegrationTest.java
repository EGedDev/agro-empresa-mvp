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
class ReportesInventarioIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.reportes.inventario";
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
    void resumenInventarioCalculaStockBajoEntradasSalidasYUnidadesNetas() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Reporte Inventario");
        Long productoOperativoId = crearProducto(token, categoriaId, "Producto Operativo", new BigDecimal("10.00"), 10, 3);
        crearProducto(token, categoriaId, "Producto Bajo Stock", new BigDecimal("8.00"), 1, 2);

        registrarMovimientoInventario(
                token,
                productoOperativoId,
                "ENTRADA_MANUAL",
                5,
                "Ingreso para reporte"
        );
        registrarMovimientoInventario(
                token,
                productoOperativoId,
                "AJUSTE_NEGATIVO",
                2,
                "Ajuste para reporte"
        );

        LocalDate hoy = LocalDate.now();
        JsonNode resumen = getJson(
                "/api/v1/reportes/inventario/resumen?desde=" + hoy + "&hasta=" + hoy,
                token
        );

        assertThat(resumen.path("desde").asText()).isEqualTo(hoy.toString());
        assertThat(resumen.path("hasta").asText()).isEqualTo(hoy.toString());
        assertThat(resumen.path("productosActivos").asLong()).isEqualTo(2L);
        assertThat(resumen.path("productosConStockBajo").asLong()).isEqualTo(1L);
        assertThat(resumen.path("entradas").path("cantidadMovimientos").asLong()).isEqualTo(1L);
        assertThat(resumen.path("entradas").path("unidades").asLong()).isEqualTo(5L);
        assertThat(resumen.path("salidas").path("cantidadMovimientos").asLong()).isEqualTo(1L);
        assertThat(resumen.path("salidas").path("unidades").asLong()).isEqualTo(2L);
        assertThat(resumen.path("unidadesNetas").asLong()).isEqualTo(3L);
        assertThat(resumen.path("generadoEn").asText()).isNotBlank();
    }

    @Test
    void resumenInventarioRechazaRangoInvalido() throws Exception {
        String token = obtenerTokenAdmin();

        mockMvc.perform(get("/api/v1/reportes/inventario/resumen?desde=2026-05-18&hasta=2026-05-17")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("La fecha final no puede ser anterior a la fecha inicial"));
    }

    @Test
    void usuarioInventarioNoPuedeConsultarReportesOperativos() throws Exception {
        String adminToken = obtenerTokenAdmin();
        crearUsuario(adminToken, "inventario.reportes", "Usuario Inventario", "INVENTARIO");
        String inventarioToken = login("inventario.reportes", ADMIN_PASSWORD);
        LocalDate hoy = LocalDate.now();
        String correlationId = "inventario-forbidden-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/reportes/inventario/resumen?desde=" + hoy + "&hasta=" + hoy)
                        .header("Authorization", bearer(inventarioToken))
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
                                "nombre", "Administrador Reportes Inventario"
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

    private void registrarMovimientoInventario(
            String token,
            Long productoId,
            String tipo,
            Integer cantidad,
            String motivo
    ) throws Exception {
        postJson(
                "/api/v1/inventario/movimientos",
                Map.of(
                        "productoId", productoId,
                        "tipo", tipo,
                        "cantidad", cantidad,
                        "motivo", motivo
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
