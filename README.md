# FoodRush API

RESTful food delivery API (**FoodRush**), built with **Java 25** and **Spring Boot 4.1.1**, designed for high concurrency, transactional consistency, distributed caching with Redis, stateless security via JWT, and event-driven architecture with RabbitMQ.

## Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4.1.1 (`spring-boot-starter-webmvc`)
- **Persistence & Database:**
  - Spring Data JPA / Hibernate (`spring-boot-starter-data-jpa`)
  - PostgreSQL 17 (`org.postgresql:postgresql`)
  - Flyway Migrations (`spring-boot-starter-flyway`, `flyway-database-postgresql`)
- **Cache & NoSQL:** Spring Cache (`spring-boot-starter-cache`) & Spring Data Redis (`spring-boot-starter-data-redis`)
- **Messaging & Queues:** Spring AMQP / RabbitMQ (`spring-boot-starter-amqp`) with Topic Exchange, Dead Letter Exchange (DLX/DLQ), and JSON serialization
- **Observability & Metrics:** Spring Boot Actuator (`spring-boot-starter-actuator`), Micrometer Prometheus (`micrometer-registry-prometheus`), Prometheus, and Grafana
- **Security & Authentication:**
  - Spring Security (`spring-boot-starter-security`)
  - JSON Web Token — JJWT `0.12.6` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) with `HS512` signature and `BCrypt` password hashing
- **API Documentation (OpenAPI / Swagger):**
  - SpringDoc OpenAPI `2.8.5` (`springdoc-openapi-starter-webmvc-ui`) with HTTP Bearer JWT authentication support
- **Validation & Error Handling:**
  - Jakarta Bean Validation (`spring-boot-starter-validation`) + custom constraint validators (`@ValidCPF`, `@ValidCNPJ`, `@ValidPhone`, `@ValidCEP`)
  - Standardized responses using **RFC 7807 (`ProblemDetail`)** via `GlobalExceptionHandler`
- **Productivity & Build:** Lombok, Spring Boot Configuration Processor, and Apache Maven (`./mvnw`)

## Prerequisites

- **JDK 25** installed
- **Docker** & **Docker Compose** (to run PostgreSQL, Redis, RabbitMQ, Prometheus, and Grafana locally)

## Project Structure

