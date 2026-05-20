package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.catalogo.producto.Producto;
import com.agroempresa.erp.cliente.Cliente;
import com.agroempresa.erp.finanzas.EstadoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fechaVenta;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private EstadoVenta estado;

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

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Venta() {
    }

    public Venta(Cliente cliente) {
        this(cliente, LocalDate.now());
    }

    public Venta(Cliente cliente, LocalDate fechaVencimiento) {
        this.cliente = cliente;
        this.fechaVenta = LocalDateTime.now();
        this.fechaVencimiento = fechaVencimiento == null ? LocalDate.now() : fechaVencimiento;
        this.estado = EstadoVenta.REGISTRADA;
        this.total = BigDecimal.ZERO;
        this.totalPagado = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    public void agregarDetalle(Producto producto, Integer cantidad) {
        agregarDetalle(producto, cantidad, producto.getCostoPromedio());
    }

    public void agregarDetalle(Producto producto, Integer cantidad, BigDecimal costoUnitario) {
        VentaDetalle detalle = new VentaDetalle(producto, cantidad, producto.getPrecioVenta(), costoUnitario);
        detalle.asignarVenta(this);
        this.detalles.add(detalle);
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.total = this.detalles.stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        actualizarEstadoPago();
    }

    public void registrarPago(BigDecimal monto) {
        if (this.estado == EstadoVenta.CANCELADA) {
            throw new IllegalStateException("No se puede registrar pagos para una venta cancelada");
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

    public void anularPago(BigDecimal monto) {
        if (this.estado == EstadoVenta.CANCELADA) {
            throw new IllegalStateException("No se puede anular pagos de una venta cancelada");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }

        if (monto.compareTo(this.totalPagado) > 0) {
            throw new IllegalArgumentException("El pago a anular no puede superar el total pagado");
        }

        this.totalPagado = this.totalPagado.subtract(monto);
        actualizarEstadoPago();
    }

    public void registrarDevolucion(BigDecimal monto) {
        if (this.estado == EstadoVenta.CANCELADA) {
            throw new IllegalStateException("No se puede registrar devoluciones para una venta cancelada");
        }

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de la devolucion debe ser mayor a cero");
        }

        if (monto.compareTo(this.total) > 0) {
            throw new IllegalArgumentException("La devolucion no puede superar el total de la venta");
        }

        BigDecimal nuevoTotal = this.total.subtract(monto);
        if (this.totalPagado.compareTo(nuevoTotal) > 0) {
            throw new IllegalStateException(
                    "No se puede registrar la devolucion porque existen pagos por encima del nuevo total"
            );
        }

        this.total = nuevoTotal;
        actualizarEstadoPago();
    }

    public void cancelar() {
        if (this.totalPagado.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("No se puede cancelar una venta con pagos registrados");
        }

        this.estado = EstadoVenta.CANCELADA;
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

    public String getNumero() {
        return numero;
    }

    public void asignarNumero(String numero) {
        if (this.numero != null) {
            throw new IllegalStateException("La venta ya tiene numero asignado");
        }

        this.numero = numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public EstadoVenta getEstado() {
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
