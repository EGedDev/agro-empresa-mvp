import {
  AlertCircle,
  BarChart3,
  KeyRound,
  LayoutDashboard,
  Loader2,
  LogOut,
  PackageSearch,
  Plus,
  RefreshCw,
  Save,
  ShieldCheck,
  ShoppingCart,
  Truck,
  UserPlus,
  Users,
  WalletCards
} from "lucide-react";
import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  LoginResponse,
  PaginaResponse,
  Usuario,
  apiBaseUrl,
  apiRequest,
  bootstrapAdmin,
  getPage,
  login
} from "./api";

type Session = {
  token: string;
  usuario: Usuario;
};

type ModuleKey = "dashboard" | "catalogo" | "contactos" | "ventas" | "compras" | "cartera";

type EstadoCarga<T> = {
  data?: T;
  error?: string;
  loading: boolean;
};

const SESSION_KEY = "agro-erp-session";

const modules: Array<{
  key: ModuleKey;
  label: string;
  icon: ReactNode;
}> = [
  { key: "dashboard", label: "Panel", icon: <LayoutDashboard size={18} /> },
  { key: "catalogo", label: "Catalogo", icon: <PackageSearch size={18} /> },
  { key: "contactos", label: "Contactos", icon: <Users size={18} /> },
  { key: "ventas", label: "Ventas", icon: <ShoppingCart size={18} /> },
  { key: "compras", label: "Compras", icon: <Truck size={18} /> },
  { key: "cartera", label: "Cartera", icon: <WalletCards size={18} /> }
];

export function App() {
  const [session, setSession] = useState<Session | null>(() => readSession());

  if (!session) {
    return <LoginScreen onSession={setSession} />;
  }

  return <Workspace session={session} onLogout={() => closeSession(setSession)} />;
}

function LoginScreen({ onSession }: { onSession: (session: Session) => void }) {
  const [mode, setMode] = useState<"login" | "bootstrap">("login");
  const [username, setUsername] = useState("admin");
  const [nombre, setNombre] = useState("Administrador");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (mode === "bootstrap") {
        await bootstrapAdmin(username.trim(), nombre.trim(), password);
      }

      const response = await login(username.trim(), password);
      openSession(response, onSession);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div className="brand-mark">
          <ShieldCheck size={26} />
          <div>
            <strong>Agro ERP</strong>
            <span>{apiBaseUrl()}</span>
          </div>
        </div>

        <div className="auth-tabs" role="tablist" aria-label="Modo de acceso">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")} type="button">
            <KeyRound size={16} />
            Ingresar
          </button>
          <button className={mode === "bootstrap" ? "active" : ""} onClick={() => setMode("bootstrap")} type="button">
            <UserPlus size={16} />
            Primer admin
          </button>
        </div>

        <form className="auth-form" onSubmit={submit}>
          <label>
            Usuario
            <input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>

          {mode === "bootstrap" && (
            <label>
              Nombre
              <input value={nombre} onChange={(event) => setNombre(event.target.value)} />
            </label>
          )}

          <label>
            Password
            <input
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>

          {error && <InlineError message={error} />}

          <button className="primary-action" disabled={loading || !username.trim() || !password} type="submit">
            {loading ? <Loader2 className="spin" size={17} /> : <ShieldCheck size={17} />}
            {mode === "login" ? "Entrar" : "Crear y entrar"}
          </button>
        </form>
      </section>
    </main>
  );
}

