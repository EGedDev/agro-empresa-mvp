package com.agroempresa.erp.comercial.solicitud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudAtencionRepository extends JpaRepository<SolicitudAtencion, Long> {

    Page<SolicitudAtencion> findByEstado(EstadoSolicitudAtencion estado, Pageable pageable);
}
