# Ödev 2 — Refresh Token Mimarisi

Spring Boot ile yazılmış basit bir JWT kimlik doğrulama API'si. Access token ve refresh token mantığını göstermek için yapılmıştır. Küçük bir frontend de içerir.

---

## Proje Yapısı

```
src/main/java/.../odev_2/
├── config/
│   ├── GlobalExceptionHandler.java   # Hata yönetimi
│   └── SecurityConfig.java           # Spring Security ayarları
├── controllers/
│   ├── AuthController.java           # /api/auth/** endpoint'leri
│   └── UserController.java           # /api/users/** endpoint'leri
├── domain/
│   └── User.java                     # Kullanıcı modeli
├── dto/                              # İstek ve cevap nesneleri
├── filter/
│   └── JwtFilter.java                # Her istekte token doğrulama
├── repositories/                     # Kullanıcı veri saklama (in-memory)
└── services/                         # İş mantığı ve JWT işlemleri

src/main/resources/
├── static/
│   ├── index.html                    # Ana sayfa (token zamanlayıcıları)
│   └── login.html                    # Giriş / kayıt sayfası
└── application.properties
```

---

## API Endpoint'leri

| Method | URL                  | Açıklama                 |
| ------ | -------------------- | ------------------------ |
| POST   | `/api/auth/signup`   | Yeni kullanıcı kaydı     |
| POST   | `/api/auth/signin`   | Giriş yap                |
| POST   | `/api/auth/refresh`  | Yeni access token al     |
| POST   | `/api/auth/logout`   | Çıkış yap                |
| GET    | `/api/users/current` | Mevcut kullanıcı bilgisi |

---

## Auth ve Refresh Token Akışı

```
┌──────────┐                        ┌──────────────┐
│  Client  │                        │    Server    │
└────┬─────┘                        └──────┬───────┘
     │                                     │
     │  POST /api/auth/signin              │
     │ ──────────────────────────────────> │
     │                                     │
     │  { accessToken } + refreshToken     │
     │  (cookie, HttpOnly)                 │
     │ <────────────────────────────────── │
     │                                     │
     │  GET /api/users/current             │
     │  Authorization: Bearer <token>      │
     │ ──────────────────────────────────> │
     │                                     │
     │  { username }                       │
     │ <────────────────────────────────── │
     │                                     │
     │  --- Access token süresi doldu ---  │
     │                                     │
     │  POST /api/auth/refresh             │
     │  (cookie otomatik gönderilir)       │
     │ ──────────────────────────────────> │
     │                                     │
     │  { yeni accessToken }               │
     │ <────────────────────────────────── │
     │                                     │
```

**Önemli noktalar:**

- **Access token** — kısa ömürlü, `Authorization` header ile gönderilir
- **Refresh token** — daha uzun ömürlü, `HttpOnly` cookie olarak saklanır, JavaScript erişemez
- Refresh token **döndürülmez** (rotate edilmez), sadece yeni access token üretilir

---

## Gereksinimler

**Java ile çalıştırmak için:**

- Java 21+
- Maven 3.9+

**Docker ile çalıştırmak için:**

- Docker
- Docker Compose

---

## Nasıl Çalıştırılır

### Java ile

1. `.env` dosyasını oluştur:

```bash
cp .env.example .env
```

2. `.env` dosyasını düzenle ve `JWT_SECRET` değerini ayarla:

```env
JWT_SECRET=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seQ==
JWT_EXPIRATION=1
JWT_REFRESH_EXPIRATION=3
BCRYPT_STRENGTH=10
```

3. Uygulamayı başlat:

```bash
./mvnw spring-boot:run
```

4. Tarayıcıda aç: [http://localhost:8080](http://localhost:8080)

---

### Docker ile

1. `.env` dosyasını oluştur (yukarıdaki gibi) ya da değerleri doğrudan `docker-compose.yml` içinde tanımla.

2. Uygulamayı derle ve başlat:

```bash
docker compose up --build -d
```

3. Tarayıcıda aç: [http://localhost:8080](http://localhost:8080)

4. Durdurmak için:

```bash
docker compose down
```
