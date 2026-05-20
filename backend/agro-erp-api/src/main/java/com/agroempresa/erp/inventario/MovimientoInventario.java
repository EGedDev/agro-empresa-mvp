package com.agroempresa.erp.inventario;

import com.agroempresa.erp.catalogo.producto.Producto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    private static final int ESCALA_COSTO = 4;
    private static final int ESCALA_VALOR = 2;

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

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorMovimiento;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorInventarioAnterior;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorInventarioNuevo;

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
        this(
                producto,
                tipo,
                cantidad,
                stockAnterior,
                stockNuevo,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                motivo,
                referenciaTipo,
                referenciaId
        );
    }

    public MovimientoInventario(
            Producto producto,
            TipoMovimientoInventario tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            BigDecimal costoUnitario,
            BigDecimal valorMovimiento,
            BigDecimal valorInventarioAnterior,
            BigDecimal valorInventarioNuevo,
            String motivo,
            String referenciaTipo,
            Long referenciaId
    ) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.costoUnitario = normalizarCosto(costoUnitario);
        this.valorMovimiento = normalizarValor(valorMovimiento);
        this.valorInventarioAnterior = normalizarValor(valorInventarioAnterior);
        this.valorInventarioNuevo = normalizarValor(valorInventarioNuevo);
        this.motivo = motivo;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
    }

    private BigDecimal normalizarCosto(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarValor(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(ESCALA_VALOR, RoundingMode.HALF_UP);
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

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getValorMovimiento() {
        return valorMovimiento;
    }

    public BigDecimal getValorInventarioAnterior() {
        return valorInventarioAnterior;
    }

    public BigDecimal getValorInventarioNuevo() {
        return valorInventarioNuevo;
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
