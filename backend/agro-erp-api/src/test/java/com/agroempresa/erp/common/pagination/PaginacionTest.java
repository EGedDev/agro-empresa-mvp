package com.agroempresa.erp.common.pagination;

import com.agroempresa.erp.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginacionTest {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "nombre", "nombre",
            "creadoEn", "creadoEn"
    );

    @Test
    void creaPageableConOrdenPermitido() {
        Pageable pageable = Paginacion.crear(
                1,
                25,
                "creadoEn,desc",
                CAMPOS_ORDENABLES,
                Sort.by(Sort.Direction.ASC, "nombre")
        );

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("creadoEn").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rechazaCampoDeOrdenamientoNoPermitido() {
        assertThatThrownBy(() -> Paginacion.crear(
                0,
                20,
                "passwordHash,asc",
                CAMPOS_ORDENABLES,
                Sort.by(Sort.Direction.ASC, "nombre")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Campo de ordenamiento no permitido: passwordHash");
    }

    @Test
    void rechazaFormatoDeOrdenamientoConPartesExtra() {
        assertThatThrownBy(() -> Paginacion.crear(
                0,
                20,
                "nombre,asc,extra",
                CAMPOS_ORDENABLES,
                Sort.by(Sort.Direction.ASC, "nombre")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Formato de ordenamiento invalido. Use campo,direccion");
    }
}
