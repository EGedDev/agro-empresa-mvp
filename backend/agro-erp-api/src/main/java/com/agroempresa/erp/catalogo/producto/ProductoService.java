package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

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
    public List<ProductoResponse> listar() {
        return productoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarConStockBajo() {
        return productoRepository.findProductosConStockBajo()
                .stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

@Transactional(readOnly = true)
public ProductoResponse obtenerPorId(Long id) {
    Producto producto = buscarProductoPorId(id);
    return ProductoResponse.desdeEntidad(producto);
}



    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));

        return ProductoResponse.desdeEntidad(producto);
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
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));

        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new BusinessException("Ya existe otro producto con ese nombre");
        }

        Categoria categoria = obtenerCategoriaActiva(request.categoriaId());

        producto.actualizar(
                request.nombre(),
                request.descripcion(),
                request.precioVenta(),
                request.stockActual(),
                request.stockMinimo(),
                categoria
        );

        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional
    public ProductoResponse desactivar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));

        if (!producto.getActivo()) {
            throw new BusinessException("El producto ya se encuentra desactivado");
        }

        producto.desactivar();

        return ProductoResponse.desdeEntidad(producto);
    }

    private Categoria obtenerCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new BusinessException("Categoría no encontrada"));

        if (!categoria.getActivo()) {
            throw new BusinessException("No se puede usar una categoría desactivada");
        }

        return categoria;
    }

private Producto buscarProductoPorId(Long id) {
    return productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto con id: " + id));
}




}