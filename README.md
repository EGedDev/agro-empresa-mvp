# Agro Empresa MVP

Sistema web en desarrollo para digitalizar la gestión logística y comercial de Itaven SAC,
una MYPE del sector agrícola. El objetivo es construir una plataforma que permita administrar
catálogo, clientes, ventas e inventario, reduciendo procesos manuales y dejando una base
preparada para un futuro frontend comercial en React.

## Estado

Proyecto en etapa inicial de MVP. Actualmente el desarrollo se concentra en el backend con
Spring Boot.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server con JWT
- Springdoc OpenAPI / Swagger UI
- Bean Validation
- PostgreSQL
- Flyway para migraciones de base de datos
- H2 para tests
- Docker Compose
- Maven Wrapper

## Módulos Backend

- Catálogo de categorías
- Catálogo de productos
- Clientes
- Ventas
- Proveedores
- Compras
- Inventario y movimientos de stock
- Pagos de ventas y compras
- Autenticación, roles y auditoría
- Manejo global de errores
- Health check

## Estructura

```text
backend/agro-erp-api   API REST con Spring Boot
infra                  Servicios de infraestructura local
frontend               Futuro frontend React
docs                   Documentación del proyecto
```

## Configuración Local

El backend lee la configuración sensible desde variables de entorno. No subas archivos
`.env` con credenciales reales. Usa `.env.example` solo como plantilla local.

Desde la raíz del proyecto:

```powershell
Copy-Item .env.example .env
```

Antes de ejecutar el backend en PowerShell, define las variables necesarias:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/agro_db"
$env:DB_USERNAME="agro_user"
$env:DB_PASSWORD="change_me_for_local_dev"
$env:JWT_SECRET="change_this_local_development_secret_before_using_real_data"
```

## Ejecutar Base De Datos

Desde la raíz del proyecto:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

Docker Compose toma los valores desde `.env`.

## Ejecutar Backend

```powershell
cd backend/agro-erp-api
.\mvnw.cmd spring-boot:run
```

La API corre por defecto en:

```text
http://localhost:8080
```

## Migraciones De Base De Datos

El perfil `dev` usa Flyway para crear y versionar el esquema de PostgreSQL. Hibernate queda
en modo `validate`, por lo que valida las entidades contra la base, pero no modifica tablas
automaticamente.

Las migraciones viven en:

```text
backend/agro-erp-api/src/main/resources/db/migration
```

Si tu base local ya tenia tablas creadas por `ddl-auto=update`, respalda la data que quieras
conservar y recrea el volumen local, o define `FLYWAY_BASELINE_ON_MIGRATE=true` solo para
adoptar esa base existente bajo control de Flyway.

## Seguridad

La API usa JWT Bearer tokens. Primero crea el administrador inicial una sola vez:

```http
POST /api/v1/auth/bootstrap-admin
```

Luego inicia sesión en:

```http
POST /api/v1/auth/login
```

Roles disponibles:

```text
ADMIN, VENTAS, COMPRAS, INVENTARIO, GERENCIA
```

Los endpoints de usuarios quedan reservados para `ADMIN`, y los eventos de auditoría para
`ADMIN` o `GERENCIA`.

## Documentacion OpenAPI

La API publica su contrato OpenAPI y una consola Swagger UI para explorar endpoints durante
desarrollo:

```text
GET /v3/api-docs
GET /swagger-ui.html
```

El contrato documenta autenticacion JWT Bearer y marca los `POST` criticos que requieren
`Idempotency-Key`.

## Trazabilidad Operativa

Cada request recibe un `X-Correlation-Id`. Puedes enviarlo desde el cliente o dejar que la API
genere uno automaticamente. El mismo valor vuelve en la respuesta, aparece en errores y queda
registrado en auditoria junto con IP de origen y `User-Agent` cuando hay eventos auditables.

Headers soportados:

- `X-Correlation-Id`: identificador principal recomendado.
- `X-Request-Id`: alias aceptado cuando no se envia `X-Correlation-Id`.
- `X-Forwarded-For` / `X-Real-IP`: origen usado cuando la API corre detras de proxy.

## Ejecutar Tests

Los tests usan el perfil `test` y una base H2 en memoria, por lo que no dependen de
PostgreSQL local.

```powershell
cd backend/agro-erp-api
.\mvnw.cmd test
```

## Endpoints Principales

- `GET /api/v1/health`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`
- `POST /api/v1/auth/bootstrap-admin`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/usuarios`
- `POST /api/v1/usuarios`
- `GET /api/v1/categorias`
- `POST /api/v1/categorias`
- `GET /api/v1/productos`
- `GET /api/v1/productos/stock-bajo`
- `GET /api/v1/clientes`
- `GET /api/v1/proveedores`
- `POST /api/v1/proveedores`
- `POST /api/v1/ventas`
- `PATCH /api/v1/ventas/{id}/cancelar`
- `GET /api/v1/ventas/{ventaId}/pagos`
- `POST /api/v1/ventas/{ventaId}/pagos`
- `POST /api/v1/compras`
- `PATCH /api/v1/compras/{id}/cancelar`
- `GET /api/v1/compras/{compraId}/pagos`
- `POST /api/v1/compras/{compraId}/pagos`
- `GET /api/v1/inventario/movimientos`
- `POST /api/v1/inventario/movimientos`
- `GET /api/v1/inventario/movimientos/producto/{productoId}`
- `GET /api/v1/auditoria/eventos`
- `GET /api/v1/reportes/finanzas/resumen`

### Paginacion y filtros

Los listados principales devuelven una respuesta paginada con esta estructura:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 0,
  "totalPaginas": 0,
  "primera": true,
  "ultima": true
}
```

