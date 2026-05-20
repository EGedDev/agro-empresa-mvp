package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.producto.Producto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "venta_detalles")
public class VentaDetalle {

    private static final int ESCALA_COSTO = 4;
    private static final int ESCALA_VALOR = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal costoTotal;

    protected VentaDetalle() {
    }

    public VentaDetalle(Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this(producto, cantidad, precioUnitario, producto.getCostoPromedio());
    }

    public VentaDetalle(Producto producto, Integer cantidad, BigDecimal precioUnitario, BigDecimal costoUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        this.costoUnitario = costoUnitario.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
        this.costoTotal = this.costoUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(ESCALA_VALOR, RoundingMode.HALF_UP);
    }

    public void asignarVenta(Venta venta) {
        this.venta = venta;
    }

    public Long getId() {
        return id;
    }

    public Venta getVenta() {
        return venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }
}
