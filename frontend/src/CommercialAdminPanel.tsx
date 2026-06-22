import { ExternalLink, FileText, ImagePlus, Loader2, RefreshCw, Save, Store } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { apiBaseUrl, apiRequest, getPage, uploadFile } from "./api";

type ProductoAdmin = {
  id: number;
  nombre: string;
  descripcion?: string | null;
  precioVenta?: number | string | null;
  stockActual?: number | null;
  stockMinimo?: number | null;
  imagenUrl?: string | null;
  imagenAlt?: string | null;
  resumenComercial?: string | null;
  descripcionWeb?: string | null;
  informacionAdicional?: string | null;
  ingredienteActivo?: string | null;
  composicion?: string | null;
  formulacion?: string | null;
  numeroRegistro?: string | null;
  presentaciones?: string | null;
  cultivos?: string | null;
  modoUso?: string | null;
  fichaTecnicaUrl?: string | null;
  destacado?: boolean | null;
  visibleWeb?: boolean | null;
  ordenWeb?: number | null;
  categoriaId?: number | null;
  categoriaNombre?: string | null;
};

type CategoriaAdmin = {
  id: number;
  nombre: string;
};

type EditorForm = {
  nombre: string;
  categoriaId: string;
  precioVenta: string;
  stockMinimo: string;
  descripcion: string;
  resumenComercial: string;
  descripcionWeb: string;
  informacionAdicional: string;
  ingredienteActivo: string;
  composicion: string;
  formulacion: string;
  numeroRegistro: string;
  presentaciones: string;
  cultivos: string;
  modoUso: string;
  imagenAlt: string;
  visibleWeb: boolean;
  destacado: boolean;
  ordenWeb: string;
};

const emptyForm: EditorForm = {
  nombre: "",
  categoriaId: "",
  precioVenta: "",
  stockMinimo: "0",
  descripcion: "",
  resumenComercial: "",
  descripcionWeb: "",
  informacionAdicional: "",
  ingredienteActivo: "",
  composicion: "",
  formulacion: "",
  numeroRegistro: "",
  presentaciones: "",
  cultivos: "",
  modoUso: "",
  imagenAlt: "",
  visibleWeb: true,
  destacado: false,
  ordenWeb: "0"
};

