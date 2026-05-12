package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.cliente.Cliente;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fechaVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoVenta estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Venta() {
    }

    public Venta(Cliente cliente) {
        this.cliente = cliente;
        this.fechaVenta = LocalDateTime.now();
        this.estado = EstadoVenta.REGISTRADA;
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(Producto producto, Integer cantidad) {
        VentaDetalle detalle = new VentaDetalle(producto, cantidad, producto.getPrecioVenta());
        detalle.asignarVenta(this);
        this.detalles.add(detalle);
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.total = this.detalles.stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cancelar() {
        this.estado = EstadoVenta.CANCELADA;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void antesDeActualizar() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<VentaDetalle> getDetalles() {
        return detalles;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
