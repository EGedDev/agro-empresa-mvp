package com.agroempresa.erp.finanzas.pago.compra;

import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.finanzas.MetodoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_compra")
public class PagoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private MetodoPago metodoPago;

    @Column(length = 120)
    private String referencia;

    @Column(nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected PagoCompra() {
    }

    public PagoCompra(
            Compra compra,
            BigDecimal monto,
            MetodoPago metodoPago,
            String referencia
    ) {
        this.compra = compra;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.fechaPago = LocalDateTime.now();
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Compra getCompra() {
        return compra;
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

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
