package com.agroempresa.erp.finanzas.cartera;

import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.numeracion.NumeroDocumento;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.finanzas.cartera.dto.CuentaPorCobrarResponse;
import com.agroempresa.erp.finanzas.cartera.dto.CuentaPorPagarResponse;
import com.agroempresa.erp.finanzas.cartera.dto.ResumenCarteraItem;
import com.agroempresa.erp.finanzas.cartera.dto.ResumenCarteraResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CarteraService {

    private static final int ESCALA_MONETARIA = 2;
    private static final List<EstadoPago> ESTADOS_ABIERTOS = List.of(EstadoPago.PENDIENTE, EstadoPago.PARCIAL);

    private static final Map<String, String> CAMPOS_ORDENABLES_COBRAR = Map.of(
            "id", "id",
            "numero", "numero",
            "fecha", "fechaVenta",
            "fechaVencimiento", "fechaVencimiento",
            "total", "total",
            "totalPagado", "totalPagado",
            "saldoPendiente", "saldoPendiente",
            "estadoPago", "estadoPago",
            "creadoEn", "creadoEn"
    );

    private static final Map<String, String> CAMPOS_ORDENABLES_PAGAR = Map.of(
            "id", "id",
            "numero", "numero",
            "fecha", "fechaCompra",
            "fechaVencimiento", "fechaVencimiento",
            "total", "total",
            "totalPagado", "totalPagado",
            "saldoPendiente", "saldoPendiente",
            "estadoPago", "estadoPago",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_COBRAR_DEFAULT = Sort.by(Sort.Direction.ASC, "fechaVencimiento");
    private static final Sort ORDEN_PAGAR_DEFAULT = Sort.by(Sort.Direction.ASC, "fechaVencimiento");

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;

    public CarteraService(VentaRepository ventaRepository, CompraRepository compraRepository) {
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CuentaPorCobrarResponse> listarCuentasPorCobrar(
            String numero,
            Long clienteId,
            EstadoPago estadoPago,
            LocalDate desde,
            LocalDate hasta,
            LocalDate venceDesde,
            LocalDate venceHasta,
            Boolean vencida,
            Integer page,
            Integer size,
            String sort
    ) {
        validarConsulta(estadoPago, desde, hasta, venceDesde, venceHasta);
        LocalDate fechaReferencia = LocalDate.now();

        return PaginaResponse.desde(
                ventaRepository.buscarCuentasPorCobrar(
                        EstadoVenta.REGISTRADA,
                        ESTADOS_ABIERTOS,
                        NumeroDocumento.normalizarFiltro(numero),
                        clienteId,
                        estadoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        venceDesde,
                        venceHasta,
                        vencida,
                        fechaReferencia,
                        Paginacion.crear(page, size, sort, CAMPOS_ORDENABLES_COBRAR, ORDEN_COBRAR_DEFAULT)
                ),
                CuentaPorCobrarResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CuentaPorPagarResponse> listarCuentasPorPagar(
            String numero,
            Long proveedorId,
            EstadoPago estadoPago,
            LocalDate desde,
            LocalDate hasta,
            LocalDate venceDesde,
            LocalDate venceHasta,
            Boolean vencida,
            Integer page,
            Integer size,
            String sort
    ) {
        validarConsulta(estadoPago, desde, hasta, venceDesde, venceHasta);
        LocalDate fechaReferencia = LocalDate.now();

        return PaginaResponse.desde(
                compraRepository.buscarCuentasPorPagar(
                        EstadoCompra.REGISTRADA,
                        ESTADOS_ABIERTOS,
                        NumeroDocumento.normalizarFiltro(numero),
                        proveedorId,
                        estadoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        venceDesde,
                        venceHasta,
                        vencida,
                        fechaReferencia,
                        Paginacion.crear(page, size, sort, CAMPOS_ORDENABLES_PAGAR, ORDEN_PAGAR_DEFAULT)
                ),
                CuentaPorPagarResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public ResumenCarteraResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = inicioDia(desde);
        LocalDateTime finExclusivo = inicioDiaPosterior(hasta);
        LocalDate fechaReferencia = LocalDate.now();
        ResumenCarteraItem cuentasPorCobrar = resumenCuentasPorCobrar(inicio, finExclusivo, fechaReferencia);
        ResumenCarteraItem cuentasPorPagar = resumenCuentasPorPagar(inicio, finExclusivo, fechaReferencia);

        return new ResumenCarteraResponse(
                desde,
                hasta,
                cuentasPorCobrar,
                cuentasPorPagar,
                cuentasPorCobrar.saldoPendiente()
                        .subtract(cuentasPorPagar.saldoPendiente())
                        .setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY),
                LocalDateTime.now()
        );
    }

    private ResumenCarteraItem resumenCuentasPorCobrar(
            LocalDateTime desde,
            LocalDateTime hastaExclusivo,
            LocalDate fechaReferencia
    ) {
        long cantidadTotal = ventaRepository.contarCuentasPorCobrar(
                EstadoVenta.REGISTRADA,
                ESTADOS_ABIERTOS,
                desde,
                hastaExclusivo
        );
        BigDecimal saldoTotal = monto(ventaRepository.sumarCuentasPorCobrar(
                EstadoVenta.REGISTRADA,
                ESTADOS_ABIERTOS,
                desde,
                hastaExclusivo
        ));
        long cantidadVencida = ventaRepository.contarCuentasPorCobrarVencidas(
                EstadoVenta.REGISTRADA,
                ESTADOS_ABIERTOS,
                fechaReferencia,
                desde,
                hastaExclusivo
        );
        BigDecimal saldoVencido = monto(ventaRepository.sumarCuentasPorCobrarVencidas(
                EstadoVenta.REGISTRADA,
                ESTADOS_ABIERTOS,
                fechaReferencia,
                desde,
                hastaExclusivo
        ));

        return resumen(cantidadTotal, saldoTotal, cantidadVencida, saldoVencido);
    }

    private ResumenCarteraItem resumenCuentasPorPagar(
            LocalDateTime desde,
            LocalDateTime hastaExclusivo,
            LocalDate fechaReferencia
    ) {
        long cantidadTotal = compraRepository.contarCuentasPorPagar(
                EstadoCompra.REGISTRADA,
                ESTADOS_ABIERTOS,
                desde,
                hastaExclusivo
        );
        BigDecimal saldoTotal = monto(compraRepository.sumarCuentasPorPagar(
                EstadoCompra.REGISTRADA,
                ESTADOS_ABIERTOS,
                desde,
                hastaExclusivo
        ));
        long cantidadVencida = compraRepository.contarCuentasPorPagarVencidas(
                EstadoCompra.REGISTRADA,
                ESTADOS_ABIERTOS,
                fechaReferencia,
                desde,
                hastaExclusivo
        );
        BigDecimal saldoVencido = monto(compraRepository.sumarCuentasPorPagarVencidas(
                EstadoCompra.REGISTRADA,
                ESTADOS_ABIERTOS,
                fechaReferencia,
                desde,
                hastaExclusivo
        ));

        return resumen(cantidadTotal, saldoTotal, cantidadVencida, saldoVencido);
    }

    private ResumenCarteraItem resumen(
            long cantidadTotal,
            BigDecimal saldoTotal,
            long cantidadVencida,
            BigDecimal saldoVencido
    ) {
        return new ResumenCarteraItem(
                cantidadTotal,
                saldoTotal,
                cantidadVencida,
                saldoVencido,
                cantidadTotal - cantidadVencida,
                saldoTotal.subtract(saldoVencido).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY)
        );
    }

    private void validarConsulta(
            EstadoPago estadoPago,
            LocalDate desde,
            LocalDate hasta,
            LocalDate venceDesde,
            LocalDate venceHasta
    ) {
        validarEstadoAbierto(estadoPago);
        validarRangoFechas(desde, hasta);
        validarRangoVencimiento(venceDesde, venceHasta);
    }

    private void validarEstadoAbierto(EstadoPago estadoPago) {
        if (estadoPago != null && !ESTADOS_ABIERTOS.contains(estadoPago)) {
            throw new BusinessException("El estado de pago para cartera debe ser PENDIENTE o PARCIAL");
        }
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private void validarRangoVencimiento(LocalDate venceDesde, LocalDate venceHasta) {
        if (venceDesde != null && venceHasta != null && venceHasta.isBefore(venceDesde)) {
            throw new BusinessException("La fecha final de vencimiento no puede ser anterior a la fecha inicial");
        }
    }

    private BigDecimal monto(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        return monto.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
    }

    private LocalDateTime inicioDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosterior(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay();
    }
}
