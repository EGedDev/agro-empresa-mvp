package com.agroempresa.erp.reportes;

import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.finanzas.pago.compra.PagoCompraRepository;
import com.agroempresa.erp.finanzas.pago.venta.PagoVentaRepository;
import com.agroempresa.erp.reportes.dto.ResumenFinancieroResponse;
import com.agroempresa.erp.reportes.dto.ResumenOperacionesFinancieras;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReporteFinancieroService {

    private static final int ESCALA_MONETARIA = 2;

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final PagoCompraRepository pagoCompraRepository;
    private final DevolucionVentaRepository devolucionVentaRepository;
    private final DevolucionCompraRepository devolucionCompraRepository;

    public ReporteFinancieroService(
            VentaRepository ventaRepository,
            CompraRepository compraRepository,
            PagoVentaRepository pagoVentaRepository,
            PagoCompraRepository pagoCompraRepository,
            DevolucionVentaRepository devolucionVentaRepository,
            DevolucionCompraRepository devolucionCompraRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.pagoCompraRepository = pagoCompraRepository;
        this.devolucionVentaRepository = devolucionVentaRepository;
        this.devolucionCompraRepository = devolucionCompraRepository;
    }

    @Transactional(readOnly = true)
    public ResumenFinancieroResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        ResumenOperacionesFinancieras ventas = new ResumenOperacionesFinancieras(
                ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.REGISTRADA, inicio, finExclusivo),
                monto(ventaRepository.sumarTotalPorEstadoYPeriodo(EstadoVenta.REGISTRADA, inicio, finExclusivo)),
                monto(ventaRepository.sumarSaldoPendientePorEstadoYPeriodo(EstadoVenta.REGISTRADA, inicio, finExclusivo))
        );

        ResumenOperacionesFinancieras compras = new ResumenOperacionesFinancieras(
                compraRepository.contarPorEstadoYPeriodo(EstadoCompra.REGISTRADA, inicio, finExclusivo),
                monto(compraRepository.sumarTotalPorEstadoYPeriodo(EstadoCompra.REGISTRADA, inicio, finExclusivo)),
                monto(compraRepository.sumarSaldoPendientePorEstadoYPeriodo(EstadoCompra.REGISTRADA, inicio, finExclusivo))
        );

        BigDecimal cobrosRecibidos = monto(pagoVentaRepository.sumarMontoPorPeriodo(inicio, finExclusivo));
        BigDecimal pagosRealizados = monto(pagoCompraRepository.sumarMontoPorPeriodo(inicio, finExclusivo));
        BigDecimal devolucionesVenta = monto(devolucionVentaRepository.sumarTotalPorPeriodo(inicio, finExclusivo));
        BigDecimal devolucionesCompra = monto(devolucionCompraRepository.sumarTotalPorPeriodo(inicio, finExclusivo));

        return new ResumenFinancieroResponse(
                desde,
                hasta,
                ventas,
                compras,
                devolucionesVenta,
                devolucionesCompra,
                cobrosRecibidos,
                pagosRealizados,
                cobrosRecibidos.subtract(pagosRealizados).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY),
                LocalDateTime.now()
        );
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }

        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private BigDecimal monto(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return monto.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
    }
}
