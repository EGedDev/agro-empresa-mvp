package com.agroempresa.erp.proveedor;

import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import com.agroempresa.erp.proveedor.dto.ProveedorRequest;
import com.agroempresa.erp.proveedor.dto.ProveedorResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProveedorService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "nombre", "nombre",
            "documentoIdentidad", "documentoIdentidad",
            "activo", "activo",
            "creadoEn", "creadoEn",
            "actualizadoEn", "actualizadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.ASC, "nombre");

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProveedorResponse> listar(
            String buscar,
            Boolean activo,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        return PaginaResponse.desde(
                proveedorRepository.buscar(
                        Paginacion.normalizarTextoBusqueda(buscar),
                        activo,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                ProveedorResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProveedorResponse> listarActivos(Integer pagina, Integer tamanio, String orden) {
        return listar(null, true, pagina, tamanio, orden);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtenerPorId(Long id) {
        Proveedor proveedor = buscarProveedorPorId(id);
        return ProveedorResponse.desdeEntidad(proveedor);
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        validarRequest(request);

        String nombre = normalizar(request.nombre());
        String documentoIdentidad = normalizar(request.documentoIdentidad());

        validarDocumentoDisponible(documentoIdentidad, null);

        Proveedor proveedor = new Proveedor(
                nombre,
                documentoIdentidad,
                normalizar(request.telefono()),
                normalizar(request.email()),
                normalizar(request.direccion())
        );

        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        return ProveedorResponse.desdeEntidad(proveedorGuardado);
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        validarRequest(request);

        Proveedor proveedor = buscarProveedorPorId(id);

        String nombre = normalizar(request.nombre());
        String documentoIdentidad = normalizar(request.documentoIdentidad());

        validarDocumentoDisponible(documentoIdentidad, id);

        proveedor.actualizar(
                nombre,
                documentoIdentidad,
                normalizar(request.telefono()),
                normalizar(request.email()),
                normalizar(request.direccion())
        );

        return ProveedorResponse.desdeEntidad(proveedor);
    }

    @Transactional
    public ProveedorResponse desactivar(Long id) {
        Proveedor proveedor = buscarProveedorPorId(id);

        if (!proveedor.getActivo()) {
            throw new BusinessException("El proveedor ya se encuentra desactivado");
        }

        proveedor.desactivar();

        return ProveedorResponse.desdeEntidad(proveedor);
    }

    private Proveedor buscarProveedorPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con id: " + id));
    }

    private void validarRequest(ProveedorRequest request) {
        if (request == null) {
            throw new BusinessException("Los datos del proveedor son obligatorios");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("El nombre del proveedor es obligatorio");
        }
    }

    private void validarDocumentoDisponible(String documentoIdentidad, Long idActual) {
        if (documentoIdentidad == null) {
            return;
        }

        proveedorRepository.findByDocumentoIdentidadIgnoreCase(documentoIdentidad)
                .filter(proveedor -> !proveedor.getId().equals(idActual))
                .ifPresent(proveedor -> {
                    throw new BusinessException("Ya existe un proveedor con el documento: " + documentoIdentidad);
                });
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
