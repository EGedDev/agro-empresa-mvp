package com.agroempresa.erp.proveedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByDocumentoIdentidadIgnoreCase(String documentoIdentidad);

    @Query("""
            SELECT p
            FROM Proveedor p
            WHERE (lower(p.nombre) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(p.documentoIdentidad, '')) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(p.email, '')) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(p.telefono, '')) LIKE concat('%', :buscar, '%'))
              AND (:activo IS NULL OR p.activo = :activo)
            """)
    Page<Proveedor> buscar(
            @Param("buscar") String buscar,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
