package com.agroempresa.erp.idempotencia;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "solicitudes_idempotentes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solicitudes_idempotentes_scope_key",
                columnNames = {"username", "metodo_http", "ruta", "idempotency_key"}
        ),
        indexes = {
                @Index(name = "idx_solicitudes_idempotentes_creado_en", columnList = "creado_en")
        }
)
public class SolicitudIdempotente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "metodo_http", nullable = false, length = 10)
    private String metodoHttp;

    @Column(nullable = false, length = 250)
    private String ruta;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudIdempotente estado;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_content_type", length = 160)
    private String responseContentType;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    protected SolicitudIdempotente() {
    }

    public SolicitudIdempotente(
            String idempotencyKey,
            String username,
            String metodoHttp,
            String ruta,
            String requestHash
    ) {
        this.idempotencyKey = idempotencyKey;
        this.username = username;
        this.metodoHttp = metodoHttp;
        this.ruta = ruta;
        this.requestHash = requestHash;
        this.estado = EstadoSolicitudIdempotente.EN_PROCESO;
    }

    public void completar(Integer responseStatus, String responseContentType, String responseBody) {
        this.estado = EstadoSolicitudIdempotente.COMPLETADA;
        this.responseStatus = responseStatus;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
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

    public String getRequestHash() {
        return requestHash;
    }

    public EstadoSolicitudIdempotente getEstado() {
        return estado;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
