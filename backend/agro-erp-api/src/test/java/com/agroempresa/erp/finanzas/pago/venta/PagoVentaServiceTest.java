package com.agroempresa.erp.finanzas.pago.venta;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.MovimientoCajaService;
import com.agroempresa.erp.finanzas.pago.venta.dto.PagoVentaResponse;
import com.agroempresa.erp.finanzas.pago.venta.dto.RegistrarPagoVentaRequest;
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
class PagoVentaServiceTest {

    @Mock
    private PagoVentaRepository pagoVentaRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private MovimientoCajaService movimientoCajaService;

    @InjectMocks
    private PagoVentaService pagoVentaService;

    @Test
    void registrarPagoParcialActualizaSaldoDeVenta() {
        Venta venta = crearVentaConTotal(1L, 2);
        RegistrarPagoVentaRequest request = new RegistrarPagoVentaRequest(
                new BigDecimal("100.00"),
                MetodoPago.TRANSFERENCIA,
                "OP-123"
        );

        when(ventaRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.save(any(PagoVenta.class))).thenAnswer(invocation -> {
            PagoVenta pagoVenta = invocation.getArgument(0);
            ReflectionTestUtils.setField(pagoVenta, "id", 30L);
            return pagoVenta;
        });

        PagoVentaResponse response = pagoVentaService.registrar(1L, request);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.ventaId()).isEqualTo(1L);
        assertThat(response.monto()).isEqualByComparingTo("100.00");
        assertThat(venta.getTotalPagado()).isEqualByComparingTo("100.00");
        assertThat(venta.getSaldoPendiente()).isEqualByComparingTo("140.00");
        assertThat(venta.getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);
        verify(movimientoCajaService).registrarIngresoPorPagoVenta(any(PagoVenta.class));
    }

    @Test
    void registrarPagoCompletoMarcaVentaComoPagada() {
        Venta venta = crearVentaConTotal(1L, 2);
        RegistrarPagoVentaRequest request = new RegistrarPagoVentaRequest(
                new BigDecimal("240.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(ventaRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.save(any(PagoVenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pagoVentaService.registrar(1L, request);

        assertThat(venta.getTotalPagado()).isEqualByComparingTo("240.00");
        assertThat(venta.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(venta.getEstadoPago()).isEqualTo(EstadoPago.PAGADA);
        verify(movimientoCajaService).registrarIngresoPorPagoVenta(any(PagoVenta.class));
    }

    @Test
    void registrarPagoRechazaSobrepago() {
        Venta venta = crearVentaConTotal(1L, 2);
        RegistrarPagoVentaRequest request = new RegistrarPagoVentaRequest(
                new BigDecimal("241.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(ventaRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> pagoVentaService.registrar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El pago no puede superar el saldo pendiente");

        assertThat(venta.getTotalPagado()).isEqualByComparingTo("0.00");
        assertThat(venta.getSaldoPendiente()).isEqualByComparingTo("240.00");
        assertThat(venta.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        verifyNoInteractions(pagoVentaRepository);
        verifyNoInteractions(movimientoCajaService);
    }

    @Test
    void registrarPagoRechazaVentaCancelada() {
        Venta venta = crearVentaConTotal(1L, 2);
        venta.cancelar();
        RegistrarPagoVentaRequest request = new RegistrarPagoVentaRequest(
                new BigDecimal("10.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(ventaRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> pagoVentaService.registrar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No se puede registrar pagos para una venta cancelada");

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        verifyNoInteractions(pagoVentaRepository);
        verifyNoInteractions(movimientoCajaService);
    }

    private Venta crearVentaConTotal(Long ventaId, Integer cantidad) {
        Cliente cliente = new Cliente("Agro Cliente", "12345678", null, null, null);
        Producto producto = new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                10,
                2,
                new Categoria("Fertilizantes", null)
        );
        Venta venta = new Venta(cliente);
        venta.agregarDetalle(producto, cantidad);
        ReflectionTestUtils.setField(venta, "id", ventaId);
        return venta;
    }
}
