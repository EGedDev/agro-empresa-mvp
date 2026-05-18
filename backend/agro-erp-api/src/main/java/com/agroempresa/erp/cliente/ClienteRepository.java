package com.agroempresa.erp.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumentoIdentidadIgnoreCase(String documentoIdentidad);

    @Query("""
            SELECT c
            FROM Cliente c
            WHERE (:buscar IS NULL
                   OR lower(c.nombre) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(c.documentoIdentidad, '')) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(c.email, '')) LIKE concat('%', :buscar, '%')
                   OR lower(coalesce(c.telefono, '')) LIKE concat('%', :buscar, '%'))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Cliente> buscar(
            @Param("buscar") String buscar,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
