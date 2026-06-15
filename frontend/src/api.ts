const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type Usuario = {
  id: number;
  username: string;
  nombre: string;
  rol: string;
  activo: boolean;
};

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  usuario: Usuario;
};

export type PaginaResponse<T> = {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
};

type ApiOptions = {
  method?: string;
  body?: unknown;
  token?: string;
  idempotent?: boolean;
};

type UploadOptions = {
  token: string;
  file: File;
  fieldName?: string;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
    public readonly correlationId?: string
  ) {
    super(message);
  }
}

export async function apiRequest<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("X-Correlation-Id", crypto.randomUUID());

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  if (options.idempotent) {
    headers.set("Idempotency-Key", crypto.randomUUID());
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  const contentType = response.headers.get("content-type") ?? "";
  const data = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    throw new ApiError(
      data?.message ?? "No se pudo completar la operacion",
      response.status,
      data?.code,
      data?.correlationId ?? response.headers.get("X-Correlation-Id") ?? undefined
    );
  }

  return data as T;
}

export async function uploadFile<T>(path: string, options: UploadOptions): Promise<T> {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("Authorization", `Bearer ${options.token}`);
  headers.set("X-Correlation-Id", crypto.randomUUID());

  const formData = new FormData();
  formData.set(options.fieldName ?? "imagen", options.file);

  const response = await fetch(`${API_URL}${path}`, {
    method: "POST",
    headers,
    body: formData
  });

  const contentType = response.headers.get("content-type") ?? "";
  const data = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    throw new ApiError(
      data?.message ?? "No se pudo completar la carga",
      response.status,
      data?.code,
      data?.correlationId ?? response.headers.get("X-Correlation-Id") ?? undefined
    );
  }

  return data as T;
}

export function login(username: string, password: string) {
  return apiRequest<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { username, password }
  });
}

export function bootstrapAdmin(username: string, nombre: string, password: string) {
  return apiRequest<Usuario>("/api/v1/auth/bootstrap-admin", {
    method: "POST",
    body: { username, nombre, password }
  });
}

export function getPage<T>(path: string, token: string) {
  return apiRequest<PaginaResponse<T>>(path, { token });
}

export function apiBaseUrl() {
  return API_URL;
}
