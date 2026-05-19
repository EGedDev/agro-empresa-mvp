package com.agroempresa.erp.integration;

import com.agroempresa.erp.auditoria.AuditoriaEventoRepository;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraRepository;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaRepository;
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
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevolucionesIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.devoluciones";
    private static final String ADMIN_PASSWORD = "Password123!";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitudIdempotenteRepository solicitudIdempotenteRepository;

    @Autowired
    private DevolucionVentaRepository devolucionVentaRepository;

    @Autowired
    private DevolucionCompraRepository devolucionCompraRepository;

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
        devolucionVentaRepository.deleteAll();
        devolucionCompraRepository.deleteAll();
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
    void devolucionVentaAjustaSaldoStockInventarioReporteEIdempotencia() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Dev Venta");
        Long productoId = crearProducto(token, categoriaId, "Producto DV", new BigDecimal("10.00"), 5, 1);
        Long clienteId = crearCliente(token, "Cliente DV", "CLI-DV-1");

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
        Long ventaDetalleId = venta.path("detalles").get(0).path("id").asLong();

        Map<String, Object> request = Map.of(
                "motivo", "Producto devuelto por cliente",
                "detalles", List.of(Map.of(
                        "ventaDetalleId", ventaDetalleId,
                        "cantidad", 1
                ))
        );
        String idempotencyKey = "devolucion-venta-" + UUID.randomUUID();

        JsonNode devolucion = postJsonConKey(
                "/api/v1/ventas/" + ventaId + "/devoluciones",
                request,
                token,
                idempotencyKey
        );
        postJsonConKey(
                "/api/v1/ventas/" + ventaId + "/devoluciones",
                request,
                token,
                idempotencyKey
        );

        assertThat(devolucion.path("ventaId").asLong()).isEqualTo(ventaId);
        assertThat(devolucion.path("total").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(devolucion.path("detalles").get(0).path("cantidad").asInt()).isEqualTo(1);

        JsonNode ventaActualizada = getJson("/api/v1/ventas/" + ventaId, token);
        assertThat(ventaActualizada.path("total").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(ventaActualizada.path("totalPagado").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(ventaActualizada.path("saldoPendiente").decimalValue()).isEqualByComparingTo("20.00");

        JsonNode producto = getJson("/api/v1/productos/" + productoId, token);
        assertThat(producto.path("stockActual").asInt()).isEqualTo(3);

        JsonNode devoluciones = getJson("/api/v1/ventas/" + ventaId + "/devoluciones", token);
        assertThat(devoluciones.path("totalElementos").asLong()).isEqualTo(1L);

        JsonNode movimientos = getJson(
                "/api/v1/inventario/movimientos?productoId=" + productoId + "&sort=creadoEn,asc",
                token
        );
        assertThat(movimientos.path("totalElementos").asLong()).isEqualTo(2L);
        assertThat(movimientos.path("contenido").get(0).path("tipo").asText()).isEqualTo("SALIDA_POR_VENTA");
        assertThat(movimientos.path("contenido").get(1).path("tipo").asText())
                .isEqualTo("ENTRADA_POR_DEVOLUCION_VENTA");
        assertThat(movimientos.path("contenido").get(1).path("referenciaTipo").asText())
                .isEqualTo("DEVOLUCION_VENTA");

        LocalDate hoy = LocalDate.now();
        JsonNode resumen = getJson("/api/v1/reportes/finanzas/resumen?desde=" + hoy + "&hasta=" + hoy, token);
        assertThat(resumen.path("devolucionesVenta").decimalValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void devolucionCompraAjustaSaldoStockInventarioYReporte() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Dev Compra");
        Long productoId = crearProducto(token, categoriaId, "Producto DC", new BigDecimal("10.00"), 2, 1);
        Long proveedorId = crearProveedor(token, "Proveedor DC", "PROV-DC-1");

        JsonNode compra = postJson(
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
        );
        Long compraId = compra.path("id").asLong();
        Long compraDetalleId = compra.path("detalles").get(0).path("id").asLong();

        JsonNode devolucion = postJson(
                "/api/v1/compras/" + compraId + "/devoluciones",
                Map.of(
                        "motivo", "Producto devuelto al proveedor",
                        "detalles", List.of(Map.of(
                                "compraDetalleId", compraDetalleId,
                                "cantidad", 2
                        ))
                ),
                token
        );

        assertThat(devolucion.path("compraId").asLong()).isEqualTo(compraId);
        assertThat(devolucion.path("total").decimalValue()).isEqualByComparingTo("12.00");

        JsonNode compraActualizada = getJson("/api/v1/compras/" + compraId, token);
        assertThat(compraActualizada.path("total").decimalValue()).isEqualByComparingTo("6.00");
        assertThat(compraActualizada.path("saldoPendiente").decimalValue()).isEqualByComparingTo("6.00");

        JsonNode producto = getJson("/api/v1/productos/" + productoId, token);
        assertThat(producto.path("stockActual").asInt()).isEqualTo(3);

        JsonNode movimientos = getJson(
                "/api/v1/inventario/movimientos?productoId=" + productoId + "&sort=creadoEn,asc",
                token
        );
        assertThat(movimientos.path("totalElementos").asLong()).isEqualTo(2L);
        assertThat(movimientos.path("contenido").get(0).path("tipo").asText()).isEqualTo("ENTRADA_POR_COMPRA");
        assertThat(movimientos.path("contenido").get(1).path("tipo").asText())
                .isEqualTo("SALIDA_POR_DEVOLUCION_COMPRA");

        LocalDate hoy = LocalDate.now();
        JsonNode resumen = getJson("/api/v1/reportes/finanzas/resumen?desde=" + hoy + "&hasta=" + hoy, token);
        assertThat(resumen.path("devolucionesCompra").decimalValue()).isEqualByComparingTo("12.00");
    }

    @Test
    void devolucionVentaConPagosExcedentesSeRechaza() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Dev Pago");
        Long productoId = crearProducto(token, categoriaId, "Producto DP", new BigDecimal("10.00"), 5, 1);
        Long clienteId = crearCliente(token, "Cliente DP", "CLI-DP-1");

        JsonNode venta = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 2
                        ))
                ),
                token
        );
        Long ventaId = venta.path("id").asLong();
        Long ventaDetalleId = venta.path("detalles").get(0).path("id").asLong();

        postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("20.00"),
                        "metodoPago", "EFECTIVO",
                        "referencia", "Pago completo"
                ),
                token
        );

        mockMvc.perform(post("/api/v1/ventas/" + ventaId + "/devoluciones")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "devolucion-pago-excedente-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "motivo", "Devolucion con pago excedente",
                                "detalles", List.of(Map.of(
                                        "ventaDetalleId", ventaDetalleId,
                                        "cantidad", 1
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "No se puede registrar la devolucion porque existen pagos por encima del nuevo total"
                ));

        JsonNode producto = getJson("/api/v1/productos/" + productoId, token);
        assertThat(producto.path("stockActual").asInt()).isEqualTo(3);
        assertThat(devolucionVentaRepository.count()).isZero();
    }

    @Test
    void devolucionCompraConStockInsuficienteSeRechaza() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Dev Stock");
        Long productoId = crearProducto(token, categoriaId, "Producto DS", new BigDecimal("10.00"), 0, 1);
        Long proveedorId = crearProveedor(token, "Proveedor DS", "PROV-DS-1");
        Long clienteId = crearCliente(token, "Cliente DS", "CLI-DS-1");

        JsonNode compra = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 2,
                                "costoUnitario", new BigDecimal("6.00")
                        ))
                ),
                token
        );
        Long compraId = compra.path("id").asLong();
        Long compraDetalleId = compra.path("detalles").get(0).path("id").asLong();

        postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 2
                        ))
                ),
                token
        );

        mockMvc.perform(post("/api/v1/compras/" + compraId + "/devoluciones")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "devolucion-stock-insuficiente-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "motivo", "Devolucion sin stock disponible",
                                "detalles", List.of(Map.of(
                                        "compraDetalleId", compraDetalleId,
                                        "cantidad", 1
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "Stock insuficiente para devolver la compra del producto: Producto DS"
                ));

        assertThat(devolucionCompraRepository.count()).isZero();
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Devoluciones"
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

    private Long crearCliente(String token, String nombre, String documento) throws Exception {
        return postJsonSinKey(
                "/api/v1/clientes",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", documento,
                        "telefono", "999000111",
                        "email", documento.toLowerCase() + "@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProveedor(String token, String nombre, String documento) throws Exception {
        return postJsonSinKey(
                "/api/v1/proveedores",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", documento,
                        "telefono", "999000222",
                        "email", documento.toLowerCase() + "@test.local",
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
        return jsonNode(mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
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
