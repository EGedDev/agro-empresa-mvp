package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import com.agroempresa.erp.catalogo.categoria.CategoriaRepository;
import com.agroempresa.erp.catalogo.producto.dto.ActualizarProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoRequest;
import com.agroempresa.erp.catalogo.producto.dto.ProductoResponse;
import com.agroempresa.erp.common.error.BusinessException;
import com.agroempresa.erp.common.error.RecursoNoEncontradoException;
import com.agroempresa.erp.common.media.MediaProperties;
import com.agroempresa.erp.common.pagination.PaginaResponse;
import com.agroempresa.erp.common.pagination.Paginacion;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductoService {

    private static final Map<String, String> CAMPOS_ORDENABLES = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("nombre", "nombre"),
            Map.entry("precioVenta", "precioVenta"),
            Map.entry("costoPromedio", "costoPromedio"),
            Map.entry("valorInventario", "valorInventario"),
            Map.entry("stockActual", "stockActual"),
            Map.entry("stockMinimo", "stockMinimo"),
            Map.entry("visibleWeb", "visibleWeb"),
            Map.entry("destacado", "destacado"),
            Map.entry("ordenWeb", "ordenWeb"),
            Map.entry("creadoEn", "creadoEn"),
            Map.entry("actualizadoEn", "actualizadoEn")
    );

    private static final Sort ORDEN_DEFAULT = Sort.by(Sort.Direction.ASC, "nombre");
    private static final Sort ORDEN_WEB_DEFAULT = Sort.by(
            Sort.Order.desc("destacado"),
            Sort.Order.asc("ordenWeb"),
            Sort.Order.asc("nombre")
    );
    private static final long TAMANIO_MAXIMO_IMAGEN = 5 * 1024 * 1024;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MediaProperties mediaProperties;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            MediaProperties mediaProperties
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.mediaProperties = mediaProperties;
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
                        Paginacion.normalizarTextoBusqueda(buscar),
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
    public PaginaResponse<ProductoResponse> listarWeb(
            String buscar,
            Long categoriaId,
            Integer pagina,
            Integer tamanio,
            String orden
    ) {
        return PaginaResponse.desde(
                productoRepository.buscarWeb(
                        Paginacion.normalizarTextoBusqueda(buscar),
                        categoriaId,
                        Paginacion.crear(pagina, tamanio, orden, CAMPOS_ORDENABLES, ORDEN_WEB_DEFAULT)
                ),
                ProductoResponse::desdeEntidad
        );
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
                categoria,
                request.costoInicial(),
                request.imagenUrl(),
                request.imagenAlt(),
                request.resumenComercial(),
                request.visibleWeb(),
                request.destacado(),
                request.ordenWeb()
        );

        Producto productoGuardado = productoRepository.save(producto);
        productoGuardado.configurarFichaWeb(
                request.descripcionWeb(), request.informacionAdicional(), request.ingredienteActivo(),
                request.composicion(), request.formulacion(), request.numeroRegistro(), request.presentaciones(),
                request.cultivos(), request.modoUso(), request.fichaTecnicaUrl()
        );

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
                categoria,
                conservarSiNulo(request.imagenUrl(), producto.getImagenUrl()),
                conservarSiNulo(request.imagenAlt(), producto.getImagenAlt()),
                conservarSiNulo(request.resumenComercial(), producto.getResumenComercial()),
                request.visibleWeb() == null ? producto.getVisibleWeb() : request.visibleWeb(),
                request.destacado() == null ? producto.getDestacado() : request.destacado(),
                request.ordenWeb() == null ? producto.getOrdenWeb() : request.ordenWeb()
        );
        producto.configurarFichaWeb(
                request.descripcionWeb(), request.informacionAdicional(), request.ingredienteActivo(),
                request.composicion(), request.formulacion(), request.numeroRegistro(), request.presentaciones(),
                request.cultivos(), request.modoUso(), conservarSiNulo(request.fichaTecnicaUrl(), producto.getFichaTecnicaUrl())
        );

        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional
    public ProductoResponse actualizarImagen(Long id, MultipartFile archivo) {
        Producto producto = buscarProductoParaActualizar(id);
        String imagenUrl = guardarImagen(producto.getId(), archivo);
        producto.actualizarImagen(imagenUrl);
        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional
    public ProductoResponse actualizarFichaTecnica(Long id, MultipartFile archivo) {
        Producto producto = buscarProductoParaActualizar(id);
        producto.actualizarFichaTecnica(guardarPdf(producto.getId(), archivo));
        return ProductoResponse.desdeEntidad(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerWebPorId(Long id) {
        Producto producto = productoRepository.findByIdAndActivoTrueAndVisibleWebTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el producto publicado"));
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

    private String guardarImagen(Long productoId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debes enviar una imagen de producto");
        }

        if (archivo.getSize() > TAMANIO_MAXIMO_IMAGEN) {
            throw new BusinessException("La imagen no debe superar 5 MB");
        }

        String extension = extensionValida(archivo);
        String nombreArchivo = "producto-%d-%s%s".formatted(productoId, UUID.randomUUID(), extension);
        Path directorio = mediaProperties.uploadDir().resolve("productos").normalize().toAbsolutePath();
        Path destino = directorio.resolve(nombreArchivo).normalize();

        if (!destino.startsWith(directorio)) {
            throw new BusinessException("Nombre de archivo invalido");
        }

        try {
            Files.createDirectories(directorio);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo guardar la imagen del producto");
        }

        return "/media/productos/" + nombreArchivo;
    }

    private String extensionValida(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";

        if (nombreOriginal != null) {
            int punto = nombreOriginal.lastIndexOf('.');
            if (punto >= 0) {
                extension = nombreOriginal.substring(punto).toLowerCase(Locale.ROOT);
            }
        }

        if (List.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
            return extension;
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }

        throw new BusinessException("Solo se permiten imagenes JPG, PNG o WEBP");
    }

    private String guardarPdf(Long productoId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debes enviar una ficha tecnica en PDF");
        }
        if (archivo.getSize() > 15 * 1024 * 1024) {
            throw new BusinessException("El PDF no debe superar 15 MB");
        }
        String nombreOriginal = archivo.getOriginalFilename();
        boolean extensionPdf = nombreOriginal != null && nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (!extensionPdf && !"application/pdf".equalsIgnoreCase(archivo.getContentType())) {
            throw new BusinessException("Solo se permiten archivos PDF");
        }

        String nombreArchivo = "ficha-%d-%s.pdf".formatted(productoId, UUID.randomUUID());
        Path directorio = mediaProperties.uploadDir().resolve("fichas-tecnicas").normalize().toAbsolutePath();
        Path destino = directorio.resolve(nombreArchivo).normalize();
        try {
            Files.createDirectories(directorio);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo guardar la ficha tecnica");
        }
        return "/media/fichas-tecnicas/" + nombreArchivo;
    }

    private String conservarSiNulo(String valorNuevo, String valorActual) {
        return valorNuevo == null ? valorActual : valorNuevo;
    }
}
