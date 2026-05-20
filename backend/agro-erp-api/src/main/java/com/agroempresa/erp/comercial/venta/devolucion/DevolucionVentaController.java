package com.agroempresa.erp.comercial.venta.devolucion;

import com.agroempresa.erp.comercial.venta.devolucion.dto.DevolucionVentaResponse;
import com.agroempresa.erp.comercial.venta.devolucion.dto.RegistrarDevolucionVentaRequest;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ventas/{ventaId}/devoluciones")
@Validated
public class DevolucionVentaController {

    private final DevolucionVentaService devolucionVentaService;

    public DevolucionVentaController(DevolucionVentaService devolucionVentaService) {
        this.devolucionVentaService = devolucionVentaService;
    }

    @GetMapping
    public PaginaResponse<DevolucionVentaResponse> listarPorVenta(
            @PathVariable @Positive(message = "El id de la venta debe ser mayor a cero") Long ventaId,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return devolucionVentaService.listarPorVenta(ventaId, numero, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DevolucionVentaResponse registrar(
            @PathVariable @Positive(message = "El id de la venta debe ser mayor a cero") Long ventaId,
            @Valid @RequestBody RegistrarDevolucionVentaRequest request
    ) {
        return devolucionVentaService.registrar(ventaId, request);
    }
}
