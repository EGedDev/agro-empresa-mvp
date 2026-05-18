package com.agroempresa.erp.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    @Query("""
            SELECT a
            FROM AuditoriaEvento a
            WHERE (:username IS NULL OR lower(a.username) LIKE concat('%', :username, '%'))
              AND (:accion IS NULL OR lower(a.accion) LIKE concat('%', :accion, '%'))
              AND (:recursoTipo IS NULL OR lower(a.recursoTipo) = :recursoTipo)
              AND (:recursoId IS NULL OR a.recursoId = :recursoId)
              AND (:correlationId IS NULL OR lower(a.correlationId) = :correlationId)
              AND (:desde IS NULL OR a.creadoEn >= :desde)
              AND (:hastaExclusivo IS NULL OR a.creadoEn < :hastaExclusivo)
            """)
    Page<AuditoriaEvento> buscar(
            @Param("username") String username,
            @Param("accion") String accion,
            @Param("recursoTipo") String recursoTipo,
            @Param("recursoId") Long recursoId,
            @Param("correlationId") String correlationId,
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo,
            Pageable pageable
    );
}