```text
foodrush-api/
├── docker/
│   ├── grafana/
│   │   └── provisioning/
│   │       └── datasources/
│   │           └── prometheus.yml
│   └── prometheus/
│       └── prometheus.yml
├── src/
│   ├── main/
│   │   ├── java/com/foodrush/api/
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── consumer/
│   │   │   │   ├── CourierDispatchConsumer.java
│   │   │   │   ├── CustomerTimelineConsumer.java
│   │   │   │   └── RestaurantOrderConsumer.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── MenuController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── RestaurantController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── AddressRequestDTO.java
│   │   │   │   │   ├── ComplementGroupRequestDTO.java
│   │   │   │   │   ├── ComplementItemRequestDTO.java
│   │   │   │   │   ├── LoginRequestDTO.java
│   │   │   │   │   ├── MenuItemRequestDTO.java
│   │   │   │   │   ├── MenuItemSizeRequestDTO.java
│   │   │   │   │   ├── OrderCreateRequestDTO.java
│   │   │   │   │   ├── OrderItemComplementRequestDTO.java
│   │   │   │   │   ├── OrderItemRequestDTO.java
│   │   │   │   │   ├── OrderStatusUpdateRequestDTO.java
│   │   │   │   │   ├── RefreshTokenRequestDTO.java
│   │   │   │   │   ├── RestaurantPatchRequestDTO.java
│   │   │   │   │   ├── RestaurantRequestDTO.java
│   │   │   │   │   ├── UserPatchRequestDTO.java
│   │   │   │   │   └── UserRequestDTO.java
│   │   │   │   └── response/
│   │   │   │       ├── AddressResponseDTO.java
│   │   │   │       ├── AuthResponseDTO.java
│   │   │   │       ├── ComplementGroupDetailDTO.java
│   │   │   │       ├── ComplementItemDetailDTO.java
│   │   │   │       ├── DeliveryFeeEstimateResponseDTO.java
│   │   │   │       ├── MenuItemResponseDTO.java
│   │   │   │       ├── MenuItemSizeDetailDTO.java
│   │   │   │       ├── OrderItemComplementResponseDTO.java
│   │   │   │       ├── OrderItemResponseDTO.java
│   │   │   │       ├── OrderResponseDTO.java
│   │   │   │       ├── RestaurantResponseDTO.java
│   │   │   │       └── UserResponseDTO.java
│   │   │   ├── enums/
│   │   │   │   ├── OrderStatus.java
│   │   │   │   └── RoleType.java
│   │   │   ├── event/
│   │   │   │   └── OrderStatusChangedEvent.java
│   │   │   ├── exception/
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── Address.java
│   │   │   │   ├── ComplementGroup.java
│   │   │   │   ├── ComplementItem.java
│   │   │   │   ├── MenuItem.java
│   │   │   │   ├── MenuItemSize.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── OrderItemComplement.java
│   │   │   │   ├── Restaurant.java
│   │   │   │   └── User.java
│   │   │   ├── producer/
│   │   │   │   └── OrderEventProducer.java
│   │   │   ├── repository/
│   │   │   │   ├── AddressRepository.java
│   │   │   │   ├── ComplementGroupRepository.java
│   │   │   │   ├── ComplementItemRepository.java
│   │   │   │   ├── MenuItemRepository.java
│   │   │   │   ├── MenuItemSizeRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── RestaurantRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/
│   │   │   │   ├── DeliveryCalculationService.java
│   │   │   │   ├── GeocodingService.java
│   │   │   │   ├── MenuService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── RestaurantService.java
│   │   │   │   └── UserService.java
│   │   │   ├── util/
│   │   │   │   └── GeoDistanceUtils.java
│   │   │   ├── validation/
│   │   │   │   ├── CepValidatorConstraint.java
│   │   │   │   ├── CnpjValidatorConstraint.java
│   │   │   │   ├── CpfValidatorConstraint.java
│   │   │   │   ├── PhoneValidatorConstraint.java
│   │   │   │   ├── ValidCEP.java
│   │   │   │   ├── ValidCNPJ.java
│   │   │   │   ├── ValidCPF.java
│   │   │   │   └── ValidPhone.java
│   │   │   └── ApiApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       │   ├── V1__init_schema.sql
│   │       │   ├── V2__seed_mock_data.sql
│   │       │   └── V3__seed_dishes_and_complements.sql
│   │       └── application.yml
│   └── test/
│       └── java/com/foodrush/api/
│           ├── consumer/
│           │   └── OrderConsumersTest.java
│           ├── controller/
│           │   ├── AuthControllerTest.java
│           │   ├── MenuControllerTest.java
│           │   └── OrderControllerTest.java
│           ├── producer/
│           │   └── OrderEventProducerTest.java
│           ├── service/
│           │   ├── DeliveryCalculationServiceTest.java
│           │   ├── MenuServiceTest.java
│           │   ├── OrderServiceTest.java
│           │   ├── RestaurantServiceTest.java
│           │   └── UserServiceTest.java
│           ├── util/
│           │   └── GeoDistanceUtilsTest.java
│           └── ApiApplicationTests.java
├── .env.example
├── docker-compose.yml
├── pom.xml
├── mvnw
└── mvnw.cmd
```

## Data Model (Flyway Migrations)

