package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.dto.MovimientoCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenCajaPorMetodoPagoResponse;
import com.agroempresa.erp.finanzas.caja.dto.ResumenCajaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finanzas/caja")
@Validated
public class MovimientoCajaController {

    private final MovimientoCajaService movimientoCajaService;

    public MovimientoCajaController(MovimientoCajaService movimientoCajaService) {
        this.movimientoCajaService = movimientoCajaService;
    }

    @GetMapping("/movimientos")
    public PaginaResponse<MovimientoCajaResponse> listar(
            @RequestParam(required = false) TipoMovimientoCaja tipo,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) String referenciaTipo,
            @RequestParam(required = false) Long referenciaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return movimientoCajaService.listar(
                tipo,
                metodoPago,
                referenciaTipo,
                referenciaId,
                desde,
                hasta,
                page,
                size,
                sort
        );
    }

    @GetMapping("/resumen")
    public ResumenCajaResponse obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return movimientoCajaService.obtenerResumen(desde, hasta);
    }

    @GetMapping("/resumen/metodos")
    public ResumenCajaPorMetodoPagoResponse obtenerResumenPorMetodoPago(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return movimientoCajaService.obtenerResumenPorMetodoPago(desde, hasta);
    }
}
