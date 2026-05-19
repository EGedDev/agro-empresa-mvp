package com.agroempresa.erp.comercial.compra.devolucion;

import com.agroempresa.erp.comercial.compra.Compra;
import com.agroempresa.erp.comercial.compra.CompraDetalle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devoluciones_compra")
public class DevolucionCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(nullable = false)
    private LocalDateTime fechaDevolucion;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "devolucionCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DevolucionCompraDetalle> detalles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected DevolucionCompra() {
    }

    public DevolucionCompra(Compra compra, String motivo) {
        this.compra = compra;
        this.motivo = motivo;
        this.fechaDevolucion = LocalDateTime.now();
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(CompraDetalle compraDetalle, Integer cantidad) {
        DevolucionCompraDetalle detalle = new DevolucionCompraDetalle(compraDetalle, cantidad);
        detalle.asignarDevolucionCompra(this);
        this.detalles.add(detalle);
        recalcularTotal();
    }

    private void recalcularTotal() {
        this.total = this.detalles.stream()
                .map(DevolucionCompraDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
            throw new IllegalStateException("La devolucion ya tiene numero asignado");
        }

        this.numero = numero;
    }

    public Compra getCompra() {
        return compra;
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

    public List<DevolucionCompraDetalle> getDetalles() {
        return detalles;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
