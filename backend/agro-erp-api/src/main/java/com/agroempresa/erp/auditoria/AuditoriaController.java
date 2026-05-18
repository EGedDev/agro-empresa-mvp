package com.agroempresa.erp.auditoria;

import com.agroempresa.erp.auditoria.dto.AuditoriaEventoResponse;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria/eventos")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PaginaResponse<AuditoriaEventoResponse> listarEventos(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String recursoTipo,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return auditoriaService.listarEventos(
                username,
                accion,
                recursoTipo,
                recursoId,
                correlationId,
                desde,
                hasta,
                page,
                size,
                sort
        );
    }
}
