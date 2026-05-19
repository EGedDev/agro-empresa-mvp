package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.finanzas.MetodoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    @Query("""
            SELECT m
            FROM MovimientoCaja m
            WHERE (:tipo IS NULL OR m.tipo = :tipo)
              AND (:metodoPago IS NULL OR m.metodoPago = :metodoPago)
              AND (:referenciaTipo IS NULL OR lower(m.referenciaTipo) = :referenciaTipo)
              AND (:referenciaId IS NULL OR m.referenciaId = :referenciaId)
              AND (:desde IS NULL OR m.fechaMovimiento >= :desde)
              AND (:hastaExclusivo IS NULL OR m.fechaMovimiento < :hastaExclusivo)
            """)
    Page<MovimientoCaja> buscar(
            @Param("tipo") TipoMovimientoCaja tipo,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("referenciaTipo") String referenciaTipo,
            @Param("referenciaId") Long referenciaId,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(m)
            FROM MovimientoCaja m
            WHERE m.tipo = :tipo
              AND m.fechaMovimiento >= :desde
              AND m.fechaMovimiento < :hastaExclusivo
            """)
    long contarPorTipoYPeriodo(
            @Param("tipo") TipoMovimientoCaja tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(m.monto)
            FROM MovimientoCaja m
            WHERE m.tipo = :tipo
              AND m.fechaMovimiento >= :desde
              AND m.fechaMovimiento < :hastaExclusivo
            """)
    BigDecimal sumarMontoPorTipoYPeriodo(
            @Param("tipo") TipoMovimientoCaja tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT m.metodoPago AS metodoPago,
                   m.tipo AS tipo,
                   COUNT(m) AS cantidadMovimientos,
                   SUM(m.monto) AS total
            FROM MovimientoCaja m
            WHERE m.fechaMovimiento >= :desde
              AND m.fechaMovimiento < :hastaExclusivo
            GROUP BY m.metodoPago, m.tipo
            """)
    List<ResumenPorMetodoPago> resumirPorMetodoPagoYTipo(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    interface ResumenPorMetodoPago {
        MetodoPago getMetodoPago();

        TipoMovimientoCaja getTipo();

        Long getCantidadMovimientos();

        BigDecimal getTotal();
    }
}
