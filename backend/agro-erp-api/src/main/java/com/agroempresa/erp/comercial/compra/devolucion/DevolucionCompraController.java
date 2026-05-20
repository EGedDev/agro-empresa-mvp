package com.agroempresa.erp.comercial.compra.devolucion;

import com.agroempresa.erp.comercial.compra.devolucion.dto.DevolucionCompraResponse;
import com.agroempresa.erp.comercial.compra.devolucion.dto.RegistrarDevolucionCompraRequest;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compras/{compraId}/devoluciones")
@Validated
public class DevolucionCompraController {

    private final DevolucionCompraService devolucionCompraService;

    public DevolucionCompraController(DevolucionCompraService devolucionCompraService) {
        this.devolucionCompraService = devolucionCompraService;
    }

    @GetMapping
    public PaginaResponse<DevolucionCompraResponse> listarPorCompra(
            @PathVariable @Positive(message = "El id de la compra debe ser mayor a cero") Long compraId,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return devolucionCompraService.listarPorCompra(compraId, numero, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DevolucionCompraResponse registrar(
            @PathVariable @Positive(message = "El id de la compra debe ser mayor a cero") Long compraId,
            @Valid @RequestBody RegistrarDevolucionCompraRequest request
    ) {
        return devolucionCompraService.registrar(compraId, request);
    }
}
