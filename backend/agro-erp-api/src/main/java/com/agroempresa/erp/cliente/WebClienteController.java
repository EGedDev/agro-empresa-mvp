package com.agroempresa.erp.cliente;

import com.agroempresa.erp.cliente.dto.ClienteRequest;
import com.agroempresa.erp.cliente.dto.ClienteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/web/clientes")
@Validated
public class WebClienteController {

    private final ClienteService clienteService;

    public WebClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registrarClienteWeb(@Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(request);
    }
}
