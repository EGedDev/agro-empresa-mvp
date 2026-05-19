package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "cierres_caja",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cierres_caja_periodo",
                        columnNames = {"fecha_desde", "fecha_hasta"}
                )
        },
        indexes = {
                @Index(name = "idx_cierres_caja_periodo", columnList = "fecha_desde, fecha_hasta"),
                @Index(name = "idx_cierres_caja_creado_en", columnList = "creado_en")
        }
)
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaDesde;

    @Column(nullable = false)
    private LocalDate fechaHasta;

    @Column(nullable = false)
    private Long cantidadIngresos;

    @Column(nullable = false)
    private Long cantidadEgresos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIngresos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEgresos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoCalculado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoReportado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 500)
    private String observaciones;

    @Column(nullable = false, length = 80)
    private String cerradoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "cierreCaja", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("metodoPago ASC")
    private List<CierreCajaMetodoPago> metodos = new ArrayList<>();

    protected CierreCaja() {
    }

    public CierreCaja(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long cantidadIngresos,
            Long cantidadEgresos,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal saldoCalculado,
            BigDecimal saldoReportado,
            String observaciones,
            String cerradoPor
    ) {
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.cantidadIngresos = cantidadIngresos;
        this.cantidadEgresos = cantidadEgresos;
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.saldoCalculado = saldoCalculado;
        this.saldoReportado = saldoReportado;
        this.diferencia = saldoReportado.subtract(saldoCalculado);
        this.observaciones = observaciones;
        this.cerradoPor = cerradoPor;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public Long getCantidadIngresos() {
        return cantidadIngresos;
    }

    public Long getCantidadEgresos() {
        return cantidadEgresos;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public BigDecimal getTotalEgresos() {
        return totalEgresos;
    }

    public BigDecimal getSaldoCalculado() {
        return saldoCalculado;
    }

    public BigDecimal getSaldoReportado() {
        return saldoReportado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getCerradoPor() {
        return cerradoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<CierreCajaMetodoPago> getMetodos() {
        return Collections.unmodifiableList(metodos);
    }

    public void agregarMetodo(
            MetodoPago metodoPago,
            Long cantidadIngresos,
            Long cantidadEgresos,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal saldoCalculado,
            BigDecimal saldoReportado
    ) {
        metodos.add(new CierreCajaMetodoPago(
                this,
                metodoPago,
                cantidadIngresos,
                cantidadEgresos,
                totalIngresos,
                totalEgresos,
                saldoCalculado,
                saldoReportado
        ));
    }
}
