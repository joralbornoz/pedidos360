# Pedidos360

Sistema de gestión de pedidos con autenticación federada (OAuth 2.0 / OpenID Connect), arquitectura de microservicios, mensajería asíncrona con RabbitMQ y despliegue completo en la nube.

Proyecto desarrollado para la asignatura **DSY1107 — Desarrollo Cloud Native I**, DuocUC.

## Integrantes

- Jorge Albornoz ([@joralbornoz](https://github.com/joralbornoz))
- Abraham Lopez ([@AbrahamSilverhand](https://github.com/AbrahamSilverhand))
- Sebastián Rodriguez ([@Mihokus](https://github.com/Mihokus))

## URLs en producción

| Componente               | URL                                                                                                              |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| Frontend                 | [https://thankful-river-06284c40f.5.azurestaticapps.net](https://thankful-river-06284c40f.5.azurestaticapps.net) |
| API Management (Gateway) | [https://apim-pedidos360.azure-api.net](https://apim-pedidos360.azure-api.net)                                   |
| BFF                      | [https://pedidos360.onrender.com](https://pedidos360.onrender.com)                                               |
| Microservicio Orders     | [https://pedidos360-orders.onrender.com](https://pedidos360-orders.onrender.com)                                 |
| Microservicio Catalog    | [https://pedidos360-catalog.onrender.com](https://pedidos360-catalog.onrender.com)                               |
| Microservicio RabbitMQ   | [https://pedidos360-rabbitmq.onrender.com](https://pedidos360-rabbitmq.onrender.com)                             |
| RabbitMQ Management UI   | [http://20.114.208.83:15672](http://20.114.208.83:15672)                                                         |

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
      ├──► ms-pedidos360-catalog  ──►  Supabase (PostgreSQL)
      └──► ms-pedidos360-rabbitmq ──►  RabbitMQ Cluster (Azure VM)
                                            │
                              ┌─────────────┼─────────────┐
                              ▼             ▼             ▼
                     notifications       stock          audit
                        queue            queue          queue
                              │             │             │
                              ▼             ▼             ▼
                         notifications   stock.dlq    audit.dlq
                           .dlq
```

El frontend nunca llama directamente a los microservicios: toda petición pasa primero por Azure API Management (que valida el token) y luego por el BFF (que revalida el token, verifica el rol, y reenvía la petición al microservicio correspondiente). Los eventos de negocio (cambios de estado de pedidos, movimientos de stock, auditoría) se publican de forma asíncrona a través de RabbitMQ.

## Tecnologías

**Frontend**

- Angular 18 (standalone components)
- MSAL Angular (`@azure/msal-angular`) — autenticación OAuth 2.0 / OIDC con Authorization Code + PKCE
- Desplegado en Azure Static Web Apps con CI/CD desde GitHub Actions

**Backend**

- Spring Boot / Java 21
- Spring Security con OAuth2 Resource Server (validación de JWT)
- Spring Data JPA + PostgreSQL (Supabase)
- Spring AMQP (`spring-boot-starter-amqp`) para integración con RabbitMQ
- Desplegado en Render (contenedores Docker)

**Mensajería**

- RabbitMQ 3.x en clúster de 2 nodos sobre Azure VM (IP: `20.114.208.83`)
- Topología: 1 Topic Exchange principal + 1 Direct Exchange DLX
- 3 colas de trabajo + 3 Dead-Letter Queues independientes
- ACK/NACK explícito con `AcknowledgeMode.MANUAL`

**Identidad**

- Microsoft Entra ID (tenant propio, `joralapp.onmicrosoft.com`)
- Roles de aplicación: `Admin`, `Operator`, `Customer`
- Self-service sign-up habilitado (los usuarios pueden registrarse desde el propio login)

**Gateway**

- Azure API Management — validación de JWT, CORS, enrutamiento hacia el BFF

## Estructura del repositorio

```
pedidos360/
├── frontend-pedidos360/      Angular + MSAL
├── ms-pedidos360-bff/        Backend For Frontend (Spring Security, proxy)
├── ms-pedidos360-orders/     Microservicio de pedidos (CRUD completo)
├── ms-pedidos360-catalog/    Microservicio de catálogo (CRUD completo)
└── ms-pedidos360-rabbitmq/   Microservicio de mensajería (producers, consumers, topología)
```

## Topología RabbitMQ

### Exchanges

| Exchange              | Tipo   | Descripción                                      |
| --------------------- | ------ | ------------------------------------------------ |
| `pedidos360.exchange` | Topic  | Exchange principal; recibe todos los eventos     |
| `pedidos360.dlx`      | Direct | Dead-Letter Exchange; recibe mensajes rechazados |

### Colas y routing keys

| Cola                         | Routing key             | Dead-Letter Queue                |
| ---------------------------- | ----------------------- | -------------------------------- |
| `q.pedidos360.notifications` | `order.status.*`        | `q.pedidos360.notifications.dlq` |
| `q.pedidos360.stock`         | `order.stock.decrement` | `q.pedidos360.stock.dlq`         |
| `q.pedidos360.audit`         | `audit.#`               | `q.pedidos360.audit.dlq`         |

Cada cola tiene su propia DLQ independiente, configurada con `x-dead-letter-exchange` y `x-dead-letter-routing-key`. Los mensajes que superen el número máximo de reintentos (NACK sin requeue) se derivan automáticamente a la DLQ correspondiente.

### Eventos publicados

| Routing key             | Disparado por                 | Consumidor esperado        |
| ----------------------- | ----------------------------- | -------------------------- |
| `order.status.changed`  | Cambio de estado de un pedido | Servicio de notificaciones |
| `order.stock.decrement` | Aceptación de un pedido       | Servicio de inventario     |
| `audit.orders`          | Cualquier operación en orders | Servicio de auditoría      |

## Cómo correr el proyecto localmente

### Requisitos previos

- Node.js 22+ y Angular CLI
- Java 21
- Maven (o usar el wrapper `mvnw` incluido)
- Acceso a la base de datos Supabase (solicitar credenciales al equipo)
- Acceso al clúster RabbitMQ (solicitar credenciales al equipo) o instancia local

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

# Terminal 3 — ms-pedidos360-rabbitmq (puerto 8083)
cd ms-pedidos360-rabbitmq
export RABBITMQ_HOST=20.114.208.83
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=xxxxx
export RABBITMQ_PASSWORD=xxxxx
./mvnw spring-boot:run

# Terminal 4 — ms-pedidos360-bff (puerto 8080)
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

| Variable               | Dónde           | Descripción                                                     |
| ---------------------- | --------------- | --------------------------------------------------------------- |
| `SUPABASE_DB_PASSWORD` | orders, catalog | Contraseña de conexión a la base de datos                       |
| `AZURE_AD_ISSUER_URI`  | bff             | Issuer del tenant de Azure AD (tiene valor por defecto)         |
| `AZURE_AD_AUDIENCE`    | bff             | Audience esperado del JWT (tiene valor por defecto)             |
| `ORDERS_SERVICE_URL`   | bff             | URL del microservicio de orders (por defecto `localhost:8081`)  |
| `CATALOG_SERVICE_URL`  | bff             | URL del microservicio de catalog (por defecto `localhost:8082`) |
| `RABBITMQ_HOST`        | rabbitmq        | Host del broker RabbitMQ                                        |
| `RABBITMQ_PORT`        | rabbitmq        | Puerto AMQP (por defecto `5672`)                                |
| `RABBITMQ_USERNAME`    | rabbitmq        | Usuario de RabbitMQ                                             |
| `RABBITMQ_PASSWORD`    | rabbitmq        | Contraseña de RabbitMQ                                          |

## Autenticación y autorización

El sistema usa el flujo **Authorization Code con PKCE** (estándar recomendado para SPAs) a través de MSAL. Tras iniciar sesión, el frontend obtiene un JWT con los claims `roles`, `name`, `preferred_username`, entre otros.

Cada petición al backend pasa por dos capas de validación de JWT (defensa en profundidad):

1. **Azure API Management**: valida issuer, audience y firma antes de reenviar al BFF.
2. **BFF (Spring Security)**: vuelve a validar el JWT y verifica el rol (`@PreAuthorize`) antes de reenviar al microservicio correspondiente.

### Roles y accesos

| Rol      | Acceso                                        |
| -------- | --------------------------------------------- |
| Admin    | Ve KPIs globales, gestiona pedidos y catálogo |
| Operator | Gestiona pedidos y catálogo                   |
| Customer | Ve y crea sus propios pedidos                 |

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
- `PUT /api/catalog/{id}` — actualizar producto _(agregado en EP3)_
- `DELETE /api/catalog/{id}` — eliminar producto _(agregado en EP3)_

**RabbitMQ** (`ms-pedidos360-rabbitmq`, vía BFF en `/api/messages`)

- `POST /api/messages/publish` — publicar un mensaje manualmente (Admin/Operator)
- `GET /api/messages/queue/{queueName}` — consultar mensajes en cola (Admin)

## Notas de arquitectura

- El BFF y los microservicios se despliegan de forma independiente en Render; el BFF nunca se conecta directamente a la base de datos, únicamente reenvía peticiones ya autenticadas a los microservicios correspondientes.
- Los microservicios (`orders`, `catalog`) no validan JWT por sí mismos: confían en que el BFF ya realizó esa validación antes de reenviarles la petición.
- La política CORS está configurada tanto en Azure API Management como en el BFF, restringida a los orígenes conocidos del frontend (desarrollo y producción).
- El clúster RabbitMQ corre en una Azure VM con 2 nodos (`20.114.208.83:5672`). La consola de administración está disponible en el puerto `15672`.
- Las colas usan **ACK/NACK explícito** (`AcknowledgeMode.MANUAL`): un mensaje solo se elimina de la cola cuando el consumer confirma el procesamiento exitoso. En caso de error, el mensaje se redirige automáticamente a la DLQ correspondiente.
- Las Dead-Letter Queues son **independientes por cola** (no hay una DLQ compartida), lo que permite inspeccionar y reencolar fallos de forma granular por tipo de evento.
