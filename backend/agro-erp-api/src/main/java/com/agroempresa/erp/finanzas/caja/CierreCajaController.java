package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.caja.dto.CierreCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.DiferenciaCierreCajaResponse;
import com.agroempresa.erp.finanzas.caja.dto.RegistrarCierreCajaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finanzas/caja/cierres")
@Validated
public class CierreCajaController {

    private final CierreCajaService cierreCajaService;

    public CierreCajaController(CierreCajaService cierreCajaService) {
        this.cierreCajaService = cierreCajaService;
    }

    @GetMapping
    public PaginaResponse<CierreCajaResponse> listar(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return cierreCajaService.listar(numero, desde, hasta, page, size, sort);
    }

    @GetMapping("/diferencias")
    public PaginaResponse<DiferenciaCierreCajaResponse> listarDiferencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) Boolean soloConDiferencia,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return cierreCajaService.listarDiferencias(
                desde,
                hasta,
                metodoPago,
                soloConDiferencia,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{id}")
    public CierreCajaResponse obtenerPorId(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id
    ) {
        return cierreCajaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CierreCajaResponse registrar(@Valid @RequestBody RegistrarCierreCajaRequest request) {
        return cierreCajaService.registrar(request);
    }
}
