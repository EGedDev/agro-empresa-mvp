package com.agroempresa.erp.reportes;

import com.agroempresa.erp.reportes.dto.ResumenComprasProductoResponse;
import com.agroempresa.erp.reportes.dto.ResumenComprasProveedorResponse;
import com.agroempresa.erp.reportes.dto.ResumenVentasClienteResponse;
import com.agroempresa.erp.reportes.dto.ResumenVentasProductoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes/gerenciales")
public class ReporteGerencialController {

    private final ReporteGerencialService reporteGerencialService;

    public ReporteGerencialController(ReporteGerencialService reporteGerencialService) {
        this.reporteGerencialService = reporteGerencialService;
    }

    @GetMapping("/ventas/clientes")
    public List<ResumenVentasClienteResponse> ventasPorCliente(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite
    ) {
        return reporteGerencialService.ventasPorCliente(desde, hasta, limite);
    }

    @GetMapping("/ventas/productos")
    public List<ResumenVentasProductoResponse> ventasPorProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite
    ) {
        return reporteGerencialService.ventasPorProducto(desde, hasta, limite);
    }

    @GetMapping("/compras/proveedores")
    public List<ResumenComprasProveedorResponse> comprasPorProveedor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite
    ) {
        return reporteGerencialService.comprasPorProveedor(desde, hasta, limite);
    }

    @GetMapping("/compras/productos")
    public List<ResumenComprasProductoResponse> comprasPorProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite
    ) {
        return reporteGerencialService.comprasPorProducto(desde, hasta, limite);
    }
}
