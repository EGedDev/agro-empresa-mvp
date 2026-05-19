package com.agroempresa.erp.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("""
            SELECT m
            FROM MovimientoInventario m
            WHERE (:productoId IS NULL OR m.producto.id = :productoId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND (:referenciaTipo IS NULL OR lower(coalesce(m.referenciaTipo, '')) = :referenciaTipo)
              AND (:desde IS NULL OR m.creadoEn >= :desde)
              AND (:hastaExclusivo IS NULL OR m.creadoEn < :hastaExclusivo)
            """)
    Page<MovimientoInventario> buscar(
            @Param("productoId") Long productoId,
            @Param("tipo") TipoMovimientoInventario tipo,
            @Param("referenciaTipo") String referenciaTipo,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

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
}
