# Pedidos360

Sistema de gestión de pedidos con autenticación federada (OAuth 2.0 / OpenID Connect), arquitectura de microservicios, mensajería asíncrona con RabbitMQ, streaming de eventos con Apache Kafka y despliegue completo en la nube.

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
      ├──► ms-pedidos360-kafka    ──►  Kafka Cluster (3 ZooKeeper + 3 brokers)
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

El frontend nunca llama directamente a los microservicios: toda petición pasa primero por Azure API Management (que valida el token) y luego por el BFF (que revalida el token, verifica el rol, y reenvía la petición al microservicio correspondiente). Los eventos de negocio (cambios de estado de pedidos, movimientos de stock, auditoría) se publican de forma asíncrona a través de RabbitMQ. Además, orders y catalog publican sus eventos en Kafka, donde quedan guardados y se procesan como un stream para analítica en tiempo real, historial de pedidos y monitoreo de inventario (ver [Arquitectura Kafka](#arquitectura-kafka)).

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
- Spring for Apache Kafka (`spring-kafka`) para productores, consumidores y administración de Kafka
- Desplegado en Render (contenedores Docker)

**Mensajería**

- RabbitMQ 3.x en clúster de 2 nodos sobre Azure VM (IP: `20.114.208.83`)
- Topología: 1 Topic Exchange principal + 1 Direct Exchange DLX
- 3 colas de trabajo + 3 Dead-Letter Queues independientes
- ACK/NACK explícito con `AcknowledgeMode.MANUAL`
- Apache Kafka (Confluent 7.6) con 3 nodos ZooKeeper y 3 brokers en Docker Compose, administrado con Kafka UI

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
├── ms-pedidos360-kafka/      Microservicio de streaming (consumers, analítica, DLT y administración de Kafka)
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

## Arquitectura Kafka

RabbitMQ se usa para notificaciones y tareas puntuales (el mensaje se borra al consumirlo). Kafka se usa para **streaming de eventos**: los eventos quedan guardados en el tópico según su retención, varios consumidores los pueden leer de forma independiente y se pueden volver a procesar.

```
ms-pedidos360-orders  ──produce──►  pedidos360.order-events  (3 particiones, 3 réplicas)
                                        │
                     ┌──────────────────┴──────────────────┐
                     ▼                                     ▼
       pedidos360-orders-analytics           pedidos360-orders-tracking
       (3 hilos: KPIs en tiempo real)         (1 hilo: historial por pedido)

ms-pedidos360-catalog ──produce──►  pedidos360.catalog-events (3 particiones, 3 réplicas, compactado)
                                        │
                                        ▼
                          pedidos360-catalog-inventory
                       (3 hilos: stock actual + alertas de stock bajo)

Mensajes que fallan ──►  <tópico>.DLT  ──►  pedidos360-dlt-monitor
```

### Infraestructura (`ms-pedidos360-kafka/docker-compose.yml`)

| Componente | Cantidad | Detalle |
| --- | --- | --- |
| ZooKeeper | 3 nodos | Ensemble con mayoría: tolera la caída de 1 nodo |
| Brokers Kafka | 3 | `default.replication.factor=3`, `min.insync.replicas=2`, `auto.create.topics=false`, `unclean.leader.election=false` |
| Kafka UI | 1 | `http://<host>:8090` con login |
| kafka-init | 1 | Ejecuta `scripts/create-topics.sh` y termina |

Cada broker expone un listener interno (`kafka1..3:29092`, red docker) y uno externo (`9092`, `9093`, `9094`) para los microservicios. La IP pública se configura con `KAFKA_PUBLIC_HOST` en el archivo `.env`.

### Tópicos

| Tópico | Particiones | Réplicas | Configuración | Por qué |
| --- | --- | --- | --- | --- |
| `pedidos360.order-events` | 3 | 3 | `delete`, retención 7 días | Historial de eventos de pedidos; permite reprocesar la última semana |
| `pedidos360.catalog-events` | 3 | 3 | `compact` | Key = `product-{id}`: Kafka conserva el último evento de cada producto (estado actual del catálogo) |
| `pedidos360.order-events.DLT` | 3 | 3 | `delete`, retención 14 días | Mensajes de pedidos que fallaron |
| `pedidos360.catalog-events.DLT` | 3 | 3 | `delete`, retención 14 días | Mensajes de catálogo que fallaron |

Todos usan `min.insync.replicas=2`: con `acks=all` un mensaje solo se confirma cuando está en al menos 2 brokers.

### Eventos

| Evento | Productor | Key | Campos principales |
| --- | --- | --- | --- |
| `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `ORDER_DELETED` | orders | `order-{id}` | `eventId`, `eventType`, `orderId`, `cliente`, `estadoAnterior`, `estadoNuevo`, `timestamp`, `source` |
| `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED`, `STOCK_DECREASED` | catalog | `product-{id}` | `eventId`, `eventType`, `productId`, `nombre`, `precio`, `stock`, `timestamp`, `source` |

La key por entidad garantiza que todos los eventos de un mismo pedido o producto vayan a la misma partición y se procesen en orden.

### Configuración de clientes

- **Productores:** `acks=all`, idempotencia activada, reintentos acotados por `delivery.timeout.ms=120000`, `linger.ms=10`, `batch.size=32768`, compresión `lz4`. Serializador JSON propio (sin header `__TypeId__`, para no acoplar los microservicios por el nombre de las clases).
- **Consumidores:** `auto.offset.reset=earliest`, commit manejado por el contenedor (`AckMode.RECORD`), `max.poll.records=50`, `session.timeout.ms=15000`, rebalanceo cooperativo (`CooperativeStickyAssignor`), `isolation.level=read_committed`. Un deserializador tipado por tópico.

### Consumer groups

| Grupo | Tópico | Hilos | Responsabilidad |
| --- | --- | --- | --- |
| `pedidos360-orders-analytics` | order-events | 3 | KPIs en tiempo real (pedidos por estado, totales, últimos eventos) |
| `pedidos360-orders-tracking` | order-events | 1 | Historial de estados por pedido |
| `pedidos360-catalog-inventory` | catalog-events | 3 | Stock actual por producto y alertas cuando baja de 5 unidades |
| `pedidos360-dlt-monitor` | ambos DLT | 1 | Registro de mensajes fallidos con su motivo |

`orders-analytics` y `orders-tracking` leen el mismo tópico en grupos distintos, así que cada uno recibe todos los eventos. Dentro de un grupo con 3 hilos, Kafka reparte una partición por hilo. Los consumidores de pedidos son idempotentes por `eventId`.

### Manejo de errores

1. `ErrorHandlingDeserializer`: un mensaje que no se puede leer no bloquea la partición.
2. Validación del payload (`@NotNull`, `@NotBlank`, `@PositiveOrZero`) antes de procesar.
3. `DefaultErrorHandler` con backoff exponencial: 3 reintentos (1 s, 2 s, 4 s) para fallas temporales.
4. Si se agotan los reintentos, o si el error no se arregla reintentando (JSON inválido, datos inválidos), el mensaje va a `<tópico>.DLT` con headers que indican el tópico, la partición, el offset y el motivo del error.

### Endpoints de Kafka (`ms-pedidos360-kafka`, vía BFF en `/api/kafka`)

**Streaming** (Admin, Operator)

- `GET /api/kafka/streaming/orders/kpis` — KPIs de pedidos en tiempo real
- `GET /api/kafka/streaming/orders/{orderId}/timeline` — historial de un pedido
- `GET /api/kafka/streaming/inventory` — stock actual y alertas de stock bajo
- `GET /api/kafka/streaming/dlt` — últimos mensajes enviados al DLT

**Administración** (Admin)

- `GET /api/kafka/admin/cluster` — brokers y controlador
- `GET /api/kafka/admin/topics` — listar tópicos
- `GET /api/kafka/admin/topics/{name}` — particiones, líder, réplicas, ISR, offsets y configuración
- `POST /api/kafka/admin/topics` — crear tópico (`{"name","partitions","replicationFactor","configs"}`)
- `DELETE /api/kafka/admin/topics/{name}` — eliminar tópico
- `POST /api/kafka/admin/topics/{name}/partitions?total=N` — aumentar particiones
- `GET | PUT /api/kafka/admin/topics/{name}/config` — ver o modificar configuración
- `DELETE /api/kafka/admin/topics/{name}/config/{key}` — volver al valor por defecto
- `GET /api/kafka/admin/consumer-groups` — listar grupos
- `GET /api/kafka/admin/consumer-groups/{groupId}` — consumidores, offsets y lag por partición
- `POST /api/kafka/admin/consumer-groups/{groupId}/reset-offsets` — reset a `earliest`, `latest` u `offset`
- `DELETE /api/kafka/admin/consumer-groups/{groupId}` — eliminar grupo
- `GET /api/kafka/admin/listeners` y `POST /api/kafka/admin/listeners/{id}/stop|start` — detener o iniciar consumidores
- `POST /api/kafka/publish/order-event` y `/catalog-event` — publicar eventos de prueba

Para reprocesar un tópico: detener el listener del grupo, resetear sus offsets a `earliest` y volver a iniciarlo.

## Cómo correr el proyecto localmente

### Requisitos previos

- Node.js 22+ y Angular CLI
- Java 21
- Maven (o usar el wrapper `mvnw` incluido)
- Acceso a la base de datos Supabase (solicitar credenciales al equipo)
- Acceso al clúster RabbitMQ (solicitar credenciales al equipo) o instancia local
- Docker Desktop (para el clúster Kafka; necesita unos 4 GB de RAM libres)

### 1. Clúster Kafka

```bash
cd ms-pedidos360-kafka
cp .env.example .env          # cambiar KAFKA_UI_PASSWORD (y KAFKA_PUBLIC_HOST si es una VM)
docker compose up -d
docker compose logs kafka-init   # debe terminar mostrando los 4 tópicos
```

Kafka UI queda en `http://localhost:8090`.

### 2. Backend — microservicios

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

# Terminal 4 — ms-pedidos360-kafka (puerto 8084)
cd ms-pedidos360-kafka
./mvnw spring-boot:run

# Terminal 5 — ms-pedidos360-bff (puerto 8080)
cd ms-pedidos360-bff
./mvnw spring-boot:run
```

### 3. Frontend

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
| `KAFKA_BOOTSTRAP_SERVERS` | orders, catalog, kafka | Brokers Kafka (por defecto `localhost:9092,localhost:9093,localhost:9094`) |
| `KAFKA_SERVICE_URL`    | bff             | URL del microservicio de Kafka (por defecto `localhost:8084`)   |
| `KAFKA_PUBLIC_HOST`    | docker-compose  | Host que anuncian los brokers a los clientes externos           |
| `KAFKA_UI_PASSWORD`    | docker-compose  | Contraseña de Kafka UI                                          |

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
