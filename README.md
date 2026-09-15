# Pedidos360

Sistema de gestión de pedidos con autenticación federada (OAuth 2.0 / OpenID Connect), arquitectura de microservicios y despliegue completo en la nube.

Proyecto desarrollado para la asignatura **DSY1107 — Desarrollo Cloud Native I**, DuocUC.

## Integrantes

- Jorge Albornoz ([@joralbornoz](https://github.com/joralbornoz))
- Abraham Lopez ([@AbrahamSilverhand](https://github.com/AbrahamSilverhand))
- Sebastián Rodriguez ([@Mihokus](https://github.com/Mihokus))

## URLs en producción

| Componente | URL |
|---|---|
| Frontend | https://thankful-river-06284c40f.5.azurestaticapps.net |
| API Management (Gateway) | https://apim-pedidos360.azure-api.net |
| BFF | https://pedidos360.onrender.com |
| Microservicio Orders | https://pedidos360-orders.onrender.com |
| Microservicio Catalog | https://pedidos360-catalog.onrender.com |

## Arquitectura

```
Angular (MSAL)
      │
      ▼
Azure API Management  ──►  valida JWT (issuer, audience, firma)
      │
      ▼
BFF (Spring Security)  ──►  valida JWT + rol, actúa como proxy
      │
      ├──► ms-pedidos360-orders   ──►  Supabase (PostgreSQL)
      └──► ms-pedidos360-catalog  ──►  Supabase (PostgreSQL)
```

El frontend nunca llama directamente a los microservicios: toda petición pasa primero por Azure API Management (que valida el token) y luego por el BFF (que revalida el token, verifica el rol, y reenvía la petición al microservicio correspondiente).

## Tecnologías

**Frontend**
- Angular 18 (standalone components)
- MSAL Angular (`@azure/msal-angular`) — autenticación OAuth 2.0 / OIDC con Authorization Code + PKCE
- Desplegado en Azure Static Web Apps con CI/CD desde GitHub Actions

**Backend**
- Spring Boot 4 / Java 21
- Spring Security con OAuth2 Resource Server (validación de JWT)
- Spring Data JPA + PostgreSQL (Supabase)
- Desplegado en Render (contenedores Docker)

**Identidad**
- Microsoft Entra ID (tenant propio, `joralapp.onmicrosoft.com`)
- Roles de aplicación: `Admin`, `Operator`, `Customer`
- Self-service sign-up habilitado (los usuarios pueden registrarse desde el propio login)

**Gateway**
- Azure API Management — validación de JWT, CORS, enrutamiento hacia el BFF

## Estructura del repositorio

```
pedidos360/
├── frontend-pedidos360/     Angular + MSAL
├── ms-pedidos360-bff/       Backend For Frontend (Spring Security, proxy)
├── ms-pedidos360-orders/    Microservicio de pedidos (CRUD completo)
└── ms-pedidos360-catalog/   Microservicio de catálogo (CRUD completo)
```

## Cómo correr el proyecto localmente

### Requisitos previos
- Node.js 22+ y Angular CLI
- Java 21
- Maven (o usar el wrapper `mvnw` incluido)
- Acceso a la base de datos Supabase (solicitar credenciales al equipo)

### 1. Backend — microservicios

Cada microservicio necesita la variable de entorno `SUPABASE_DB_PASSWORD`.

```bash
# Terminal 1 — ms-pedidos360-orders (puerto 8081)
cd ms-pedidos360-orders
export SUPABASE_DB_PASSWORD=xxxxx
./mvnw spring-boot:run

# Terminal 2 — ms-pedidos360-catalog (puerto 8082)
cd ms-pedidos360-catalog
export SUPABASE_DB_PASSWORD=xxxxx
./mvnw spring-boot:run

# Terminal 3 — ms-pedidos360-bff (puerto 8080)
cd ms-pedidos360-bff
./mvnw spring-boot:run
```

### 2. Frontend

```bash
cd frontend-pedidos360
npm install
ng serve
```

Accede en `http://localhost:4200`.

### Variables de entorno relevantes

| Variable | Dónde | Descripción |
|---|---|---|
| `SUPABASE_DB_PASSWORD` | orders, catalog | Contraseña de conexión a la base de datos |
| `AZURE_AD_ISSUER_URI` | bff | Issuer del tenant de Azure AD (tiene valor por defecto) |
| `AZURE_AD_AUDIENCE` | bff | Audience esperado del JWT (tiene valor por defecto) |
| `ORDERS_SERVICE_URL` | bff | URL del microservicio de orders (por defecto `localhost:8081`) |
| `CATALOG_SERVICE_URL` | bff | URL del microservicio de catalog (por defecto `localhost:8082`) |

## Autenticación y autorización

El sistema usa el flujo **Authorization Code con PKCE** (estándar recomendado para SPAs) a través de MSAL. Tras iniciar sesión, el frontend obtiene un JWT con los claims `roles`, `name`, `preferred_username`, entre otros.

Cada petición al backend pasa por dos capas de validación de JWT (defensa en profundidad):
1. **Azure API Management**: valida issuer, audience y firma antes de reenviar al BFF.
2. **BFF (Spring Security)**: vuelve a validar el JWT y verifica el rol (`@PreAuthorize`) antes de reenviar al microservicio correspondiente.

### Roles y accesos

| Rol | Acceso |
|---|---|
| Admin | Ve KPIs globales, gestiona pedidos y catálogo |
| Operator | Gestiona pedidos y catálogo |
| Customer | Ve y crea sus propios pedidos |

## Endpoints principales

**Orders** (`ms-pedidos360-orders`, vía BFF en `/api/orders`)
- `GET /api/orders` — listar pedidos
- `GET /api/orders/{id}` — detalle de un pedido
- `POST /api/orders` — crear pedido
- `PUT /api/orders/{id}` — actualizar pedido
- `PATCH /api/orders/{id}/status` — cambiar estado
- `DELETE /api/orders/{id}` — eliminar pedido

**Catalog** (`ms-pedidos360-catalog`, vía BFF en `/api/catalog`)
- `GET /api/catalog` — listar productos
- `POST /api/catalog` — crear producto
- `PUT /api/catalog/{id}` — actualizar producto
- `DELETE /api/catalog/{id}` — eliminar producto

## Notas de arquitectura

- El BFF y los microservicios se despliegan de forma independiente en Render; el BFF nunca se conecta directamente a la base de datos, únicamente reenvía peticiones ya autenticadas a los microservicios correspondientes.
- Los microservicios (`orders`, `catalog`) no validan JWT por sí mismos: confían en que el BFF ya realizó esa validación antes de reenviarles la petición.
- La política CORS está configurada tanto en Azure API Management como en el BFF, restringida a los orígenes conocidos del frontend (desarrollo y producción).
