alter table productos add column if not exists costo_promedio numeric(12, 4);
alter table productos add column if not exists valor_inventario numeric(14, 2);

update productos
set costo_promedio = 0,
    valor_inventario = 0
where costo_promedio is null
   or valor_inventario is null;

alter table productos alter column costo_promedio set not null;
alter table productos alter column valor_inventario set not null;

alter table productos drop constraint if exists chk_productos_costo_promedio_no_negativo;
alter table productos
    add constraint chk_productos_costo_promedio_no_negativo check (costo_promedio >= 0);

alter table productos drop constraint if exists chk_productos_valor_inventario_no_negativo;
alter table productos
    add constraint chk_productos_valor_inventario_no_negativo check (valor_inventario >= 0);

create index if not exists idx_productos_valor_inventario on productos (valor_inventario);

alter table venta_detalles add column if not exists costo_unitario numeric(12, 4);
alter table venta_detalles add column if not exists costo_total numeric(14, 2);

update venta_detalles
set costo_unitario = 0,
    costo_total = 0
where costo_unitario is null
   or costo_total is null;

alter table venta_detalles alter column costo_unitario set not null;
alter table venta_detalles alter column costo_total set not null;

alter table venta_detalles drop constraint if exists chk_venta_detalles_costo_unitario_no_negativo;
alter table venta_detalles
    add constraint chk_venta_detalles_costo_unitario_no_negativo check (costo_unitario >= 0);

alter table venta_detalles drop constraint if exists chk_venta_detalles_costo_total_no_negativo;
alter table venta_detalles
    add constraint chk_venta_detalles_costo_total_no_negativo check (costo_total >= 0);

alter table movimientos_inventario add column if not exists costo_unitario numeric(12, 4);
alter table movimientos_inventario add column if not exists valor_movimiento numeric(14, 2);
alter table movimientos_inventario add column if not exists valor_inventario_anterior numeric(14, 2);
alter table movimientos_inventario add column if not exists valor_inventario_nuevo numeric(14, 2);

update movimientos_inventario
set costo_unitario = 0,
    valor_movimiento = 0,
    valor_inventario_anterior = 0,
    valor_inventario_nuevo = 0
where costo_unitario is null
   or valor_movimiento is null
   or valor_inventario_anterior is null
   or valor_inventario_nuevo is null;

alter table movimientos_inventario alter column costo_unitario set not null;
alter table movimientos_inventario alter column valor_movimiento set not null;
alter table movimientos_inventario alter column valor_inventario_anterior set not null;
alter table movimientos_inventario alter column valor_inventario_nuevo set not null;

alter table movimientos_inventario drop constraint if exists chk_movimientos_inventario_costo_unitario_no_negativo;
alter table movimientos_inventario
    add constraint chk_movimientos_inventario_costo_unitario_no_negativo check (costo_unitario >= 0);

alter table movimientos_inventario drop constraint if exists chk_movimientos_inventario_valor_movimiento_no_negativo;
alter table movimientos_inventario
    add constraint chk_movimientos_inventario_valor_movimiento_no_negativo check (valor_movimiento >= 0);

alter table movimientos_inventario drop constraint if exists chk_movimientos_inventario_valor_anterior_no_negativo;
alter table movimientos_inventario
    add constraint chk_movimientos_inventario_valor_anterior_no_negativo check (valor_inventario_anterior >= 0);

alter table movimientos_inventario drop constraint if exists chk_movimientos_inventario_valor_nuevo_no_negativo;
alter table movimientos_inventario
    add constraint chk_movimientos_inventario_valor_nuevo_no_negativo check (valor_inventario_nuevo >= 0);

create index if not exists idx_movimientos_inventario_valor_movimiento on movimientos_inventario (valor_movimiento);
