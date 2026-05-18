package com.agroempresa.erp.inventario;

import com.agroempresa.erp.catalogo.producto.Producto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private TipoMovimientoInventario tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer stockNuevo;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(length = 80)
    private String referenciaTipo;

    private Long referenciaId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(
            Producto producto,
            TipoMovimientoInventario tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            String motivo,
            String referenciaTipo,
            Long referenciaId
    ) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoMovimientoInventario getTipo() {
        return tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public Integer getStockNuevo() {
        return stockNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
