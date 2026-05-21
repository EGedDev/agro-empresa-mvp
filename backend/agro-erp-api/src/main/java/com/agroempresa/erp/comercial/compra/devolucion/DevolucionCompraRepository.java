package com.agroempresa.erp.comercial.compra.devolucion;

import com.agroempresa.erp.reportes.dto.AcumuladoProductoReporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DevolucionCompraRepository extends JpaRepository<DevolucionCompra, Long> {

    @Query("""
            SELECT d
            FROM DevolucionCompra d
            WHERE d.compra.id = :compraId
              AND (:numero IS NULL OR d.numero = :numero)
            """)
    Page<DevolucionCompra> buscarPorCompra(
            @Param("compraId") Long compraId,
            @Param("numero") String numero,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(d.cantidad), 0)
            FROM DevolucionCompraDetalle d
            WHERE d.compraDetalle.id = :compraDetalleId
            """)
    Long sumarCantidadDevueltaPorDetalle(@Param("compraDetalleId") Long compraDetalleId);

    @Query("""
            SELECT COALESCE(SUM(d.total), 0)
            FROM DevolucionCompra d
            WHERE d.fechaDevolucion >= :desde
              AND d.fechaDevolucion < :hastaExclusivo
            """)
    BigDecimal sumarTotalPorPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT new com.agroempresa.erp.reportes.dto.AcumuladoProductoReporte(
                d.producto.id,
                d.producto.nombre,
                COALESCE(SUM(d.cantidad), 0),
                COALESCE(SUM(d.subtotal), 0)
            )
            FROM DevolucionCompraDetalle d
            WHERE d.devolucionCompra.fechaDevolucion >= :desde
              AND d.devolucionCompra.fechaDevolucion < :hastaExclusivo
            GROUP BY d.producto.id, d.producto.nombre
            """)
    List<AcumuladoProductoReporte> resumirDevolucionesPorProducto(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
