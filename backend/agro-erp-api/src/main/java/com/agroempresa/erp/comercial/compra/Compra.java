package com.agroempresa.erp.comercial.compra;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.finanzas.EstadoPago;
import com.agroempresa.erp.proveedor.Proveedor;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fechaCompra;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private EstadoCompra estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPagado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private EstadoPago estadoPago;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompraDetalle> detalles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Compra() {
    }

    public Compra(Proveedor proveedor) {
        this.proveedor = proveedor;
        this.fechaCompra = LocalDateTime.now();
        this.estado = EstadoCompra.REGISTRADA;
        this.total = BigDecimal.ZERO;
        this.totalPagado = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    public void agregarDetalle(Producto producto, Integer cantidad, BigDecimal costoUnitario) {
        CompraDetalle detalle = new CompraDetalle(producto, cantidad, costoUnitario);
        detalle.asignarCompra(this);
        this.detalles.add(detalle);
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.total = this.detalles.stream()
                .map(CompraDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        actualizarEstadoPago();
    }

    public void registrarPago(BigDecimal monto) {
        if (this.estado == EstadoCompra.CANCELADA) {
            throw new IllegalStateException("No se puede registrar pagos para una compra cancelada");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        if (monto.compareTo(this.saldoPendiente) > 0) {
            throw new IllegalArgumentException("El pago no puede superar el saldo pendiente");
        }

        this.totalPagado = this.totalPagado.add(monto);
        actualizarEstadoPago();
    }

    public void cancelar() {
        if (this.totalPagado.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("No se puede cancelar una compra con pagos registrados");
        }

        this.estado = EstadoCompra.CANCELADA;
        this.estadoPago = EstadoPago.CANCELADA;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    private void actualizarEstadoPago() {
        this.saldoPendiente = this.total.subtract(this.totalPagado);

        if (this.totalPagado.compareTo(BigDecimal.ZERO) == 0) {
            this.estadoPago = EstadoPago.PENDIENTE;
            return;
        }

        if (this.totalPagado.compareTo(this.total) == 0) {
            this.estadoPago = EstadoPago.PAGADA;
            return;
        }

        this.estadoPago = EstadoPago.PARCIAL;
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

    public Proveedor getProveedor() {
        return proveedor;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getTotalPagado() {
        return totalPagado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public List<CompraDetalle> getDetalles() {
        return detalles;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
