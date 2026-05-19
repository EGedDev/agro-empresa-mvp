create table secuencias_documento (
    codigo varchar(40) primary key,
    prefijo varchar(12) not null,
    siguiente_numero bigint not null,
    version bigint not null,
    constraint chk_secuencias_documento_siguiente_positivo check (siguiente_numero > 0)
);

alter table ventas add column numero varchar(20);
update ventas set numero = concat('V-', lpad(id::text, 6, '0')) where numero is null;
alter table ventas alter column numero set not null;
alter table ventas add constraint uk_ventas_numero unique (numero);

alter table compras add column numero varchar(20);
update compras set numero = concat('C-', lpad(id::text, 6, '0')) where numero is null;
alter table compras alter column numero set not null;
alter table compras add constraint uk_compras_numero unique (numero);

alter table pagos_venta add column numero varchar(20);
update pagos_venta set numero = concat('PV-', lpad(id::text, 6, '0')) where numero is null;
alter table pagos_venta alter column numero set not null;
alter table pagos_venta add constraint uk_pagos_venta_numero unique (numero);

alter table pagos_compra add column numero varchar(20);
update pagos_compra set numero = concat('PC-', lpad(id::text, 6, '0')) where numero is null;
alter table pagos_compra alter column numero set not null;
alter table pagos_compra add constraint uk_pagos_compra_numero unique (numero);

alter table devoluciones_venta add column numero varchar(20);
update devoluciones_venta set numero = concat('DV-', lpad(id::text, 6, '0')) where numero is null;
alter table devoluciones_venta alter column numero set not null;
alter table devoluciones_venta add constraint uk_devoluciones_venta_numero unique (numero);

alter table devoluciones_compra add column numero varchar(20);
update devoluciones_compra set numero = concat('DC-', lpad(id::text, 6, '0')) where numero is null;
alter table devoluciones_compra alter column numero set not null;
alter table devoluciones_compra add constraint uk_devoluciones_compra_numero unique (numero);

alter table cierres_caja add column numero varchar(20);
update cierres_caja set numero = concat('CC-', lpad(id::text, 6, '0')) where numero is null;
alter table cierres_caja alter column numero set not null;
alter table cierres_caja add constraint uk_cierres_caja_numero unique (numero);

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'VENTA', 'V', coalesce(max(id), 0) + 1, 0 from ventas;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'COMPRA', 'C', coalesce(max(id), 0) + 1, 0 from compras;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'PAGO_VENTA', 'PV', coalesce(max(id), 0) + 1, 0 from pagos_venta;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'PAGO_COMPRA', 'PC', coalesce(max(id), 0) + 1, 0 from pagos_compra;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'DEVOLUCION_VENTA', 'DV', coalesce(max(id), 0) + 1, 0 from devoluciones_venta;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'DEVOLUCION_COMPRA', 'DC', coalesce(max(id), 0) + 1, 0 from devoluciones_compra;

insert into secuencias_documento (codigo, prefijo, siguiente_numero, version)
select 'CIERRE_CAJA', 'CC', coalesce(max(id), 0) + 1, 0 from cierres_caja;
