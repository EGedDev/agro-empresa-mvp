package com.agroempresa.erp.reportes;

import com.agroempresa.erp.comercial.compra.CompraRepository;
import com.agroempresa.erp.comercial.compra.EstadoCompra;
import com.agroempresa.erp.comercial.compra.devolucion.DevolucionCompraRepository;
import com.agroempresa.erp.comercial.venta.EstadoVenta;
import com.agroempresa.erp.comercial.venta.VentaRepository;
import com.agroempresa.erp.comercial.venta.devolucion.DevolucionVentaRepository;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.reportes.dto.AcumuladoProductoReporte;
import com.agroempresa.erp.reportes.dto.ResumenComprasProductoResponse;
import com.agroempresa.erp.reportes.dto.ResumenComprasProveedorResponse;
import com.agroempresa.erp.reportes.dto.ResumenVentasClienteResponse;
import com.agroempresa.erp.reportes.dto.ResumenVentasProductoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteGerencialService {

    private static final int ESCALA_MONETARIA = 2;
    private static final int LIMITE_DEFAULT = 10;
    private static final int LIMITE_MAXIMO = 100;

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final DevolucionVentaRepository devolucionVentaRepository;
    private final DevolucionCompraRepository devolucionCompraRepository;

    public ReporteGerencialService(
            VentaRepository ventaRepository,
            CompraRepository compraRepository,
            DevolucionVentaRepository devolucionVentaRepository,
            DevolucionCompraRepository devolucionCompraRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.devolucionVentaRepository = devolucionVentaRepository;
        this.devolucionCompraRepository = devolucionCompraRepository;
    }

    @Transactional(readOnly = true)
    public List<ResumenVentasClienteResponse> ventasPorCliente(LocalDate desde, LocalDate hasta, Integer limite) {
        validarRangoFechas(desde, hasta);
        Periodo periodo = periodo(desde, hasta);

        return ventaRepository.resumirVentasPorCliente(EstadoVenta.REGISTRADA, periodo.inicio(), periodo.finExclusivo())
                .stream()
                .map(this::normalizar)
                .sorted(Comparator
                        .comparing(ResumenVentasClienteResponse::total)
                        .reversed()
                        .thenComparing(ResumenVentasClienteResponse::clienteNombre))
                .limit(normalizarLimite(limite))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResumenVentasProductoResponse> ventasPorProducto(LocalDate desde, LocalDate hasta, Integer limite) {
        validarRangoFechas(desde, hasta);
        Periodo periodo = periodo(desde, hasta);
        Map<Long, AcumuladoProductoGerencial> acumulados = new LinkedHashMap<>();

        for (AcumuladoProductoReporte vendido : ventaRepository.resumirVentasPorProducto(
                EstadoVenta.REGISTRADA,
                periodo.inicio(),
                periodo.finExclusivo()
        )) {
            acumulados.computeIfAbsent(vendido.productoId(), id -> crearAcumulado(vendido))
                    .registrarBruto(vendido.unidades(), vendido.total());
        }

        for (AcumuladoProductoReporte devuelto : devolucionVentaRepository.resumirDevolucionesPorProducto(
                periodo.inicio(),
                periodo.finExclusivo()
        )) {
            acumulados.computeIfAbsent(devuelto.productoId(), id -> crearAcumulado(devuelto))
                    .registrarDevolucion(devuelto.unidades(), devuelto.total());
        }

        return acumulados.values()
                .stream()
                .map(AcumuladoProductoGerencial::toVentasResponse)
                .sorted(Comparator
                        .comparing(ResumenVentasProductoResponse::totalNeto)
                        .reversed()
                        .thenComparing(ResumenVentasProductoResponse::productoNombre))
                .limit(normalizarLimite(limite))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResumenComprasProveedorResponse> comprasPorProveedor(LocalDate desde, LocalDate hasta, Integer limite) {
        validarRangoFechas(desde, hasta);
        Periodo periodo = periodo(desde, hasta);

        return compraRepository.resumirComprasPorProveedor(
                        EstadoCompra.REGISTRADA,
                        periodo.inicio(),
                        periodo.finExclusivo()
                )
                .stream()
                .map(this::normalizar)
                .sorted(Comparator
                        .comparing(ResumenComprasProveedorResponse::total)
                        .reversed()
                        .thenComparing(ResumenComprasProveedorResponse::proveedorNombre))
                .limit(normalizarLimite(limite))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResumenComprasProductoResponse> comprasPorProducto(LocalDate desde, LocalDate hasta, Integer limite) {
        validarRangoFechas(desde, hasta);
        Periodo periodo = periodo(desde, hasta);
        Map<Long, AcumuladoProductoGerencial> acumulados = new LinkedHashMap<>();

        for (AcumuladoProductoReporte comprado : compraRepository.resumirComprasPorProducto(
                EstadoCompra.REGISTRADA,
                periodo.inicio(),
                periodo.finExclusivo()
        )) {
            acumulados.computeIfAbsent(comprado.productoId(), id -> crearAcumulado(comprado))
                    .registrarBruto(comprado.unidades(), comprado.total());
        }

        for (AcumuladoProductoReporte devuelto : devolucionCompraRepository.resumirDevolucionesPorProducto(
                periodo.inicio(),
                periodo.finExclusivo()
        )) {
            acumulados.computeIfAbsent(devuelto.productoId(), id -> crearAcumulado(devuelto))
                    .registrarDevolucion(devuelto.unidades(), devuelto.total());
        }

        return acumulados.values()
                .stream()
                .map(AcumuladoProductoGerencial::toComprasResponse)
                .sorted(Comparator
                        .comparing(ResumenComprasProductoResponse::totalNeto)
                        .reversed()
                        .thenComparing(ResumenComprasProductoResponse::productoNombre))
                .limit(normalizarLimite(limite))
                .toList();
    }

    private ResumenVentasClienteResponse normalizar(ResumenVentasClienteResponse resumen) {
        return new ResumenVentasClienteResponse(
                resumen.clienteId(),
                resumen.clienteNombre(),
                resumen.ventas(),
                monto(resumen.total()),
                monto(resumen.saldoPendiente())
        );
    }

    private ResumenComprasProveedorResponse normalizar(ResumenComprasProveedorResponse resumen) {
        return new ResumenComprasProveedorResponse(
                resumen.proveedorId(),
                resumen.proveedorNombre(),
                resumen.compras(),
                monto(resumen.total()),
                monto(resumen.saldoPendiente())
        );
    }

    private AcumuladoProductoGerencial crearAcumulado(AcumuladoProductoReporte acumulado) {
        return new AcumuladoProductoGerencial(acumulado.productoId(), acumulado.productoNombre());
    }

    private Periodo periodo(LocalDate desde, LocalDate hasta) {
        return new Periodo(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private int normalizarLimite(Integer limite) {
        if (limite == null) {
            return LIMITE_DEFAULT;
        }

        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new BusinessException("El limite debe estar entre 1 y 100");
        }

        return limite;
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
        return (valor == null ? BigDecimal.ZERO : valor).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private long cantidad(Long valor) {
        return valor == null ? 0L : valor;
    }

    private record Periodo(
            LocalDateTime inicio,
            LocalDateTime finExclusivo
    ) {
    }

    private final class AcumuladoProductoGerencial {

        private final Long productoId;
        private final String productoNombre;
        private long unidadesBrutas;
        private long unidadesDevueltas;
        private BigDecimal totalBruto = BigDecimal.ZERO;
        private BigDecimal totalDevuelto = BigDecimal.ZERO;

        private AcumuladoProductoGerencial(Long productoId, String productoNombre) {
            this.productoId = productoId;
            this.productoNombre = productoNombre;
        }

        private void registrarBruto(Long unidades, BigDecimal total) {
            this.unidadesBrutas += cantidad(unidades);
            this.totalBruto = this.totalBruto.add(monto(total));
        }

        private void registrarDevolucion(Long unidades, BigDecimal total) {
            this.unidadesDevueltas += cantidad(unidades);
            this.totalDevuelto = this.totalDevuelto.add(monto(total));
        }

        private ResumenVentasProductoResponse toVentasResponse() {
            return new ResumenVentasProductoResponse(
                    this.productoId,
                    this.productoNombre,
                    this.unidadesBrutas,
                    this.unidadesDevueltas,
                    this.unidadesBrutas - this.unidadesDevueltas,
                    monto(this.totalBruto),
                    monto(this.totalDevuelto),
                    monto(this.totalBruto.subtract(this.totalDevuelto))
            );
        }

        private ResumenComprasProductoResponse toComprasResponse() {
            return new ResumenComprasProductoResponse(
                    this.productoId,
                    this.productoNombre,
                    this.unidadesBrutas,
                    this.unidadesDevueltas,
                    this.unidadesBrutas - this.unidadesDevueltas,
                    monto(this.totalBruto),
                    monto(this.totalDevuelto),
                    monto(this.totalBruto.subtract(this.totalDevuelto))
            );
        }
    }
}
