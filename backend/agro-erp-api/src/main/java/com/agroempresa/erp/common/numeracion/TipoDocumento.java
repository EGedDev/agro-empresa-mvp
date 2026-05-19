package com.agroempresa.erp.common.numeracion;

public enum TipoDocumento {
    VENTA("V"),
    COMPRA("C"),
    PAGO_VENTA("PV"),
    PAGO_COMPRA("PC"),
    DEVOLUCION_VENTA("DV"),
    DEVOLUCION_COMPRA("DC"),
    CIERRE_CAJA("CC");

    private final String prefijo;

    TipoDocumento(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
