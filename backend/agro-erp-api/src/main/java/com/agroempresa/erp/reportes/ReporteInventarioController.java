package com.agroempresa.erp.reportes;

import com.agroempresa.erp.reportes.dto.ResumenInventarioResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes/inventario")
public class ReporteInventarioController {

    private final ReporteInventarioService reporteInventarioService;

    public ReporteInventarioController(ReporteInventarioService reporteInventarioService) {
        this.reporteInventarioService = reporteInventarioService;
    }

    @GetMapping("/resumen")
    public ResumenInventarioResponse obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return reporteInventarioService.obtenerResumen(desde, hasta);
    }
}
