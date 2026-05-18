package com.agroempresa.erp.catalogo.categoria;

import com.agroempresa.erp.catalogo.categoria.dto.CategoriaRequest;
import com.agroempresa.erp.catalogo.categoria.dto.CategoriaResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CategoriaService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "nombre", "nombre",
            "activo", "activo",
            "creadoEn", "creadoEn",
            "actualizadoEn", "actualizadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.ASC, "nombre");

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CategoriaResponse> listar(
            String buscar,
            Boolean activo,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        return PaginaResponse.desde(
                categoriaRepository.buscar(
                        Paginacion.normalizarTexto(buscar),
                        activo,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                this::toResponse
        );
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        Categoria categoria = buscarCategoriaPorId(id);
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombreNormalizado = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new BusinessException("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria(
                nombreNormalizado,
                request.descripcion()
        );

        Categoria categoriaGuardada = categoriaRepository.save(categoria);

        return toResponse(categoriaGuardada);
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarCategoriaPorId(id);

        String nombreNormalizado = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new BusinessException("Ya existe otra categoría con ese nombre");
        }

        categoria.actualizar(
                nombreNormalizado,
                request.descripcion()
        );

        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse desactivar(Long id) {
        Categoria categoria = buscarCategoriaPorId(id);

        if (!categoria.getActivo()) {
            throw new BusinessException("La categoría ya se encuentra desactivada");
        }

        categoria.desactivar();

        return toResponse(categoria);
    }

    private Categoria buscarCategoriaPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la categoría con id: " + id));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActivo(),
                categoria.getCreadoEn(),
                categoria.getActualizadoEn()
        );
    }
}
