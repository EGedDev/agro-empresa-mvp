# Agro Empresa MVP

Sistema web en desarrollo para digitalizar la gestion logistica y comercial de
una tienda de productos agricolas. El objetivo es construir una plataforma que
permita administrar catalogo, clientes, ventas e inventario, reduciendo procesos
manuales y dejando una base preparada para un futuro frontend comercial en React.

## Estado

Proyecto en etapa inicial de MVP. Actualmente el desarrollo se concentra en el
backend con Spring Boot.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Docker Compose
- Maven Wrapper

## Modulos Backend

- Catalogo de categorias
- Catalogo de productos
- Clientes
- Ventas
- Inventario y movimientos de stock
- Manejo global de errores
- Health check

## Estructura

```text
backend/agro-erp-api   API REST con Spring Boot
infra                  Servicios de infraestructura local
frontend               Futuro frontend React
docs                   Documentacion del proyecto
```

## Ejecutar Base De Datos

Desde la raiz del proyecto:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

La base de datos local de desarrollo queda disponible con los valores definidos
en `infra/docker-compose.yml`.

## Configuracion

El backend lee la configuracion desde variables de entorno. Puedes usar
`.env.example` como referencia para tu entorno local:

```properties
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5433/agro_db
DB_USERNAME=agro_user
DB_PASSWORD=change_me_for_local_dev
JPA_DDL_AUTO=update
JPA_SHOW_SQL=true
```

No subas archivos `.env` con credenciales reales. Los valores del ejemplo son
solo para desarrollo local.

## Ejecutar Backend

```powershell
cd backend/agro-erp-api
.\mvnw.cmd spring-boot:run
```

La API corre por defecto en:

```text
http://localhost:8080
```

## Ejecutar Tests

```powershell
cd backend/agro-erp-api
.\mvnw.cmd test
```

## Endpoints Principales

- `GET /api/v1/health`
- `GET /api/v1/categorias`
- `POST /api/v1/categorias`
- `GET /api/v1/productos`
- `GET /api/v1/productos/stock-bajo`
- `GET /api/v1/clientes`
- `POST /api/v1/ventas`
- `PATCH /api/v1/ventas/{id}/cancelar`
- `GET /api/v1/inventario/movimientos`
- `GET /api/v1/inventario/movimientos/producto/{productoId}`

## Proximos Pasos

- Fortalecer validaciones de requests
- Completar movimientos de inventario
- Agregar tests de reglas de negocio
- Incorporar autenticacion y roles
- Documentar API con OpenAPI/Swagger
- Construir frontend React para panel interno y web comercial
