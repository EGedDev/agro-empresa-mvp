package com.agroempresa.erp.common.numeracion;

import java.util.Locale;

public final class NumeroDocumento {

    private NumeroDocumento() {
    }

    public static String normalizarFiltro(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }

        return numero.trim().toUpperCase(Locale.ROOT);
    }
}
