package com.agroempresa.erp.comercial.compra.devolucion;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.comercial.compra.CompraDetalle;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "devolucion_compra_detalles")
public class DevolucionCompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_compra_id", nullable = false)
    private DevolucionCompra devolucionCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_detalle_id", nullable = false)
    private CompraDetalle compraDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected DevolucionCompraDetalle() {
    }

    public DevolucionCompraDetalle(CompraDetalle compraDetalle, Integer cantidad) {
        this.compraDetalle = compraDetalle;
        this.producto = compraDetalle.getProducto();
        this.cantidad = cantidad;
        this.costoUnitario = compraDetalle.getCostoUnitario();
        this.subtotal = this.costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public void asignarDevolucionCompra(DevolucionCompra devolucionCompra) {
        this.devolucionCompra = devolucionCompra;
    }

    public Long getId() {
        return id;
    }

    public DevolucionCompra getDevolucionCompra() {
        return devolucionCompra;
    }

    public CompraDetalle getCompraDetalle() {
        return compraDetalle;
    }

    public Producto getProducto() {
        return producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
