package com.agroempresa.erp.common.error;

public class BusinessException extends IllegalArgumentException {

    public BusinessException(String message) {
        super(message);
    }
}