package com.agroempresa.erp.common.pagination;

import com.agroempresa.erp.common.error.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class Paginacion {

    public static final int PAGINA_DEFAULT = 0;
    public static final int TAMANIO_DEFAULT = 20;
    public static final int TAMANIO_MAXIMO = 100;

    private Paginacion() {
    }

    public static Pageable crear(
            Integer pagina,
            Integer tamanio,
            String orden,
            Map<String, String> camposOrdenables,
            Sort ordenDefault
    ) {
        int paginaNormalizada = pagina == null ? PAGINA_DEFAULT : pagina;
        int tamanioNormalizado = tamanio == null ? TAMANIO_DEFAULT : tamanio;

        if (paginaNormalizada < 0) {
            throw new BusinessException("La pagina no puede ser negativa");
        }

        if (tamanioNormalizado < 1 || tamanioNormalizado > TAMANIO_MAXIMO) {
            throw new BusinessException("El tamanio de pagina debe estar entre 1 y " + TAMANIO_MAXIMO);
        }

        return PageRequest.of(paginaNormalizada, tamanioNormalizado, construirOrden(orden, camposOrdenables, ordenDefault));
    }

    public static String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim().toLowerCase();
    }

    public static String normalizarTextoBusqueda(String valor) {
        String textoNormalizado = normalizarTexto(valor);
        return textoNormalizado == null ? "" : textoNormalizado;
    }

    private static Sort construirOrden(String orden, Map<String, String> camposOrdenables, Sort ordenDefault) {
        if (orden == null || orden.isBlank()) {
            return ordenDefault;
        }

        String[] partes = orden.split(",", -1);
        if (partes.length > 2) {
            throw new BusinessException("Formato de ordenamiento invalido. Use campo,direccion");
        }

        String campoSolicitado = partes[0].trim();

        if (campoSolicitado.isBlank()) {
            return ordenDefault;
        }

        String propiedad = camposOrdenables.get(campoSolicitado);
        if (propiedad == null) {
            throw new BusinessException("Campo de ordenamiento no permitido: " + campoSolicitado);
        }

        Sort.Direction direccion = Sort.Direction.ASC;
        if (partes.length > 1 && !partes[1].isBlank()) {
            try {
                direccion = Sort.Direction.fromString(partes[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Direccion de ordenamiento invalida: " + partes[1].trim());
            }
        }

        return Sort.by(direccion, propiedad);
    }
}
