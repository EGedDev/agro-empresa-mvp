package com.agroempresa.erp.catalogo.producto;

import com.agroempresa.erp.catalogo.categoria.Categoria;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
public class Producto {

    private static final int ESCALA_COSTO = 4;
    private static final int ESCALA_VALOR = 2;
    private static final BigDecimal COSTO_CERO = BigDecimal.ZERO.setScale(ESCALA_COSTO);
    private static final BigDecimal VALOR_CERO = BigDecimal.ZERO.setScale(ESCALA_VALOR);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal costoPromedio;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorInventario;

    @Column(nullable = false)
    private Integer stockActual;

    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "imagen_alt", length = 160)
    private String imagenAlt;

    @Column(name = "resumen_comercial", length = 700)
    private String resumenComercial;

    @Column(name = "descripcion_web", columnDefinition = "text")
    private String descripcionWeb;

    @Column(name = "informacion_adicional", columnDefinition = "text")
    private String informacionAdicional;

    @Column(name = "ingrediente_activo", length = 300)
    private String ingredienteActivo;

    @Column(name = "composicion", length = 500)
    private String composicion;

    @Column(name = "formulacion", length = 300)
    private String formulacion;

    @Column(name = "numero_registro", length = 200)
    private String numeroRegistro;

    @Column(name = "presentaciones", length = 500)
    private String presentaciones;

    @Column(name = "cultivos", length = 700)
    private String cultivos;

    @Column(name = "modo_uso", columnDefinition = "text")
    private String modoUso;

    @Column(name = "ficha_tecnica_url", length = 500)
    private String fichaTecnicaUrl;

    @Column(name = "visible_web", nullable = false)
    private Boolean visibleWeb = true;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(name = "orden_web", nullable = false)
    private Integer ordenWeb = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn;

    protected Producto() {
    }

    public Producto(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            Categoria categoria
    ) {
        this(nombre, descripcion, precioVenta, stockActual, stockMinimo, categoria, COSTO_CERO);
    }

    public Producto(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            Categoria categoria,
            BigDecimal costoInicial
    ) {
        this(nombre, descripcion, precioVenta, stockActual, stockMinimo, categoria, costoInicial, null, null, null, true, false, 0);
    }

    public Producto(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            Categoria categoria,
            BigDecimal costoInicial,
            String imagenUrl,
            String imagenAlt,
            String resumenComercial,
            Boolean visibleWeb,
            Boolean destacado,
            Integer ordenWeb
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        this.activo = true;
        inicializarValoracion(costoInicial);
        configurarComercial(imagenUrl, imagenAlt, resumenComercial, visibleWeb, destacado, ordenWeb);
    }

    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockMinimo,
            Categoria categoria,
            String imagenUrl,
            String imagenAlt,
            String resumenComercial,
            Boolean visibleWeb,
            Boolean destacado,
            Integer ordenWeb
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        configurarComercial(imagenUrl, imagenAlt, resumenComercial, visibleWeb, destacado, ordenWeb);
    }

    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precioVenta,
            Integer stockMinimo,
            Categoria categoria
    ) {
        actualizar(nombre, descripcion, precioVenta, stockMinimo, categoria, imagenUrl, imagenAlt, resumenComercial, visibleWeb, destacado, ordenWeb);
    }

    public void actualizarImagen(String imagenUrl) {
        this.imagenUrl = textoOpcional(imagenUrl);
        this.imagenAlt = textoOpcional(this.imagenAlt) == null ? this.nombre : this.imagenAlt;
        this.visibleWeb = true;
    }

    public void actualizarFichaTecnica(String fichaTecnicaUrl) {
        this.fichaTecnicaUrl = textoOpcional(fichaTecnicaUrl);
    }

    public void configurarFichaWeb(
            String descripcionWeb,
            String informacionAdicional,
            String ingredienteActivo,
            String composicion,
            String formulacion,
            String numeroRegistro,
            String presentaciones,
            String cultivos,
            String modoUso,
            String fichaTecnicaUrl
    ) {
        this.descripcionWeb = textoOpcional(descripcionWeb);
        this.informacionAdicional = textoOpcional(informacionAdicional);
        this.ingredienteActivo = textoOpcional(ingredienteActivo);
        this.composicion = textoOpcional(composicion);
        this.formulacion = textoOpcional(formulacion);
        this.numeroRegistro = textoOpcional(numeroRegistro);
        this.presentaciones = textoOpcional(presentaciones);
        this.cultivos = textoOpcional(cultivos);
        this.modoUso = textoOpcional(modoUso);
        this.fichaTecnicaUrl = textoOpcional(fichaTecnicaUrl);
    }

    public void configurarComercial(
            String imagenUrl,
            String imagenAlt,
            String resumenComercial,
            Boolean visibleWeb,
            Boolean destacado,
            Integer ordenWeb
    ) {
        this.imagenUrl = textoOpcional(imagenUrl);
        this.imagenAlt = textoOpcional(imagenAlt);
        this.resumenComercial = textoOpcional(resumenComercial);
        this.visibleWeb = visibleWeb == null || visibleWeb;
        this.destacado = destacado != null && destacado;
        this.ordenWeb = ordenWeb == null ? 0 : ordenWeb;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void descontarStock(Integer cantidad) {
        descontarStockConCosto(cantidad, this.costoPromedio);
    }

    public void descontarStockConCosto(Integer cantidad, BigDecimal costoUnitario) {
        validarCantidadMovimiento(cantidad);

        if (this.stockActual < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para descontar inventario");
        }

        BigDecimal valorSalida = valorMovimiento(cantidad, costoUnitario);
        this.stockActual = this.stockActual - cantidad;
        actualizarValorInventario(this.valorInventario.subtract(valorSalida));
    }

    public void aumentarStock(Integer cantidad) {
        aumentarStockConCosto(cantidad, this.costoPromedio);
    }

    public void aumentarStockConCosto(Integer cantidad, BigDecimal costoUnitario) {
        validarCantidadMovimiento(cantidad);

        BigDecimal valorEntrada = valorMovimiento(cantidad, costoUnitario);
        try {
            this.stockActual = Math.addExact(this.stockActual, cantidad);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("El stock supera el límite permitido");
        }

        actualizarValorInventario(this.valorInventario.add(valorEntrada));
    }

    private void validarCantidadMovimiento(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
    }

    private void inicializarValoracion(BigDecimal costoInicial) {
        this.costoPromedio = normalizarCosto(costoInicial);

        if (this.stockActual == null || this.stockActual == 0) {
            this.valorInventario = VALOR_CERO;
            this.costoPromedio = COSTO_CERO;
            return;
        }

        this.valorInventario = valorMovimiento(this.stockActual, this.costoPromedio);
    }

    private BigDecimal valorMovimiento(Integer cantidad, BigDecimal costoUnitario) {
        return normalizarCosto(costoUnitario)
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(ESCALA_VALOR, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarCosto(BigDecimal costoUnitario) {
        if (costoUnitario == null) {
            return COSTO_CERO;
        }

        if (costoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }

        return costoUnitario.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private void actualizarValorInventario(BigDecimal nuevoValor) {
        if (this.stockActual == 0) {
            this.valorInventario = VALOR_CERO;
            this.costoPromedio = COSTO_CERO;
            return;
        }

        this.valorInventario = nuevoValor.max(BigDecimal.ZERO).setScale(ESCALA_VALOR, RoundingMode.HALF_UP);
        this.costoPromedio = this.valorInventario.divide(
                BigDecimal.valueOf(this.stockActual),
                ESCALA_COSTO,
                RoundingMode.HALF_UP
        );
    }


    @PrePersist
    protected void antesDeCrear() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void antesDeActualizar() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }

    public BigDecimal getValorInventario() {
        return valorInventario;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public String getImagenAlt() {
        return imagenAlt;
    }

    public String getResumenComercial() {
        return resumenComercial;
    }

    public String getDescripcionWeb() { return descripcionWeb; }
    public String getInformacionAdicional() { return informacionAdicional; }
    public String getIngredienteActivo() { return ingredienteActivo; }
    public String getComposicion() { return composicion; }
    public String getFormulacion() { return formulacion; }
    public String getNumeroRegistro() { return numeroRegistro; }
    public String getPresentaciones() { return presentaciones; }
    public String getCultivos() { return cultivos; }
    public String getModoUso() { return modoUso; }
    public String getFichaTecnicaUrl() { return fichaTecnicaUrl; }

    public Boolean getVisibleWeb() {
        return visibleWeb;
    }

    public Boolean getDestacado() {
        return destacado;
    }

    public Integer getOrdenWeb() {
        return ordenWeb;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