Parametros comunes:

- `page`: pagina basada en cero. Valor por defecto: `0`.
- `size`: elementos por pagina. Valor por defecto: `20`, maximo: `100`.
- `sort`: campo y direccion, por ejemplo `nombre,asc` o `fechaVenta,desc`.

Filtros disponibles:

- `GET /api/v1/categorias?buscar=&activo=`
- `GET /api/v1/productos?buscar=&activo=&categoriaId=&stockBajo=`
- `GET /api/v1/clientes?buscar=&activo=`
- `GET /api/v1/proveedores?buscar=&activo=`
- `GET /api/v1/usuarios?buscar=&rol=&activo=`
- `GET /api/v1/ventas?clienteId=&estado=&estadoPago=&desde=&hasta=`
- `GET /api/v1/compras?proveedorId=&estado=&estadoPago=&desde=&hasta=`
- `GET /api/v1/inventario/movimientos?productoId=&tipo=&referenciaTipo=&desde=&hasta=`
- `GET /api/v1/auditoria/eventos?username=&accion=&recursoTipo=&recursoId=&correlationId=&desde=&hasta=`
- `GET /api/v1/reportes/finanzas/resumen?desde=&hasta=`
- `GET /api/v1/ventas/{ventaId}/pagos?metodoPago=&desde=&hasta=`
- `GET /api/v1/compras/{compraId}/pagos?metodoPago=&desde=&hasta=`

Las fechas usan formato ISO `YYYY-MM-DD`.

### Idempotencia en operaciones criticas

Los `POST` que pueden duplicar dinero o stock exigen el header `Idempotency-Key`:

- `POST /api/v1/ventas`
- `POST /api/v1/compras`
- `POST /api/v1/ventas/{ventaId}/pagos`
- `POST /api/v1/compras/{compraId}/pagos`
- `POST /api/v1/inventario/movimientos`

Reglas:

- La clave debe ser unica por usuario, metodo HTTP y ruta.
- Si se repite la misma clave con el mismo body, la API devuelve la respuesta guardada sin ejecutar de nuevo la operacion.
- Si se repite la misma clave con otro body, la API responde `409 CONFLICT`.
- Si falta la clave en una operacion critica, la API responde `428 PRECONDITION_REQUIRED`.
- En reintentos exitosos, la respuesta incluye `Idempotency-Replayed: true`.

Ejemplo:

```http
POST /api/v1/ventas
Authorization: Bearer <token>
Idempotency-Key: 7f8e5a2d-2b7f-4f44-a5b7-9ad3e421d2b2
Content-Type: application/json
```

## Próximos Pasos

- Ampliar reportes operativos del MVP
- Construir frontend React para panel interno y web comercial
