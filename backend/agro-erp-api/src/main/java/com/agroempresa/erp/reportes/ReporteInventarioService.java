package com.agroempresa.erp.reportes;

import com.agroempresa.erp.catalogo.producto.ProductoRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.inventario.MovimientoInventarioRepository;
import com.agroempresa.erp.inventario.TipoMovimientoInventario;
import com.agroempresa.erp.reportes.dto.ResumenInventarioResponse;
import com.agroempresa.erp.reportes.dto.ResumenMovimientosInventario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ReporteInventarioService {

    private static final Set<TipoMovimientoInventario> TIPOS_ENTRADA = Set.of(
            TipoMovimientoInventario.ENTRADA_MANUAL,
            TipoMovimientoInventario.ENTRADA_POR_CANCELACION,
            TipoMovimientoInventario.ENTRADA_POR_COMPRA,
            TipoMovimientoInventario.AJUSTE_POSITIVO
    );

    private static final Set<TipoMovimientoInventario> TIPOS_SALIDA = Set.of(
            TipoMovimientoInventario.SALIDA_POR_VENTA,
            TipoMovimientoInventario.SALIDA_POR_CANCELACION_COMPRA,
            TipoMovimientoInventario.AJUSTE_NEGATIVO
    );

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public ReporteInventarioService(
            ProductoRepository productoRepository,
            MovimientoInventarioRepository movimientoInventarioRepository
    ) {
        this.productoRepository = productoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional(readOnly = true)
    public ResumenInventarioResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        ResumenMovimientosInventario entradas = resumenMovimientos(TIPOS_ENTRADA, inicio, finExclusivo);
        ResumenMovimientosInventario salidas = resumenMovimientos(TIPOS_SALIDA, inicio, finExclusivo);

        return new ResumenInventarioResponse(
                desde,
                hasta,
                productoRepository.countByActivoTrue(),
                productoRepository.contarActivosConStockBajo(),
                entradas,
                salidas,
                entradas.unidades() - salidas.unidades(),
                LocalDateTime.now()
        );
    }

    private ResumenMovimientosInventario resumenMovimientos(
            Set<TipoMovimientoInventario> tipos,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo
    ) {
        return new ResumenMovimientosInventario(
                movimientoInventarioRepository.contarPorTiposYPeriodo(tipos, desde, hastaExclusivo),
                cantidad(movimientoInventarioRepository.sumarCantidadPorTiposYPeriodo(tipos, desde, hastaExclusivo))
        );
    }

    private long cantidad(Long valor) {
        return valor == null ? 0L : valor;
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }

        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }
}
