package com.agroempresa.erp.comercial.venta.devolucion;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.comercial.venta.VentaDetalle;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "devolucion_venta_detalles")
public class DevolucionVentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_venta_id", nullable = false)
    private DevolucionVenta devolucionVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_detalle_id", nullable = false)
    private VentaDetalle ventaDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected DevolucionVentaDetalle() {
    }

    public DevolucionVentaDetalle(VentaDetalle ventaDetalle, Integer cantidad) {
        this.ventaDetalle = ventaDetalle;
        this.producto = ventaDetalle.getProducto();
        this.cantidad = cantidad;
        this.precioUnitario = ventaDetalle.getPrecioUnitario();
        this.subtotal = this.precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public void asignarDevolucionVenta(DevolucionVenta devolucionVenta) {
        this.devolucionVenta = devolucionVenta;
    }

    public Long getId() {
        return id;
    }

    public DevolucionVenta getDevolucionVenta() {
        return devolucionVenta;
    }

    public VentaDetalle getVentaDetalle() {
        return ventaDetalle;
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
}
