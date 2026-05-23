import {
  AlertCircle,
  BarChart3,
  KeyRound,
  LayoutDashboard,
  Loader2,
  LogOut,
  PackageSearch,
  RefreshCw,
  ShieldCheck,
  ShoppingCart,
  Truck,
  UserPlus,
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

type ModuleKey = "dashboard" | "productos" | "ventas" | "compras" | "cartera";

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
  { key: "productos", label: "Productos", icon: <PackageSearch size={18} /> },
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
        {activeModule === "productos" && (
          <ResourcePanel
            columns={["nombre", "precioVenta", "costoPromedio", "stockActual", "stockMinimo", "activo"]}
            endpoint="/api/v1/productos?size=10&sort=nombre,asc"
            token={session.token}
          />
        )}
        {activeModule === "ventas" && (
          <ResourcePanel
            columns={["numero", "clienteNombre", "estado", "estadoPago", "total", "saldoPendiente"]}
            endpoint="/api/v1/ventas?size=10&sort=fechaVenta,desc"
            token={session.token}
          />
        )}
        {activeModule === "compras" && (
          <ResourcePanel
            columns={["numero", "proveedorNombre", "estado", "estadoPago", "total", "saldoPendiente"]}
            endpoint="/api/v1/compras?size=10&sort=fechaCompra,desc"
            token={session.token}
          />
        )}
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

function PanelHeader({ icon, title, onRefresh }: { icon: ReactNode; title: string; onRefresh: () => void }) {
  return (
    <div className="panel-header">
      <div>
        {icon}
        <h2>{title}</h2>
      </div>
      <button aria-label="Actualizar" onClick={onRefresh} type="button">
        <RefreshCw size={16} />
      </button>
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
  return date.toISOString().slice(0, 10);
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
