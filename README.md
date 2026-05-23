# Agro Empresa MVP

Sistema web en desarrollo para digitalizar la gestión logística y comercial de Itaven SAC,
una MYPE del sector agrícola. El objetivo es construir una plataforma que permita administrar
catálogo, clientes, ventas e inventario, reduciendo procesos manuales y dejando una base
preparada para operacion interna con frontend React.

## Estado

Proyecto en etapa inicial de MVP. El backend concentra el nucleo transaccional del ERP y el
frontend React ya inicia el panel interno para consumir la API.

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
- React
- Vite
- TypeScript

## Módulos Backend

- Catálogo de categorías
- Catálogo de productos
- Clientes
- Ventas
- Proveedores
- Compras
- Inventario y movimientos de stock
- Kardex valorizado con costo promedio ponderado
- Pagos de ventas y compras
- Anulaciones de pagos con trazabilidad contable
- Devoluciones de ventas y compras con ajuste de inventario
- Caja y movimientos financieros
- Cierres de caja por periodo
- Cartera de cuentas por cobrar y pagar
- Autenticación, roles y auditoría
- Manejo global de errores
- Health check

## Estructura

```text
backend/agro-erp-api   API REST con Spring Boot
infra                  Servicios de infraestructura local
frontend               Aplicacion interna React
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
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173"
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

## Ejecutar Frontend

Desde otra terminal:

```powershell
cd frontend
npm install
npm run dev
```

La app interna corre por defecto en:

```text
http://127.0.0.1:5173
```

Configura `VITE_API_URL` si el backend usa otro host o puerto.

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

## CORS para frontend

El backend permite preflight CORS solo desde origenes configurados. En desarrollo el valor
recomendado es:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

La API expone `X-Correlation-Id` e `Idempotency-Replayed` para que el frontend pueda
mostrar trazabilidad y distinguir respuestas recuperadas por idempotencia.

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
- `POST /api/v1/ventas/{ventaId}/pagos/{pagoId}/anular`
- `GET /api/v1/ventas/{ventaId}/devoluciones`
- `POST /api/v1/ventas/{ventaId}/devoluciones`
- `POST /api/v1/compras`
- `PATCH /api/v1/compras/{id}/cancelar`
- `GET /api/v1/compras/{compraId}/pagos`
- `POST /api/v1/compras/{compraId}/pagos`
- `POST /api/v1/compras/{compraId}/pagos/{pagoId}/anular`
- `GET /api/v1/compras/{compraId}/devoluciones`
- `POST /api/v1/compras/{compraId}/devoluciones`
- `GET /api/v1/inventario/movimientos`
- `POST /api/v1/inventario/movimientos`
- `GET /api/v1/inventario/movimientos/producto/{productoId}`
- `GET /api/v1/auditoria/eventos`
- `GET /api/v1/reportes/finanzas/resumen`
- `GET /api/v1/reportes/finanzas/rentabilidad`
- `GET /api/v1/reportes/finanzas/rentabilidad/productos`
- `GET /api/v1/reportes/gerenciales/ventas/clientes`
- `GET /api/v1/reportes/gerenciales/ventas/productos`
- `GET /api/v1/reportes/gerenciales/compras/proveedores`
- `GET /api/v1/reportes/gerenciales/compras/productos`
- `GET /api/v1/reportes/inventario/resumen`
- `GET /api/v1/finanzas/caja/movimientos`
- `GET /api/v1/finanzas/caja/resumen`
- `GET /api/v1/finanzas/caja/resumen/metodos`
- `GET /api/v1/finanzas/caja/cierres`
- `GET /api/v1/finanzas/caja/cierres/diferencias`
- `POST /api/v1/finanzas/caja/cierres`
- `GET /api/v1/finanzas/cartera/cuentas-por-cobrar`
- `GET /api/v1/finanzas/cartera/cuentas-por-pagar`
- `GET /api/v1/finanzas/cartera/resumen`

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
- `GET /api/v1/reportes/finanzas/rentabilidad?desde=&hasta=`
- `GET /api/v1/reportes/finanzas/rentabilidad/productos?desde=&hasta=&limite=`
- `GET /api/v1/reportes/gerenciales/ventas/clientes?desde=&hasta=&limite=`
- `GET /api/v1/reportes/gerenciales/ventas/productos?desde=&hasta=&limite=`
- `GET /api/v1/reportes/gerenciales/compras/proveedores?desde=&hasta=&limite=`
- `GET /api/v1/reportes/gerenciales/compras/productos?desde=&hasta=&limite=`
- `GET /api/v1/reportes/inventario/resumen?desde=&hasta=`
- `GET /api/v1/finanzas/caja/movimientos?tipo=&metodoPago=&referenciaTipo=&referenciaId=&desde=&hasta=`
- `GET /api/v1/finanzas/caja/resumen?desde=&hasta=`
- `GET /api/v1/finanzas/caja/resumen/metodos?desde=&hasta=`
- `GET /api/v1/finanzas/caja/cierres?desde=&hasta=`
- `GET /api/v1/finanzas/caja/cierres/diferencias?desde=&hasta=&metodoPago=&soloConDiferencia=`
- `GET /api/v1/finanzas/cartera/cuentas-por-cobrar?numero=&clienteId=&estadoPago=&desde=&hasta=&venceDesde=&venceHasta=&vencida=`
- `GET /api/v1/finanzas/cartera/cuentas-por-pagar?numero=&proveedorId=&estadoPago=&desde=&hasta=&venceDesde=&venceHasta=&vencida=`
- `GET /api/v1/finanzas/cartera/resumen?desde=&hasta=`
- `GET /api/v1/ventas?numero=&clienteId=&estado=&estadoPago=&desde=&hasta=`
- `GET /api/v1/compras?numero=&proveedorId=&estado=&estadoPago=&desde=&hasta=`
- `GET /api/v1/ventas/{ventaId}/pagos?numero=&metodoPago=&desde=&hasta=`
- `GET /api/v1/compras/{compraId}/pagos?numero=&metodoPago=&desde=&hasta=`
- `GET /api/v1/ventas/{ventaId}/devoluciones?numero=`
- `GET /api/v1/compras/{compraId}/devoluciones?numero=`
- `GET /api/v1/finanzas/caja/cierres?numero=&desde=&hasta=`

Las fechas usan formato ISO `YYYY-MM-DD`.

En ventas y compras, `fechaVencimiento` es opcional. Si no se envia, la API usa el dia de
registro como vencimiento para mantener compatibilidad con ventas/compras al contado.

### Numeracion documental

La API asigna un `numero` interno a los documentos operativos principales. Este numero es
transaccional, legible y no depende del `id` tecnico de base de datos.

- Ventas: `V-000001`
- Compras: `C-000001`
- Pagos de venta: `PV-000001`
- Pagos de compra: `PC-000001`
- Devoluciones de venta: `DV-000001`
- Devoluciones de compra: `DC-000001`
- Cierres de caja: `CC-000001`

Los correlativos se generan con bloqueo transaccional para evitar duplicados cuando dos
usuarios registran documentos al mismo tiempo.

Los listados aceptan `numero` como filtro exacto normalizado, por ejemplo
`GET /api/v1/ventas?numero=V-000001` o
`GET /api/v1/finanzas/caja/cierres?numero=CC-000001`.

### Inventario valorizado

Productos y movimientos de inventario mantienen valorizacion para control operativo y
financiero:

- `ProductoResponse` expone `costoPromedio` y `valorInventario`.
- Las compras y entradas manuales actualizan el costo promedio ponderado.
- Las ventas, anulaciones y devoluciones registran movimientos con `costoUnitario`,
  `valorMovimiento`, `valorInventarioAnterior` y `valorInventarioNuevo`.
- `GET /api/v1/reportes/inventario/resumen` incluye `valorInventarioTotal` y el valor
  acumulado de entradas/salidas del periodo.

### Rentabilidad

El reporte `GET /api/v1/reportes/finanzas/rentabilidad` calcula ingresos brutos, costo de
ventas, devoluciones, costo devuelto, utilidad bruta y margen bruto porcentual del periodo.

`GET /api/v1/reportes/finanzas/rentabilidad/productos` devuelve los productos ordenados por
utilidad bruta, con unidades vendidas/devueltas, ingresos netos, costo neto y margen.

### Reportes gerenciales

Los reportes gerenciales agregan operaciones por periodo y aceptan `limite` entre 1 y 100:

- Ventas por cliente: cantidad de ventas, total neto y saldo pendiente.
- Ventas por producto: unidades vendidas/devueltas/netas y total neto.
- Compras por proveedor: cantidad de compras, total neto y saldo pendiente.
- Compras por producto: unidades compradas/devueltas/netas y total neto.

### Anulaciones de pagos

Los pagos no se eliminan fisicamente. Para corregir un pago se usa una anulacion con motivo
obligatorio:

- `POST /api/v1/ventas/{ventaId}/pagos/{pagoId}/anular`
- `POST /api/v1/compras/{compraId}/pagos/{pagoId}/anular`

La anulacion marca el pago como `anulado`, recalcula el saldo de la venta o compra y registra
un movimiento inverso en caja (`REVERSO_PAGO_VENTA` o `REVERSO_PAGO_COMPRA`). Si el periodo
de caja del dia esta cerrado, la anulacion se rechaza para no alterar saldos conciliados.

### Devoluciones

Las devoluciones se registran sobre detalles originales de venta o compra para evitar
ambiguedad cuando un producto aparece en mas de una linea. Cada devolucion conserva motivo,
fecha, total y detalle aplicado.

- `POST /api/v1/ventas/{ventaId}/devoluciones`: reduce el total/saldo de la venta y registra
  una entrada de inventario `ENTRADA_POR_DEVOLUCION_VENTA`.
- `POST /api/v1/compras/{compraId}/devoluciones`: reduce el total/saldo de la compra y registra
  una salida de inventario `SALIDA_POR_DEVOLUCION_COMPRA`.

La API bloquea devoluciones que superen la cantidad disponible del detalle original. Tambien
bloquea devoluciones que dejarian pagos registrados por encima del nuevo total; primero se debe
anular o ajustar el pago correspondiente. En compras, si no hay stock suficiente para devolver
al proveedor, la operacion se rechaza.

### Cierres de caja

El cierre de caja calcula ingresos, egresos y saldo neto desde `movimientos_caja`.
Opcionalmente puede registrar el saldo reportado por metodo de pago para controlar
diferencias de efectivo, transferencias, billeteras u otros medios.

```json
{
  "desde": "2026-05-19",
  "hasta": "2026-05-19",
  "saldoReportado": 4.00,
  "observaciones": "Cierre diario",
  "metodos": [
    {
      "metodoPago": "EFECTIVO",
      "saldoReportado": 16.00
    },
    {
      "metodoPago": "TRANSFERENCIA",
      "saldoReportado": -12.00
    }
  ]
}
```

Si se envia `metodos`, la API exige que todos los metodos con movimientos en el periodo
tengan saldo reportado y que la suma por metodo coincida con `saldoReportado`.

Despues de registrar un cierre, la API bloquea nuevos movimientos de caja con fecha dentro
del periodo cerrado. Esto evita que pagos posteriores alteren saldos ya conciliados.

El reporte `GET /api/v1/finanzas/caja/cierres/diferencias` muestra cierres con sobrantes
o faltantes. Por defecto devuelve solo cierres con diferencia; usa `soloConDiferencia=false`
para incluir cierres cuadrados. Tambien permite filtrar por `metodoPago`.

### Idempotencia en operaciones criticas

Los `POST` que pueden duplicar dinero o stock exigen el header `Idempotency-Key`:

- `POST /api/v1/ventas`
- `POST /api/v1/compras`
- `POST /api/v1/ventas/{ventaId}/pagos`
- `POST /api/v1/compras/{compraId}/pagos`
- `POST /api/v1/ventas/{ventaId}/pagos/{pagoId}/anular`
- `POST /api/v1/compras/{compraId}/pagos/{pagoId}/anular`
- `POST /api/v1/ventas/{ventaId}/devoluciones`
- `POST /api/v1/compras/{compraId}/devoluciones`
- `POST /api/v1/inventario/movimientos`
- `POST /api/v1/finanzas/caja/cierres`

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
