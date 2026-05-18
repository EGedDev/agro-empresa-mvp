package com.agroempresa.erp.finanzas.pago.venta;

import com.agroempresa.erp.finanzas.MetodoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {

    @Query("""
            SELECT p
            FROM PagoVenta p
            WHERE p.venta.id = :ventaId
              AND (:metodoPago IS NULL OR p.metodoPago = :metodoPago)
              AND (:desde IS NULL OR p.fechaPago >= :desde)
              AND (:hastaExclusivo IS NULL OR p.fechaPago < :hastaExclusivo)
            """)
    Page<PagoVenta> buscarPorVenta(
            @Param("ventaId") Long ventaId,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM PagoVenta p
            WHERE p.fechaPago >= :desde
              AND p.fechaPago < :hastaExclusivo
            """)
    BigDecimal sumarMontoPorPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
