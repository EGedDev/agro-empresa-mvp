package com.agroempresa.erp.finanzas.pago.venta;

import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.MovimientoCajaService;
import com.agroempresa.erp.finanzas.pago.venta.dto.PagoVentaResponse;
import com.agroempresa.erp.finanzas.pago.venta.dto.RegistrarPagoVentaRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PagoVentaService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "fechaPago", "fechaPago",
            "monto", "monto",
            "metodoPago", "metodoPago",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaPago");

    private final PagoVentaRepository pagoVentaRepository;
    private final VentaRepository ventaRepository;
    private final AuditoriaService auditoriaService;
    private final MovimientoCajaService movimientoCajaService;

    public PagoVentaService(
            PagoVentaRepository pagoVentaRepository,
            VentaRepository ventaRepository,
            AuditoriaService auditoriaService,
            MovimientoCajaService movimientoCajaService
    ) {
        this.pagoVentaRepository = pagoVentaRepository;
        this.ventaRepository = ventaRepository;
        this.auditoriaService = auditoriaService;
        this.movimientoCajaService = movimientoCajaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PagoVentaResponse> listarPorVenta(
            Long ventaId,
            MetodoPago metodoPago,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (ventaId == null) {
            throw new BusinessException("La venta es obligatoria");
        }

        if (!ventaRepository.existsById(ventaId)) {
            throw new RecursoNoEncontradoException("No se encontró la venta con id: " + ventaId);
        }

        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                pagoVentaRepository.buscarPorVenta(
                        ventaId,
                        metodoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                PagoVentaResponse::desdeEntidad
        );
    }

    @Transactional
    public PagoVentaResponse registrar(Long ventaId, RegistrarPagoVentaRequest request) {
        if (ventaId == null) {
            throw new BusinessException("La venta es obligatoria");
        }

        validarRequest(request);

        Venta venta = ventaRepository.findByIdParaActualizar(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la venta con id: " + ventaId
                ));

        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new BusinessException("No se puede registrar pagos para una venta cancelada");
        }

        try {
            venta.registrarPago(request.monto());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }

        PagoVenta pagoVenta = new PagoVenta(
                venta,
                request.monto(),
                request.metodoPago(),
                normalizar(request.referencia())
        );

        PagoVenta pagoGuardado = pagoVentaRepository.save(pagoVenta);

        auditoriaService.registrar(
                "PAGO_VENTA_REGISTRADO",
                "VENTA",
                venta.getId(),
                "Monto: " + pagoGuardado.getMonto()
        );

        movimientoCajaService.registrarIngresoPorPagoVenta(pagoGuardado);

        return PagoVentaResponse.desdeEntidad(pagoGuardado);
    }

    private void validarRequest(RegistrarPagoVentaRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del pago son obligatorios");
        }

        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto del pago debe ser mayor a cero");
        }

        if (request.metodoPago() == null) {
            throw new BusinessException("El método de pago es obligatorio");
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private LocalDateTime inicioDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    private LocalDateTime inicioDiaPosterior(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay();
    }
}
