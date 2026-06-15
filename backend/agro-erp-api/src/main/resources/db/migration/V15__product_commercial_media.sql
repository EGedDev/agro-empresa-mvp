alter table productos add column if not exists imagen_url varchar(500);
alter table productos add column if not exists imagen_alt varchar(160);
alter table productos add column if not exists resumen_comercial varchar(700);
alter table productos add column if not exists visible_web boolean;
alter table productos add column if not exists destacado boolean;
alter table productos add column if not exists orden_web integer;

update productos
set visible_web = true
where visible_web is null;

update productos
set destacado = false
where destacado is null;

update productos
set orden_web = 0
where orden_web is null;

alter table productos alter column visible_web set not null;
alter table productos alter column destacado set not null;
alter table productos alter column orden_web set not null;

create index if not exists idx_productos_visible_web on productos (visible_web);
create index if not exists idx_productos_destacado on productos (destacado);
create index if not exists idx_productos_orden_web on productos (orden_web);
