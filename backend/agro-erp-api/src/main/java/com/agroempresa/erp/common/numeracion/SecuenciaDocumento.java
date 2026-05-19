package com.agroempresa.erp.common.numeracion;

import jakarta.persistence.*;

@Entity
@Table(name = "secuencias_documento")
public class SecuenciaDocumento {

    private static final int ANCHO_NUMERO = 6;

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 12)
    private String prefijo;

    @Column(nullable = false)
    private Long siguienteNumero;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected SecuenciaDocumento() {
    }

    public SecuenciaDocumento(String codigo, String prefijo, Long siguienteNumero) {
        this.codigo = codigo;
        this.prefijo = prefijo;
        this.siguienteNumero = siguienteNumero;
    }

    public String generarSiguiente() {
        String numero = "%s-%0" + ANCHO_NUMERO + "d";
        String correlativo = numero.formatted(prefijo, siguienteNumero);
        siguienteNumero++;
        return correlativo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public Long getSiguienteNumero() {
        return siguienteNumero;
    }

    public Long getVersion() {
        return version;
    }
}
