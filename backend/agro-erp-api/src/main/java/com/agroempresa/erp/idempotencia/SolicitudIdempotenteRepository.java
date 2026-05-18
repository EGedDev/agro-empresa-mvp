package com.agroempresa.erp.idempotencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitudIdempotenteRepository extends JpaRepository<SolicitudIdempotente, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SolicitudIdempotente s
            WHERE s.username = :username
              AND s.metodoHttp = :metodoHttp
              AND s.ruta = :ruta
              AND s.idempotencyKey = :idempotencyKey
            """)
    Optional<SolicitudIdempotente> findByScopeParaActualizar(
            @Param("username") String username,
            @Param("metodoHttp") String metodoHttp,
            @Param("ruta") String ruta,
            @Param("idempotencyKey") String idempotencyKey
    );
}
