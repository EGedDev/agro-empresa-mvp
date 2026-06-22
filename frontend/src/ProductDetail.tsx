"use client";

import { ArrowLeft, Check, ExternalLink, FileText, Leaf, Package, ShoppingBag } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { apiBaseUrl, apiRequest } from "./api";
import { productosComerciales } from "./comercialData";
import WhatsAppButton from "./WhatsAppButton";

type ProductApi = {
  id: number; nombre: string; descripcion?: string | null; resumenComercial?: string | null;
  descripcionWeb?: string | null; informacionAdicional?: string | null; ingredienteActivo?: string | null;
  composicion?: string | null; formulacion?: string | null; numeroRegistro?: string | null;
  presentaciones?: string | null; cultivos?: string | null; modoUso?: string | null;
  fichaTecnicaUrl?: string | null; imagenUrl?: string | null; imagenAlt?: string | null;
  categoriaNombre?: string | null; precioVenta?: number | string | null;
};

export function ProductDetail({ productId }: { productId: string }) {
  const numericId = productId.startsWith("api-") ? productId.slice(4) : "";
  const fallback = useMemo(() => productosComerciales.find((item) => item.id === productId), [productId]);
  const [product, setProduct] = useState<ProductApi | null>(null);
  const [loading, setLoading] = useState(Boolean(numericId));
  const [tab, setTab] = useState<"descripcion" | "informacion" | "descargas">("descripcion");

  useEffect(() => {
    if (!numericId) return;
    apiRequest<ProductApi>(`/api/v1/web/productos/${numericId}`)
      .then(setProduct)
      .finally(() => setLoading(false));
  }, [numericId]);

  if (loading) return <div className="product-detail-state">Cargando información del producto…</div>;
  if (!product && !fallback) return <div className="product-detail-state">Producto no encontrado.</div>;

  const view = product ? {
    nombre: product.nombre,
    categoria: product.categoriaNombre ?? "Producto agrícola",
    imagen: mediaUrl(product.imagenUrl),
    imagenAlt: product.imagenAlt ?? product.nombre,
    resumen: product.resumenComercial ?? product.descripcion ?? "Solución profesional para el manejo del cultivo.",
    descripcion: product.descripcionWeb ?? product.resumenComercial ?? product.descripcion,
    adicional: product.informacionAdicional,
    ingrediente: product.ingredienteActivo,
    composicion: product.composicion,
    formulacion: product.formulacion,
    registro: product.numeroRegistro,
    presentaciones: product.presentaciones,
    cultivos: product.cultivos,
    uso: product.modoUso,
    pdf: mediaUrl(product.fichaTecnicaUrl),
    precio: product.precioVenta
  } : {
    nombre: fallback!.nombre, categoria: fallback!.categoria, imagen: fallback!.imagen, imagenAlt: fallback!.nombre,
    resumen: fallback!.descripcion, descripcion: fallback!.descripcion, adicional: fallback!.beneficios.join("\n"),
    ingrediente: null, composicion: null, formulacion: fallback!.linea, registro: null, presentaciones: null,
    cultivos: fallback!.uso, uso: fallback!.uso, pdf: "", precio: fallback!.precioVenta
  };

  const facts = [
    ["Ingrediente activo", view.ingrediente], ["Composición", view.composicion], ["Formulación", view.formulacion],
    ["Registro", view.registro], ["Presentaciones", view.presentaciones]
  ].filter((item) => item[1]);

  return (
    <div className="product-detail-shell">
      <header className="detail-nav">
        <a className="commercial-brand" href="/tienda"><img src="/itaven-logo.svg" alt="ITAVEN" /><span>ITAVEN</span></a>
        <a href="/tienda#catalogo"><ArrowLeft size={17} /> Volver al catálogo</a>
      </header>
      <main className="product-detail-main">
        <nav className="detail-breadcrumb">Inicio / Catálogo / <strong>{view.nombre}</strong></nav>
        <section className="product-detail-hero">
          <div className="detail-image"><span>{view.categoria}</span><img src={view.imagen} alt={view.imagenAlt} /></div>
          <div className="detail-summary">
            <span className="section-kicker"><Leaf size={16} /> {view.categoria}</span>
            <h1>{view.nombre}</h1>
            <p>{view.resumen}</p>
            {facts.length > 0 && <dl>{facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>}
            <div className="detail-price"><small>Precio referencial</small><strong>{formatPrice(view.precio)}</strong></div>
            <a className="detail-quote" href={`https://wa.me/51928978841?text=${encodeURIComponent(`Hola, deseo información sobre ${view.nombre}.`)}`} target="_blank" rel="noreferrer"><ShoppingBag size={18} /> Solicitar cotización</a>
          </div>
        </section>

        <section className="detail-tabs">
          <div role="tablist">
            <button className={tab === "descripcion" ? "active" : ""} onClick={() => setTab("descripcion")}>Descripción</button>
            <button className={tab === "informacion" ? "active" : ""} onClick={() => setTab("informacion")}>Información adicional</button>
            <button className={tab === "descargas" ? "active" : ""} onClick={() => setTab("descargas")}>Descargas</button>
          </div>
          <article className="detail-tab-content">
            {tab === "descripcion" && <><h2>Descripción del producto</h2><RichText text={view.descripcion || "Información en preparación."} />{view.uso && <><h3>Modo de uso</h3><RichText text={view.uso} /></>}</>}
            {tab === "informacion" && <><h2>Información técnica adicional</h2>{view.cultivos && <InfoRow icon={<Leaf />} label="Cultivos recomendados" value={view.cultivos} />}{view.presentaciones && <InfoRow icon={<Package />} label="Presentaciones" value={view.presentaciones} />}<RichText text={view.adicional || "Información adicional en preparación."} /></>}
            {tab === "descargas" && <><h2>Documentos del producto</h2>{view.pdf ? <a className="download-card" href={view.pdf} target="_blank" rel="noreferrer"><FileText size={28} /><span><strong>Ficha técnica</strong><small>PDF · Se abrirá en una nueva pestaña</small></span><ExternalLink size={18} /></a> : <div className="download-empty"><FileText size={25} /> La ficha técnica todavía no ha sido publicada.</div>}</>}
          </article>
        </section>
      </main>
      <WhatsAppButton />
    </div>
  );
}

function RichText({ text }: { text: string }) { return <div className="detail-rich-text">{text.split("\n").filter(Boolean).map((line) => <p key={line}>{line}</p>)}</div>; }
function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) { return <div className="detail-info-row">{icon}<div><strong>{label}</strong><span>{value}</span></div></div>; }
function mediaUrl(value?: string | null) { if (!value) return "/comercial/productos-cutout/citcomax-plus.png"; return value.startsWith("/") && !value.startsWith("/comercial") ? `${apiBaseUrl()}${value}` : value; }
function formatPrice(value: unknown) { const amount = Number(value); return Number.isFinite(amount) && amount > 0 ? `PEN ${amount.toFixed(2)}` : "Consultar"; }
