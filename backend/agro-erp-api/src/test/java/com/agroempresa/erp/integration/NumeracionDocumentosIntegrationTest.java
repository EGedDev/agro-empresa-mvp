package com.agroempresa.erp.integration;

import com.agroempresa.erp.auditoria.AuditoriaEventoRepository;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.cliente.ClienteRepository;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraRepository;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaRepository;
import com.agroempresa.erp.common.numeracion.SecuenciaDocumentoRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NumeracionDocumentosIntegrationTest {

    private static final String ADMIN_USERNAME = "admin.numeracion";
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
    private SecuenciaDocumentoRepository secuenciaDocumentoRepository;

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
        secuenciaDocumentoRepository.deleteAll();
        auditoriaEventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void documentosOperativosRecibenCorrelativosProfesionalesIndependientesDelIdInterno() throws Exception {
        String token = obtenerTokenAdmin();
        LocalDate hoy = LocalDate.now();
        Long categoriaId = crearCategoria(token);
        Long productoId = crearProducto(token, categoriaId);
        Long clienteId = crearCliente(token);
        Long proveedorId = crearProveedor(token);

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

        JsonNode compra = postJson(
                "/api/v1/compras",
                Map.of(
                        "proveedorId", proveedorId,
                        "detalles", List.of(Map.of(
                                "productoId", productoId,
                                "cantidad", 3,
                                "costoUnitario", new BigDecimal("4.00")
                        ))
                ),
                token
        );
        Long compraId = compra.path("id").asLong();
        Long compraDetalleId = compra.path("detalles").get(0).path("id").asLong();

        JsonNode pagoVenta = postJson(
                "/api/v1/ventas/" + ventaId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("5.00"),
                        "metodoPago", "EFECTIVO",
                        "referencia", "Pago numeracion venta"
                ),
                token
        );
        JsonNode pagoCompra = postJson(
                "/api/v1/compras/" + compraId + "/pagos",
                Map.of(
                        "monto", new BigDecimal("5.00"),
                        "metodoPago", "TRANSFERENCIA",
                        "referencia", "Pago numeracion compra"
                ),
                token
        );
        JsonNode devolucionVenta = postJson(
                "/api/v1/ventas/" + ventaId + "/devoluciones",
                Map.of(
                        "motivo", "Control de correlativo venta",
                        "detalles", List.of(Map.of(
                                "ventaDetalleId", ventaDetalleId,
                                "cantidad", 1
                        ))
                ),
                token
        );
        JsonNode devolucionCompra = postJson(
                "/api/v1/compras/" + compraId + "/devoluciones",
                Map.of(
                        "motivo", "Control de correlativo compra",
                        "detalles", List.of(Map.of(
                                "compraDetalleId", compraDetalleId,
                                "cantidad", 1
                        ))
                ),
                token
        );
        JsonNode cierreCaja = postJson(
                "/api/v1/finanzas/caja/cierres",
                Map.of(
                        "desde", hoy.toString(),
                        "hasta", hoy.toString(),
                        "saldoReportado", BigDecimal.ZERO,
                        "observaciones", "Cierre para numeracion"
                ),
                token
        );

        assertThat(venta.path("numero").asText()).isEqualTo("V-000001");
        assertThat(compra.path("numero").asText()).isEqualTo("C-000001");
        assertThat(pagoVenta.path("numero").asText()).isEqualTo("PV-000001");
        assertThat(pagoCompra.path("numero").asText()).isEqualTo("PC-000001");
        assertThat(devolucionVenta.path("numero").asText()).isEqualTo("DV-000001");
        assertThat(devolucionCompra.path("numero").asText()).isEqualTo("DC-000001");
        assertThat(cierreCaja.path("numero").asText()).isEqualTo("CC-000001");
        assertThat(secuenciaDocumentoRepository.count()).isEqualTo(7L);

        assertListadoPorNumero("/api/v1/ventas?numero=v-000001", token, "V-000001");
        assertListadoPorNumero("/api/v1/compras?numero=c-000001", token, "C-000001");
        assertListadoPorNumero("/api/v1/ventas/" + ventaId + "/pagos?numero=pv-000001", token, "PV-000001");
        assertListadoPorNumero("/api/v1/compras/" + compraId + "/pagos?numero=pc-000001", token, "PC-000001");
        assertListadoPorNumero("/api/v1/ventas/" + ventaId + "/devoluciones?numero=dv-000001", token, "DV-000001");
        assertListadoPorNumero(
                "/api/v1/compras/" + compraId + "/devoluciones?numero=dc-000001",
                token,
                "DC-000001"
        );
        assertListadoPorNumero("/api/v1/finanzas/caja/cierres?numero=cc-000001", token, "CC-000001");
    }

    private String obtenerTokenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bootstrap-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD,
                                "nombre", "Administrador Numeracion"
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

    private Long crearCategoria(String token) throws Exception {
        return postJsonSinKey(
                "/api/v1/categorias",
                Map.of(
                        "nombre", "Numeracion",
                        "descripcion", "Categoria para correlativos"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProducto(String token, Long categoriaId) throws Exception {
        return postJsonSinKey(
                "/api/v1/productos",
                Map.of(
                        "nombre", "Producto Numeracion",
                        "descripcion", "Producto para correlativos",
                        "precioVenta", new BigDecimal("10.00"),
                        "stockActual", 10,
                        "stockMinimo", 1,
                        "categoriaId", categoriaId
                ),
                token
        ).path("id").asLong();
    }

    private Long crearCliente(String token) throws Exception {
        return postJsonSinKey(
                "/api/v1/clientes",
                Map.of(
                        "nombre", "Cliente Numeracion",
                        "documentoIdentidad", "CLI-NUM-1",
                        "telefono", "999000111",
                        "email", "cliente.numeracion@test.local",
                        "direccion", "Direccion de prueba"
                ),
                token
        ).path("id").asLong();
    }

    private Long crearProveedor(String token) throws Exception {
        return postJsonSinKey(
                "/api/v1/proveedores",
                Map.of(
                        "nombre", "Proveedor Numeracion",
                        "documentoIdentidad", "PROV-NUM-1",
                        "telefono", "999000222",
                        "email", "proveedor.numeracion@test.local",
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

    private void assertListadoPorNumero(String url, String token, String numeroEsperado) throws Exception {
        JsonNode pagina = jsonNode(mockMvc.perform(get(url)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(pagina.path("totalElementos").asLong()).isEqualTo(1L);
        assertThat(pagina.path("contenido").get(0).path("numero").asText()).isEqualTo(numeroEsperado);
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
