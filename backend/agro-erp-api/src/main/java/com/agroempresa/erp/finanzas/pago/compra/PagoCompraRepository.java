package com.agroempresa.erp.finanzas.pago.compra;

import com.agroempresa.erp.finanzas.MetodoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PagoCompraRepository extends JpaRepository<PagoCompra, Long> {

    @Query("""
            SELECT p
            FROM PagoCompra p
            WHERE p.compra.id = :compraId
              AND (:metodoPago IS NULL OR p.metodoPago = :metodoPago)
              AND (:desde IS NULL OR p.fechaPago >= :desde)
              AND (:hastaExclusivo IS NULL OR p.fechaPago < :hastaExclusivo)
            """)
    Page<PagoCompra> buscarPorCompra(
            @Param("compraId") Long compraId,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM PagoCompra p
            WHERE p.fechaPago >= :desde
              AND p.fechaPago < :hastaExclusivo
            """)
    BigDecimal sumarMontoPorPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
