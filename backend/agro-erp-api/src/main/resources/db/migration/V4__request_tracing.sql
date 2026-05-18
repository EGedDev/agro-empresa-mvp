alter table auditoria_eventos
    add column correlation_id varchar(120),
    add column ip_address varchar(80),
    add column user_agent varchar(255);

create index idx_auditoria_eventos_correlation_id on auditoria_eventos (correlation_id);
