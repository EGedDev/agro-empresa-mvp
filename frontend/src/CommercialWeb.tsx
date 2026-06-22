
"use client";

import {
  ArrowRight,
  BadgeCheck,
  Check,
  ChevronRight,
  Leaf,
  LogIn,
  Menu,
  MessageCircle,
  Minus,
  PackageSearch,
  Phone,
  Plus,
  Search,
  Send,
  ShieldCheck,
  ShoppingBag,
  ShoppingCart,
  Sparkles,
  Sprout,
  Trash2,
  UserPlus,
  X,
  Zap
} from "lucide-react";
import { Canvas, useFrame } from "@react-three/fiber";
import { motion } from "framer-motion";

import WhatsAppButton from "./WhatsAppButton";
import { FormEvent, ReactNode, useEffect, useMemo, useRef, useState } from "react";
import type { Group } from "three";
import { ApiError, PaginaResponse, apiBaseUrl, apiRequest } from "./api";
import {
  BannerComercial,
  ProductoComercial,
  bannersComerciales,
  categoriasComerciales,
  piezasPromocionales,
  productosComerciales
} from "./comercialData";

type ApiProducto = {
  id: number;
  nombre: string;
  descripcion?: string | null;
  precioVenta?: number | string | null;
  imagenUrl?: string | null;
  imagenAlt?: string | null;
  resumenComercial?: string | null;
  destacado?: boolean | null;
  categoriaNombre?: string | null;
  stockActual?: number | null;
};

type CartItem = {
  product: ProductoComercial;
  quantity: number;
};

type LeadForm = {
  nombre: string;
  documentoIdentidad: string;
  telefono: string;
  email: string;
  direccion: string;
  cultivo: string;
  interes: string;
  mensaje: string;
};

const leadInicial: LeadForm = {
  nombre: "",
  documentoIdentidad: "",
  telefono: "",
  email: "",
  direccion: "",
  cultivo: "",
  interes: "",
  mensaje: ""
};

