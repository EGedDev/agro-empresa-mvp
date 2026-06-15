create table if not exists solicitudes_atencion (
    id bigserial primary key,
    nombre varchar(160) not null,
    documento_identidad varchar(20),
    telefono varchar(30),
    email varchar(160),
    direccion varchar(250),
    cultivo varchar(120),
    interes varchar(120),
    mensaje varchar(500),
    estado varchar(30) not null default 'PENDIENTE',
    atendido_por varchar(120),
    creado_en timestamp not null default current_timestamp,
    actualizado_en timestamp not null default current_timestamp
);

create index if not exists idx_solicitudes_atencion_estado
    on solicitudes_atencion (estado);

create index if not exists idx_solicitudes_atencion_creado_en
    on solicitudes_atencion (creado_en);
