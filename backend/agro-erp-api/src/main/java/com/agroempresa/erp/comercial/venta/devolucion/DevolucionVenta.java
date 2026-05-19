package com.agroempresa.erp.comercial.venta.devolucion;

import com.agroempresa.erp.comercial.venta.Venta;
import com.agroempresa.erp.comercial.venta.VentaDetalle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devoluciones_venta")
public class DevolucionVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(nullable = false)
    private LocalDateTime fechaDevolucion;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "devolucionVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DevolucionVentaDetalle> detalles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected DevolucionVenta() {
    }

    public DevolucionVenta(Venta venta, String motivo) {
        this.venta = venta;
        this.motivo = motivo;
        this.fechaDevolucion = LocalDateTime.now();
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(VentaDetalle ventaDetalle, Integer cantidad) {
        DevolucionVentaDetalle detalle = new DevolucionVentaDetalle(ventaDetalle, cantidad);
        detalle.asignarDevolucionVenta(this);
        this.detalles.add(detalle);
        recalcularTotal();
    }

    private void recalcularTotal() {
        this.total = this.detalles.stream()
                .map(DevolucionVentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Venta getVenta() {
        return venta;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public String getMotivo() {
        return motivo;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<DevolucionVentaDetalle> getDetalles() {
        return detalles;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
