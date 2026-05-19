package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cierres_caja_metodos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cierres_caja_metodo",
                        columnNames = {"cierre_caja_id", "metodo_pago"}
                )
        },
        indexes = {
                @Index(name = "idx_cierres_caja_metodos_cierre", columnList = "cierre_caja_id"),
                @Index(name = "idx_cierres_caja_metodos_metodo", columnList = "metodo_pago")
        }
)
public class CierreCajaMetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cierre_caja_id", nullable = false)
    private CierreCaja cierreCaja;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private MetodoPago metodoPago;

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

    protected CierreCajaMetodoPago() {
    }

    CierreCajaMetodoPago(
            CierreCaja cierreCaja,
            MetodoPago metodoPago,
            Long cantidadIngresos,
            Long cantidadEgresos,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal saldoCalculado,
            BigDecimal saldoReportado
    ) {
        this.cierreCaja = cierreCaja;
        this.metodoPago = metodoPago;
        this.cantidadIngresos = cantidadIngresos;
        this.cantidadEgresos = cantidadEgresos;
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.saldoCalculado = saldoCalculado;
        this.saldoReportado = saldoReportado;
        this.diferencia = saldoReportado.subtract(saldoCalculado);
    }

    public Long getId() {
        return id;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
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
}
