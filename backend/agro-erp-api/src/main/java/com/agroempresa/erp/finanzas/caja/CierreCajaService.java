package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.auditoria.AuditoriaService;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.dto.CierreCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.DiferenciaCierreCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.RegistrarCierreCajaRequest;
import com.agroempresa.erp.finanzas.caja.dto.RegistrarCierreMetodoPagoRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CierreCajaService {

    private static final int ESCALA_MONETARIA = 2;
    private static final BigDecimal MONTO_CERO = BigDecimal.ZERO.setScale(
            ESCALA_MONETARIA,
            RoundingMode.UNNECESSARY
    );
    private static final String USUARIO_SISTEMA = "sistema";

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "desde", "fechaDesde",
            "hasta", "fechaHasta",
            "saldoCalculado", "saldoCalculado",
            "saldoReportado", "saldoReportado",
            "diferencia", "diferencia",
            "creadoEn", "creadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.DESC, "fechaHasta");

    private final CierreCajaRepository cierreCajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final AuditoriaService auditoriaService;

    public CierreCajaService(
            CierreCajaRepository cierreCajaRepository,
            MovimientoCajaRepository movimientoCajaRepository,
            AuditoriaService auditoriaService
    ) {
        this.cierreCajaRepository = cierreCajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CierreCajaResponse> listar(
            LocalDate desde,
            LocalDate hasta,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarRangoFechas(desde, hasta);

        return PaginaResponse.desde(
                cierreCajaRepository.buscar(
                        desde,
                        hasta,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                CierreCajaResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public CierreCajaResponse obtenerPorId(Long id) {
        return CierreCajaResponse.desdeEntidad(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<DiferenciaCierreCajaResponse> listarDiferencias(
            LocalDate desde,
            LocalDate hasta,
            MetodoPago metodoPago,
            Boolean soloConDiferencia,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        validarRangoFechas(desde, hasta);

        boolean filtrarSoloConDiferencia = soloConDiferencia == null || soloConDiferencia;

        return PaginaResponse.desde(
                cierreCajaRepository.buscarDiferencias(
                        desde,
                        hasta,
                        metodoPago,
                        filtrarSoloConDiferencia,
                        MONTO_CERO,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                cierreCaja -> DiferenciaCierreCajaResponse.desdeEntidad(
                        cierreCaja,
                        metodoPago,
                        filtrarSoloConDiferencia
                )
        );
    }

    @Transactional
    public CierreCajaResponse registrar(RegistrarCierreCajaRequest request) {
        validarRequest(request);
        validarPeriodoDisponible(request.desde(), request.hasta());

        LocalDateTime inicio = request.desde().atStartOfDay();
        LocalDateTime finExclusivo = request.hasta().plusDays(1).atStartOfDay();
        ResumenPeriodoCaja resumen = calcularResumen(inicio, finExclusivo);
        BigDecimal saldoReportado = monto(request.saldoReportado());
        Map<MetodoPago, BigDecimal> saldosReportadosPorMetodo = saldosReportadosPorMetodo(request.metodos());
        validarSaldosReportadosPorMetodo(saldosReportadosPorMetodo, saldoReportado, resumen);

        CierreCaja cierreCaja = new CierreCaja(
                request.desde(),
                request.hasta(),
                resumen.cantidadIngresos(),
                resumen.cantidadEgresos(),
                resumen.totalIngresos(),
                resumen.totalEgresos(),
                resumen.saldoCalculado(),
                saldoReportado,
                normalizarObservaciones(request.observaciones()),
                obtenerUsernameActual()
        );
        agregarDetallePorMetodo(cierreCaja, resumen, saldosReportadosPorMetodo);

        CierreCaja cierreGuardado = guardar(cierreCaja);

        auditoriaService.registrar(
                "CIERRE_CAJA_REGISTRADO",
                "CIERRE_CAJA",
                cierreGuardado.getId(),
                "Periodo: " + cierreGuardado.getFechaDesde()
                        + " a " + cierreGuardado.getFechaHasta()
                        + ", saldo calculado: " + cierreGuardado.getSaldoCalculado()
                        + ", saldo reportado: " + cierreGuardado.getSaldoReportado()
        );

        return CierreCajaResponse.desdeEntidad(cierreGuardado);
    }

    private CierreCaja guardar(CierreCaja cierreCaja) {
        try {
            return cierreCajaRepository.save(cierreCaja);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Ya existe un cierre de caja para el periodo informado");
        }
    }

    private CierreCaja buscarPorId(Long id) {
        return cierreCajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro el cierre de caja con id: " + id
                ));
    }

    private void validarRequest(RegistrarCierreCajaRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del cierre de caja son obligatorios");
        }

        if (request.desde() == null || request.hasta() == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }

        validarRangoFechas(request.desde(), request.hasta());

        if (request.saldoReportado() == null) {
            throw new BusinessException("El saldo reportado es obligatorio");
        }

        monto(request.saldoReportado());
    }

    private void validarPeriodoDisponible(LocalDate desde, LocalDate hasta) {
        if (cierreCajaRepository.contarSolapados(desde, hasta) > 0) {
            throw new BusinessException("Ya existe un cierre de caja que se solapa con el periodo informado");
        }
    }

    private ResumenPeriodoCaja calcularResumen(LocalDateTime inicio, LocalDateTime finExclusivo) {
        Map<MetodoPago, ResumenMetodoCaja> metodos = new EnumMap<>(MetodoPago.class);
        movimientoCajaRepository.resumirPorMetodoPagoYTipo(inicio, finExclusivo)
                .forEach(fila -> metodos
                        .computeIfAbsent(fila.getMetodoPago(), metodoPago -> new ResumenMetodoCaja())
                        .agregar(fila.getTipo(), fila.getCantidadMovimientos(), monto(fila.getTotal()))
                );

        long cantidadIngresos = metodos.values().stream()
                .mapToLong(ResumenMetodoCaja::cantidadIngresos)
                .sum();
        long cantidadEgresos = metodos.values().stream()
                .mapToLong(ResumenMetodoCaja::cantidadEgresos)
                .sum();
        BigDecimal totalIngresos = metodos.values().stream()
                .map(ResumenMetodoCaja::totalIngresos)
                .reduce(MONTO_CERO, BigDecimal::add);
        BigDecimal totalEgresos = metodos.values().stream()
                .map(ResumenMetodoCaja::totalEgresos)
                .reduce(MONTO_CERO, BigDecimal::add);

        return new ResumenPeriodoCaja(
                cantidadIngresos,
                cantidadEgresos,
                totalIngresos,
                totalEgresos,
                saldoNeto(totalIngresos, totalEgresos),
                metodos
        );
    }

    private Map<MetodoPago, BigDecimal> saldosReportadosPorMetodo(List<RegistrarCierreMetodoPagoRequest> metodos) {
        Map<MetodoPago, BigDecimal> saldos = new EnumMap<>(MetodoPago.class);

        if (metodos == null || metodos.isEmpty()) {
            return saldos;
        }

        for (RegistrarCierreMetodoPagoRequest metodo : metodos) {
            if (metodo == null) {
                throw new BusinessException("Los datos del metodo de pago son obligatorios");
            }

            if (metodo.metodoPago() == null) {
                throw new BusinessException("El metodo de pago es obligatorio");
            }

            if (metodo.saldoReportado() == null) {
                throw new BusinessException("El saldo reportado por metodo es obligatorio");
            }

            BigDecimal saldoAnterior = saldos.put(metodo.metodoPago(), monto(metodo.saldoReportado()));

            if (saldoAnterior != null) {
                throw new BusinessException("No se puede repetir el metodo de pago en el cierre de caja");
            }
        }

        return saldos;
    }

    private void validarSaldosReportadosPorMetodo(
            Map<MetodoPago, BigDecimal> saldosReportadosPorMetodo,
            BigDecimal saldoReportado,
            ResumenPeriodoCaja resumen
    ) {
        if (saldosReportadosPorMetodo.isEmpty()) {
            return;
        }

        for (Map.Entry<MetodoPago, ResumenMetodoCaja> resumenMetodo : resumen.metodos().entrySet()) {
            if (resumenMetodo.getValue().tieneMovimientos()
                    && !saldosReportadosPorMetodo.containsKey(resumenMetodo.getKey())) {
                throw new BusinessException(
                        "Debe reportar saldo para todos los metodos de pago con movimientos en el periodo"
                );
            }
        }

        BigDecimal sumaReportadaPorMetodo = saldosReportadosPorMetodo.values().stream()
                .reduce(MONTO_CERO, BigDecimal::add);

        if (monto(sumaReportadaPorMetodo).compareTo(saldoReportado) != 0) {
            throw new BusinessException(
                    "La suma de saldos reportados por metodo debe coincidir con el saldo reportado del cierre"
            );
        }
    }

    private void agregarDetallePorMetodo(
            CierreCaja cierreCaja,
            ResumenPeriodoCaja resumen,
            Map<MetodoPago, BigDecimal> saldosReportadosPorMetodo
    ) {
        if (saldosReportadosPorMetodo.isEmpty()) {
            return;
        }

        for (MetodoPago metodoPago : MetodoPago.values()) {
            if (!saldosReportadosPorMetodo.containsKey(metodoPago)
                    && !resumen.metodos().containsKey(metodoPago)) {
                continue;
            }

            ResumenMetodoCaja resumenMetodo = resumen.metodos().getOrDefault(metodoPago, ResumenMetodoCaja.vacio());
            cierreCaja.agregarMetodo(
                    metodoPago,
                    resumenMetodo.cantidadIngresos(),
                    resumenMetodo.cantidadEgresos(),
                    resumenMetodo.totalIngresos(),
                    resumenMetodo.totalEgresos(),
                    resumenMetodo.saldoCalculado(),
                    saldosReportadosPorMetodo.getOrDefault(metodoPago, MONTO_CERO)
            );
        }
    }

    private String obtenerUsernameActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return USUARIO_SISTEMA;
        }

        return authentication.getName();
    }

    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }

        return observaciones.trim();
    }

    private BigDecimal monto(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        try {
            return monto.setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BusinessException("Los montos deben tener como maximo 2 decimales");
        }
    }

    private BigDecimal saldoNeto(BigDecimal ingresos, BigDecimal egresos) {
        return ingresos.subtract(egresos).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial");
        }
    }

    private record ResumenPeriodoCaja(
            long cantidadIngresos,
            long cantidadEgresos,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal saldoCalculado,
            Map<MetodoPago, ResumenMetodoCaja> metodos
    ) {
    }

    private static final class ResumenMetodoCaja {
        private long cantidadIngresos;
        private long cantidadEgresos;
        private BigDecimal totalIngresos = MONTO_CERO;
        private BigDecimal totalEgresos = MONTO_CERO;

        private static ResumenMetodoCaja vacio() {
            return new ResumenMetodoCaja();
        }

        private void agregar(TipoMovimientoCaja tipo, Long cantidad, BigDecimal total) {
            long cantidadMovimientos = cantidad == null ? 0L : cantidad;

            if (tipo == TipoMovimientoCaja.INGRESO) {
                cantidadIngresos += cantidadMovimientos;
                totalIngresos = totalIngresos.add(total);
                return;
            }

            cantidadEgresos += cantidadMovimientos;
            totalEgresos = totalEgresos.add(total);
        }

        private boolean tieneMovimientos() {
            return cantidadIngresos > 0 || cantidadEgresos > 0;
        }

        private long cantidadIngresos() {
            return cantidadIngresos;
        }

        private long cantidadEgresos() {
            return cantidadEgresos;
        }

        private BigDecimal totalIngresos() {
            return totalIngresos;
        }

        private BigDecimal totalEgresos() {
            return totalEgresos;
        }

        private BigDecimal saldoCalculado() {
            return totalIngresos.subtract(totalEgresos).setScale(ESCALA_MONETARIA, RoundingMode.UNNECESSARY);
        }
    }
}
