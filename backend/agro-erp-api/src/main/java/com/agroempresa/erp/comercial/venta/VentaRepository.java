package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.finanzas.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
            SELECT v
            FROM Venta v
            WHERE (:numero IS NULL OR v.numero = :numero)
              AND (:clienteId IS NULL OR v.cliente.id = :clienteId)
              AND (:estado IS NULL OR v.estado = :estado)
              AND (:estadoPago IS NULL OR v.estadoPago = :estadoPago)
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
            """)
    Page<Venta> buscar(
            @Param("numero") String numero,
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("estadoPago") EstadoPago estadoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> findByIdParaActualizar(@Param("id") Long id);

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fechaVenta >= :desde
              AND v.fechaVenta < :hastaExclusivo
            """)
    long contarPorEstadoYPeriodo(
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(v.total), 0)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fechaVenta >= :desde
              AND v.fechaVenta < :hastaExclusivo
            """)
    BigDecimal sumarTotalPorEstadoYPeriodo(
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COALESCE(SUM(v.saldoPendiente), 0)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fechaVenta >= :desde
              AND v.fechaVenta < :hastaExclusivo
            """)
    BigDecimal sumarSaldoPendientePorEstadoYPeriodo(
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.estado = :estado
              AND v.estadoPago IN :estadosPago
              AND v.saldoPendiente > 0
              AND (:numero IS NULL OR v.numero = :numero)
              AND (:clienteId IS NULL OR v.cliente.id = :clienteId)
              AND (:estadoPago IS NULL OR v.estadoPago = :estadoPago)
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
              AND (:venceDesde IS NULL OR v.fechaVencimiento >= :venceDesde)
              AND (:venceHasta IS NULL OR v.fechaVencimiento <= :venceHasta)
              AND (
                    :vencida IS NULL
                    OR (:vencida = true AND v.fechaVencimiento < :fechaReferencia)
                    OR (:vencida = false AND v.fechaVencimiento >= :fechaReferencia)
              )
            """)
    Page<Venta> buscarCuentasPorCobrar(
            @Param("estado") EstadoVenta estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("numero") String numero,
            @Param("clienteId") Long clienteId,
            @Param("estadoPago") EstadoPago estadoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            @Param("venceDesde") LocalDate venceDesde,
            @Param("venceHasta") LocalDate venceHasta,
            @Param("vencida") Boolean vencida,
            @Param("fechaReferencia") LocalDate fechaReferencia,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.estadoPago IN :estadosPago
              AND v.saldoPendiente > 0
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
            """)
    long contarCuentasPorCobrar(
            @Param("estado") EstadoVenta estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(v.saldoPendiente)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.estadoPago IN :estadosPago
              AND v.saldoPendiente > 0
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
            """)
    BigDecimal sumarCuentasPorCobrar(
            @Param("estado") EstadoVenta estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.estadoPago IN :estadosPago
              AND v.saldoPendiente > 0
              AND v.fechaVencimiento < :fechaReferencia
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
            """)
    long contarCuentasPorCobrarVencidas(
            @Param("estado") EstadoVenta estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("fechaReferencia") LocalDate fechaReferencia,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(v.saldoPendiente)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.estadoPago IN :estadosPago
              AND v.saldoPendiente > 0
              AND v.fechaVencimiento < :fechaReferencia
              AND (:desde IS NULL OR v.fechaVenta >= :desde)
              AND (:hastaExclusivo IS NULL OR v.fechaVenta < :hastaExclusivo)
            """)
    BigDecimal sumarCuentasPorCobrarVencidas(
            @Param("estado") EstadoVenta estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("fechaReferencia") LocalDate fechaReferencia,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
