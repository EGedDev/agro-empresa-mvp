package com.agroempresa.erp.comercial.venta;

import com.agroempresa.erp.cliente.Cliente;
import jakarta.persistence.*;
import com.agroempresa.erp.catalogo.producto.Producto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // Indica que esta clase es una entidad JPA y se mapeará a una tabla en la base de datos
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


    // Constructor protegido para JPA
    protected Venta() {
    }


    // Constructor público para crear una nueva venta con un cliente específico. Al crear una venta, se establece la fecha de venta como la fecha y hora actual, el estado inicial como "REGISTRADA" y el total como cero.
    public Venta(Cliente cliente) { // Constructor público para crear una nueva venta con un cliente específico
        this.cliente = cliente;
        this.fechaVenta = LocalDateTime.now();
        this.estado = EstadoVenta.REGISTRADA;
        this.total = BigDecimal.ZERO;
    }

    // Método para agregar un detalle a la venta
public void agregarDetalle(Producto producto, Integer cantidad) {
    VentaDetalle detalle = new VentaDetalle(producto, cantidad, producto.getPrecioVenta());
    detalle.asignarVenta(this);
    this.detalles.add(detalle);
    recalcularTotal();
}


    
    public void recalcularTotal() { // Método para recalcular el total de la venta sumando los subtotales de cada detalle. Se utiliza un stream para mapear cada detalle a su subtotal y luego se reduce la lista de subtotales sumándolos, comenzando con un valor inicial de BigDecimal.ZERO.
        this.total = this.detalles.stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

  public void cancelar() {
    this.estado = EstadoVenta.CANCELADA;
}


    @PrePersist // Método anotado con @PrePersist que se ejecuta antes de que la entidad sea persistida en la base de datos. Este método establece las fechas de creación y actualización al momento actual.
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate // Método anotado con @PreUpdate que se ejecuta antes de que la entidad sea actualizada en la base de datos. Este método actualiza la fecha de actualización al momento actual.
    protected void antesDeActualizar() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getId() { // Método getter para obtener el ID de la venta. Este método devuelve el valor del campo "id", que es la clave primaria de la entidad Venta.
        return id;
    }

    public Cliente getCliente() { // Método getter para obtener el cliente asociado a la venta. Este método devuelve el valor del campo "cliente", que es una referencia a la entidad Cliente asociada a esta venta.
        return cliente;
    }

    public LocalDateTime getFechaVenta() { // Método getter para obtener la fecha de la venta. Este método devuelve el valor del campo "fechaVenta", que representa la fecha y hora en que se realizó la venta.
        return fechaVenta;
    }

    public EstadoVenta getEstado() { // Método getter para obtener el estado de la venta. Este método devuelve el valor del campo "estado", que representa el estado actual de la venta.
        return estado;
    }

    public BigDecimal getTotal() { // Método getter para obtener el total de la venta. Este método devuelve el valor del campo "total", que representa el monto total de la venta.
        return total;
    }

    public List<VentaDetalle> getDetalles() { // Método getter para obtener los detalles de la venta. Este método devuelve el valor del campo "detalles", que es una lista de objetos de tipo VentaDetalle.
        return detalles;
    }

    public LocalDateTime getCreadoEn() { // Método getter para obtener la fecha de creación de la venta. Este método devuelve el valor del campo "creadoEn", que representa la fecha y hora en que se creó la venta.
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() { // Método getter para obtener la fecha de actualización de la venta. Este método devuelve el valor del campo "actualizadoEn", que representa la fecha y hora en que se actualizó la venta.
        return actualizadoEn;
    }
}