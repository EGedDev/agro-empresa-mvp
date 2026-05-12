package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Producto() {
    }

    public Producto(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            Categoria categoria
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        this.activo = true;
    }

    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            Categoria categoria
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void descontarStock(Integer cantidad) {
        validarCantidadMovimiento(cantidad);

        if (this.stockActual < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para descontar inventario");
        }

        this.stockActual = this.stockActual - cantidad;
    }

    public void aumentarStock(Integer cantidad) {
        validarCantidadMovimiento(cantidad);
        this.stockActual = this.stockActual + cantidad;
    }

    private void validarCantidadMovimiento(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
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

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
