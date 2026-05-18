package com.agroempresa.erp.common.validation;

import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.comercial.compra.dto.CompraDetalleRequest;
import com.agroempresa.erp.finanzas.MetodoPago;
import com.agroempresa.erp.finanzas.pago.compra.dto.RegistrarPagoCompraRequest;
import com.agroempresa.erp.finanzas.pago.venta.dto.RegistrarPagoVentaRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacionMonetariaTest {

    private final Validator validator;

    ValidacionMonetariaTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void productoRequestRechazaPrecioConMasDeDosDecimales() {
        ProductoRequest request = new ProductoRequest(
                "Urea",
                null,
                new BigDecimal("120.999"),
                10,
                2,
                1L
        );

        Set<ConstraintViolation<ProductoRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(ConstraintViolation::getMessage)
                .contains("El precio de venta debe tener como máximo 10 enteros y 2 decimales");
    }

    @Test
    void compraDetalleRequestRechazaCostoConMasDeDosDecimales() {
        CompraDetalleRequest request = new CompraDetalleRequest(
                1L,
                3,
                new BigDecimal("30.999")
        );

        Set<ConstraintViolation<CompraDetalleRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(ConstraintViolation::getMessage)
                .contains("El costo unitario debe tener como máximo 10 enteros y 2 decimales");
    }

    @Test
    void registrarPagoVentaRequestRechazaMontoConMasDeDosDecimales() {
        RegistrarPagoVentaRequest request = new RegistrarPagoVentaRequest(
                new BigDecimal("10.999"),
                MetodoPago.EFECTIVO,
                null
        );

        Set<ConstraintViolation<RegistrarPagoVentaRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(ConstraintViolation::getMessage)
                .contains("El monto del pago debe tener como máximo 10 enteros y 2 decimales");
    }

    @Test
    void registrarPagoCompraRequestRechazaMontoConMasDeDosDecimales() {
        RegistrarPagoCompraRequest request = new RegistrarPagoCompraRequest(
                new BigDecimal("10.999"),
                MetodoPago.EFECTIVO,
                null
        );

        Set<ConstraintViolation<RegistrarPagoCompraRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(ConstraintViolation::getMessage)
                .contains("El monto del pago debe tener como máximo 10 enteros y 2 decimales");
    }
}
