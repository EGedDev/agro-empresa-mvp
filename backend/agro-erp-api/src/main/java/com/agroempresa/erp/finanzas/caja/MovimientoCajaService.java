package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.dto.MovimientoCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenCajaPorMetodoPagoResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenMetodoPagoCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenMovimientoCaja;
import com.agroempresa.erp.finanzas.pago.compra.PagoCompra;
import com.agroempresa.erp.finanzas.pago.venta.PagoVenta;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoCajaService {

    private static final int ESCALA_MONETARIA = 2;

    private static final String REFERENCIA_PAGO_VENTA = "PAGO_VENTA";
    private static final String REFERENCIA_PAGO_COMPRA = "PAGO_COMPRA";
    private static final String REFERENCIA_REVERSO_PAGO_VENTA = "REVERSO_PAGO_VENTA";
    private static final String REFERENCIA_REVERSO_PAGO_COMPRA = "REVERSO_PAGO_COMPRA";
    private static final int LONGITUD_MAXIMA_REFERENCIA = 120;

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "tipo", "tipo",
            "monto", "monto",
            "metodoPago", "metodoPago",
            "fechaMovimiento", "fechaMovimiento",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaMovimiento");

    private final MovimientoCajaRepository movimientoCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;

    public MovimientoCajaService(
            MovimientoCajaRepository movimientoCajaRepository,
            CierreCajaRepository cierreCajaRepository
    ) {
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.cierreCajaRepository = cierreCajaRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoCajaResponse> listar(
            TipoMovimientoCaja tipo,
            MetodoPago metodoPago,
            String referenciaTipo,
            Long referenciaId,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                movimientoCajaRepository.buscar(
                        tipo,
                        metodoPago,
                        Paginacion.normalizarTexto(referenciaTipo),
                        referenciaId,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                MovimientoCajaResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRangoFechasObligatorias(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        ResumenMovimientoCaja ingresos = resumenPorTipo(TipoMovimientoCaja.INGRESO, inicio, finExclusivo);
        ResumenMovimientoCaja egresos = resumenPorTipo(TipoMovimientoCaja.EGRESO, inicio, finExclusivo);

        return new ResumenCajaResponse(
                desde,
                hasta,
                ingresos,
                egresos,
                ingresos.total().subtract(egresos.total()).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public ResumenCajaPorMetodoPagoResponse obtenerResumenPorMetodoPago(LocalDate desde, LocalDate hasta) {
        validarRangoFechasObligatorias(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        Map<MetodoPago, AcumuladoMetodoPago> acumulados = new EnumMap<>(MetodoPago.class);

        movimientoCajaRepository.resumirPorMetodoPagoYTipo(inicio, finExclusivo)
                .forEach(fila -> acumulados
                        .computeIfAbsent(fila.getMetodoPago(), metodoPago -> new AcumuladoMetodoPago())
                        .agregar(fila.getTipo(), fila.getCantidadMovimientos(), monto(fila.getTotal()))
                );

        List<ResumenMetodoPagoCajaResponse> metodos = new ArrayList<>();
        BigDecimal saldoNeto = monto(BigDecimal.ZERO);

        for (MetodoPago metodoPago : MetodoPago.values()) {
            AcumuladoMetodoPago acumulado = acumulados.get(metodoPago);

            if (acumulado == null) {
                continue;
            }

            BigDecimal saldoMetodo = saldoNeto(acumulado.ingresosTotal, acumulado.egresosTotal);
            saldoNeto = saldoNeto.add(saldoMetodo);
            metodos.add(new ResumenMetodoPagoCajaResponse(
                    metodoPago,
                    new ResumenMovimientoCaja(acumulado.ingresosCantidad, acumulado.ingresosTotal),
                    new ResumenMovimientoCaja(acumulado.egresosCantidad, acumulado.egresosTotal),
                    saldoMetodo
            ));
        }

        return new ResumenCajaPorMetodoPagoResponse(
                desde,
                hasta,
                metodos,
                monto(saldoNeto),
                LocalDateTime.now()
        );
    }

    @Transactional
    public MovimientoCaja registrarIngresoPorPagoVenta(PagoVenta pagoVenta) {
        return registrarMovimiento(
                TipoMovimientoCaja.INGRESO,
                pagoVenta.getMonto(),
                pagoVenta.getMetodoPago(),
                pagoVenta.getReferencia(),
                REFERENCIA_PAGO_VENTA,
                pagoVenta.getId(),
                pagoVenta.getFechaPago()
        );
    }

    @Transactional
    public MovimientoCaja registrarEgresoPorPagoCompra(PagoCompra pagoCompra) {
        return registrarMovimiento(
                TipoMovimientoCaja.EGRESO,
                pagoCompra.getMonto(),
                pagoCompra.getMetodoPago(),
                pagoCompra.getReferencia(),
                REFERENCIA_PAGO_COMPRA,
                pagoCompra.getId(),
                pagoCompra.getFechaPago()
        );
    }

    @Transactional
    public MovimientoCaja registrarReversoIngresoPorPagoVenta(PagoVenta pagoVenta) {
        return registrarMovimiento(
                TipoMovimientoCaja.EGRESO,
                pagoVenta.getMonto(),
                pagoVenta.getMetodoPago(),
                referenciaAnulacion(pagoVenta.getReferencia()),
                REFERENCIA_REVERSO_PAGO_VENTA,
                pagoVenta.getId(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public MovimientoCaja registrarReversoEgresoPorPagoCompra(PagoCompra pagoCompra) {
        return registrarMovimiento(
                TipoMovimientoCaja.INGRESO,
                pagoCompra.getMonto(),
                pagoCompra.getMetodoPago(),
                referenciaAnulacion(pagoCompra.getReferencia()),
                REFERENCIA_REVERSO_PAGO_COMPRA,
                pagoCompra.getId(),
                LocalDateTime.now()
        );
    }

    private MovimientoCaja registrarMovimiento(
            TipoMovimientoCaja tipo,
            BigDecimal monto,
            MetodoPago metodoPago,
            String referencia,
            String referenciaTipo,
            Long referenciaId,
            LocalDateTime fechaMovimiento
    ) {
        validarPeriodoAbierto(fechaMovimiento);

        MovimientoCaja movimientoCaja = new MovimientoCaja(
                tipo,
                monto,
                metodoPago,
                referencia,
                referenciaTipo,
                referenciaId,
                fechaMovimiento
        );

        return movimientoCajaRepository.save(movimientoCaja);
    }

    private void validarPeriodoAbierto(LocalDateTime fechaMovimiento) {
        if (fechaMovimiento == null) {
            throw new BusinessException("La fecha del movimiento de caja es obligatoria");
        }

        if (cierreCajaRepository.contarQueIncluyenFecha(fechaMovimiento.toLocalDate()) > 0) {
            throw new BusinessException("No se pueden registrar movimientos en un periodo de caja cerrado");
        }
    }

    private ResumenMovimientoCaja resumenPorTipo(
            TipoMovimientoCaja tipo,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo
    ) {
        return new ResumenMovimientoCaja(
                movimientoCajaRepository.contarPorTipoYPeriodo(tipo, desde, hastaExclusivo),
                monto(movimientoCajaRepository.sumarMontoPorTipoYPeriodo(tipo, desde, hastaExclusivo))
        );
    }

    private BigDecimal monto(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return monto.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
    }

    private BigDecimal saldoNeto(BigDecimal ingresos, BigDecimal egresos) {
        return ingresos.subtract(egresos).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
    }

    private String referenciaAnulacion(String referenciaOriginal) {
        String referencia = referenciaOriginal == null || referenciaOriginal.isBlank()
                ? "Anulacion de pago"
                : "Anulacion: " + referenciaOriginal.trim();

        if (referencia.length() <= LONGITUD_MAXIMA_REFERENCIA) {
            return referencia;
        }

        return referencia.substring(0, LONGITUD_MAXIMA_REFERENCIA);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private void validarRangoFechasObligatorias(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }

        validarRangoFechas(desde, hasta);
    }

    private LocalDateTime inicioDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosterior(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay();
    }

    private static final class AcumuladoMetodoPago {
        private long ingresosCantidad;
        private BigDecimal ingresosTotal = BigDecimal.ZERO.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
        private long egresosCantidad;
        private BigDecimal egresosTotal = BigDecimal.ZERO.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);

        private void agregar(TipoMovimientoCaja tipo, Long cantidad, BigDecimal total) {
            long cantidadMovimientos = cantidad == null ? 0L : cantidad;

            if (tipo == TipoMovimientoCaja.INGRESO) {
                ingresosCantidad += cantidadMovimientos;
                ingresosTotal = ingresosTotal.add(total);
                return;
            }

            egresosCantidad += cantidadMovimientos;
            egresosTotal = egresosTotal.add(total);
        }
    }
}
