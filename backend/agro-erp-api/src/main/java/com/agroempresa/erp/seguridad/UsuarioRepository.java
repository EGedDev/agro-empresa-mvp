package com.agroempresa.erp.seguridad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRolAndActivoTrue(RolUsuario rol);

    @Query("""
            SELECT u
            FROM Usuario u
            WHERE (lower(u.username) LIKE concat('%', :buscar, '%')
                   OR lower(u.nombre) LIKE concat('%', :buscar, '%'))
              AND (:rol IS NULL OR u.rol = :rol)
              AND (:activo IS NULL OR u.activo = :activo)
            """)
    Page<Usuario> buscar(
            @Param("buscar") String buscar,
            @Param("rol") RolUsuario rol,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
