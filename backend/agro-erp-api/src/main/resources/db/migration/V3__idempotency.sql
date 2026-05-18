CREATE TABLE solicitudes_idempotentes (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL,
    metodo_http VARCHAR(10) NOT NULL,
    ruta VARCHAR(250) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    response_status INTEGER,
    response_content_type VARCHAR(160),
    response_body TEXT,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uk_solicitudes_idempotentes_scope_key
        UNIQUE (username, metodo_http, ruta, idempotency_key),
    CONSTRAINT ck_solicitudes_idempotentes_estado
        CHECK (estado IN ('EN_PROCESO', 'COMPLETADA'))
);

CREATE INDEX idx_solicitudes_idempotentes_creado_en
    ON solicitudes_idempotentes (creado_en);
