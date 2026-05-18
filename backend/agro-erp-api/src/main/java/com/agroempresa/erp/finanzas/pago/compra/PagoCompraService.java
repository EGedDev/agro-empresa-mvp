package com.agroempresa.erp.finanzas.pago.compra;

import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.compra.dto.PagoCompraResponse;
import com.agroempresa.erp.finanzas.pago.compra.dto.RegistrarPagoCompraRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PagoCompraService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "fechaPago", "fechaPago",
            "monto", "monto",
            "metodoPago", "metodoPago",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaPago");

    private final PagoCompraRepository pagoCompraRepository;
    private final CompraRepository compraRepository;
    private final AuditoriaService auditoriaService;

    public PagoCompraService(
            PagoCompraRepository pagoCompraRepository,
            CompraRepository compraRepository,
            AuditoriaService auditoriaService
    ) {
        this.pagoCompraRepository = pagoCompraRepository;
        this.compraRepository = compraRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PagoCompraResponse> listarPorCompra(
            Long compraId,
            MetodoPago metodoPago,
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        if (compraId == null) {
            throw new BusinessException("La compra es obligatoria");
        }

        if (!compraRepository.existsById(compraId)) {
            throw new RecursoNoEncontradoException("No se encontró la compra con id: " + compraId);
        }

        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                pagoCompraRepository.buscarPorCompra(
                        compraId,
                        metodoPago,
                        inicioDia(desde),
                        inicioDiaPosterior(hasta),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                PagoCompraResponse::desdeEntidad
        );
    }

    @Transactional
    public PagoCompraResponse registrar(Long compraId, RegistrarPagoCompraRequest request) {
        if (compraId == null) {
            throw new BusinessException("La compra es obligatoria");
        }

        validarRequest(request);

        Compra compra = compraRepository.findByIdParaActualizar(compraId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la compra con id: " + compraId
                ));

        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("No se puede registrar pagos para una compra cancelada");
        }

        try {
            compra.registrarPago(request.monto());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }

        PagoCompra pagoCompra = new PagoCompra(
                compra,
                request.monto(),
                request.metodoPago(),
                normalizar(request.referencia())
        );

        PagoCompra pagoGuardado = pagoCompraRepository.save(pagoCompra);

        auditoriaService.registrar(
                "PAGO_COMPRA_REGISTRADO",
                "COMPRA",
                compra.getId(),
                "Monto: " + pagoGuardado.getMonto()
        );

        return PagoCompraResponse.desdeEntidad(pagoGuardado);
    }

    private void validarRequest(RegistrarPagoCompraRequest request) {
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
