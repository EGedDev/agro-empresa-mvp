package com.agroempresa.erp.finanzas.cartera;

import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.Venta;
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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final LocalDateTime FECHA_INICIO_SISTEMA = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_FIN_SISTEMA = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

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
        String numeroNormalizado = NumeroDocumento.normalizarFiltro(numero);

        return PaginaResponse.desde(
                ventaRepository.findAll(
                        construirFiltroCuentasPorCobrar(
                                numeroNormalizado,
                                clienteId,
                                estadoPago,
                                inicioDia(desde),
                                inicioDiaPosterior(hasta),
                                venceDesde,
                                venceHasta,
                                vencida,
                                fechaReferencia
                        ),
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
        String numeroNormalizado = NumeroDocumento.normalizarFiltro(numero);

        return PaginaResponse.desde(
                compraRepository.findAll(
                        construirFiltroCuentasPorPagar(
                                numeroNormalizado,
                                proveedorId,
                                estadoPago,
                                inicioDia(desde),
                                inicioDiaPosterior(hasta),
                                venceDesde,
                                venceHasta,
                                vencida,
                                fechaReferencia
                        ),
                        Paginacion.crear(page, size, sort, CAMPOS_ORDENABLES_PAGAR, ORDEN_PAGAR_DEFAULT)
                ),
                CuentaPorPagarResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public ResumenCarteraResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = inicioDiaOInicioSistema(desde);
        LocalDateTime finExclusivo = inicioDiaPosteriorOFinSistema(hasta);
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

    private Specification<Venta> construirFiltroCuentasPorCobrar(
            String numero,
            Long clienteId,
            EstadoPago estadoPago,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo,
            LocalDate venceDesde,
            LocalDate venceHasta,
            Boolean vencida,
            LocalDate fechaReferencia
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("estado"), EstadoVenta.REGISTRADA));
            predicates.add(root.get("estadoPago").in(ESTADOS_ABIERTOS));
            predicates.add(criteriaBuilder.greaterThan(root.get("saldoPendiente"), BigDecimal.ZERO));

            if (numero != null) {
                predicates.add(criteriaBuilder.equal(root.get("numero"), numero));
            }

            if (clienteId != null) {
                predicates.add(criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }

            if (estadoPago != null) {
                predicates.add(criteriaBuilder.equal(root.get("estadoPago"), estadoPago));
            }

            if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaVenta"), desde));
            }

            if (hastaExclusivo != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("fechaVenta"), hastaExclusivo));
            }

            if (venceDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaVencimiento"), venceDesde));
            }

            if (venceHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaVencimiento"), venceHasta));
            }

            if (Boolean.TRUE.equals(vencida)) {
                predicates.add(criteriaBuilder.lessThan(root.get("fechaVencimiento"), fechaReferencia));
            } else if (Boolean.FALSE.equals(vencida)) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaVencimiento"), fechaReferencia));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
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

    private Specification<Compra> construirFiltroCuentasPorPagar(
            String numero,
            Long proveedorId,
            EstadoPago estadoPago,
            LocalDateTime desde,
            LocalDateTime hastaExclusivo,
            LocalDate venceDesde,
            LocalDate venceHasta,
            Boolean vencida,
            LocalDate fechaReferencia
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("estado"), EstadoCompra.REGISTRADA));
            predicates.add(root.get("estadoPago").in(ESTADOS_ABIERTOS));
            predicates.add(criteriaBuilder.greaterThan(root.get("saldoPendiente"), BigDecimal.ZERO));

            if (numero != null) {
                predicates.add(criteriaBuilder.equal(root.get("numero"), numero));
            }

            if (proveedorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("proveedor").get("id"), proveedorId));
            }

            if (estadoPago != null) {
                predicates.add(criteriaBuilder.equal(root.get("estadoPago"), estadoPago));
            }

            if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCompra"), desde));
            }

            if (hastaExclusivo != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("fechaCompra"), hastaExclusivo));
            }

            if (venceDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaVencimiento"), venceDesde));
            }

            if (venceHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaVencimiento"), venceHasta));
            }

            if (Boolean.TRUE.equals(vencida)) {
                predicates.add(criteriaBuilder.lessThan(root.get("fechaVencimiento"), fechaReferencia));
            } else if (Boolean.FALSE.equals(vencida)) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaVencimiento"), fechaReferencia));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
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

    private LocalDateTime inicioDiaOInicioSistema(LocalDate fecha) {
        return fecha == null ? FECHA_INICIO_SISTEMA : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosteriorOFinSistema(LocalDate fecha) {
        return fecha == null ? FECHA_FIN_SISTEMA : fecha.plusDays(1).atStartOfDay();
    }
}
