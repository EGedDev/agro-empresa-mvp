alter table ventas
    add column if not exists fecha_vencimiento date;

update ventas
set fecha_vencimiento = fecha_venta::date
where fecha_vencimiento is null;

alter table ventas
    alter column fecha_vencimiento set not null;

create index if not exists idx_ventas_fecha_vencimiento
    on ventas (fecha_vencimiento);

alter table compras
    add column if not exists fecha_vencimiento date;

update compras
set fecha_vencimiento = fecha_compra::date
where fecha_vencimiento is null;

alter table compras
    alter column fecha_vencimiento set not null;

create index if not exists idx_compras_fecha_vencimiento
    on compras (fecha_vencimiento);
