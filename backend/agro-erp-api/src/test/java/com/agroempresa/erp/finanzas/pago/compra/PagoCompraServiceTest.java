package com.agroempresa.erp.finanzas.pago.compra;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.numeracion.NumeracionService;
import com.agroempresa.erp.common.numeracion.TipoDocumento;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.MovimientoCajaService;
import com.agroempresa.erp.finanzas.pago.compra.dto.PagoCompraResponse;
import com.agroempresa.erp.finanzas.pago.compra.dto.RegistrarPagoCompraRequest;
import com.agroempresa.erp.proveedor.Proveedor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoCompraServiceTest {

    @Mock
    private PagoCompraRepository pagoCompraRepository;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private MovimientoCajaService movimientoCajaService;

    @Mock
    private NumeracionService numeracionService;

    @InjectMocks
    private PagoCompraService pagoCompraService;

    @Test
    void registrarPagoParcialActualizaSaldoDeCompra() {
        Compra compra = crearCompraConTotal(1L, 3);
        RegistrarPagoCompraRequest request = new RegistrarPagoCompraRequest(
                new BigDecimal("30.00"),
                MetodoPago.TRANSFERENCIA,
                "OP-456"
        );

        when(compraRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(compra));
        when(numeracionService.generar(TipoDocumento.PAGO_COMPRA)).thenReturn("PC-000001");
        when(pagoCompraRepository.save(any(PagoCompra.class))).thenAnswer(invocation -> {
            PagoCompra pagoCompra = invocation.getArgument(0);
            ReflectionTestUtils.setField(pagoCompra, "id", 40L);
            return pagoCompra;
        });

        PagoCompraResponse response = pagoCompraService.registrar(1L, request);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.numero()).isEqualTo("PC-000001");
        assertThat(response.compraId()).isEqualTo(1L);
        assertThat(response.monto()).isEqualByComparingTo("30.00");
        assertThat(compra.getTotalPagado()).isEqualByComparingTo("30.00");
        assertThat(compra.getSaldoPendiente()).isEqualByComparingTo("60.00");
        assertThat(compra.getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);
        verify(movimientoCajaService).registrarEgresoPorPagoCompra(any(PagoCompra.class));
    }

    @Test
    void registrarPagoCompletoMarcaCompraComoPagada() {
        Compra compra = crearCompraConTotal(1L, 3);
        RegistrarPagoCompraRequest request = new RegistrarPagoCompraRequest(
                new BigDecimal("90.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(compraRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(compra));
        when(numeracionService.generar(TipoDocumento.PAGO_COMPRA)).thenReturn("PC-000002");
        when(pagoCompraRepository.save(any(PagoCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pagoCompraService.registrar(1L, request);

        assertThat(compra.getTotalPagado()).isEqualByComparingTo("90.00");
        assertThat(compra.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(compra.getEstadoPago()).isEqualTo(EstadoPago.PAGADA);
        verify(movimientoCajaService).registrarEgresoPorPagoCompra(any(PagoCompra.class));
    }

    @Test
    void registrarPagoRechazaSobrepago() {
        Compra compra = crearCompraConTotal(1L, 3);
        RegistrarPagoCompraRequest request = new RegistrarPagoCompraRequest(
                new BigDecimal("91.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(compraRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> pagoCompraService.registrar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El pago no puede superar el saldo pendiente");

        assertThat(compra.getTotalPagado()).isEqualByComparingTo("0.00");
        assertThat(compra.getSaldoPendiente()).isEqualByComparingTo("90.00");
        assertThat(compra.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        verifyNoInteractions(pagoCompraRepository);
        verifyNoInteractions(movimientoCajaService);
    }

    @Test
    void registrarPagoRechazaCompraCancelada() {
        Compra compra = crearCompraConTotal(1L, 3);
        compra.cancelar();
        RegistrarPagoCompraRequest request = new RegistrarPagoCompraRequest(
                new BigDecimal("10.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(compraRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> pagoCompraService.registrar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede registrar pagos para una compra cancelada");

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.CANCELADA);
        verifyNoInteractions(pagoCompraRepository);
        verifyNoInteractions(movimientoCajaService);
    }

    private Compra crearCompraConTotal(Long compraId, Integer cantidad) {
        Proveedor proveedor = new Proveedor("Proveedor Agro", "20123456789", null, null, null);
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                10,
                2,
                new Categoria("Fertilizantes", null)
        );
        Compra compra = new Compra(proveedor);
        compra.agregarDetalle(producto, cantidad, new BigDecimal("30.00"));
        ReflectionTestUtils.setField(compra, "id", compraId);
        return compra;
    }
}
