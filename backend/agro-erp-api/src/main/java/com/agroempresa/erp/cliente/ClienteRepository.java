package com.agroempresa.erp.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNombreAsc();

    List<Cliente> findByActivoTrueOrderByNombreAsc();

    Optional<Cliente> findByDocumentoIdentidadIgnoreCase(String documentoIdentidad);
}