export function CommercialWeb() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("Todos");
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchFocused, setSearchFocused] = useState(false);
  const [cartOpen, setCartOpen] = useState(false);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [apiProducts, setApiProducts] = useState<ApiProducto[]>([]);
  const [leadForm, setLeadForm] = useState<LeadForm>(leadInicial);
  const [leadState, setLeadState] = useState<{ loading: boolean; message?: string; error?: string }>({ loading: false });

  useEffect(() => {
    let active = true;

    apiRequest<PaginaResponse<ApiProducto>>("/api/v1/web/productos?size=120&sort=ordenWeb,asc")
      .then((response) => {
        if (active) {
          setApiProducts(response.contenido);
        }
      })
      .catch(() => {
        if (active) {
          setApiProducts([]);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const products = useMemo(() => productosDesdeApi(apiProducts), [apiProducts]);
  const availableCategories = useMemo(() => {
    const apiCategories = products
      .map((product) => product.categoria)
      .filter((item): item is string => Boolean(item && item.trim().length > 0));

    return ["Todos", ...Array.from(new Set([...categoriasComerciales.filter((item) => item !== "Todos"), ...apiCategories]))];
  }, [products]);
  const filteredProducts = products.filter((product) => {
    const text = `${product.nombre} ${product.descripcion} ${product.linea} ${product.uso}`.toLowerCase();
    const matchesQuery = query.trim().length === 0 || text.includes(query.trim().toLowerCase());
    const matchesCategory = category === "Todos" || product.categoria === category;
    return matchesQuery && matchesCategory;
  });
  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);
  const cartTotal = cart.reduce((total, item) => total + productPrice(item.product) * item.quantity, 0);
  const pricedItems = cart.filter((item) => productPrice(item.product) > 0);
  const pendingPriceCount = cart.length - pricedItems.length;
  const quoteMessage = encodeURIComponent(cartMessage(cart));
  const searchSuggestions = useMemo(() => {
    const term = query.trim().toLowerCase();

    if (!term) {
      return products.slice(0, 6);
    }

    return products
      .filter((product) => {
        const text = `${product.nombre} ${product.descripcion} ${product.categoria} ${product.linea} ${product.uso}`.toLowerCase();
        const matchesCategory = category === "Todos" || product.categoria === category;
        return matchesCategory && text.includes(term);
      })
      .slice(0, 6);
  }, [category, products, query]);
  const leadValido = leadForm.nombre.trim().length > 1 && (leadForm.telefono.trim().length > 5 || leadForm.email.trim().length > 5);

  useEffect(() => {
    if (!availableCategories.includes(category)) {
      setCategory("Todos");
    }
  }, [availableCategories, category]);

  function addToCart(product: ProductoComercial) {
    setCart((current) => {
      const existing = current.find((item) => item.product.id === product.id);

      if (existing) {
        return current.map((item) => (item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item));
      }

      return [...current, { product, quantity: 1 }];
    });
    setCartOpen(true);
  }

  function changeCartQuantity(productId: string, quantity: number) {
    setCart((current) => {
      if (quantity <= 0) {
        return current.filter((item) => item.product.id !== productId);
      }

      return current.map((item) => (item.product.id === productId ? { ...item, quantity } : item));
    });
  }

  function quantityFor(productId: string) {
    return cart.find((item) => item.product.id === productId)?.quantity ?? 0;
  }

  function scrollToSection(sectionId: string) {
    const element = document.getElementById(sectionId);

    if (!element) {
      return;
    }

    const headerHeight = document.querySelector<HTMLElement>(".commercial-nav")?.offsetHeight ?? 0;
    const offset = headerHeight + 18;
    const top = element.getBoundingClientRect().top + window.scrollY - offset;
    window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
    setMenuOpen(false);
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    setSearchFocused(false);
    scrollToSection("catalogo");
  }

  function chooseSuggestion(product: ProductoComercial) {
    setQuery(product.nombre);
    setCategory(product.categoria);
    setSearchFocused(false);
    scrollToSection("catalogo");
  }

  function updateLead<K extends keyof LeadForm>(key: K, value: LeadForm[K]) {
    setLeadForm((current) => ({ ...current, [key]: value }));
    setLeadState({ loading: false });
  }

  async function submitLead(event: FormEvent) {
    event.preventDefault();
    setLeadState({ loading: true });

    try {
      await apiRequest("/api/v1/web/solicitudes-atencion", {
        method: "POST",
        body: {
          nombre: leadForm.nombre.trim(),
          documentoIdentidad: emptyToNull(leadForm.documentoIdentidad),
          telefono: emptyToNull(leadForm.telefono),
          email: emptyToNull(leadForm.email),
          direccion: emptyToNull(leadForm.direccion),
          cultivo: emptyToNull(leadForm.cultivo),
          interes: emptyToNull(leadForm.interes),
          mensaje: emptyToNull(leadForm.mensaje)
        }
      });
      setLeadForm(leadInicial);
      setLeadState({ loading: false, message: "Solicitud recibida. Un asesor de ventas la vera en el ERP y te contactara en breve." });
    } catch (caught) {
      setLeadState({ loading: false, error: errorMessage(caught) });
    }
  }

  return (
    <div className="commercial-shell">
      <header className="commercial-nav marketplace-nav">
        <a className="commercial-brand" href="/" aria-label="ITAVEN">
          <img src="/itaven-logo.svg" alt="ITAVEN" />
          <span>ITAVEN</span>
        </a>
        <form className="market-search" onSubmit={submitSearch}>
          <select aria-label="Categoria" value={category} onChange={(event) => setCategory(event.target.value)}>
            {availableCategories.map((item) => (
              <option key={item} value={item}>
                {item === "Todos" ? "Todo" : item}
              </option>
            ))}
          </select>
          <div className="market-search-box">
            <input
              value={query}
              onBlur={() => window.setTimeout(() => setSearchFocused(false), 120)}
              onChange={(event) => {
                setQuery(event.target.value);
                setSearchFocused(true);
              }}
              onFocus={() => setSearchFocused(true)}
              placeholder="Buscar fertilizantes, bioestimulantes, cultivo u objetivo"
            />
            {searchFocused && searchSuggestions.length > 0 && (
              <div className="search-suggestions" role="listbox">
                {searchSuggestions.map((product) => (
                  <button key={product.id} onMouseDown={() => chooseSuggestion(product)} type="button">
                    <img src={productVisual(product)} alt="" />
                    <span>
                      <strong>{product.nombre}</strong>
                      <small>{product.categoria} · {formatPrice(product.precioVenta)}</small>
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <button aria-label="Buscar" type="submit">
            <Search size={23} />
          </button>
        </form>
        <div className="market-actions">
          <a className="commercial-login-link" href="/login">
            <LogIn size={17} />
            <span>Iniciar sesion</span>
          </a>
          <button className="market-cart-button" onClick={() => setCartOpen((open) => !open)} type="button">
            <span className="cart-badge">{cartCount}</span>
            <ShoppingCart size={30} />
            <strong>Carrito</strong>
          </button>
        </div>
        <button
          aria-label={menuOpen ? "Cerrar menu" : "Abrir menu"}
          className="commercial-menu-button"
          onClick={() => setMenuOpen((open) => !open)}
          type="button"
        >
          {menuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
        <nav className={menuOpen ? "open" : ""}>
          <button onClick={() => scrollToSection("catalogo")} type="button">Todo</button>
          <button onClick={() => scrollToSection("soluciones")} type="button">Soluciones</button>
          <button onClick={() => scrollToSection("catalogo")} type="button">Catalogo</button>
          <button onClick={() => scrollToSection("registro")} type="button">Registrarme</button>
          <button onClick={() => scrollToSection("promociones")} type="button">Promociones</button>
          <button className="nav-cart-link" onClick={() => setCartOpen(true)} type="button">Ver carrito</button>
        </nav>
      </header>

      <main>
        <Hero banner={bannersComerciales[2]} />
        <section className="commercial-signal-strip" aria-label="Indicadores comerciales">
          <SignalMetric value="+1000" label="clientes satisfechos" />
          <SignalMetric value="+10" label="anos de trayectoria" />
          <SignalMetric value="24h" label="respuesta comercial" />
          <SignalMetric value="ERP" label="ventas conectadas" />
        </section>

        <section className="commercial-section commercial-featured" id="soluciones">
          <div className="section-heading">
            <div>
              <span className="section-kicker">
                <Sprout size={16} />
                Soluciones por objetivo
              </span>
              <h2>Una arquitectura comercial para vender tecnologia agricola con criterio.</h2>
            </div>
            <p>
              La web ordena cada producto por etapa del cultivo, necesidad tecnica y accion comercial.
              El cliente cotiza; el equipo trabaja desde el ERP.
            </p>
          </div>
          <div className="solution-grid">
            <SolutionCard
              icon={<Leaf size={22} />}
              title="Raices y arranque"
              text="Mayor enraizamiento, absorcion y recuperacion postrasplante."
              products="Rain Roots, Humiven Max, Saltrex Max"
            />
            <SolutionCard
              icon={<Zap size={22} />}
              title="Nutricion dirigida"
              text="Elementos clave para floracion, crecimiento, correccion y llenado."
              products="Potasio, Fosforo, Nitrogeno, Setplus"
            />
            <SolutionCard
              icon={<ShoppingBag size={22} />}
              title="Calidad comercial"
              text="Firmeza, dulzor, color y vida de anaquel para vender mejor."
              products="Citcomax Plus, Sugar Max, Fruit Max"
            />
          </div>
        </section>

        <section className="commercial-section catalog-section" id="catalogo">
          <div className="catalog-toolbar">
            <div>
              <span className="section-kicker">
                <PackageSearch size={16} />
                Catalogo tecnico comercial
              </span>
              <h2>Productos ITAVEN listos para cotizar.</h2>
            </div>
            <label className="commercial-search">
              <Search size={18} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar producto, cultivo u objetivo"
              />
            </label>
          </div>

          <div className="category-tabs" role="tablist" aria-label="Categorias de producto">
            {availableCategories.map((item) => (
              <button className={category === item ? "active" : ""} key={item} onClick={() => setCategory(item)} type="button">
                {item}
              </button>
            ))}
          </div>

          <div className="product-grid">
            {filteredProducts.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                quantity={quantityFor(product.id)}
                onAdd={() => addToCart(product)}
                onDecrease={() => changeCartQuantity(product.id, quantityFor(product.id) - 1)}
              />
            ))}
          </div>
        </section>

        <section className="commercial-section client-register" id="registro">
          <div className="register-copy">
            <span className="section-kicker">
              <UserPlus size={16} />
              Asesoria rapida
            </span>
            <h2>Dejanos tu WhatsApp y un asesor te contacta.</h2>
            <p>
              Pide una cotizacion, consulta por un problema de cultivo o solicita una recomendacion tecnica sin llenar formularios largos.
            </p>
            <div className="route-map" aria-label="Beneficios de contacto">
              <span>Respuesta por WhatsApp</span>
              <span>Asesoria tecnica</span>
              <span>Cotizacion rapida</span>
              <span>Productos ITAVEN</span>
            </div>
          </div>

          <form className="client-form" onSubmit={submitLead}>
            <div className="client-form-head">
              <ShieldCheck size={20} />
              <div>
                <strong>Recibe asesoria por WhatsApp</strong>
                <span>Solo necesitamos tus datos esenciales para contactarte.</span>
              </div>
            </div>
            <label>
              Nombre o empresa
              <input value={leadForm.nombre} onChange={(event) => updateLead("nombre", event.target.value)} />
            </label>
            <label>
              WhatsApp
              <input inputMode="tel" placeholder="Ej. 983 979 147" value={leadForm.telefono} onChange={(event) => updateLead("telefono", event.target.value)} />
            </label>
            <label>
              Cultivo principal
              <input placeholder="Ej. palto, arandano, uva" value={leadForm.cultivo} onChange={(event) => updateLead("cultivo", event.target.value)} />
            </label>
            <div className="quick-interest" role="group" aria-label="Que necesitas">
              <span>Que necesitas?</span>
              {["Cotizar productos", "Resolver un problema", "Asesoria tecnica"].map((item) => (
                <button
                  className={leadForm.interes === item ? "active" : ""}
                  key={item}
                  onClick={() => updateLead("interes", item)}
                  type="button"
                >
                  {item}
                </button>
              ))}
            </div>
            <label className="full-line">
              Mensaje opcional
              <textarea
                value={leadForm.mensaje}
                onChange={(event) => updateLead("mensaje", event.target.value)}
                placeholder="Cuentanos en una frase que necesitas."
              />
            </label>

            {leadState.message && <div className="lead-message success">{leadState.message}</div>}
            {leadState.error && <div className="lead-message error">{leadState.error}</div>}

            <button className="client-submit" disabled={!leadValido || leadState.loading} type="submit">
              {leadState.loading ? "Enviando..." : "Solicitar asesoria"}
              <Send size={17} />
            </button>
          </form>
        </section>

        <section className="commercial-section promo-section" id="promociones">
          <div className="section-heading">
            <div>
              <span className="section-kicker">
                <BadgeCheck size={16} />
                Piezas comerciales
              </span>
              <h2>Campanas visuales con producto real, no decoracion vacia.</h2>
            </div>
            <p>Las imagenes oficiales quedan contenidas en marcos consistentes aunque vengan en tamanos distintos.</p>
          </div>
          <div className="promo-grid">
            {piezasPromocionales.map((image) => (
              <img key={image} src={image} alt="Pieza promocional ITAVEN" loading="lazy" />
            ))}
          </div>
        </section>

        <section className="commercial-section commercial-close">
          <img src="/comercial/portadas/antistress-peptidos.jpeg" alt="Linea ITAVEN antistress y peptidos" />
          <div>
            <span className="section-kicker">Acompanamiento tecnico</span>
            <h2>Productos, asesoria y cotizacion en un solo contacto.</h2>
            <p>
              Un asesor revisa tu cultivo, necesidad y zona para recomendarte la linea adecuada antes de comprar.
            </p>
            <div className="close-actions">
              <a className="commercial-cta dark" href="https://wa.me/51928978841" target="_blank" rel="noreferrer">
                Escribir por WhatsApp
                <MessageCircle size={18} />
              </a>
              <a className="commercial-cta secondary" href="#catalogo">
                Ver catalogo
                <ArrowRight size={18} />
              </a>
            </div>
          </div>
        </section>



{/* ... (Aquí termina tu última sección de acompañamiento técnico) ... */}
        <section className="commercial-section commercial-close">
          <img src="/comercial/portadas/antistress-peptidos.jpeg" alt="Linea ITAVEN antistress y peptidos" />
          <div>
            <span className="section-kicker">Acompanamiento tecnico</span>
            <h2>Productos, asesoria y cotizacion en un solo contacto.</h2>
            <p>
              Un asesor revisa tu cultivo, necesidad y zona para recomendarte la linea adecuada antes de comprar.
            </p>
            <div className="close-actions">
              <a className="commercial-cta dark" href="https://wa.me/51928978841" target="_blank" rel="noreferrer">
                Escribir por WhatsApp
                <MessageCircle size={18} />
              </a>
              <a className="commercial-cta secondary" href="#catalogo">
                Ver catalogo
                <ArrowRight size={18} />
              </a>
            </div>
          </div>
        </section>








{/* ¡AQUÍ SE QUEDA EL FOOTER, JUSTO ANTES DE CERRAR EL MAIN! */}
        <footer className="commercial-footer">
          <div className="footer-container">
            
            {/* Columna 1: Soluciones */}
            <div className="footer-column">
              <h3>Soluciones</h3>
              <ul>
                <li><a href="#fertilizantes">Fertilizantes</a></li>
                <li><a href="#bioestimulantes">Bioestimulantes</a></li>
                <li><a href="#proteccion">Protección de Cultivos</a></li>
                <li><a href="#asesoria">Asesoría Técnica</a></li>
              </ul>
            </div>

            {/* Columna 2: Contáctenos */}
            <div className="footer-column">
              <h3>Contáctenos</h3>
              <div className="contact-info">
                <p className="label">DIRECCIÓN</p>
                <p>Huaura, Región Lima - Perú</p>
                
                <p className="label">TELÉFONO</p>
                <p>+51 983 979 147</p>
                
                <p className="label">EMAIL</p>
                <p>contacto@itavensac.com</p>
              </div>
            </div>

            {/* Columna 3: Sobre la empresa y Redes */}
            <div className="footer-column">
              <h3>ITAVEN SAC</h3>
              <p className="footer-description">
                Tecnología agrícola conectada al ERP. Innovación, sostenibilidad y compromiso con el agro peruano.
              </p>
              <div className="footer-socials">
                <a href="#" className="social-icon">fb</a>
                <a href="#" className="social-icon">in</a>
                <a href="#" className="social-icon">yt</a>
                <a href="#" className="social-icon">ig</a>
              </div>
            </div>

          </div>

          {/* Barra inferior de créditos */}
          <div className="footer-bottom">
            <div className="footer-bottom-content">
              <div className="footer-copy">
                <span>Copyright © 2026 - ITAVEN SAC</span>
                <a href="#privacidad">Políticas de Privacidad</a>
              </div>
              <div className="footer-credits">
                Desarrollado por <span className="dev-name">Ing.Eli Garro </span>
              </div>
            </div>
          </div>
        </footer>
      

      </main>




        













      <aside className={cartOpen ? "cart-panel open" : "cart-panel"} aria-label="Carrito de compras">
        <div className="cart-panel-head">
          <div>
            <span>Carrito</span>
            <strong>{cartCount} productos</strong>
          </div>
          <button aria-label="Cerrar carrito" onClick={() => setCartOpen(false)} type="button">
            <X size={20} />
          </button>
        </div>
        <div className="cart-subtotal-box">
          <span>Subtotal ({cartCount} {cartCount === 1 ? "producto" : "productos"})</span>
          <strong>{formatPrice(cartTotal)}</strong>
          <small>Moneda: PEN, sol peruano.</small>
          {pendingPriceCount > 0 && <small>{pendingPriceCount} producto(s) tienen precio por confirmar.</small>}
        </div>
        {cart.length === 0 ? (
          <div className="cart-empty">
            <ShoppingCart size={34} />
            <strong>Tu carrito esta vacio</strong>
            <span>Agrega productos desde el catalogo para cotizar.</span>
          </div>
        ) : (
          <div className="cart-items">
            {cart.map((item) => (
              <article key={item.product.id} className="cart-line">
                <img src={productVisual(item.product)} alt={item.product.nombre} />
                <div>
                  <strong>{item.product.nombre}</strong>
                  <span className="cart-status">Disponible para cotizar</span>
                  <span>{item.product.categoria}</span>
                  <span>Precio unitario: {formatPrice(item.product.precioVenta)}</span>
                  <em>Subtotal: {formatPrice(productPrice(item.product) * item.quantity)}</em>
                  <div className="cart-line-controls">
                    <button aria-label={`Quitar ${item.product.nombre}`} onClick={() => changeCartQuantity(item.product.id, item.quantity - 1)} type="button">
                      {item.quantity === 1 ? <Trash2 size={15} /> : <Minus size={15} />}
                    </button>
                    <span>{item.quantity}</span>
                    <button aria-label={`Agregar ${item.product.nombre}`} onClick={() => changeCartQuantity(item.product.id, item.quantity + 1)} type="button">
                      <Plus size={15} />
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
        <div className="cart-panel-actions">
          <div className="cart-grand-total">
            <span>Total estimado</span>
            <strong>{formatPrice(cartTotal)}</strong>
          </div>
          <a className={cart.length === 0 ? "disabled" : ""} href={`https://wa.me/51983971947?text=${quoteMessage}`} target="_blank" rel="noreferrer">
            <MessageCircle size={18} />
            Cotizar por WhatsApp
          </a>
          <button onClick={() => setCartOpen(false)} type="button">
            Seguir comprando
          </button>
        </div>
      </aside>
      {cartOpen && <button className="cart-scrim" aria-label="Cerrar carrito" onClick={() => setCartOpen(false)} type="button" />}
<WhatsAppButton />

    </div>
  );
}

function Hero({ banner }: { banner: BannerComercial }) {
  return (
    <motion.section className="commercial-hero" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}>
      <HeroCinematicBackground banner={banner} />
      <div className="commercial-hero-grid" />
      <div className="commercial-hero-copy">
        <span className="commercial-pill">
          <Sparkles size={15} />
          Tecnologia agricola conectada al ERP
        </span>
        <h1>Productos ITAVEN con asesoria real y operacion conectada.</h1>
        <p>
          Mas de 1000 clientes satisfechos y mas de 10 anos de trayectoria. Dejalo en nuestras manos:
          cotiza, conversa con un asesor y lleva cada venta al ERP sin perder control.
        </p>
        <div className="commercial-hero-actions">
          <a className="commercial-cta" href="#catalogo">
            Ver catalogo
            <ArrowRight size={18} />
          </a>
          <a className="commercial-cta secondary" href="#registro">
            Conectate con un asesor
            <ChevronRight size={18} />
          </a>
          <a className="commercial-cta ghost" href="tel:+51983979147">
            <Phone size={18} />
            983 979 147
          </a>
        </div>
      </div>

      <div className="hero-showcase" aria-label="Productos destacados ITAVEN">
        <FuturisticCropScene />
        <div className="hero-orbit" />
        <HeroBottleShowcase />
      </div>
    </motion.section>
  );
}

function HeroCinematicBackground({ banner }: { banner: BannerComercial }) {
  return (
    <div className="hero-cinematic-bg" aria-hidden="true">
      <figure className="cinematic-frame drone">
        <img src="/comercial/portadas/cultivos-libres-enfermedades.jpeg" alt="" />
      </figure>
      <figure className="cinematic-frame people">
        <img src={banner.imagen} alt="" />
        <div className="people-scene">
          <span />
          <span />
          <span />
        </div>
      </figure>
      <figure className="cinematic-frame strawberry">
        <img src="/comercial/anuncios/sugar-calidad-fruto.jpeg" alt="" />
      </figure>
      <figure className="cinematic-frame orange">
        <img src="/comercial/anuncios/phospati-energia.jpeg" alt="" />
      </figure>
    </div>
  );
}

function HeroBottleShowcase() {
  const showcaseProducts = useMemo(() => productosComerciales, []);
  const initialProductIndex = Math.max(0, productosComerciales.findIndex((product) => product.id === "citcomax-plus"));
  const [activeIndex, setActiveIndex] = useState(initialProductIndex);
  const [isInspecting, setIsInspecting] = useState(false);
  const activeProduct = showcaseProducts[activeIndex % showcaseProducts.length];
  const activeProductImage = heroProductImage(activeProduct);

  useEffect(() => {
    if (isInspecting || showcaseProducts.length < 2) {
      return;
    }

    const timer = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % showcaseProducts.length);
    }, 9800);

    return () => window.clearInterval(timer);
  }, [isInspecting, showcaseProducts.length]);

  return (
    <div
      className={isInspecting ? "hero-bottle-stage inspecting" : "hero-bottle-stage"}
      onMouseEnter={() => setIsInspecting(true)}
      onMouseLeave={() => setIsInspecting(false)}
    >
      <div className="bottle-orbit" aria-hidden="true" />
      <div className="bottle-holo-base" aria-hidden="true" />
      <div
        key={activeProduct.id}
        className="single-bottle-shell"
        onBlur={() => setIsInspecting(false)}
        onFocus={() => setIsInspecting(true)}
        tabIndex={0}
      >
        <div className="bottle-scan-lines" aria-hidden="true" />
        <motion.div
          animate={
            isInspecting
              ? { rotateX: 0, rotateY: 0, scale: 1.05, y: -6 }
              : { rotateX: [2, 4, 2], rotateY: [24, 0, -24, -10], scale: [1, 1.035, 1.02, 1], y: [0, -10, -6, 0] }
          }
          aria-label={`${activeProduct.nombre} girando en 3D`}
          className="rotating-bottle"
          transition={
            isInspecting
              ? { duration: 0.55, ease: [0.22, 1, 0.36, 1] }
              : { duration: 9.8, ease: "easeInOut", repeat: Infinity }
          }
        >
          <div className="bottle-image-stack">
            <img className="bottle-edge bottle-edge-cyan" src={activeProductImage} alt="" aria-hidden="true" />
            <img className="bottle-edge bottle-edge-lime" src={activeProductImage} alt="" aria-hidden="true" />
            <img className="bottle-edge bottle-edge-runner" src={activeProductImage} alt="" aria-hidden="true" />
            <span className="bottle-edge-spark" aria-hidden="true" />
            <img className="bottle-product-image" src={activeProductImage} alt={activeProduct.nombre} />
          </div>
        </motion.div>
      </div>
      <div className="bottle-info-panel" role="tooltip">
        <span className="type-line" style={{ animationDelay: "60ms" }}>{activeProduct.categoria} / {activeProduct.linea}</span>
        <strong className="type-line" style={{ animationDelay: "210ms" }}>{activeProduct.nombre}</strong>
        <p className="type-line" style={{ animationDelay: "390ms" }}>{activeProduct.descripcion}</p>
        <div className="type-line bottle-info-usage" style={{ animationDelay: "570ms" }}>
          <small>Uso recomendado</small>
          <em>{activeProduct.uso}</em>
        </div>
        <ul>
          {activeProduct.beneficios.map((benefit, index) => (
            <li className="type-line" key={benefit} style={{ animationDelay: `${760 + index * 150}ms` }}>
              <Check size={13} />
              {benefit}
            </li>
          ))}
        </ul>
        <div className="type-line bottle-info-action" style={{ animationDelay: "1260ms" }}>Listo para cotizacion asistida</div>
      </div>
      <div className="bottle-status" aria-live="polite">
        <span>Producto {activeIndex + 1} / {showcaseProducts.length}</span>
        <strong>{activeProduct.nombre}</strong>
      </div>
    </div>
  );
}

function FuturisticCropScene() {
  return (
    <div className="hero-canvas" aria-hidden="true">
      <Canvas camera={{ position: [0, 0.4, 6], fov: 42 }} dpr={[1, 1.6]}>
        <ambientLight intensity={0.7} />
        <pointLight color="#d7ff43" intensity={4.6} position={[3.6, 2.2, 4]} />
        <pointLight color="#54d8ff" intensity={2.2} position={[-3, -1.8, 3]} />
        <FuturisticCore />
      </Canvas>
    </div>
  );
}

function FuturisticCore() {
  const groupRef = useRef<Group>(null);

  useFrame((_, delta) => {
    if (!groupRef.current) {
      return;
    }

    groupRef.current.rotation.y += delta * 0.28;
    groupRef.current.rotation.x = Math.sin(Date.now() * 0.00045) * 0.08;
  });

  return (
    <group ref={groupRef}>
      <mesh rotation={[1.2, 0.2, 0.3]}>
        <torusGeometry args={[1.42, 0.018, 16, 160]} />
        <meshStandardMaterial color="#d7ff43" emissive="#6aaa22" emissiveIntensity={0.9} metalness={0.6} roughness={0.2} />
      </mesh>
      <mesh rotation={[0.4, 1.1, 0.6]}>
        <torusGeometry args={[1.95, 0.012, 16, 160]} />
        <meshStandardMaterial color="#6fe8ff" emissive="#2aa8bb" emissiveIntensity={0.55} metalness={0.45} roughness={0.24} />
      </mesh>
      <mesh position={[0, 0, 0]}>
        <icosahedronGeometry args={[0.7, 2]} />
        <meshStandardMaterial color="#1f6f43" emissive="#123d28" emissiveIntensity={0.6} metalness={0.25} roughness={0.42} />
      </mesh>
      {[-1.6, -0.55, 0.7, 1.7].map((x, index) => (
        <mesh key={x} position={[x, Math.sin(index) * 0.5, -0.35 + index * 0.18]}>
          <sphereGeometry args={[0.055 + index * 0.012, 18, 18]} />
          <meshStandardMaterial color={index % 2 === 0 ? "#d7ff43" : "#7ee8ff"} emissive={index % 2 === 0 ? "#8bbd1c" : "#2aa8bb"} emissiveIntensity={1.2} />
        </mesh>
      ))}
    </group>
  );
}

function SignalMetric({ value, label }: { value: string; label: string }) {
  return (
    <div>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function SolutionCard({ icon, title, text, products }: { icon: ReactNode; title: string; text: string; products: string }) {
  return (
    <article className="solution-card">
      <div className="solution-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
      <span>{products}</span>
    </article>
  );
}

function ProductCard({
  product,
  quantity,
  onAdd,
  onDecrease
}: {
  product: ProductoComercial;
  quantity: number;
  onAdd: () => void;
  onDecrease: () => void;
}) {
  return (
    <motion.article
      className={product.destacado ? "product-card featured" : "product-card"}
      whileHover={{ y: -5 }}
      transition={{ type: "spring", stiffness: 260, damping: 24 }}
    >
      <div className="product-image-frame">
        <a href={`/producto/${product.id}`} aria-label={`Ver información de ${product.nombre}`}>
          <img src={productVisual(product)} alt={product.nombre} loading="lazy" />
        </a>
      </div>
      <div className="product-content">
        <div className="product-meta">
          <span>{product.categoria}</span>
          {product.destacado && <strong>Destacado</strong>}
        </div>
        <h3><a className="product-detail-link" href={`/producto/${product.id}`}>{product.nombre}</a></h3>
        <div className="product-price">
          <span>Precio</span>
          <strong>{formatPrice(product.precioVenta)}</strong>
        </div>
        <p>{product.descripcion}</p>
        <ul>
          {product.beneficios.slice(0, 3).map((benefit) => (
            <li key={benefit}>
              <Check size={14} />
              {benefit}
            </li>
          ))}
        </ul>
        <div className="cart-actions">
          <a className="product-more-link" href={`/producto/${product.id}`}>Ver información completa <ChevronRight size={15} /></a>
          {quantity > 0 && (
            <button aria-label={`Quitar ${product.nombre}`} className="quantity-button" onClick={onDecrease} type="button">
              <Minus size={15} />
            </button>
          )}
          {quantity > 0 && <span>{quantity}</span>}
          <button className={quantity > 0 ? "quote-button selected" : "quote-button"} onClick={onAdd} type="button">
            {quantity > 0 ? (
              <>
                <Plus size={16} />
                Agregar mas
              </>
            ) : (
              <>
                <ShoppingBag size={16} />
                Agregar al carrito
              </>
            )}
          </button>
        </div>





      </div>
    </motion.article>
  );
}

function productosDesdeApi(apiProducts: ApiProducto[]) {
  const tieneCatalogoConfigurado = apiProducts.some((product) => product.imagenUrl || product.resumenComercial || product.destacado);

  if (!tieneCatalogoConfigurado) {
    return productosComerciales.map(withDefaultPrice);
  }

  const apiMapped = apiProducts.map((product, index) => {
    const fallback = productosComerciales[index % productosComerciales.length];
    return {
      id: `api-${product.id}`,
      nombre: product.nombre,
      categoria: product.categoriaNombre ?? fallback.categoria,
      linea: product.categoriaNombre ?? fallback.linea,
      descripcion: product.resumenComercial ?? product.descripcion ?? fallback.descripcion,
      precioVenta: product.precioVenta ?? defaultProductPrice(fallback.id),
      beneficios: fallback.beneficios,
      uso: fallback.uso,
      imagen: mediaUrl(product.imagenUrl) ?? fallback.imagen,
      destacado: Boolean(product.destacado)
    };
  });

  const nombresApi = new Set(apiMapped.map((product) => normalizar(product.nombre)));
  const staticMissing = productosComerciales.filter((product) => !nombresApi.has(normalizar(product.nombre)));

  return [...apiMapped, ...staticMissing.map(withDefaultPrice)];
}

function withDefaultPrice(product: ProductoComercial): ProductoComercial {
  return {
    ...product,
    precioVenta: product.precioVenta ?? defaultProductPrice(product.id)
  };
}

function defaultProductPrice(productId: string) {
  const prices: Record<string, number> = {
    potasio: 95,
    fosforo: 88,
    "super-silmag": 72,
    nitrogeno: 82,
    "citcomax-plus": 64,
    "sugar-max": 68,
    "fruit-max": 70,
    "rain-roots": 76,
    "humiven-max": 78,
    "saltrex-max": 74,
    "zoik-max": 69,
    "bio-mite": 58,
    "nemaplus-max": 62,
    combat: 65,
    conan: 59,
    pretor: 61,
    karacit: 63
  };

  return prices[productId] ?? 60;
}

function mediaUrl(value?: string | null) {
  if (!value) {
    return null;
  }

  if (value.startsWith("http") || value.startsWith("/comercial")) {
    return value;
  }

  if (value.startsWith("/")) {
    return `${apiBaseUrl()}${value}`;
  }

  return value;
}

function productVisual(product: ProductoComercial) {
  if (product.imagen.includes("/comercial/productos/")) {
    return heroProductImage(product);
  }

  return product.imagen;
}

function heroProductImage(product: ProductoComercial) {
  const fileName = product.imagen.split("/").pop()?.replace(/\.(jpe?g|png|webp)$/i, ".png") ?? `${product.id}.png`;
  return `/comercial/productos-cutout/${fileName}?v=20260529a`;
}

function normalizar(value: string) {
  return value.trim().toLowerCase();
}

function productPrice(product: ProductoComercial) {
  const numeric = Number(product.precioVenta ?? 0);
  return Number.isFinite(numeric) ? numeric : 0;
}

function formatPrice(value: unknown) {
  const numeric = Number(value ?? 0);

  return `PEN ${new Intl.NumberFormat("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number.isFinite(numeric) ? numeric : 0)}`;
}

function cartSummary(cart: CartItem[]) {
  return cart.map((item) => `${item.quantity} x ${item.product.nombre}`).join(", ");
}

function cartMessage(cart: CartItem[]) {
  if (cart.length === 0) {
    return "Hola ITAVEN, quiero cotizar productos de su catalogo.";
  }

  const lines = cart.map((item) => {
    const lineTotal = productPrice(item.product) * item.quantity;
    const price = productPrice(item.product) > 0 ? ` - ${formatPrice(lineTotal)}` : "";
    return `${item.quantity} x ${item.product.nombre}${price}`;
  });
  const total = cart.reduce((sum, item) => sum + productPrice(item.product) * item.quantity, 0);
  const totalLine = total > 0 ? `\nTotal referencial: ${formatPrice(total)}` : "";

  return `Hola ITAVEN, quiero cotizar este carrito:\n${lines.join("\n")}${totalLine}`;
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function errorMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    return caught.correlationId ? `${caught.message} (${caught.correlationId})` : caught.message;
  }

  if (caught instanceof Error) {
    return caught.message;
  }

  return "No se pudo registrar la solicitud";
}
