package com.agroempresa.erp.finanzas.caja;

import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "movimientos_caja",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movimientos_caja_referencia",
                        columnNames = {"referencia_tipo", "referencia_id"}
                )
        },
        indexes = {
                @Index(name = "idx_movimientos_caja_tipo", columnList = "tipo"),
                @Index(name = "idx_movimientos_caja_metodo_pago", columnList = "metodo_pago"),
                @Index(name = "idx_movimientos_caja_fecha", columnList = "fecha_movimiento"),
                @Index(name = "idx_movimientos_caja_referencia", columnList = "referencia_tipo, referencia_id")
        }
)
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoMovimientoCaja tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private MetodoPago metodoPago;

    @Column(length = 120)
    private String referencia;

    @Column(nullable = false, length = 80)
    private String referenciaTipo;

    @Column(nullable = false)
    private Long referenciaId;

    @Column(nullable = false)
    private LocalDateTime fechaMovimiento;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(
            TipoMovimientoCaja tipo,
            BigDecimal monto,
            MetodoPago metodoPago,
            String referencia,
            String referenciaTipo,
            Long referenciaId,
            LocalDateTime fechaMovimiento
    ) {
        this.tipo = tipo;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.fechaMovimiento = fechaMovimiento;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
