package com.agroempresa.erp.auditoria;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(nullable = false, length = 80)
    private String recursoTipo;

    private Long recursoId;

    @Column(length = 500)
    private String detalle;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected AuditoriaEvento() {
    }

    public AuditoriaEvento(
            String username,
            String accion,
            String recursoTipo,
            Long recursoId,
            String detalle,
            String correlationId,
            String ipAddress,
            String userAgent
    ) {
        this.username = username;
        this.accion = accion;
        this.recursoTipo = recursoTipo;
        this.recursoId = recursoId;
        this.detalle = detalle;
        this.correlationId = correlationId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAccion() {
        return accion;
    }

    public String getRecursoTipo() {
        return recursoTipo;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
