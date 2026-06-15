package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.reportes.dto.AcumuladoProductoReporte;
import com.agroempresa.erp.reportes.dto.ResumenComprasProveedorResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    @Query("""
            SELECT c
            FROM Compra c
            WHERE (:numero IS NULL OR c.numero = :numero)
              AND (:proveedorId IS NULL OR c.proveedor.id = :proveedorId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:estadoPago IS NULL OR c.estadoPago = :estadoPago)
              AND (:desde IS NULL OR c.fechaCompra >= :desde)
              AND (:hastaExclusivo IS NULL OR c.fechaCompra < :hastaExclusivo)
            """)
    Page<Compra> buscar(
            @Param("numero") String numero,
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

    @Query("""
            SELECT new com.agroempresa.erp.reportes.dto.ResumenComprasProveedorResponse(
                c.proveedor.id,
                c.proveedor.nombre,
                COUNT(c),
                COALESCE(SUM(c.total), 0),
                COALESCE(SUM(c.saldoPendiente), 0)
            )
            FROM Compra c
            WHERE c.estado = :estado
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            GROUP BY c.proveedor.id, c.proveedor.nombre
            """)
    List<ResumenComprasProveedorResponse> resumirComprasPorProveedor(
            @Param("estado") EstadoCompra estado,
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
            FROM CompraDetalle d
            WHERE d.compra.estado = :estado
              AND d.compra.fechaCompra >= :desde
              AND d.compra.fechaCompra < :hastaExclusivo
            GROUP BY d.producto.id, d.producto.nombre
            """)
    List<AcumuladoProductoReporte> resumirComprasPorProducto(
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT c
            FROM Compra c
            WHERE c.estado = :estado
              AND c.estadoPago IN :estadosPago
              AND c.saldoPendiente > 0
              AND (:numero IS NULL OR c.numero = :numero)
              AND (:proveedorId IS NULL OR c.proveedor.id = :proveedorId)
              AND (:estadoPago IS NULL OR c.estadoPago = :estadoPago)
              AND (:desde IS NULL OR c.fechaCompra >= :desde)
              AND (:hastaExclusivo IS NULL OR c.fechaCompra < :hastaExclusivo)
              AND (:venceDesde IS NULL OR c.fechaVencimiento >= :venceDesde)
              AND (:venceHasta IS NULL OR c.fechaVencimiento <= :venceHasta)
              AND (
                    :vencida IS NULL
                    OR (:vencida = true AND c.fechaVencimiento < :fechaReferencia)
                    OR (:vencida = false AND c.fechaVencimiento >= :fechaReferencia)
              )
            """)
    Page<Compra> buscarCuentasPorPagar(
            @Param("estado") EstadoCompra estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("numero") String numero,
            @Param("proveedorId") Long proveedorId,
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
            SELECT COUNT(c)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.estadoPago IN :estadosPago
              AND c.saldoPendiente > 0
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    long contarCuentasPorPagar(
            @Param("estado") EstadoCompra estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(c.saldoPendiente)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.estadoPago IN :estadosPago
              AND c.saldoPendiente > 0
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    BigDecimal sumarCuentasPorPagar(
            @Param("estado") EstadoCompra estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT COUNT(c)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.estadoPago IN :estadosPago
              AND c.saldoPendiente > 0
              AND c.fechaVencimiento < :fechaReferencia
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    long contarCuentasPorPagarVencidas(
            @Param("estado") EstadoCompra estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("fechaReferencia") LocalDate fechaReferencia,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );

    @Query("""
            SELECT SUM(c.saldoPendiente)
            FROM Compra c
            WHERE c.estado = :estado
              AND c.estadoPago IN :estadosPago
              AND c.saldoPendiente > 0
              AND c.fechaVencimiento < :fechaReferencia
              AND c.fechaCompra >= :desde
              AND c.fechaCompra < :hastaExclusivo
            """)
    BigDecimal sumarCuentasPorPagarVencidas(
            @Param("estado") EstadoCompra estado,
            @Param("estadosPago") Collection<EstadoPago> estadosPago,
            @Param("fechaReferencia") LocalDate fechaReferencia,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo
    );
}
