package com.agroempresa.erp.cliente;

import com.agroempresa.erp.cliente.dto.ClienteRequest;
import com.agroempresa.erp.cliente.dto.ClienteResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
@Validated
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public PaginaResponse<ClienteResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return clienteService.listar(buscar, activo, page, size, sort);
    }

    @GetMapping("/activos")
    public PaginaResponse<ClienteResponse> listarActivos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return clienteService.listarActivos(page, size, sort);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return clienteService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(request);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody ClienteRequest request
    ) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public ClienteResponse desactivar(@PathVariable @Positive(message = "El id debe ser mayor a cero") Long id) {
        return clienteService.desactivar(id);
    }
}
