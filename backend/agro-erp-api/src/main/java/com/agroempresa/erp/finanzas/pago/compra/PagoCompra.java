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

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

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

    @Column(nullable = false)
    private boolean anulado;

    private LocalDateTime fechaAnulacion;

    @Column(length = 300)
    private String motivoAnulacion;

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
        this.anulado = false;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public void asignarNumero(String numero) {
        if (this.numero != null) {
            throw new IllegalStateException("El pago ya tiene numero asignado");
        }

        this.numero = numero;
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

    public boolean isAnulado() {
        return anulado;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void anular(String motivo) {
        if (this.anulado) {
            throw new IllegalStateException("El pago ya fue anulado");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de anulacion es obligatorio");
        }

        this.anulado = true;
        this.fechaAnulacion = LocalDateTime.now();
        this.motivoAnulacion = motivo.trim();
    }
}