export function CommercialAdminPanel({ token }: { token: string }) {
  const [productos, setProductos] = useState<ProductoAdmin[]>([]);
  const [categorias, setCategorias] = useState<CategoriaAdmin[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [form, setForm] = useState<EditorForm>(emptyForm);
  const [file, setFile] = useState<File | null>(null);
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const selectedProduct = productos.find((product) => String(product.id) === selectedId);
  const previewUrl = useMemo(() => (file ? URL.createObjectURL(file) : null), [file]);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  async function load() {
    setLoading(true);
    setError(null);

    try {
      const [productPage, categoryPage] = await Promise.all([
        getPage<ProductoAdmin>("/api/v1/productos?size=120&sort=nombre,asc", token),
        getPage<CategoriaAdmin>("/api/v1/categorias?activo=true&size=120&sort=nombre,asc", token)
      ]);
      setProductos(productPage.contenido);
      setCategorias(categoryPage.contenido);
      setSelectedId((current) => current || String(productPage.contenido[0]?.id ?? ""));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "No se pudo cargar la vitrina");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  useEffect(() => {
    if (!selectedProduct) {
      setForm(emptyForm);
      return;
    }

    setForm({
      nombre: selectedProduct.nombre,
      categoriaId: String(selectedProduct.categoriaId ?? ""),
      precioVenta: selectedProduct.precioVenta == null ? "" : String(selectedProduct.precioVenta),
      stockMinimo: String(selectedProduct.stockMinimo ?? 0),
      descripcion: selectedProduct.descripcion ?? "",
      resumenComercial: selectedProduct.resumenComercial ?? "",
      descripcionWeb: selectedProduct.descripcionWeb ?? "",
      informacionAdicional: selectedProduct.informacionAdicional ?? "",
      ingredienteActivo: selectedProduct.ingredienteActivo ?? "",
      composicion: selectedProduct.composicion ?? "",
      formulacion: selectedProduct.formulacion ?? "",
      numeroRegistro: selectedProduct.numeroRegistro ?? "",
      presentaciones: selectedProduct.presentaciones ?? "",
      cultivos: selectedProduct.cultivos ?? "",
      modoUso: selectedProduct.modoUso ?? "",
      imagenAlt: selectedProduct.imagenAlt ?? selectedProduct.nombre,
      visibleWeb: selectedProduct.visibleWeb ?? true,
      destacado: selectedProduct.destacado ?? false,
      ordenWeb: String(selectedProduct.ordenWeb ?? 0)
    });
    setFile(null);
    setPdfFile(null);
  }, [selectedProduct]);

  async function submit(event: FormEvent) {
    event.preventDefault();

    if (!selectedProduct || !selectedId || !form.categoriaId) {
      return;
    }

    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const updated = await apiRequest<ProductoAdmin>(`/api/v1/productos/${selectedId}`, {
        method: "PUT",
        token,
        body: {
          nombre: form.nombre.trim(),
          descripcion: emptyToNull(form.descripcion),
          precioVenta: moneyInput(form.precioVenta),
          stockMinimo: integerInput(form.stockMinimo),
          categoriaId: Number(form.categoriaId),
          imagenUrl: selectedProduct.imagenUrl ?? null,
          imagenAlt: emptyToNull(form.imagenAlt),
          resumenComercial: emptyToNull(form.resumenComercial),
          descripcionWeb: emptyToNull(form.descripcionWeb),
          informacionAdicional: emptyToNull(form.informacionAdicional),
          ingredienteActivo: emptyToNull(form.ingredienteActivo),
          composicion: emptyToNull(form.composicion),
          formulacion: emptyToNull(form.formulacion),
          numeroRegistro: emptyToNull(form.numeroRegistro),
          presentaciones: emptyToNull(form.presentaciones),
          cultivos: emptyToNull(form.cultivos),
          modoUso: emptyToNull(form.modoUso),
          fichaTecnicaUrl: selectedProduct.fichaTecnicaUrl ?? null,
          visibleWeb: form.visibleWeb,
          destacado: form.destacado,
          ordenWeb: integerInput(form.ordenWeb)
        }
      });

      if (file) {
        await uploadFile<ProductoAdmin>(`/api/v1/productos/${updated.id}/imagen`, {
          token,
          file
        });
      }
      if (pdfFile) {
        await uploadFile<ProductoAdmin>(`/api/v1/productos/${updated.id}/ficha-tecnica`, {
          token, file: pdfFile, fieldName: "fichaTecnica"
        });
      }

      setFile(null);
      setPdfFile(null);
      setMessage("Producto actualizado en la vitrina");
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "No se pudo actualizar el producto");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="status-state">
        <Loader2 className="spin" size={24} />
        <span>Cargando web comercial</span>
      </div>
    );
  }

  return (
    <div className="stacked-layout">
      {(message || error) && (
        <div className={`form-notice ${error ? "error" : "success"}`}>
          {error ?? message}
        </div>
      )}

      <section className="web-admin-hero">
        <div>
          <span className="eyebrow">Administrador de vitrina</span>
          <h2>Controla lo que ve el cliente sin tocar codigo.</h2>
        </div>
        <a className="primary-action" href="/tienda" target="_blank" rel="noreferrer">
          <ExternalLink size={17} />
          Abrir tienda
        </a>
      </section>

      <div className="split-panels align-start">
        <section className="panel form-panel">
          <div className="panel-header">
            <div>
              <ImagePlus size={18} />
              <h2>Editor de producto web</h2>
            </div>
            <button aria-label="Actualizar" onClick={load} type="button">
              <RefreshCw size={16} />
            </button>
          </div>

          <form className="data-form two-columns" onSubmit={submit}>
            <label className="full-span">
              Producto
              <select value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>
                {productos.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nombre visible
              <input value={form.nombre} onChange={(event) => setForm({ ...form, nombre: event.target.value })} />
            </label>
            <label>
              Categoria
              <select value={form.categoriaId} onChange={(event) => setForm({ ...form, categoriaId: event.target.value })}>
                {categorias.map((categoria) => (
                  <option key={categoria.id} value={categoria.id}>
                    {categoria.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Precio venta
              <input inputMode="decimal" value={form.precioVenta} onChange={(event) => setForm({ ...form, precioVenta: event.target.value })} />
            </label>
            <label>
              Stock minimo
              <input inputMode="numeric" value={form.stockMinimo} onChange={(event) => setForm({ ...form, stockMinimo: event.target.value })} />
            </label>
            <label className="full-span">
              Descripcion interna
              <input value={form.descripcion} onChange={(event) => setForm({ ...form, descripcion: event.target.value })} />
            </label>
            <label className="full-span">
              Resumen comercial para la tienda
              <textarea value={form.resumenComercial} onChange={(event) => setForm({ ...form, resumenComercial: event.target.value })} />
            </label>
            <div className="full-span admin-form-section"><strong>Ficha pública del producto</strong><span>Todo este contenido aparece en la página que visita el cliente.</span></div>
            <label className="full-span">
              Descripción completa
              <textarea rows={5} value={form.descripcionWeb} onChange={(event) => setForm({ ...form, descripcionWeb: event.target.value })} />
            </label>
            <label>Ingrediente activo<input value={form.ingredienteActivo} onChange={(event) => setForm({ ...form, ingredienteActivo: event.target.value })} /></label>
            <label>Composición / concentración<input value={form.composicion} onChange={(event) => setForm({ ...form, composicion: event.target.value })} /></label>
            <label>Formulación<input value={form.formulacion} onChange={(event) => setForm({ ...form, formulacion: event.target.value })} /></label>
            <label>Número de registro<input value={form.numeroRegistro} onChange={(event) => setForm({ ...form, numeroRegistro: event.target.value })} /></label>
            <label className="full-span">Presentaciones comerciales<input value={form.presentaciones} onChange={(event) => setForm({ ...form, presentaciones: event.target.value })} placeholder="Ej. 250 ml, 1 L y 20 L" /></label>
            <label className="full-span">Cultivos recomendados<input value={form.cultivos} onChange={(event) => setForm({ ...form, cultivos: event.target.value })} placeholder="Ej. Palto, arándano, vid y cítricos" /></label>
            <label className="full-span">Modo de uso<textarea rows={4} value={form.modoUso} onChange={(event) => setForm({ ...form, modoUso: event.target.value })} /></label>
            <label className="full-span">Información adicional<textarea rows={5} value={form.informacionAdicional} onChange={(event) => setForm({ ...form, informacionAdicional: event.target.value })} /></label>
            <label>
              Orden web
              <input inputMode="numeric" value={form.ordenWeb} onChange={(event) => setForm({ ...form, ordenWeb: event.target.value })} />
            </label>
            <label>
              Texto alternativo de imagen
              <input value={form.imagenAlt} onChange={(event) => setForm({ ...form, imagenAlt: event.target.value })} />
            </label>
            <label className="check-field">
              <input checked={form.visibleWeb} type="checkbox" onChange={(event) => setForm({ ...form, visibleWeb: event.target.checked })} />
              Visible en tienda
            </label>
            <label className="check-field">
              <input checked={form.destacado} type="checkbox" onChange={(event) => setForm({ ...form, destacado: event.target.checked })} />
              Producto destacado
            </label>
            <label className="full-span">
              Imagen JPG, PNG o WEBP
              <input accept="image/jpeg,image/png,image/webp" type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
            </label>
            <label className="full-span">
              Ficha técnica en PDF (máximo 15 MB)
              <input accept="application/pdf,.pdf" type="file" onChange={(event) => setPdfFile(event.target.files?.[0] ?? null)} />
            </label>
            {(pdfFile || selectedProduct?.fichaTecnicaUrl) && <div className="pdf-admin-status full-span"><FileText size={20} /><span>{pdfFile ? `Nuevo PDF: ${pdfFile.name}` : "Ficha técnica publicada"}</span>{!pdfFile && selectedProduct?.fichaTecnicaUrl && <a href={mediaUrl(selectedProduct.fichaTecnicaUrl)} target="_blank" rel="noreferrer">Ver PDF <ExternalLink size={14} /></a>}</div>}
            <div className="web-image-preview full-span">
              {previewUrl || selectedProduct?.imagenUrl ? (
                <img src={previewUrl ?? mediaUrl(selectedProduct?.imagenUrl)} alt={form.imagenAlt || selectedProduct?.nombre || "Producto"} />
              ) : (
                <div>
                  <Store size={28} />
                  <span>Sin imagen comercial</span>
                </div>
              )}
            </div>
            <button
              className="primary-action full-span"
              disabled={saving || !selectedProduct || !form.nombre.trim() || !form.categoriaId || !form.precioVenta.trim()}
              type="submit"
            >
              {saving ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
              Guardar toda la información
            </button>
          </form>
        </section>

        <section className="panel">
          <div className="panel-header">
            <div>
              <Store size={18} />
              <h2>Catalogo publicado</h2>
            </div>
            <button aria-label="Actualizar" onClick={load} type="button">
              <RefreshCw size={16} />
            </button>
          </div>
          <div className="web-product-list">
            {productos.map((product) => (
              <article key={product.id} className={product.visibleWeb ? "" : "muted"}>
                <div className="web-product-thumb">
                  {product.imagenUrl ? (
                    <img src={mediaUrl(product.imagenUrl)} alt={product.nombre} />
                  ) : (
                    <Store size={22} />
                  )}
                </div>
                <div>
                  <strong>{product.nombre}</strong>
                  <span>{product.categoriaNombre ?? "Sin categoria"} - {formatPrice(product.precioVenta)}</span>
                  <small>{product.visibleWeb ? "Visible" : "Oculto"}{product.destacado ? " - Destacado" : ""}</small>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function moneyInput(value: string) {
  const normalizado = value.replace(",", ".").trim();
  const parsed = Number(normalizado);
  return Number.isFinite(parsed) ? parsed : 0;
}

function integerInput(value: string) {
  const parsed = Number.parseInt(value.trim(), 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function formatPrice(value: unknown) {
  const numeric = Number(value ?? 0);

  return `PEN ${new Intl.NumberFormat("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number.isFinite(numeric) ? numeric : 0)}`;
}

function mediaUrl(value?: string | null) {
  if (!value) {
    return "";
  }

  if (value.startsWith("http") || value.startsWith("/comercial")) {
    return value;
  }

  if (value.startsWith("/")) {
    return `${apiBaseUrl()}${value}`;
  }

  return value;
}
