alter table movimientos_inventario drop constraint if exists movimientos_inventario_tipo_check;
alter table movimientos_inventario drop constraint if exists chk_movimientos_inventario_tipo;

alter table movimientos_inventario
    add constraint chk_movimientos_inventario_tipo check (
        tipo in (
            'ENTRADA_MANUAL',
            'SALIDA_POR_VENTA',
            'ENTRADA_POR_CANCELACION',
            'ENTRADA_POR_DEVOLUCION_VENTA',
            'ENTRADA_POR_COMPRA',
            'SALIDA_POR_CANCELACION_COMPRA',
            'SALIDA_POR_DEVOLUCION_COMPRA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO'
        )
    );
