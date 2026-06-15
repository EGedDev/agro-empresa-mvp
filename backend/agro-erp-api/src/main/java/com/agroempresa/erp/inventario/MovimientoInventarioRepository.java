package com.agroempresa.erp.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {

    @Query("""
            SELECT COUNT(m)
            FROM MovimientoInventario m
            WHERE m.tipo IN :tipos
              AND m.creadoEn >= :desde
              AND m.creadoEn < :hastaExclusivo
            """)
    long contarPorTiposYPeriodo(
            @Param("tipos") Collection<TipoMovimientoInventario> tipos,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(m.cantidad)
            FROM MovimientoInventario m
            WHERE m.tipo IN :tipos
              AND m.creadoEn >= :desde
              AND m.creadoEn < :hastaExclusivo
            """)
    Long sumarCantidadPorTiposYPeriodo(
            @Param("tipos") Collection<TipoMovimientoInventario> tipos,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(m.valorMovimiento), 0)
            FROM MovimientoInventario m
            WHERE m.tipo IN :tipos
              AND m.creadoEn >= :desde
              AND m.creadoEn < :hastaExclusivo
            """)
    BigDecimal sumarValorMovimientoPorTiposYPeriodo(
            @Param("tipos") Collection<TipoMovimientoInventario> tipos,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
