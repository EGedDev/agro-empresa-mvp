package com.agroempresa.erp.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findTop30ByOrderByCreadoEnDesc();

    List<MovimientoInventario> findByProductoIdOrderByCreadoEnDesc(Long productoId);
}