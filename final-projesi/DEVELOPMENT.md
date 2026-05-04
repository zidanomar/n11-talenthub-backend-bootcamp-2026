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

## Test Cards (iyzipay Sandbox)

### Successful Cards

| Card Number          | Bank                     | Card Network   | Card Type              |
| -------------------- | ------------------------ | -------------- | ---------------------- |
| 5890040000000016     | Akbank                   | Mastercard       | Debit Card  |
| 5526080000000006     | Akbank                   | Mastercard       | Credit Card |
| 9792072000017956     | Akbank                   | Troy             | Credit Card |
| 4766620000000001     | Denizbank                | Visa             | Debit Card  |
| 4603450000000000     | Denizbank                | Visa             | Credit Card |
| 9792023757123604     | QNB                      | Troy             | Debit Card  |
| 4987490000000002     | QNB                      | Visa             | Debit Card  |
| 5311570000000005     | QNB                      | Mastercard       | Credit Card |
| 9792020000000001     | QNB                      | Troy             | Debit Card  |
| 9792030000000000     | QNB                      | Troy             | Credit Card |
| 5170410000000004     | Garanti Bank             | Mastercard       | Debit Card  |
| 5400360000000003     | Garanti Bank             | Mastercard       | Credit Card |
| 374427000000003      | Garanti Bank             | American Express | Credit Card |
| 4475050000000003     | Halkbank                 | Visa             | Debit Card  |
| 5528790000000008     | Halkbank                 | Mastercard       | Credit Card |
| 4059030000000009     | HSBC Bank                | Visa             | Debit Card  |
| 5504720000000003     | HSBC Bank                | Mastercard       | Credit Card |
| 5892830000000000     | İşbank                   | Mastercard       | Debit Card  |
| 4543590000000006     | İşbank                   | Visa             | Credit Card |
| 4910050000000006     | Vakıfbank                | Visa             | Debit Card  |
| 4157920000000002     | Vakıfbank                | Visa             | Credit Card |
| 6500528865390837     | Vakıfbank                | Troy             | Debit Card  |
| 6501700194147183     | Vakıfbank                | Troy             | Credit Card |
| 5168880000000002     | Yapı Kredi Bank          | Mastercard       | Debit Card  |
| 5451030000000000     | Yapı Kredi Bank          | Mastercard       | Credit Card |

### Foreign Cards

| Card Number      | Country                |
| ---------------- | ---------------------- |
| 5400010000000004 | Non-Turkish (Credit)   |
| 4054180000000007 | Non-Turkish (Debit)    |

### Error Cards

| Card Number      | Description                                       |
| ---------------- | ------------------------------------------------- |
| 5406670000000009 | Success but cannot be cancelled, refund or post auth |
| 4111111111111129 | Not sufficient funds                              |
| 4129111111111111 | Do not honour                                     |
| 4128111111111112 | Invalid transaction                               |
| 4127111111111113 | Lost card                                         |
| 4126111111111114 | Stolen card                                       |
| 4125111111111115 | Expired card                                      |
| 4124111111111116 | Invalid cvc2                                      |
| 4123111111111117 | Not permitted to card holder                      |
| 4122111111111118 | Not permitted to terminal                         |
| 4121111111111119 | Fraud suspect                                     |
| 4120111111111110 | Pickup card                                       |
| 4130111111111118 | General error                                     |
| 4131111111111117 | Success but mdStatus is 0                         |
| 4141111111111115 | Success but mdStatus is 4                         |
| 4151111111111112 | 3dsecure initialize failed                        |

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
