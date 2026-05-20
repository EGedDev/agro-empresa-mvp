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
import com.agroempresa.erp.reportes.dto.AcumuladoRentabilidadProducto;
import com.agroempresa.erp.reportes.dto.RentabilidadProductoResponse;
import com.agroempresa.erp.reportes.dto.ResumenRentabilidadResponse;
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
public class ReporteFinancieroService {

    private static final int ESCALA_MONETARIA = 2;
    private static final int ESCALA_PORCENTAJE = 2;
    private static final int LIMITE_PRODUCTOS_DEFAULT = 10;
    private static final int LIMITE_PRODUCTOS_MAXIMO = 100;

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

    @Transactional(readOnly = true)
    public ResumenRentabilidadResponse obtenerRentabilidad(LocalDate desde, LocalDate hasta) {
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        BigDecimal ingresosBrutos = monto(ventaRepository.sumarIngresosBrutosPorPeriodo(
                EstadoVenta.REGISTRADA,
                inicio,
                finExclusivo
        ));
        BigDecimal costoVentasBruto = monto(ventaRepository.sumarCostoVentasBrutoPorPeriodo(
                EstadoVenta.REGISTRADA,
                inicio,
                finExclusivo
        ));
        BigDecimal devolucionesVenta = monto(devolucionVentaRepository.sumarTotalPorPeriodo(inicio, finExclusivo));
        BigDecimal costoDevuelto = monto(devolucionVentaRepository.sumarCostoDevueltoPorPeriodo(inicio, finExclusivo));
        BigDecimal ingresosNetos = monto(ingresosBrutos.subtract(devolucionesVenta));
        BigDecimal costoVentasNeto = monto(costoVentasBruto.subtract(costoDevuelto));
        BigDecimal utilidadBruta = monto(ingresosNetos.subtract(costoVentasNeto));

        return new ResumenRentabilidadResponse(
                desde,
                hasta,
                ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.REGISTRADA, inicio, finExclusivo),
                ingresosBrutos,
                costoVentasBruto,
                devolucionesVenta,
                costoDevuelto,
                ingresosNetos,
                costoVentasNeto,
                utilidadBruta,
                porcentaje(utilidadBruta, ingresosNetos),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<RentabilidadProductoResponse> obtenerRentabilidadPorProducto(
            LocalDate desde,
            LocalDate hasta,
            Integer limite
    ) {
        validarRangoFechas(desde, hasta);
        int limiteNormalizado = normalizarLimite(limite);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        Map<Long, RentabilidadProductoAcumulada> acumulados = new LinkedHashMap<>();

        for (AcumuladoRentabilidadProducto vendido : ventaRepository.sumarRentabilidadBrutaPorProducto(
                EstadoVenta.REGISTRADA,
                inicio,
                finExclusivo
        )) {
            RentabilidadProductoAcumulada acumulado = acumulados.computeIfAbsent(
                    vendido.productoId(),
                    id -> new RentabilidadProductoAcumulada(vendido.productoId(), vendido.productoNombre())
            );
            acumulado.registrarVenta(vendido.unidades(), vendido.ingresos(), vendido.costoVentas());
        }

        for (AcumuladoRentabilidadProducto devuelto : devolucionVentaRepository.sumarRentabilidadDevueltaPorProducto(
                inicio,
                finExclusivo
        )) {
            RentabilidadProductoAcumulada acumulado = acumulados.computeIfAbsent(
                    devuelto.productoId(),
                    id -> new RentabilidadProductoAcumulada(devuelto.productoId(), devuelto.productoNombre())
            );
            acumulado.registrarDevolucion(devuelto.unidades(), devuelto.ingresos(), devuelto.costoVentas());
        }

        return acumulados.values()
                .stream()
                .map(RentabilidadProductoAcumulada::toResponse)
                .sorted(Comparator
                        .comparing(RentabilidadProductoResponse::utilidadBruta)
                        .reversed()
                        .thenComparing(RentabilidadProductoResponse::productoNombre))
                .limit(limiteNormalizado)
                .toList();
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
        return monto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentaje(BigDecimal numerador, BigDecimal denominador) {
        if (denominador == null || denominador.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
        }

        return numerador
                .multiply(BigDecimal.valueOf(100))
                .divide(denominador, ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
    }

    private int normalizarLimite(Integer limite) {
        if (limite == null) {
            return LIMITE_PRODUCTOS_DEFAULT;
        }

        if (limite < 1 || limite > LIMITE_PRODUCTOS_MAXIMO) {
            throw new BusinessException("El limite debe estar entre 1 y 100");
        }

        return limite;
    }

    private final class RentabilidadProductoAcumulada {

        private final Long productoId;
        private final String productoNombre;
        private long unidadesVendidas;
        private long unidadesDevueltas;
        private BigDecimal ingresosBrutos = BigDecimal.ZERO;
        private BigDecimal costoVentasBruto = BigDecimal.ZERO;
        private BigDecimal ingresosDevueltos = BigDecimal.ZERO;
        private BigDecimal costoDevuelto = BigDecimal.ZERO;

        private RentabilidadProductoAcumulada(Long productoId, String productoNombre) {
            this.productoId = productoId;
            this.productoNombre = productoNombre;
        }

        private void registrarVenta(Long unidades, BigDecimal ingresos, BigDecimal costoVentas) {
            this.unidadesVendidas += cantidad(unidades);
            this.ingresosBrutos = this.ingresosBrutos.add(monto(ingresos));
            this.costoVentasBruto = this.costoVentasBruto.add(monto(costoVentas));
        }

        private void registrarDevolucion(Long unidades, BigDecimal ingresos, BigDecimal costoVentas) {
            this.unidadesDevueltas += cantidad(unidades);
            this.ingresosDevueltos = this.ingresosDevueltos.add(monto(ingresos));
            this.costoDevuelto = this.costoDevuelto.add(monto(costoVentas));
        }

        private RentabilidadProductoResponse toResponse() {
            BigDecimal ingresosNetos = monto(this.ingresosBrutos.subtract(this.ingresosDevueltos));
            BigDecimal costoVentasNeto = monto(this.costoVentasBruto.subtract(this.costoDevuelto));
            BigDecimal utilidadBruta = monto(ingresosNetos.subtract(costoVentasNeto));

            return new RentabilidadProductoResponse(
                    this.productoId,
                    this.productoNombre,
                    this.unidadesVendidas,
                    this.unidadesDevueltas,
                    this.unidadesVendidas - this.unidadesDevueltas,
                    ingresosNetos,
                    costoVentasNeto,
                    utilidadBruta,
                    porcentaje(utilidadBruta, ingresosNetos)
            );
        }
    }

    private long cantidad(Long valor) {
        return valor == null ? 0L : valor;
    }
}
