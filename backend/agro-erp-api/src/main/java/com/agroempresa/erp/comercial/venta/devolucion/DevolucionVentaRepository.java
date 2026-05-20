package com.agroempresa.erp.comercial.venta.devolucion;

import com.agroempresa.erp.reportes.dto.AcumuladoRentabilidadProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long> {

    @Query("""
            SELECT d
            FROM DevolucionVenta d
            WHERE d.venta.id = :ventaId
              AND (:numero IS NULL OR d.numero = :numero)
            """)
    Page<DevolucionVenta> buscarPorVenta(
            @Param("ventaId") Long ventaId,
            @Param("numero") String numero,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(d.cantidad), 0)
            FROM DevolucionVentaDetalle d
            WHERE d.ventaDetalle.id = :ventaDetalleId
            """)
    Long sumarCantidadDevueltaPorDetalle(@Param("ventaDetalleId") Long ventaDetalleId);

    @Query("""
            SELECT COALESCE(SUM(d.total), 0)
            FROM DevolucionVenta d
            WHERE d.fechaDevolucion >= :desde
              AND d.fechaDevolucion < :hastaExclusivo
            """)
    BigDecimal sumarTotalPorPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(d.ventaDetalle.costoUnitario * d.cantidad), 0)
            FROM DevolucionVentaDetalle d
            WHERE d.devolucionVenta.fechaDevolucion >= :desde
              AND d.devolucionVenta.fechaDevolucion < :hastaExclusivo
            """)
    BigDecimal sumarCostoDevueltoPorPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT new com.agroempresa.erp.reportes.dto.AcumuladoRentabilidadProducto(
                d.producto.id,
                d.producto.nombre,
                COALESCE(SUM(d.cantidad), 0),
                COALESCE(SUM(d.subtotal), 0),
                COALESCE(SUM(d.ventaDetalle.costoUnitario * d.cantidad), 0)
            )
            FROM DevolucionVentaDetalle d
            WHERE d.devolucionVenta.fechaDevolucion >= :desde
              AND d.devolucionVenta.fechaDevolucion < :hastaExclusivo
            GROUP BY d.producto.id, d.producto.nombre
            """)
    List<AcumuladoRentabilidadProducto> sumarRentabilidadDevueltaPorProducto(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
