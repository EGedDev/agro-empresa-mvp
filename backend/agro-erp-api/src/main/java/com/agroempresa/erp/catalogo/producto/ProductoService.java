package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.dto.ActualizarProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ProductoService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "nombre", "nombre",
            "precioVenta", "precioVenta",
            "stockActual", "stockActual",
            "stockMinimo", "stockMinimo",
            "creadoEn", "creadoEn",
            "actualizadoEn", "actualizadoEn"
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.ASC, "nombre");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(
            String buscar,
            Boolean activo,
            Long categoriaId,
            Boolean stockBajo,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        return PaginaResponse.desde(
                productoRepository.buscar(
                        Paginacion.normalizarTexto(buscar),
                        activo,
                        categoriaId,
                        Boolean.TRUE.equals(stockBajo),
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_DEFAULT)
                ),
                ProductoResponse::desdeEntidad
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listarActivos(Integer pagina, Integer tamanio, String orden) {
        return listar(null, true, null, false, pagina, tamanio, orden);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listarConStockBajo(Integer pagina, Integer tamanio, String orden) {
        return listar(null, true, null, true, pagina, tamanio, orden);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = buscarProductoPorId(id);
        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return obtenerPorId(id);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new BusinessException("Ya existe un producto con ese nombre");
        }

        Categoria categoria = obtenerCategoriaActiva(request.categoriaId());

        Producto producto = new Producto(
                request.nombre(),
                request.descripcion(),
                request.precioVenta(),
                request.stockActual(),
                request.stockMinimo(),
                categoria
        );

        Producto productoGuardado = productoRepository.save(producto);

        return ProductoResponse.desdeEntidad(productoGuardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ActualizarProductoRequest request) {
        Producto producto = buscarProductoParaActualizar(id);

        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new BusinessException("Ya existe otro producto con ese nombre");
        }

        Categoria categoria = obtenerCategoriaActiva(request.categoriaId());

        producto.actualizar(
                request.nombre(),
                request.descripcion(),
                request.precioVenta(),
                request.stockMinimo(),
                categoria
        );

        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional
    public ProductoResponse desactivar(Long id) {
        Producto producto = buscarProductoParaActualizar(id);

        if (!producto.getActivo()) {
            throw new BusinessException("El producto ya se encuentra desactivado");
        }

        producto.desactivar();

        return ProductoResponse.desdeEntidad(producto);
    }

    private Categoria obtenerCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la categoría con id: " + categoriaId
                ));

        if (!categoria.getActivo()) {
            throw new BusinessException("No se puede usar una categoría desactivada");
        }

        return categoria;
    }

    private Producto buscarProductoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el producto con id: " + id
                ));
    }

    private Producto buscarProductoParaActualizar(Long id) {
        return productoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el producto con id: " + id
                ));
    }

}
