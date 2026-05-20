package com.agroempresa.erp.catalogo.producto;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            SELECT p
            FROM Producto p
            WHERE (:buscar IS NULL
                   OR lower(p.nombre) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(p.descripcion, '')) LIKE concat('%', :buscar, '%'))
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
              AND (:soloStockBajo = false OR p.stockActual <= p.stockMinimo)
            """)
    Page<Producto> buscar(
            @Param("buscar") String buscar,
            @Param("activo") Boolean activo,
            @Param("categoriaId") Long categoriaId,
            @Param("soloStockBajo") boolean soloStockBajo,
            Pageable pageable
    );

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    long countByActivoTrue();

    @Query("""
            SELECT COALESCE(SUM(p.valorInventario), 0)
            FROM Producto p
            WHERE p.activo = true
            """)
    BigDecimal sumarValorInventarioActivo();

    @Query("""
            SELECT COUNT(p)
            FROM Producto p
            WHERE p.activo = true
              AND p.stockActual <= p.stockMinimo
            """)
    long contarActivosConStockBajo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdParaActualizar(@Param("id") Long id);
}
