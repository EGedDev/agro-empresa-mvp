package com.agroempresa.erp.catalogo.categoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("""
            SELECT c
            FROM Categoria c
            WHERE (lower(c.nombre) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(c.descripcion, '')) LIKE concat('%', :buscar, '%'))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Categoria> buscar(
            @Param("buscar") String buscar,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
