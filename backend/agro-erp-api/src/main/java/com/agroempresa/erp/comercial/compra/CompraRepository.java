package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.finanzas.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("""
            SELECT c
            FROM Compra c
            WHERE (:proveedorId IS NULL OR c.proveedor.id = :proveedorId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:estadoPago IS NULL OR c.estadoPago = :estadoPago)
              AND (:desde IS NULL OR c.fechaCompra >= :desde)
              AND (:hastaExclusivo IS NULL OR c.fechaCompra < :hastaExclusivo)
            """)
    Page<Compra> buscar(
            @Param("proveedorId") Long proveedorId,
            @Param("estado") EstadoCompra estado,
            @Param("estadoPago") EstadoPago estadoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.id = :id")
    Optional<Compra> findByIdParaActualizar(@Param("id") Long id);

    @Query("""
            SELECT COUNT(c)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    long contarPorEstadoYPeriodo(
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(c.total), 0)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    BigDecimal sumarTotalPorEstadoYPeriodo(
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(c.saldoPendiente), 0)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    BigDecimal sumarSaldoPendientePorEstadoYPeriodo(
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
