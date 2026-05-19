package com.agroempresa.erp.common.numeracion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeracionService {

    private static final long PRIMER_NUMERO = 1L;

    private final SecuenciaDocumentoRepository secuenciaDocumentoRepository;

    public NumeracionService(SecuenciaDocumentoRepository secuenciaDocumentoRepository) {
        this.secuenciaDocumentoRepository = secuenciaDocumentoRepository;
    }

    @Transactional
    public synchronized String generar(TipoDocumento tipoDocumento) {
        SecuenciaDocumento secuencia = secuenciaDocumentoRepository
                .findByCodigoParaActualizar(tipoDocumento.name())
                .orElseGet(() -> secuenciaDocumentoRepository.saveAndFlush(new SecuenciaDocumento(
                        tipoDocumento.name(),
                        tipoDocumento.getPrefijo(),
                        PRIMER_NUMERO
                )));

        return secuencia.generarSiguiente();
    }
}
