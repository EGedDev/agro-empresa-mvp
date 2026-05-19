package com.agroempresa.erp.common.numeracion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuenciaDocumentoRepository extends JpaRepository<SecuenciaDocumento, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaDocumento s WHERE s.codigo = :codigo")
    Optional<SecuenciaDocumento> findByCodigoParaActualizar(@Param("codigo") String codigo);
}
