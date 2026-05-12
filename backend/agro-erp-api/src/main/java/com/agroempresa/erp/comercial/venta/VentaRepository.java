package com.agroempresa.erp.comercial.venta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByFechaVentaDesc(); // al español significa: Buscar todas las ventas, ordenadas por fecha de venta de forma descendente.  


    // Agrega este método para filtrar por estado
    // significa al español
    // Buscar por estado, ordenado por fecha de venta de forma descendente.
    List<Venta> findByEstadoOrderByFechaVentaDesc(EstadoVenta estado); // nuevo método para filtrar por estado


    // al español significa: Buscar por clienteId, ordenado por fecha de venta de forma descendente.
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);
}