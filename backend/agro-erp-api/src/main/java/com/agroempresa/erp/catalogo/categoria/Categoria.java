package com.agroempresa.erp.catalogo.categoria;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Categoria() {
    }

    public Categoria(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = true;
    }

    public void actualizar(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void desactivar() {
        this.activo = false;
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

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}