function Workspace({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [activeModule, setActiveModule] = useState<ModuleKey>("dashboard");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-mark compact">
          <ShieldCheck size={22} />
          <strong>Agro ERP</strong>
        </div>

        <nav className="module-nav">
          {modules.map((module) => (
            <button
              className={activeModule === module.key ? "active" : ""}
              key={module.key}
              onClick={() => setActiveModule(module.key)}
              type="button"
            >
              {module.icon}
              {module.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <span className="eyebrow">Sesion activa</span>
            <h1>{titleFor(activeModule)}</h1>
          </div>
          <div className="user-chip">
            <span>{session.usuario.nombre}</span>
            <strong>{session.usuario.rol}</strong>
            <button aria-label="Cerrar sesion" onClick={onLogout} type="button">
              <LogOut size={17} />
            </button>
          </div>
        </header>

        {activeModule === "dashboard" && <Dashboard token={session.token} />}
        {activeModule === "catalogo" && <CatalogoPanel token={session.token} />}
        {activeModule === "contactos" && <ContactosPanel token={session.token} />}
        {activeModule === "ventas" && <VentasPanel token={session.token} />}
        {activeModule === "compras" && <ComprasPanel token={session.token} />}
        {activeModule === "cartera" && <CarteraPanel token={session.token} />}
      </main>
    </div>
  );
}

function Dashboard({ token }: { token: string }) {
  const range = useMemo(monthRange, []);
  const [state, setState] = useState<EstadoCarga<Record<string, unknown>>>({ loading: true });

  async function load() {
    setState({ loading: true });
    try {
      const [finanzas, inventario, cartera, ventasClientes] = await Promise.all([
        apiRequest<Record<string, unknown>>(`/api/v1/reportes/finanzas/resumen?desde=${range.desde}&hasta=${range.hasta}`, { token }),
        apiRequest<Record<string, unknown>>(`/api/v1/reportes/inventario/resumen?desde=${range.desde}&hasta=${range.hasta}`, { token }),
        apiRequest<Record<string, unknown>>(`/api/v1/finanzas/cartera/resumen?desde=${range.desde}&hasta=${range.hasta}`, { token }),
        apiRequest<unknown[]>(`/api/v1/reportes/gerenciales/ventas/clientes?desde=${range.desde}&hasta=${range.hasta}&limite=5`, { token })
      ]);
      setState({ loading: false, data: { finanzas, inventario, cartera, ventasClientes } });
    } catch (caught) {
      setState({ loading: false, error: errorMessage(caught) });
    }
  }

  useEffect(() => {
    void load();
  }, []);

  if (state.loading) {
    return <LoadingState />;
  }

  if (state.error) {
    return <ErrorState message={state.error} onRetry={load} />;
  }

  const finanzas = state.data?.finanzas as Record<string, unknown>;
  const inventario = state.data?.inventario as Record<string, unknown>;
  const cartera = state.data?.cartera as Record<string, unknown>;
  const ventasClientes = (state.data?.ventasClientes as Array<Record<string, unknown>>) ?? [];

  return (
    <section className="dashboard-grid">
      <Metric title="Ingresos" value={money(pick(finanzas, ["ingresos", "totalIngresos", "ventas"]))} tone="green" />
      <Metric title="Egresos" value={money(pick(finanzas, ["egresos", "totalEgresos", "compras"]))} tone="amber" />
      <Metric title="Inventario" value={money(pick(inventario, ["valorInventarioTotal", "valorInventario"]))} tone="blue" />
      <Metric title="Pendiente" value={money(pick(cartera, ["saldoPendienteCobrar", "cuentasPorCobrar", "porCobrar"]))} tone="rose" />

      <section className="panel wide">
        <PanelHeader icon={<BarChart3 size={18} />} title="Ventas por cliente" onRefresh={load} />
        <SimpleTable
          columns={["clienteNombre", "ventas", "total", "saldoPendiente"]}
          rows={ventasClientes}
        />
      </section>
    </section>
  );
}

function CatalogoPanel({ token }: { token: string }) {
  const [reloadKey, setReloadKey] = useState(0);
  const [categorias, setCategorias] = useState<Array<Record<string, unknown>>>([]);
  const [categoriaForm, setCategoriaForm] = useState({ nombre: "", descripcion: "" });
  const [productoForm, setProductoForm] = useState({
    nombre: "",
    descripcion: "",
    precioVenta: "",
    stockActual: "0",
    costoInicial: "0",
    stockMinimo: "0",
    categoriaId: ""
  });
  const [saving, setSaving] = useState<"categoria" | "producto" | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadCategorias() {
    const response = await getPage<Record<string, unknown>>("/api/v1/categorias?activo=true&size=100&sort=nombre,asc", token);
    setCategorias(response.contenido);
    setProductoForm((current) => ({
      ...current,
      categoriaId: current.categoriaId || String(response.contenido[0]?.id ?? "")
    }));
  }

  useEffect(() => {
    void loadCategorias().catch((caught) => setError(errorMessage(caught)));
  }, [reloadKey]);

  async function crearCategoria(event: FormEvent) {
    event.preventDefault();
    setSaving("categoria");
    setError(null);
    setMessage(null);

    try {
      await apiRequest("/api/v1/categorias", {
        method: "POST",
        token,
        body: nullablePayload(categoriaForm)
      });
      setCategoriaForm({ nombre: "", descripcion: "" });
      setMessage("Categoria registrada");
      setReloadKey((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(null);
    }
  }

  async function crearProducto(event: FormEvent) {
    event.preventDefault();
    setSaving("producto");
    setError(null);
    setMessage(null);

    try {
      await apiRequest("/api/v1/productos", {
        method: "POST",
        token,
        body: {
          nombre: productoForm.nombre.trim(),
          descripcion: emptyToNull(productoForm.descripcion),
          precioVenta: moneyInput(productoForm.precioVenta),
          stockActual: integerInput(productoForm.stockActual),
          costoInicial: moneyInput(productoForm.costoInicial),
          stockMinimo: integerInput(productoForm.stockMinimo),
          categoriaId: Number(productoForm.categoriaId)
        }
      });
      setProductoForm((current) => ({
        nombre: "",
        descripcion: "",
        precioVenta: "",
        stockActual: "0",
        costoInicial: "0",
        stockMinimo: "0",
        categoriaId: current.categoriaId
      }));
      setMessage("Producto registrado");
      setReloadKey((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(null);
    }
  }

  return (
    <div className="stacked-layout">
      {(message || error) && <FormNotice kind={error ? "error" : "success"} message={error ?? message ?? ""} />}

      <div className="form-grid">
        <section className="panel form-panel">
          <PanelHeader icon={<Plus size={18} />} title="Nueva categoria" onRefresh={() => setReloadKey((value) => value + 1)} />
          <form className="data-form" onSubmit={crearCategoria}>
            <label>
              Nombre
              <input value={categoriaForm.nombre} onChange={(event) => setCategoriaForm({ ...categoriaForm, nombre: event.target.value })} />
            </label>
            <label>
              Descripcion
              <input value={categoriaForm.descripcion} onChange={(event) => setCategoriaForm({ ...categoriaForm, descripcion: event.target.value })} />
            </label>
            <button className="primary-action" disabled={saving === "categoria" || !categoriaForm.nombre.trim()} type="submit">
              {saving === "categoria" ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
              Guardar
            </button>
          </form>
        </section>

        <section className="panel form-panel">
          <PanelHeader icon={<PackageSearch size={18} />} title="Nuevo producto" onRefresh={() => setReloadKey((value) => value + 1)} />
          <form className="data-form two-columns" onSubmit={crearProducto}>
            <label>
              Nombre
              <input value={productoForm.nombre} onChange={(event) => setProductoForm({ ...productoForm, nombre: event.target.value })} />
            </label>
            <label>
              Categoria
              <select value={productoForm.categoriaId} onChange={(event) => setProductoForm({ ...productoForm, categoriaId: event.target.value })}>
                {categorias.map((categoria) => (
                  <option key={String(categoria.id)} value={String(categoria.id)}>
                    {String(categoria.nombre)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Precio venta
              <input inputMode="decimal" value={productoForm.precioVenta} onChange={(event) => setProductoForm({ ...productoForm, precioVenta: event.target.value })} />
            </label>
            <label>
              Costo inicial
              <input inputMode="decimal" value={productoForm.costoInicial} onChange={(event) => setProductoForm({ ...productoForm, costoInicial: event.target.value })} />
            </label>
            <label>
              Stock actual
              <input inputMode="numeric" value={productoForm.stockActual} onChange={(event) => setProductoForm({ ...productoForm, stockActual: event.target.value })} />
            </label>
            <label>
              Stock minimo
              <input inputMode="numeric" value={productoForm.stockMinimo} onChange={(event) => setProductoForm({ ...productoForm, stockMinimo: event.target.value })} />
            </label>
            <label className="full-span">
              Descripcion
              <input value={productoForm.descripcion} onChange={(event) => setProductoForm({ ...productoForm, descripcion: event.target.value })} />
            </label>
            <button
              className="primary-action full-span"
              disabled={saving === "producto" || !productoForm.nombre.trim() || !productoForm.categoriaId}
              type="submit"
            >
              {saving === "producto" ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
              Guardar producto
            </button>
          </form>
        </section>
      </div>

      <ResourcePanel
        columns={["nombre", "precioVenta", "costoPromedio", "stockActual", "stockMinimo", "activo"]}
        endpoint={`/api/v1/productos?size=10&sort=nombre,asc&reload=${reloadKey}`}
        token={token}
      />
    </div>
  );
}

function ContactosPanel({ token }: { token: string }) {
  const [reloadKey, setReloadKey] = useState(0);
  const [clienteForm, setClienteForm] = useState(contactoInicial());
  const [proveedorForm, setProveedorForm] = useState(contactoInicial());
  const [saving, setSaving] = useState<"cliente" | "proveedor" | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function crearContacto(tipo: "cliente" | "proveedor", event: FormEvent) {
    event.preventDefault();
    setSaving(tipo);
    setError(null);
    setMessage(null);

    const form = tipo === "cliente" ? clienteForm : proveedorForm;
    const endpoint = tipo === "cliente" ? "/api/v1/clientes" : "/api/v1/proveedores";

    try {
      await apiRequest(endpoint, {
        method: "POST",
        token,
        body: nullablePayload(form)
      });
      if (tipo === "cliente") {
        setClienteForm(contactoInicial());
      } else {
        setProveedorForm(contactoInicial());
      }
      setMessage(tipo === "cliente" ? "Cliente registrado" : "Proveedor registrado");
      setReloadKey((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(null);
    }
  }

  return (
    <div className="stacked-layout">
      {(message || error) && <FormNotice kind={error ? "error" : "success"} message={error ?? message ?? ""} />}
      <div className="split-panels">
        <ContactoForm
          title="Nuevo cliente"
          value={clienteForm}
          saving={saving === "cliente"}
          onChange={setClienteForm}
          onSubmit={(event) => crearContacto("cliente", event)}
        />
        <ContactoForm
          title="Nuevo proveedor"
          value={proveedorForm}
          saving={saving === "proveedor"}
          onChange={setProveedorForm}
          onSubmit={(event) => crearContacto("proveedor", event)}
        />
      </div>
      <div className="split-panels">
        <ResourcePanel
          columns={["nombre", "documentoIdentidad", "telefono", "email", "activo"]}
          endpoint={`/api/v1/clientes?size=8&sort=nombre,asc&reload=${reloadKey}`}
          token={token}
        />
        <ResourcePanel
          columns={["nombre", "documentoIdentidad", "telefono", "email", "activo"]}
          endpoint={`/api/v1/proveedores?size=8&sort=nombre,asc&reload=${reloadKey}`}
          token={token}
        />
      </div>
    </div>
  );
}

function VentasPanel({ token }: { token: string }) {
  return (
    <OperacionPanel
      columns={["numero", "clienteNombre", "estado", "estadoPago", "total", "saldoPendiente"]}
      endpoint="/api/v1/ventas"
      listEndpoint="/api/v1/ventas?size=10&sort=fechaVenta,desc"
      participantEndpoint="/api/v1/clientes/activos?size=100&sort=nombre,asc"
      participantId="clienteId"
      participantLabel="Cliente"
      title="Registrar venta"
      token={token}
      type="venta"
    />
  );
}

function ComprasPanel({ token }: { token: string }) {
  return (
    <OperacionPanel
      columns={["numero", "proveedorNombre", "estado", "estadoPago", "total", "saldoPendiente"]}
      endpoint="/api/v1/compras"
      listEndpoint="/api/v1/compras?size=10&sort=fechaCompra,desc"
      participantEndpoint="/api/v1/proveedores/activos?size=100&sort=nombre,asc"
      participantId="proveedorId"
      participantLabel="Proveedor"
      title="Registrar compra"
      token={token}
      type="compra"
    />
  );
}

type OperacionPanelProps = {
  columns: string[];
  endpoint: string;
  listEndpoint: string;
  participantEndpoint: string;
  participantId: "clienteId" | "proveedorId";
  participantLabel: string;
  title: string;
  token: string;
  type: "venta" | "compra";
};

function OperacionPanel({
  columns,
  endpoint,
  listEndpoint,
  participantEndpoint,
  participantId,
  participantLabel,
  title,
  token,
  type
}: OperacionPanelProps) {
  const [reloadKey, setReloadKey] = useState(0);
  const [participants, setParticipants] = useState<Array<Record<string, unknown>>>([]);
  const [productos, setProductos] = useState<Array<Record<string, unknown>>>([]);
  const [documentos, setDocumentos] = useState<Array<Record<string, unknown>>>([]);
  const [participantValue, setParticipantValue] = useState("");
  const [fechaVencimiento, setFechaVencimiento] = useState("");
  const [detalles, setDetalles] = useState([detalleInicial(type)]);
  const [documentoPagoId, setDocumentoPagoId] = useState("");
  const [pagoForm, setPagoForm] = useState({ monto: "", metodoPago: "EFECTIVO", referencia: "" });
  const [saving, setSaving] = useState(false);
  const [savingPago, setSavingPago] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadOptions() {
    const [participantPage, productPage, documentPage] = await Promise.all([
      getPage<Record<string, unknown>>(participantEndpoint, token),
      getPage<Record<string, unknown>>("/api/v1/productos/activos?size=100&sort=nombre,asc", token),
      getPage<Record<string, unknown>>(listEndpoint, token)
    ]);

    setParticipants(participantPage.contenido);
    setProductos(productPage.contenido);
    setDocumentos(documentPage.contenido);
    setParticipantValue((current) => current || String(participantPage.contenido[0]?.id ?? ""));
    setDocumentoPagoId((current) => current || String(documentPage.contenido[0]?.id ?? ""));
    setDetalles((current) =>
      current.map((detalle) => ({
        ...detalle,
        productoId: detalle.productoId || String(productPage.contenido[0]?.id ?? "")
      }))
    );
  }

  useEffect(() => {
    void loadOptions().catch((caught) => setError(errorMessage(caught)));
  }, [participantEndpoint, reloadKey]);

  function updateDetalle(index: number, key: keyof OperacionDetalleForm, value: string) {
    setDetalles((current) =>
      current.map((detalle, currentIndex) =>
        currentIndex === index ? { ...detalle, [key]: value } : detalle
      )
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      await apiRequest(endpoint, {
        method: "POST",
        token,
        idempotent: true,
        body: {
          [participantId]: Number(participantValue),
          fechaVencimiento: emptyToNull(fechaVencimiento),
          detalles: detalles.map((detalle) => ({
            productoId: Number(detalle.productoId),
            cantidad: integerInput(detalle.cantidad),
            ...(type === "compra" ? { costoUnitario: moneyInput(detalle.costoUnitario) } : {})
          }))
        }
      });
      setFechaVencimiento("");
      setDetalles([detalleInicial(type, productos[0]?.id)]);
      setMessage(type === "venta" ? "Venta registrada" : "Compra registrada");
      setReloadKey((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  async function registrarPago(event: FormEvent) {
    event.preventDefault();
    setSavingPago(true);
    setMessage(null);
    setError(null);

    const pagoEndpoint =
      type === "venta"
        ? `/api/v1/ventas/${documentoPagoId}/pagos`
        : `/api/v1/compras/${documentoPagoId}/pagos`;

    try {
      await apiRequest(pagoEndpoint, {
        method: "POST",
        token,
        idempotent: true,
        body: {
          monto: moneyInput(pagoForm.monto),
          metodoPago: pagoForm.metodoPago,
          referencia: emptyToNull(pagoForm.referencia)
        }
      });
      setPagoForm({ monto: "", metodoPago: "EFECTIVO", referencia: "" });
      setMessage("Pago registrado");
      setReloadKey((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSavingPago(false);
    }
  }

  return (
    <div className="stacked-layout">
      {(message || error) && <FormNotice kind={error ? "error" : "success"} message={error ?? message ?? ""} />}
      <div className="split-panels align-start">
        <section className="panel form-panel">
          <PanelHeader icon={type === "venta" ? <ShoppingCart size={18} /> : <Truck size={18} />} title={title} onRefresh={() => setReloadKey((value) => value + 1)} />
          <form className="data-form" onSubmit={submit}>
            <div className="form-grid compact">
              <label>
                {participantLabel}
                <select value={participantValue} onChange={(event) => setParticipantValue(event.target.value)}>
                  {participants.map((participant) => (
                    <option key={String(participant.id)} value={String(participant.id)}>
                      {String(participant.nombre)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Vencimiento
                <input type="date" value={fechaVencimiento} onChange={(event) => setFechaVencimiento(event.target.value)} />
              </label>
            </div>

            <div className="line-items">
              {detalles.map((detalle, index) => (
                <div className={`line-item ${type === "venta" ? "sale-line" : ""}`} key={index}>
                  <label>
                    Producto
                    <select value={detalle.productoId} onChange={(event) => updateDetalle(index, "productoId", event.target.value)}>
                      {productos.map((producto) => (
                        <option key={String(producto.id)} value={String(producto.id)}>
                          {String(producto.nombre)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Cantidad
                    <input inputMode="numeric" value={detalle.cantidad} onChange={(event) => updateDetalle(index, "cantidad", event.target.value)} />
                  </label>
                  {type === "compra" && (
                    <label>
                      Costo unitario
                      <input inputMode="decimal" value={detalle.costoUnitario} onChange={(event) => updateDetalle(index, "costoUnitario", event.target.value)} />
                    </label>
                  )}
                  <button
                    aria-label="Quitar linea"
                    disabled={detalles.length === 1}
                    onClick={() => setDetalles((current) => current.filter((_, currentIndex) => currentIndex !== index))}
                    type="button"
                  >
                    -
                  </button>
                </div>
              ))}
            </div>

            <div className="form-actions">
              <button className="secondary-action" onClick={() => setDetalles((current) => [...current, detalleInicial(type, productos[0]?.id)])} type="button">
                <Plus size={16} />
                Agregar linea
              </button>
              <button className="primary-action" disabled={saving || !participantValue || detalles.some((detalle) => !detalle.productoId)} type="submit">
                {saving ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
                Guardar
              </button>
            </div>
          </form>
        </section>

        <section className="panel form-panel">
          <PanelHeader icon={<WalletCards size={18} />} title="Registrar pago" onRefresh={() => setReloadKey((value) => value + 1)} />
          <form className="data-form" onSubmit={registrarPago}>
            <label>
              Documento
              <select value={documentoPagoId} onChange={(event) => setDocumentoPagoId(event.target.value)}>
                {documentos.map((documento) => (
                  <option key={String(documento.id)} value={String(documento.id)}>
                    {String(documento.numero ?? documento.id)} - {money(documento.saldoPendiente)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Monto
              <input inputMode="decimal" value={pagoForm.monto} onChange={(event) => setPagoForm({ ...pagoForm, monto: event.target.value })} />
            </label>
            <label>
              Metodo
              <select value={pagoForm.metodoPago} onChange={(event) => setPagoForm({ ...pagoForm, metodoPago: event.target.value })}>
                {["EFECTIVO", "TRANSFERENCIA", "YAPE", "PLIN", "TARJETA", "OTRO"].map((metodo) => (
                  <option key={metodo} value={metodo}>
                    {metodo}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Referencia
              <input value={pagoForm.referencia} onChange={(event) => setPagoForm({ ...pagoForm, referencia: event.target.value })} />
            </label>
            <button className="primary-action" disabled={savingPago || !documentoPagoId || !pagoForm.monto.trim()} type="submit">
              {savingPago ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
              Guardar pago
            </button>
          </form>
        </section>
      </div>

      <ResourcePanel columns={columns} endpoint={`${listEndpoint}&reload=${reloadKey}`} token={token} />
    </div>
  );
}

type ContactoFormValue = ReturnType<typeof contactoInicial>;

function ContactoForm({
  title,
  value,
  saving,
  onChange,
  onSubmit
}: {
  title: string;
  value: ContactoFormValue;
  saving: boolean;
  onChange: (value: ContactoFormValue) => void;
  onSubmit: (event: FormEvent) => void;
}) {
  return (
    <section className="panel form-panel">
      <PanelHeader icon={<Users size={18} />} title={title} />
      <form className="data-form two-columns" onSubmit={onSubmit}>
        <label className="full-span">
          Nombre
          <input value={value.nombre} onChange={(event) => onChange({ ...value, nombre: event.target.value })} />
        </label>
        <label>
          Documento
          <input value={value.documentoIdentidad} onChange={(event) => onChange({ ...value, documentoIdentidad: event.target.value })} />
        </label>
        <label>
          Telefono
          <input value={value.telefono} onChange={(event) => onChange({ ...value, telefono: event.target.value })} />
        </label>
        <label>
          Email
          <input type="email" value={value.email} onChange={(event) => onChange({ ...value, email: event.target.value })} />
        </label>
        <label>
          Direccion
          <input value={value.direccion} onChange={(event) => onChange({ ...value, direccion: event.target.value })} />
        </label>
        <button className="primary-action full-span" disabled={saving || !value.nombre.trim()} type="submit">
          {saving ? <Loader2 className="spin" size={17} /> : <Save size={17} />}
          Guardar
        </button>
      </form>
    </section>
  );
}

function ResourcePanel({ endpoint, token, columns }: { endpoint: string; token: string; columns: string[] }) {
  const [state, setState] = useState<EstadoCarga<PaginaResponse<Record<string, unknown>>>>({ loading: true });

  async function load() {
    setState({ loading: true });
    try {
      setState({ loading: false, data: await getPage<Record<string, unknown>>(endpoint, token) });
    } catch (caught) {
      setState({ loading: false, error: errorMessage(caught) });
    }
  }

  useEffect(() => {
    void load();
  }, [endpoint]);

  if (state.loading) {
    return <LoadingState />;
  }

  if (state.error) {
    return <ErrorState message={state.error} onRetry={load} />;
  }

  return (
    <section className="panel">
      <PanelHeader icon={<RefreshCw size={18} />} title={`${state.data?.totalElementos ?? 0} registros`} onRefresh={load} />
      <SimpleTable columns={columns} rows={state.data?.contenido ?? []} />
    </section>
  );
}

function CarteraPanel({ token }: { token: string }) {
  const [state, setState] = useState<EstadoCarga<Record<string, PaginaResponse<Record<string, unknown>>>>>({ loading: true });

  async function load() {
    setState({ loading: true });
    try {
      const [cobrar, pagar] = await Promise.all([
        getPage<Record<string, unknown>>("/api/v1/finanzas/cartera/cuentas-por-cobrar?size=8&sort=fechaVencimiento,asc", token),
        getPage<Record<string, unknown>>("/api/v1/finanzas/cartera/cuentas-por-pagar?size=8&sort=fechaVencimiento,asc", token)
      ]);
      setState({ loading: false, data: { cobrar, pagar } });
    } catch (caught) {
      setState({ loading: false, error: errorMessage(caught) });
    }
  }

  useEffect(() => {
    void load();
  }, []);

  if (state.loading) {
    return <LoadingState />;
  }

  if (state.error) {
    return <ErrorState message={state.error} onRetry={load} />;
  }

  return (
    <div className="split-panels">
      <section className="panel">
        <PanelHeader icon={<WalletCards size={18} />} title="Por cobrar" onRefresh={load} />
        <SimpleTable columns={["numero", "clienteNombre", "estadoPago", "saldoPendiente", "fechaVencimiento"]} rows={state.data?.cobrar.contenido ?? []} />
      </section>
      <section className="panel">
        <PanelHeader icon={<WalletCards size={18} />} title="Por pagar" onRefresh={load} />
        <SimpleTable columns={["numero", "proveedorNombre", "estadoPago", "saldoPendiente", "fechaVencimiento"]} rows={state.data?.pagar.contenido ?? []} />
      </section>
    </div>
  );
}

function PanelHeader({ icon, title, onRefresh }: { icon: ReactNode; title: string; onRefresh?: () => void }) {
  return (
    <div className="panel-header">
      <div>
        {icon}
        <h2>{title}</h2>
      </div>
      {onRefresh && (
        <button aria-label="Actualizar" onClick={onRefresh} type="button">
          <RefreshCw size={16} />
        </button>
      )}
    </div>
  );
}

function SimpleTable({ columns, rows }: { columns: string[]; rows: Array<Record<string, unknown>> }) {
  if (rows.length === 0) {
    return <div className="empty-state">Sin registros</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{label(column)}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={String(row.id ?? row.numero ?? index)}>
              {columns.map((column) => (
                <td key={column}>{cell(row[column])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Metric({ title, value, tone }: { title: string; value: string; tone: "green" | "amber" | "blue" | "rose" }) {
  return (
    <article className={`metric ${tone}`}>
      <span>{title}</span>
      <strong>{value}</strong>
    </article>
  );
}

function LoadingState() {
  return (
    <div className="status-state">
      <Loader2 className="spin" size={24} />
      <span>Cargando</span>
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="status-state error">
      <AlertCircle size={24} />
      <span>{message}</span>
      <button onClick={onRetry} type="button">
        Reintentar
      </button>
    </div>
  );
}

function InlineError({ message }: { message: string }) {
  return (
    <div className="inline-error">
      <AlertCircle size={16} />
      {message}
    </div>
  );
}

function FormNotice({ kind, message }: { kind: "success" | "error"; message: string }) {
  return (
    <div className={`form-notice ${kind}`}>
      {kind === "error" ? <AlertCircle size={17} /> : <ShieldCheck size={17} />}
      {message}
    </div>
  );
}

type OperacionDetalleForm = {
  productoId: string;
  cantidad: string;
  costoUnitario: string;
};

function detalleInicial(type: "venta" | "compra", productoId?: unknown): OperacionDetalleForm {
  return {
    productoId: productoId === undefined || productoId === null ? "" : String(productoId),
    cantidad: "1",
    costoUnitario: type === "compra" ? "0.01" : ""
  };
}

function contactoInicial() {
  return {
    nombre: "",
    documentoIdentidad: "",
    telefono: "",
    email: "",
    direccion: ""
  };
}

function nullablePayload<T extends Record<string, string>>(payload: T) {
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [key, emptyToNull(value)])
  );
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function moneyInput(value: string) {
  const normalized = value.trim().replace(",", ".");
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
}

function integerInput(value: string) {
  const parsed = Number.parseInt(value.trim(), 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function readSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as Session;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

function openSession(response: LoginResponse, setSession: (session: Session) => void) {
  const session = { token: response.accessToken, usuario: response.usuario };
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  setSession(session);
}

function closeSession(setSession: (session: Session | null) => void) {
  localStorage.removeItem(SESSION_KEY);
  setSession(null);
}

function titleFor(module: ModuleKey) {
  return modules.find((item) => item.key === module)?.label ?? "Agro ERP";
}

function monthRange() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  return {
    desde: isoDate(start),
    hasta: isoDate(now)
  };
}

function isoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function pick(source: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    if (source && source[key] !== undefined && source[key] !== null) {
      return source[key];
    }
  }

  return 0;
}

function money(value: unknown) {
  const numeric = typeof value === "number" ? value : Number(value ?? 0);
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN"
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

function cell(value: unknown) {
  if (typeof value === "boolean") {
    return value ? "Si" : "No";
  }

  if (typeof value === "number") {
    return Number.isInteger(value) ? value.toString() : money(value);
  }

  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return String(value);
}

function label(value: string) {
  return value
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (first) => first.toUpperCase());
}

function errorMessage(caught: unknown) {
  if (caught instanceof ApiError) {
    return caught.correlationId ? `${caught.message} (${caught.correlationId})` : caught.message;
  }

  if (caught instanceof Error) {
    return caught.message;
  }

  return "Error inesperado";
}
