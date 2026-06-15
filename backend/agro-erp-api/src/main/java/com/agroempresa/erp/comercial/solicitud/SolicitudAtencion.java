package com.agroempresa.erp.comercial.solicitud;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_atencion")
public class SolicitudAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "documento_identidad", length = 20)
    private String documentoIdentidad;

    @Column(length = 30)
    private String telefono;

    @Column(length = 160)
    private String email;

    @Column(length = 250)
    private String direccion;

    @Column(length = 120)
    private String cultivo;

    @Column(length = 120)
    private String interes;

    @Column(length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitudAtencion estado = EstadoSolicitudAtencion.PENDIENTE;

    @Column(name = "atendido_por", length = 120)
    private String atendidoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected SolicitudAtencion() {
    }

    public SolicitudAtencion(
            String nombre,
            String documentoIdentidad,
            String telefono,
            String email,
            String direccion,
            String cultivo,
            String interes,
            String mensaje
    ) {
        this.nombre = nombre;
        this.documentoIdentidad = documentoIdentidad;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.cultivo = cultivo;
        this.interes = interes;
        this.mensaje = mensaje;
        this.estado = EstadoSolicitudAtencion.PENDIENTE;
    }

    public void actualizarEstado(EstadoSolicitudAtencion estado, String atendidoPor) {
        this.estado = estado;
        this.atendidoPor = atendidoPor;
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

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCultivo() {
        return cultivo;
    }

    public String getInteres() {
        return interes;
    }

    public String getMensaje() {
        return mensaje;
    }

    public EstadoSolicitudAtencion getEstado() {
        return estado;
    }

    public String getAtendidoPor() {
        return atendidoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
