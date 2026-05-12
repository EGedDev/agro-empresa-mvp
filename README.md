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
- Bean Validation
- PostgreSQL
- H2 para tests
- Docker Compose
- Maven Wrapper

## Módulos Backend

- Catálogo de categorías
- Catálogo de productos
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

## Ejecutar Tests

Los tests usan el perfil `test` y una base H2 en memoria, por lo que no dependen de
PostgreSQL local.

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
- `POST /api/v1/inventario/movimientos`
- `GET /api/v1/inventario/movimientos/producto/{productoId}`

## Próximos Pasos

- Agregar autenticación y roles
- Documentar API con OpenAPI/Swagger
- Agregar paginación en listados
- Incorporar control de concurrencia para stock
- Construir frontend React para panel interno y web comercial
