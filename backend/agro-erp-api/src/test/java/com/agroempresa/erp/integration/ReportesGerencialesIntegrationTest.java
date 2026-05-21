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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesGerencialesIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.reportes.gerenciales";
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
    private DevolucionVentaRepository devolucionVentaRepository;

    @Autowired
    private DevolucionCompraRepository devolucionCompraRepository;

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
    void reportesGerencialesAgrupanVentasComprasYDescuentanDevoluciones() throws Exception {
        String token = obtenerTokenAdmin();
        Long categoriaId = crearCategoria(token, "Gerenciales");
        Long productoAId = crearProducto(token, categoriaId, "Producto A", new BigDecimal("10.00"), 30, 3);
        Long productoBId = crearProducto(token, categoriaId, "Producto B", new BigDecimal("20.00"), 10, 2);
        Long clienteAId = crearCliente(token, "Cliente Gerencial A");
        Long clienteBId = crearCliente(token, "Cliente Gerencial B");
        Long proveedorAId = crearProveedor(token, "Proveedor Gerencial A");
        Long proveedorBId = crearProveedor(token, "Proveedor Gerencial B");

        JsonNode ventaClienteAProductoA = postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteAId,
                        "detalles", List.of(Map.of(
                                "productoId", productoAId,
                                "cantidad", 5
                        ))
                ),
                token
        );
        postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteAId,
                        "detalles", List.of(Map.of(
                                "productoId", productoBId,
                                "cantidad", 1
                        ))
                ),
                token
        );
        postJson(
                "/api/v1/ventas",
                Map.of(
                        "clienteId", clienteBId,
                        "detalles", List.of(Map.of(
                                "productoId", productoAId,
                                "cantidad", 1
                        ))
                ),
                token
        );

        Long ventaId = ventaClienteAProductoA.path("id").asLong();
        Long ventaDetalleId = ventaClienteAProductoA.path("detalles").get(0).path("id").asLong();
        postJson(
                "/api/v1/ventas/" + ventaId + "/devoluciones",
                Map.of(
                        "motivo", "Devolucion gerencial",
                        "detalles", List.of(Map.of(
                                "ventaDetalleId", ventaDetalleId,
                                "cantidad", 2
                        ))
                ),
                token
        );

        JsonNode compraProveedorA = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorAId,
                        "detalles", List.of(Map.of(
                                "productoId", productoAId,
                                "cantidad", 10,
                                "costoUnitario", new BigDecimal("3.00")
                        ))
                ),
                token
        );
        postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorBId,
                        "detalles", List.of(Map.of(
                                "productoId", productoBId,
                                "cantidad", 2,
                                "costoUnitario", new BigDecimal("8.00")
                        ))
                ),
                token
        );

        Long compraId = compraProveedorA.path("id").asLong();
        Long compraDetalleId = compraProveedorA.path("detalles").get(0).path("id").asLong();
        postJson(
                "/api/v1/compras/" + compraId + "/devoluciones",
                Map.of(
                        "motivo", "Devolucion compra gerencial",
                        "detalles", List.of(Map.of(
                                "compraDetalleId", compraDetalleId,
                                "cantidad", 4
                        ))
                ),
                token
        );

        LocalDate hoy = LocalDate.now();

        JsonNode ventasClientes = getJson(
                "/api/v1/reportes/gerenciales/ventas/clientes?desde=" + hoy + "&hasta=" + hoy + "&limite=5",
                token
        );
        assertThat(ventasClientes).hasSize(2);
        assertThat(ventasClientes.get(0).path("clienteId").asLong()).isEqualTo(clienteAId);
        assertThat(ventasClientes.get(0).path("ventas").asLong()).isEqualTo(2L);
        assertThat(ventasClientes.get(0).path("total").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(ventasClientes.get(1).path("clienteId").asLong()).isEqualTo(clienteBId);
        assertThat(ventasClientes.get(1).path("total").decimalValue()).isEqualByComparingTo("10.00");

        JsonNode ventasProductos = getJson(
                "/api/v1/reportes/gerenciales/ventas/productos?desde=" + hoy + "&hasta=" + hoy + "&limite=5",
                token
        );
        assertThat(ventasProductos).hasSize(2);
        assertThat(ventasProductos.get(0).path("productoId").asLong()).isEqualTo(productoAId);
        assertThat(ventasProductos.get(0).path("unidadesVendidas").asLong()).isEqualTo(6L);
        assertThat(ventasProductos.get(0).path("unidadesDevueltas").asLong()).isEqualTo(2L);
        assertThat(ventasProductos.get(0).path("unidadesNetas").asLong()).isEqualTo(4L);
        assertThat(ventasProductos.get(0).path("totalNeto").decimalValue()).isEqualByComparingTo("40.00");
        assertThat(ventasProductos.get(1).path("productoId").asLong()).isEqualTo(productoBId);
        assertThat(ventasProductos.get(1).path("totalNeto").decimalValue()).isEqualByComparingTo("20.00");

        JsonNode comprasProveedores = getJson(
                "/api/v1/reportes/gerenciales/compras/proveedores?desde=" + hoy + "&hasta=" + hoy + "&limite=5",
                token
        );
        assertThat(comprasProveedores).hasSize(2);
        assertThat(comprasProveedores.get(0).path("proveedorId").asLong()).isEqualTo(proveedorAId);
        assertThat(comprasProveedores.get(0).path("compras").asLong()).isEqualTo(1L);
        assertThat(comprasProveedores.get(0).path("total").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(comprasProveedores.get(1).path("proveedorId").asLong()).isEqualTo(proveedorBId);
        assertThat(comprasProveedores.get(1).path("total").decimalValue()).isEqualByComparingTo("16.00");

        JsonNode comprasProductos = getJson(
                "/api/v1/reportes/gerenciales/compras/productos?desde=" + hoy + "&hasta=" + hoy + "&limite=5",
                token
        );
        assertThat(comprasProductos).hasSize(2);
        assertThat(comprasProductos.get(0).path("productoId").asLong()).isEqualTo(productoAId);
        assertThat(comprasProductos.get(0).path("unidadesCompradas").asLong()).isEqualTo(10L);
        assertThat(comprasProductos.get(0).path("unidadesDevueltas").asLong()).isEqualTo(4L);
        assertThat(comprasProductos.get(0).path("unidadesNetas").asLong()).isEqualTo(6L);
        assertThat(comprasProductos.get(0).path("totalNeto").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(comprasProductos.get(1).path("productoId").asLong()).isEqualTo(productoBId);
        assertThat(comprasProductos.get(1).path("totalNeto").decimalValue()).isEqualByComparingTo("16.00");
    }

    @Test
    void reportesGerencialesRechazanLimiteFueraDeRango() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get("/api/v1/reportes/gerenciales/ventas/clientes?desde=" + hoy + "&hasta=" + hoy + "&limite=101")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Reportes Gerenciales"
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
        String sufijoDocumento = nombre.substring(nombre.length() - 1).toUpperCase(Locale.ROOT);
        return postJsonSinKey(
                "/api/v1/clientes",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", "CLI-GER-" + sufijoDocumento,
                        "telefono", "999000111",
                        "email", "cliente.gerencial@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProveedor(String token, String nombre) throws Exception {
        String sufijoDocumento = nombre.substring(nombre.length() - 1).toUpperCase(Locale.ROOT);
        return postJsonSinKey(
                "/api/v1/proveedores",
                Map.of(
                        "nombre", nombre,
                        "documentoIdentidad", "PROV-GER-" + sufijoDocumento,
                        "telefono", "999000222",
                        "email", "proveedor.gerencial@test.local",
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
