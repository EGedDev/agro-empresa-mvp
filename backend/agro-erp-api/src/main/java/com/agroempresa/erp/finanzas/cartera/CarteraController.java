package com.agroempresa.erp.finanzas.cartera;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.finanzas.cartera.dto.CuentaPorCobrarResponse;
import com.agroempresa.erp.finanzas.cartera.dto.CuentaPorPagarResponse;
import com.agroempresa.erp.finanzas.cartera.dto.ResumenCarteraResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finanzas/cartera")
public class CarteraController {

    private final CarteraService carteraService;

    public CarteraController(CarteraService carteraService) {
        this.carteraService = carteraService;
    }

    @GetMapping("/cuentas-por-cobrar")
    public PaginaResponse<CuentaPorCobrarResponse> listarCuentasPorCobrar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceHasta,
            @RequestParam(required = false) Boolean vencida,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return carteraService.listarCuentasPorCobrar(
                clienteId,
                estadoPago,
                desde,
                hasta,
                venceDesde,
                venceHasta,
                vencida,
                page,
                size,
                sort
        );
    }

    @GetMapping("/cuentas-por-pagar")
    public PaginaResponse<CuentaPorPagarResponse> listarCuentasPorPagar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceHasta,
            @RequestParam(required = false) Boolean vencida,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return carteraService.listarCuentasPorPagar(
                proveedorId,
                estadoPago,
                desde,
                hasta,
                venceDesde,
                venceHasta,
                vencida,
                page,
                size,
                sort
        );
    }

    @GetMapping("/resumen")
    public ResumenCarteraResponse obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return carteraService.obtenerResumen(desde, hasta);
    }
}
