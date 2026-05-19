alter table pagos_venta add column anulado boolean not null default false;
alter table pagos_venta add column fecha_anulacion timestamp(6) without time zone;
alter table pagos_venta add column motivo_anulacion varchar(300);

alter table pagos_compra add column anulado boolean not null default false;
alter table pagos_compra add column fecha_anulacion timestamp(6) without time zone;
alter table pagos_compra add column motivo_anulacion varchar(300);

create index idx_pagos_venta_anulado on pagos_venta (anulado);
create index idx_pagos_venta_fecha_anulacion on pagos_venta (fecha_anulacion);
create index idx_pagos_compra_anulado on pagos_compra (anulado);
create index idx_pagos_compra_fecha_anulacion on pagos_compra (fecha_anulacion);
