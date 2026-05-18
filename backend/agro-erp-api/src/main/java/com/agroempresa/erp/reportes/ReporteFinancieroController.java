package com.agroempresa.erp.reportes;

import com.agroempresa.erp.reportes.dto.ResumenFinancieroResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes/finanzas")
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteFinancieroService;

    public ReporteFinancieroController(ReporteFinancieroService reporteFinancieroService) {
        this.reporteFinancieroService = reporteFinancieroService;
    }

    @GetMapping("/resumen")
    public ResumenFinancieroResponse obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return reporteFinancieroService.obtenerResumen(desde, hasta);
    }
}
