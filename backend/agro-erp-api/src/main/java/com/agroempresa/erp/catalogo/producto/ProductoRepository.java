package com.agroempresa.erp.catalogo.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByNombreAsc();

    List<Producto> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            SELECT p
            FROM Producto p
            WHERE p.activo = true
              AND p.stockActual <= p.stockMinimo
            ORDER BY p.nombre ASC
            """)
    List<Producto> findProductosConStockBajo();
}