- **`addresses`**: Postal code (`cep`), street details, number, complement, neighborhood, city, state (`UF`), and geolocation coordinates (`latitude`, `longitude`).
- **`users`**: Credentials, unique personal identifiers (`email`, `phone`, `cpf`), access roles (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_DRIVER`, `ADMIN`), and optional reference to `address_id`.
- **`restaurants`**: Restaurant profile (`trade_name`, `cnpj`, `phone`), owner reference (`owner_id`), address reference (`address_id`), geolocation (`latitude`, `longitude`), base delivery fee (`base_delivery_fee`), and operational status (`is_open`).
- **`menu_items` & `menu_item_sizes`**: Catalog items and size variations with optimistic lock (`version`) and pessimistic lock query (`PESSIMISTIC_WRITE`) to prevent overselling.
- **`complement_groups` & `complement_items`**: Add-on groups with min/max selection quantities and requirement flags.
- **`orders`, `order_items` & `order_item_complements`**: Orders with idempotency key (`idempotency_key`), consolidated financial totals (`subtotal`, `delivery_fee`, `total`), line items, and historical add-on price snapshots.

## Configuration and Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/theusinhoiost/foodrush-api.git
   cd foodrush-api
   ```

2. **Configure Environment Variables:**
   Copy the example `.env.example` file to `.env`:
   ```bash
   cp .env.example .env
   ```
   Default values for local development:
   | Variable | Default Value | Description |
   | :--- | :--- | :--- |
   | `POSTGRES_DB` | `foodrush_db` | PostgreSQL database name |
   | `POSTGRES_USER` | `postgres` | PostgreSQL username |
   | `POSTGRES_PASSWORD` | `postgres` | PostgreSQL password |
   | `POSTGRES_PORT` | `5432` | PostgreSQL external port |
   | `REDIS_PORT` | `6379` | Redis external port |
   | `RABBITMQ_DEFAULT_USER` | `guest` | RabbitMQ administrative user |
   | `RABBITMQ_DEFAULT_PASS` | `guest` | RabbitMQ administrative password |
   | `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
   | `RABBITMQ_MANAGEMENT_PORT`| `15672` | RabbitMQ Management Web UI port |
   | `PROMETHEUS_PORT` | `9090` | Prometheus external port |
   | `GRAFANA_PORT` | `3000` | Grafana external port |
   | `GRAFANA_ADMIN_USER` | `admin` | Grafana administrator username |
   | `GRAFANA_ADMIN_PASSWORD` | `admin` | Grafana administrator password |

3. **Start Infrastructure Services with Docker Compose:**
   ```bash
   docker compose up -d
   ```
   This will spin up:
   - **PostgreSQL 17:** `localhost:5432`
   - **Redis 7:** `localhost:6379`
   - **RabbitMQ 4 Management:** `localhost:5672` (Management UI: `http://localhost:15672` — `guest`/`guest`)
   - **Prometheus:** `http://localhost:9090`
   - **Grafana:** `http://localhost:3000` (Login: `admin` / `admin`)

4. **Build and Run the Application:**
   ```bash
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```
   Or run the main class `com.foodrush.api.ApiApplication` directly from your IDE.

## Interactive API Documentation (Swagger / OpenAPI)

Once the application is running, access Swagger UI at:

- **Swagger UI:** [http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)
- **Swagger UI Shortcut:** `http://localhost:8080/api/v1/docs`
- **OpenAPI JSON Spec:** `http://localhost:8080/api/v1/docs/json`

## API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register new user account (`UserRequestDTO` with CEP geocoding) | Public |
| `POST` | `/api/v1/auth/login` | Authenticate user and issue JWT `accessToken` + `refreshToken` | Public |
| `POST` | `/api/v1/auth/refresh` | Renew short-lived `accessToken` using valid `refreshToken` | Public |

### Users (`/api/v1/users`)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | List all registered users (cached in `users`) | Bearer JWT |
| `GET` | `/api/v1/users/{id}` | Find user details by ID (cached in `users`) | Bearer JWT |
| `PUT` | `/api/v1/users/{id}` | Full update of user profile and address details | Bearer JWT |
| `PATCH`| `/api/v1/users/{id}` | Partial update of user profile fields | Bearer JWT |
| `PATCH`| `/api/v1/users/{id}/toggle-active` | Toggle user active status (activate/deactivate) | Bearer JWT |

### Restaurants (`/api/v1/restaurants`)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/restaurants` | Register new restaurant (`RestaurantRequestDTO` with CEP geocoding) | Bearer JWT |
| `GET` | `/api/v1/restaurants` | List all restaurants (cached in `restaurants`) | Public |
| `GET` | `/api/v1/restaurants/{id}` | Find restaurant details by ID (cached in `restaurants`) | Public |
| `GET` | `/api/v1/restaurants/nearby` | Find restaurants within a geographic radius (Haversine formula: `lat`, `lon`, `radiusKm`) | Public |
| `GET` | `/api/v1/restaurants/{id}/delivery-estimate` | Estimate dynamic delivery fee and duration for client coordinates | Public |
| `PUT` | `/api/v1/restaurants/{id}` | Update restaurant profile, contact, and address | Bearer JWT |
| `PATCH`| `/api/v1/restaurants/{id}` | Partial update of restaurant profile fields | Bearer JWT |
| `PATCH`| `/api/v1/restaurants/{id}/toggle-open` | Toggle restaurant operational status (open/closed) | Bearer JWT |

### Menus & Catalog (`/api/v1/restaurants/{id}/menu` & `/api/v1/menu-items`)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/restaurants/{restaurantId}/menu` | Retrieve complete active menu with sizes, inventory, and complement groups (cached in `restaurant_menu`) | Public |
| `GET` | `/api/v1/menu-items/{id}` | Get detailed dish information by ID | Public |
| `POST` | `/api/v1/restaurants/{restaurantId}/menu-items` | Create new dish with sizes, stock quantity, and customization groups | Bearer JWT |
| `PATCH`| `/api/v1/menu-items/{id}/toggle-available` | Toggle dish availability on menu | Bearer JWT |
| `DELETE`| `/api/v1/menu-items/{id}` | Remove dish from restaurant menu | Bearer JWT |

### Orders & Checkout (`/api/v1/orders`)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/orders` | Place order with idempotent check (`idempotencyKey`), stock deduction, dynamic delivery fee, and snapshot pricing | Bearer JWT |
| `GET` | `/api/v1/orders/{id}` | Get full order details and items | Bearer JWT |
| `GET` | `/api/v1/orders/customer/{customerId}` | Retrieve customer order history sorted by most recent | Bearer JWT |
| `GET` | `/api/v1/orders/restaurant/{restaurantId}` | Filter restaurant orders by lifecycle status (`?status=PAID`, etc.) | Bearer JWT |
| `PATCH`| `/api/v1/orders/{id}/status` | Advance order lifecycle and publish event to RabbitMQ | Bearer JWT |

## Event-Driven Messaging (RabbitMQ)

The application uses a **Topic Exchange** (`foodrush.orders.exchange`) to publish order lifecycle events asynchronously and decoupled to platform actors:

- **Exchange:** `foodrush.orders.exchange` (Topic Exchange)
- **Dead Letter Exchange (DLX):** `foodrush.orders.dlx` / **DLQ:** `foodrush.orders.dlq`
- **Queues and Actors:**
  - **Restaurant (`foodrush.restaurant.orders.queue`):**
    - Bindings: `order.paid`, `order.canceled`
    - Alerts the kitchen display system (KDS) for preparation or cancellation.
  - **Courier / Dispatch (`foodrush.courier.dispatch.queue`):**
    - Binding: `order.ready`
    - Triggers the dispatch engine to locate and notify nearby couriers once food is ready.
  - **Customer (`foodrush.customer.timeline.queue`):**
    - Binding: `order.*`
    - Receives all order status transitions to broadcast real-time Push/SMS notifications to the customer app.

## Observability & Monitoring (Prometheus & Grafana)

The API provides full metrics via **Spring Boot Actuator** and **Micrometer Prometheus**:

- **Actuator Endpoints:**
  - Healthcheck: [http://localhost:8080/api/v1/actuator/health](http://localhost:8080/api/v1/actuator/health) (Database, Redis, RabbitMQ, Disk)
  - Prometheus metrics: [http://localhost:8080/api/v1/actuator/prometheus](http://localhost:8080/api/v1/actuator/prometheus)
  - Metrics index: [http://localhost:8080/api/v1/actuator/metrics](http://localhost:8080/api/v1/actuator/metrics)
- **Prometheus:** [http://localhost:9090](http://localhost:9090)
  - Automatic 5s scrape configured in `docker/prometheus/prometheus.yml`.
- **Grafana:** [http://localhost:3000](http://localhost:3000) (User: `admin` / Password: `admin`)
  - **Pre-provisioned DataSources:** Both **Prometheus** (`uid: prometheus`) and **PostgreSQL** (`uid: postgres`) are automatically configured via `docker/grafana/provisioning/datasources/prometheus.yml`.
  - **Pre-provisioned Dashboard:** Open [http://localhost:3000/d/foodrush-overview](http://localhost:3000/d/foodrush-overview) (**FoodRush API — Painel de Negócio & Observabilidade**) to view pre-built charts for:
    - Total Users, Restaurants, Menu Items, Orders, and Gross Sales Volume (R$).
    - Users by Role (`CLIENT`, `RESTAURANT`, `COURIER`, `ADMIN`) and Orders by Status (`CREATED`, `PAID`, `PREPARING`, `READY`, `IN_ROUTE`, `DELIVERED`, `CANCELED`).
    - Open vs Closed Restaurants and Restaurant Performance Table.
    - Real-time Micrometer Prometheus gauges (`foodrush_users_by_role`, `foodrush_orders_by_status`), HTTP Request Rate (req/s), and JVM Heap Memory.

## Running Tests

```bash
./mvnw test
```

## Packaging & Build

To generate a production `.jar` artifact:

```bash
./mvnw clean package
```

To build an OCI/Docker container image using Spring Boot buildpacks:

```bash
./mvnw spring-boot:build-image
```
