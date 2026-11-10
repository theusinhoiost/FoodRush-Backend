# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Nginx Reverse Proxy / API Gateway & Port Isolation (`docker/nginx/nginx.conf`, `docker-compose.yml`, `application.yml`, `.env.example`):**
  - Added `nginx:1.27-alpine` container (`foodrush-nginx`) as the single external HTTP entry point on port `80` (`${NGINX_PORT:-80}`).
  - Configured reverse proxy routing for the Spring Boot API (`/api/v1/`), Grafana (`/grafana/`), Prometheus (`/prometheus/`), and RabbitMQ Management UI (`/rabbitmq/`), plus a dedicated gateway health check endpoint (`GET /health`).
  - Configured per-IP rate limiting zones (`auth_limit` at `5r/s` burst `10` for `/api/v1/auth/` and `api_limit` at `20r/s` burst `40` for `/api/v1/`), Gzip compression, and security headers (`server_tokens off`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`).
  - Removed host port exposure (`ports:`) from Grafana (`3000`), Prometheus (`9090`), and RabbitMQ Management (`15672`), isolating them inside the internal Docker network.
  - Bound PostgreSQL (`127.0.0.1:5432`), Redis (`127.0.0.1:6379`), and RabbitMQ AMQP (`127.0.0.1:5672`) strictly to the local loopback interface so they are never exposed on external network interfaces (`nmap`).
  - Enabled `server.forward-headers-strategy: framework` in `application.yml` and sub-path serving on Grafana and Prometheus.
  - Synchronized `.env.example` and `application.yml` with unified environment variable fallbacks for PostgreSQL, Redis, RabbitMQ, Grafana, OpenCage, and JWT settings.

### Security
- **Privilege Escalation Prevention & JWT Token Type Hardening (`service/UserService`, `dto/request/UserRequestDTO`, `security/JwtTokenProvider`, `security/JwtAuthenticationFilter`, `controller/AuthController`):**
  - Blocked self-assignment of the `ADMIN` role in public user registration and profile update endpoints (`UserService`), preventing privilege escalation attacks.
  - Added explicit `token_type` claim (`ACCESS` vs `REFRESH`) to signed JWTs in `JwtTokenProvider`.
  - Updated `JwtAuthenticationFilter` to reject `REFRESH` tokens presented as Bearer access tokens, and updated `AuthController.refreshToken` to reject `ACCESS` tokens on `/api/v1/auth/refresh`.
  - Restricted external scraping of `/api/v1/actuator/prometheus` and `/api/v1/actuator/metrics` (`403 Forbidden`) at the Nginx gateway and restricted `/prometheus/` UI access to private/local networks.
  - Changed `management.endpoint.health.show-details` from `always` to `when_authorized` in `application.yml`.
  - Added unit tests `shouldThrowExceptionWhenTryingToRegisterAsAdmin` (`UserServiceTest`) and `testRefreshToken_RejectAccessToken` (`AuthControllerTest`).
- **OpenCage Geocoder Integration & Address Endpoints (`service/GeocodingService`, `service/AddressService`, `controller/AddressController`, `dto/response/AddressResponseDTO`):**
  - Integrated **OpenCage Geocoder (`jopencage` 3.0.0)** in `GeocodingService` to resolve exact street-level geographic coordinates (`latitude` and `longitude`) from Brazilian postal codes (CEP) and street numbers, with automatic fallback to **BrasilAPI CEP v2** coordinates.
  - Added `opencage.api.key` configuration property in `application.yml` (`${OPENCAGE_API_KEY:}`) and documented `OPENCAGE_API_KEY` in `.env.example`.
  - Enriched `AddressResponseDTO` with:
    - `formattedAddress`: complete human-readable address string for user display.
    - `mapUrl`: direct link to view the exact location on Google Maps.
    - `openStreetMapUrl`: direct link to view the exact location on OpenStreetMap.
  - Created `AddressService` and `AddressController` (`/api/v1/addresses`) exposing endpoints to list addresses (`GET /addresses`), get address by ID (`GET /addresses/{id}`), create/save a geocoded address (`POST /addresses`), and preview geocoded coordinates and map links (`POST /addresses/geocode`).
  - Added unit test suite `AddressAndGeocodingTest` covering OpenCage coordinate resolution, formatted address generation, and map visualization URLs.
- **Multi-Tenant Ownership Verification & Forbidden Exception Handling (`service/MenuService`, `controller/MenuController`, `exception/GlobalExceptionHandler`):**
  - Implemented `validateRestaurantOwnership` verifying that the authenticated user (`principal.getName()`) is the true owner of the restaurant or holds `ROLE_ADMIN` before allowing creation, availability toggles, or deletion of menu dishes.
  - Added centralized handler for `AccessDeniedException` in `GlobalExceptionHandler` returning RFC 7807 **`403 Forbidden`** (`https://api.foodrush.com/errors/forbidden`).
  - Added unit test `shouldThrowWhenNotOwner()` in `MenuServiceTest`.
- **Mock Menu Catalog Migration (`db/migration/V3__seed_dishes_and_complements.sql`):**
  - Added new Flyway migration `V3__seed_dishes_and_complements.sql` populating rich mock catalogs for both seeded restaurants (`FoodRush Burger Artesanal` and `Pizzaria Bella Napoli`).
  - Added dishes: *Bacon Cheddar Bomb*, *Chicken Crispy Supreme*, *Veggie Falafel Burger*, *Onion Rings Crocantes*, *Milkshake de Nutella com Ninho*, *Pizza Calabresa Especial*, *Pizza Quatro Queijos Gratinada*, *Pizza Frango com Catupiry*, and *Pizza Doce de Banana com Canela*.
  - Added multiple portion sizes with inventory stock counts and custom complement groups (e.g. *Turbine seu Cheddar*, *Bordas Recheadas de Catupiry/Cheddar/Vulcão Provolone*).
- **Menu and Catalog Management (`controller/MenuController`, `service/MenuService`, `dto/`):**
  - Implemented `MenuService` with Spring Cache integration (`@Cacheable` and `@CacheEvict` on `restaurant_menu`) for high-performance public menu retrieval.
  - Added support for hierarchical menu items: dish base prices, size variants with independent inventory (`MenuItemSize`), and complement groups with optional/required constraints and min/max quantities (`ComplementGroup` & `ComplementItem`).
  - Implemented `MenuController` with REST endpoints:
    - `GET /api/v1/restaurants/{restaurantId}/menu`: public cached retrieval of a restaurant's active catalog.
    - `GET /api/v1/menu-items/{id}`: detailed view of an individual dish with sizes and customizations.
    - `POST /api/v1/restaurants/{restaurantId}/menu-items`: authenticated creation of dishes with nested sizes and complement groups.
    - `PATCH /api/v1/menu-items/{id}/toggle-available`: flip menu item public visibility.
    - `DELETE /api/v1/menu-items/{id}`: remove dish from restaurant catalog.
  - Configured public access in `SecurityConfig` for `GET /menu-items/**` and `GET /restaurants/{id}/menu`.
  - Added unit test suites: `MenuServiceTest` (4 tests) and `MenuControllerTest` (3 tests).
- **Haversine Distance & Dynamic Delivery Fee Calculation (`util/GeoDistanceUtils`, `service/DeliveryCalculationService`, `dto/`):**
  - Created `GeoDistanceUtils` implementing the Haversine great-circle trigonometric formula to compute distance in kilometers between two geographic points (latitude/longitude).
  - Created `DeliveryCalculationService` providing dynamic delivery fee calculation (`baseFee + (distanceKm * feePerKm)`) and estimated preparation/transit minutes (`20min base + 4min/km`).
  - Added nearby restaurant spatial filtering (`findNearbyRestaurants`) within a configurable radial boundary (`radiusKm`).
  - Integrated dynamic delivery fee calculation into `OrderService.create()` when customer and restaurant have geocoded coordinates.
  - Added new REST endpoints in `RestaurantController`:
    - `GET /api/v1/restaurants/nearby?lat=...&lon=...&radiusKm=10.0`
    - `GET /api/v1/restaurants/{id}/delivery-estimate?lat=...&lon=...`
  - Added test suites: `GeoDistanceUtilsTest` (3 tests) and `DeliveryCalculationServiceTest` (2 tests).
- **Order Management & Checkout Flow (`controller/OrderController`, `service/OrderService`, `dto/`):**
  - Implemented `OrderService` with transactional checkout, idempotency key verification, restaurant operational check, pessimistic locking on stock deduction (`MenuItemSizeRepository.findByIdWithPessimisticLock`), automatic address resolution, and subtotal + delivery fee calculation.
  - Implemented `OrderController` exposing REST endpoints:
    - `POST /api/v1/orders`: create order with items, sizes, and complement add-ons.
    - `GET /api/v1/orders/{id}`: retrieve order with full details.
    - `GET /api/v1/orders/customer/{customerId}`: list order history by customer.
    - `GET /api/v1/orders/restaurant/{restaurantId}?status=...`: filter restaurant orders by status.
    - `PATCH /api/v1/orders/{id}/status`: advance order status with state machine validation and dispatch dynamic `OrderStatusChangedEvent` to RabbitMQ.
  - Added request/response DTOs: `OrderCreateRequestDTO`, `OrderItemRequestDTO`, `OrderItemComplementRequestDTO`, `OrderStatusUpdateRequestDTO`, `OrderResponseDTO`, `OrderItemResponseDTO`, and `OrderItemComplementResponseDTO`.
  - Added comprehensive unit test suites: `OrderServiceTest` (6 tests) and `OrderControllerTest` (4 tests).
- **Cross-Origin Resource Sharing (CORS) Configuration (`config/SecurityConfig`):**
  - Configured `CorsConfigurationSource` allowing standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`) and localhost frontend origins (`http://localhost:*`, `http://127.0.0.1:*`) with credentials.
  - Enabled `.cors()` in `SecurityFilterChain`.
- **Docker Environment Configuration (`.env.example`, `.env`, `docker-compose.yml`, `.gitignore`):**
  - Added `.env.example` and local `.env` files with configurable database, Redis, RabbitMQ, Prometheus, and Grafana ports and credentials.
  - Parameterized `docker-compose.yml` with environment variables and sensible fallback defaults.
  - Added `.env` to `.gitignore` while tracking `.env.example`.
- **Event-Driven Messaging with RabbitMQ (`config/RabbitMQConfig`, `event/`, `producer/`, `consumer/`):**
  - Implemented Topic Exchange `foodrush.orders.exchange` for dynamic order lifecycle routing.
  - Created 3 dedicated queues:
    - `foodrush.restaurant.orders.queue`: consumes `order.paid` and `order.canceled` to alert the kitchen.
    - `foodrush.courier.dispatch.queue`: consumes `order.ready` to dispatch nearby couriers.
    - `foodrush.customer.timeline.queue`: consumes `order.*` to broadcast real-time status updates to customers.
  - Implemented Dead Letter Exchange (DLX) `foodrush.orders.dlx` and DLQ `foodrush.orders.dlq` for unprocessable messages.
  - Created immutable event record `OrderStatusChangedEvent` and `OrderEventProducer`.
  - Created consumers: `RestaurantOrderConsumer`, `CourierDispatchConsumer`, and `CustomerTimelineConsumer`.
  - Added `READY` status to `OrderStatus` enum.
  - Added unit test suites for producer and consumers (`OrderEventProducerTest`, `OrderConsumersTest`).
- **Observability and Metrics with Prometheus & Grafana (`docker/`, `pom.xml`, `application.yml`, `config/SecurityConfig`, `config/BusinessMetricsConfig`):**
  - Added `spring-boot-starter-actuator` and `micrometer-registry-prometheus` dependencies.
  - Configured Actuator endpoints (`health`, `info`, `metrics`, `prometheus`) and HTTP request latency percentiles histogram (`percentiles-histogram`).
  - Created `BusinessMetricsConfig` exposing custom Micrometer `Gauge` metrics for users by role (`foodrush_users_by_role`), users by status (`foodrush_users_by_status`), orders by status (`foodrush_orders_by_status`), and restaurants by operational state (`foodrush_restaurants_by_status`).
  - Permitted Actuator endpoints (`/actuator/**`, `/api/v1/actuator/**`) in `SecurityConfig`.
  - Created Prometheus scraping configuration in `docker/prometheus/prometheus.yml`.
  - Added automated **Prometheus** and **PostgreSQL** datasource provisioning for Grafana in `docker/grafana/provisioning/datasources/prometheus.yml`.
  - Added pre-configured **FoodRush Business & Observability Dashboard** (`docker/grafana/provisioning/dashboards/foodrush-overview.json`) with KPI stat cards, donut/pie charts for users by role, bar gauges for orders by status, restaurant performance table, HTTP RPS, and JVM memory telemetry.
  - Added `prometheus` (port `9090`) and `grafana` (port `3000`) containers with persistent volumes to `docker-compose.yml`.
- **Address Management, Automated Geocoding, and DB Seed (`model/Address`, `service/GeocodingService`, `db/migration/`):**
  - Unified `addresses` table and foreign keys `address_id` (`users` and `restaurants`) into `V1__init_schema.sql`.
  - Created Flyway migration `V2__seed_mock_data.sql` with initial seed data (addresses, users across all roles with password `123456`, restaurants, menu items, sizes, complements, and a sample order).
  - Implemented `Address` entity, `AddressRepository`, `AddressRequestDTO` (accepting `cep`, `number`, and optional `complement`), and `AddressResponseDTO`.
  - Added custom Bean Validation constraint `@ValidCEP` (`CepValidatorConstraint`).
  - Created `GeocodingService` integrating via `RestClient` with **BrasilAPI CEP v2** (with fallback to **OpenStreetMap Nominatim**) for automated address resolution and latitude/longitude coordinates on the back-end.
  - Updated `RestaurantService`, `UserService`, and `AuthController` to resolve addresses and geolocation coordinates.
- **User Management (`service/UserService`, `dto/request/UserPatchRequestDTO`):**
  - Implemented `UserService` with constructor injection, Spring Cache integration (`@Cacheable` and `@CacheEvict` on `users`), and methods `create`, `findAll`, `findById`, `update`, `patch`, and `toggleActiveStatus` with uniqueness validation for email, phone, and CPF.
- **OpenAPI / Swagger Documentation (`config/OpenApiConfig`, `pom.xml`, `application.yml`):**
  - Added `springdoc-openapi-starter-webmvc-ui` (`2.8.5`) dependency.
  - Created `OpenApiConfig` defining API metadata (`FoodRush API (Delivery Engine)`) and HTTP Bearer JWT security scheme (`bearerAuth`).
  - Documented endpoints with `@Tag`, `@Operation`, and `@ApiResponses` (accessible at `/api/v1/swagger-ui/index.html` and `/api/v1/docs`).
  - Permitted Swagger/OpenAPI routes in `SecurityConfig`.
- **Restaurant Management & Caching (`controller/`, `service/`, `dto/`):**
  - Created `RestaurantController` with endpoints for creation (`POST`), listing (`GET`), retrieval by ID (`GET /{id}`), updating (`PUT /{id}`), and operational toggle (`PATCH /{id}/toggle-open`).
  - Created `RestaurantService` with business rules, CNPJ/phone uniqueness validation, owner verification, and cache management (`@Cacheable` and `@CacheEvict` on `restaurants`).
  - Enabled caching with `@EnableCaching` on `ApiApplication` and added `spring-boot-starter-cache`.
  - Added DTOs: `RestaurantRequestDTO`, `RestaurantPatchRequestDTO`, and `RestaurantResponseDTO`.
  - Added `phone` column to `Restaurant` entity and database schema, and `existsByPhone` to `RestaurantRepository`.
- **Custom Bean Validation Constraints (`validation/`):**
  - Implemented custom constraint validators for checksum digits and format validation: `@ValidCPF` (`CpfValidatorConstraint`), `@ValidCNPJ` (`CnpjValidatorConstraint`), `@ValidPhone` (`PhoneValidatorConstraint`), and `@ValidCEP` (`CepValidatorConstraint`).
- **JWT Authentication & Security (`config/`, `security/`, `controller/`):**
  - Added JJWT `0.12.6` dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
  - Implemented `JwtTokenProvider` for generating and verifying `HS512` signed tokens (`accessToken` and `refreshToken`).
  - Implemented `JwtAuthenticationFilter` and `UserDetailsServiceImpl` for stateless database-backed authentication.
  - Created `SecurityConfig` with `BCryptPasswordEncoder` and endpoint authorization rules.
  - Created `AuthController` with endpoints for registration (`POST /register`), authentication (`POST /login`), and token refresh (`POST /refresh`).
- **Authentication & User DTOs (`dto/`):**
  - Created immutable records with Jakarta Validation: `UserRequestDTO`, `LoginRequestDTO`, `RefreshTokenRequestDTO`, `AuthResponseDTO`, and `UserResponseDTO`.
- **Centralized Exception Handling (`exception/`):**
  - Created `BusinessException` for business rule violations.
  - Implemented `GlobalExceptionHandler` (`@RestControllerAdvice`) returning **RFC 7807 (`ProblemDetail`)** for business errors (`422`), field validation errors (`400`), and unexpected internal errors (`500`).
- **Domain, Persistence, and Migrations (`enums/`, `model/`, `repository/`, `db/migration/`):**
  - Created Flyway migration `V1__init_schema.sql` with tables: `addresses`, `users`, `restaurants`, `menu_items`, `menu_item_sizes`, `complement_groups`, `complement_items`, `orders`, `order_items`, and `order_item_complements`, along with performance indexes.
  - Added enums: `RoleType` (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_DRIVER`, `ADMIN`) and `OrderStatus` (`CREATED`, `PAID`, `PREPARING`, `READY`, `IN_ROUTE`, `DELIVERED`, `CANCELED`).
  - Implemented JPA entities: `Address`, `User`, `Restaurant`, `MenuItem`, `MenuItemSize` (with `@Version` for optimistic concurrency), `ComplementGroup`, `ComplementItem`, `Order` (with `idempotencyKey`), `OrderItem`, and `OrderItemComplement`.
  - Implemented Spring Data JPA repositories: `AddressRepository`, `UserRepository`, `RestaurantRepository`, `MenuItemRepository`, `MenuItemSizeRepository` (with `PESSIMISTIC_WRITE` locking), `ComplementGroupRepository`, `ComplementItemRepository`, and `OrderRepository`.
- **Infrastructure & Project Documentation:**
  - Added `docker-compose.yml` with healthchecks for PostgreSQL 17 (`foodrush_db`), Redis 7, RabbitMQ 4 Management, Prometheus, and Grafana.
  - Added English documentation in `README.md`, `CHANGELOG.md`, and `AGENTS.md`.

### Fixed
- Fixed and updated `AuthControllerTest` to cover registration with address/CEP geocoding, business exceptions, credentials authentication, and token renewal.
- Fixed `@ManyToOne` relationship mapping for restaurant owner (`User owner`), removing duplicate column mapping on `owner_id`.
- Resolved `java.lang.VerifyError: Constructor must call super() or this() before return` with Lombok and ECJ/Java 25 by replacing `@Data` with `@Getter` and `@Setter`.
- Configured `maven.compiler.source` and `maven.compiler.target` to avoid compilation failures on systems without `ct.sym`.
- Optimized `application.yml` with environment variable interpolation, `spring.jpa.open-in-view: false`, and removed deprecated `hibernate.dialect`.

## [0.0.1-SNAPSHOT] - 2026-10-16
- Initial release.
