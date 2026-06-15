package com.agroempresa.erp.comercial.solicitud;

import com.agroempresa.erp.comercial.solicitud.dto.SolicitudAtencionRequest;
import com.agroempresa.erp.comercial.solicitud.dto.SolicitudAtencionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/web/solicitudes-atencion")
@Validated
public class WebSolicitudAtencionController {

    private final SolicitudAtencionService solicitudAtencionService;

    public WebSolicitudAtencionController(SolicitudAtencionService solicitudAtencionService) {
        this.solicitudAtencionService = solicitudAtencionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudAtencionResponse crear(@Valid @RequestBody SolicitudAtencionRequest request) {
        return solicitudAtencionService.crear(request);
    }
}
