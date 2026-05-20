package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.finanzas.MetodoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    @Query("""
            SELECT c
            FROM CierreCaja c
            WHERE (:numero IS NULL OR c.numero = :numero)
              AND (:desde IS NULL OR c.fechaDesde >= :desde)
              AND (:hasta IS NULL OR c.fechaHasta <= :hasta)
            """)
    Page<CierreCaja> buscar(
            @Param("numero") String numero,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(c)
            FROM CierreCaja c
            WHERE c.fechaDesde <= :hasta
              AND c.fechaHasta >= :desde
            """)
    long contarSolapados(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
            SELECT COUNT(c)
            FROM CierreCaja c
            WHERE c.fechaDesde <= :fecha
              AND c.fechaHasta >= :fecha
            """)
    long contarQueIncluyenFecha(@Param("fecha") LocalDate fecha);

    @Query("""
            SELECT c
            FROM CierreCaja c
            WHERE (:desde IS NULL OR c.fechaDesde >= :desde)
              AND (:hasta IS NULL OR c.fechaHasta <= :hasta)
              AND (:metodoPago IS NULL OR EXISTS (
                    SELECT 1
                    FROM CierreCajaMetodoPago m
                    WHERE m.cierreCaja = c
                      AND m.metodoPago = :metodoPago
              ))
              AND (:soloConDiferencia = false OR (
                    (:metodoPago IS NULL AND (
                        c.diferencia <> :montoCero
                        OR EXISTS (
                            SELECT 1
                            FROM CierreCajaMetodoPago md
                            WHERE md.cierreCaja = c
                              AND md.diferencia <> :montoCero
                        )
                    ))
                    OR (:metodoPago IS NOT NULL AND EXISTS (
                        SELECT 1
                        FROM CierreCajaMetodoPago md
                        WHERE md.cierreCaja = c
                          AND md.metodoPago = :metodoPago
                          AND md.diferencia <> :montoCero
                    ))
              ))
            """)
    Page<CierreCaja> buscarDiferencias(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("soloConDiferencia") boolean soloConDiferencia,
            @Param("montoCero") BigDecimal montoCero,
            Pageable pageable
    );
}
