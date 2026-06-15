package com.agroempresa.erp.comercial.solicitud;

import com.agroempresa.erp.comercial.solicitud.dto.ActualizarEstadoSolicitudAtencionRequest;
import com.agroempresa.erp.comercial.solicitud.dto.SolicitudAtencionResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comercial/solicitudes-atencion")
@Validated
public class SolicitudAtencionController {

    private final SolicitudAtencionService solicitudAtencionService;

    public SolicitudAtencionController(SolicitudAtencionService solicitudAtencionService) {
        this.solicitudAtencionService = solicitudAtencionService;
    }

    @GetMapping
    public PaginaResponse<SolicitudAtencionResponse> listar(
            @RequestParam(required = false) EstadoSolicitudAtencion estado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return solicitudAtencionService.listar(estado, page, size, sort);
    }

    @PatchMapping("/{id}/estado")
    public SolicitudAtencionResponse actualizarEstado(
            @PathVariable @Positive(message = "El id debe ser mayor a cero") Long id,
            @Valid @RequestBody ActualizarEstadoSolicitudAtencionRequest request
    ) {
        return solicitudAtencionService.actualizarEstado(id, request);
    }
}
