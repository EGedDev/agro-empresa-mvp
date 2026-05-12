package com.agroempresa.erp.cliente;

import com.agroempresa.erp.cliente.dto.ClienteRequest;
import com.agroempresa.erp.cliente.dto.ClienteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listar();
    }

    @GetMapping("/activos")
    public List<ClienteResponse> listarActivos() {
        return clienteService.listarActivos();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
        return clienteService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(request);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request
    ) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public ClienteResponse desactivar(@PathVariable Long id) {
        return clienteService.desactivar(id);
    }
}
