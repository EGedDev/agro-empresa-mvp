package com.agroempresa.erp.inventario;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.inventario.dto.MovimientoInventarioResponse;
import com.agroempresa.erp.inventario.dto.RegistrarMovimientoInventarioRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private InventarioService inventarioService;

    @Test
    void registrarMovimientoManualConAjusteNegativoDescuentaStockYRegistraMovimiento() {
        Producto producto = crearProductoConStock(10);
        RegistrarMovimientoInventarioRequest request = new RegistrarMovimientoInventarioRequest(
                1L,
                TipoMovimientoInventario.AJUSTE_NEGATIVO,
                3,
                null,
                "Ajuste por conteo fisico"
        );

        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(producto));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioResponse response = inventarioService.registrarMovimientoManual(request);

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());

        MovimientoInventario movimiento = movimientoCaptor.getValue();

        assertThat(producto.getStockActual()).isEqualTo(7);
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoInventario.AJUSTE_NEGATIVO);
        assertThat(movimiento.getStockAnterior()).isEqualTo(10);
        assertThat(movimiento.getStockNuevo()).isEqualTo(7);
        assertThat(movimiento.getCostoUnitario()).isEqualByComparingTo(new BigDecimal("8.5000"));
        assertThat(movimiento.getValorMovimiento()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(movimiento.getValorInventarioAnterior()).isEqualByComparingTo(new BigDecimal("85.00"));
        assertThat(movimiento.getValorInventarioNuevo()).isEqualByComparingTo(new BigDecimal("59.50"));
        assertThat(response.stockNuevo()).isEqualTo(7);
        assertThat(response.valorMovimiento()).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    void registrarMovimientoManualRechazaTiposReservadosParaProcesosInternos() {
        Producto producto = crearProductoConStock(10);
        RegistrarMovimientoInventarioRequest request = new RegistrarMovimientoInventarioRequest(
                1L,
                TipoMovimientoInventario.SALIDA_POR_VENTA,
                2,
                null,
                "Intento no permitido"
        );

        when(productoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> inventarioService.registrarMovimientoManual(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tipo de movimiento no permitido para registro manual");
    }

    private Producto crearProductoConStock(Integer stockActual) {
        Categoria categoria = new Categoria("Fertilizantes", null);

        return new Producto(
                "Urea",
                null,
                BigDecimal.valueOf(120),
                stockActual,
                2,
                categoria,
                new BigDecimal("8.50")
        );
    }
}
