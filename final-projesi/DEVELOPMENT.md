# Development

## Prerequisites

- Docker + Docker Compose
- (Optional, for local non-Docker dev) Java 21, Node 22+, Maven 3.9+

## Quick Start

```bash
# 1. Configure secrets
cp .env.example .env
# Edit .env — set IYZIPAY_API_KEY and IYZIPAY_SECRET_KEY (sandbox keys from iyzipay)

# 2. Boot the stack
docker compose up -d --build

# 3. Wait for services to register with Eureka (~30–60s)
open http://localhost:8761

# 4. Open the storefront
open http://localhost:8080

# 5. Default Merchant
username: odin | password: gungnir

# 6. Default Customer
username: thor | password: mjolnir
```

That's it. Everything (Postgres, Redis, RabbitMQ, Keycloak, all microservices, and the web frontend) runs inside Docker Compose. The only external dependency you must configure is iyzipay sandbox credentials.

## Endpoints

| URL                                   | What                                                               |
| ------------------------------------- | ------------------------------------------------------------------ |
| http://localhost:8080                 | Storefront (gateway routes `/` → client-web, `/api/**` → services) |
| http://localhost:8761                 | Eureka dashboard                                                   |
| http://localhost:15672                | RabbitMQ management UI (guest/guest)                               |
| http://localhost:8090                 | Keycloak admin (admin/admin)                                       |
| http://localhost:8081/swagger-ui.html | Product                                                            |
| http://localhost:8082/swagger-ui.html | Cart                                                               |
| http://localhost:8084/swagger-ui.html | Order                                                              |
| http://localhost:8085/swagger-ui.html | User                                                               |
| http://localhost:8086/swagger-ui.html | Payment                                                            |

## Default Credentials

| System   | User       | Password   |
| -------- | ---------- | ---------- |
| Keycloak | `admin`    | `admin`    |
| RabbitMQ | `guest`    | `guest`    |
| Postgres | `postgres` | `postgres` |

Sign up a storefront user via the UI (`/login?mode=signup`) — it provisions both Keycloak and the local user record.

## Common Commands

```bash
# Rebuild and restart a single service after code change
docker compose build product && docker compose up -d product

# Tail logs
docker compose logs -f order payment shipping

# Stop everything
docker compose down

# Wipe volumes (Postgres data, product images, Keycloak realm state)
docker compose down -v

# Run all backend tests
make test

# Run tests for one service
make test-product
```
