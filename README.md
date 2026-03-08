<div align="center">

# 💎 GemStore Backend

### AI-Powered Gemstone Marketplace Platform

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-4169E1?style=flat-square&logo=postgresql)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-231F20?style=flat-square&logo=apachekafka)

Backend service for **GemStore** — a marketplace platform for buying and selling gemstones with **machine learning–based price prediction**, real-time messaging, and WebSocket notifications.

[Related Repos](#-related-repositories) · [Architecture](#-architecture) · [API Docs](#-core-api-endpoints) · [Getting Started](#-getting-started)

</div>

---

## 📦 Related Repositories

| Repository | Description |
|---|---|
| [gemstore-web](https://github.com/mnmmusharraf/gemstore-web) | React user-facing application |
| [gemstore-admin](https://github.com/mnmmusharraf/gemstore-admin) | React admin dashboard |
| [gemstore-ml-service](https://github.com/mnmmusharraf/gemstore-ml-service) | FastAPI ML price prediction service |

---

## 🏗 Architecture

```
                        +----------------------+
                        |   React Frontend     |
                        |   (gemstore-web)     |
                        +----------+-----------+
                                   |
                               REST API
                                   |
                                   v
                    +-------------------------------+
                    |      Spring Boot Backend      |
                    |          (This Repo)          |
                    |-------------------------------|
                    | Auth · Listings · Messaging   |
                    | Notifications · Prediction    |
                    +------+------------+-----------+
                           |            |
               REST API    |            |  Kafka Events
                           v            v
              +----------------+   +------------------+
              |  FastAPI ML    |   |   Apache Kafka   |
              |  Price Model   |   |   (Messaging)    |
              +-------+--------+   +--------+---------+
                      |                     |
                      v                     v
               +-------------+      +--------------+
               |  PostgreSQL |      |  WebSocket   |
               |  Database   |      | Notifications|
               +-------------+      +--------------+
```

---

## ✨ Features

### User & Auth
- Registration, login, and JWT authentication
- Google OAuth2 social login
- Role-based access control

### Marketplace
- Create, search, and manage gemstone listings
- Image uploads per listing
- Like, favorite, and follow system
- Listing view tracking and price history
- Advanced search filters

### Messaging
- Kafka-powered messaging pipeline
- Message delivery status tracking
- Real-time typing events

### Notifications
- WebSocket-based real-time notification delivery
- Events: new messages, followers, listing interactions

### AI Price Prediction
Integration with an external FastAPI ML service to estimate gemstone prices.

```
User enters gem details
        │
        ▼
Spring Boot API
        │
        ▼
FastAPI ML Service
        │
        ▼
Predicted price returned to user
```

### Admin
- Report moderation tools
- Reported listing management
- User moderation

---

## 🛠 Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5, Spring Security |
| Authentication | JWT, OAuth2 (Google) |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Migrations | Liquibase |
| Messaging | Apache Kafka, Spring Kafka |
| Real-time | Spring WebSocket |
| HTTP Client | Spring WebClient |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI / Swagger |
| Testing | JUnit, MockMvc, MockWebServer, H2, Spring Security Test, Kafka Test |

---

## 📁 Project Structure

```
src/main/java/com/gemstore/backend/
├── config/
│   ├── SecurityConfig
│   ├── JwtFilter
│   ├── KafkaConfig
│   ├── WebSocketAuthConfig
│   └── CorsConfig
├── controllers/
│   ├── auth/
│   ├── listing/
│   ├── user/
│   ├── message/
│   ├── notification/
│   ├── prediction/
│   ├── admin/
│   └── report/
├── services/
│   ├── auth/
│   ├── listing/
│   ├── user/
│   ├── message/
│   ├── notification/
│   ├── prediction/
│   └── report/
├── entities/
│   ├── user/
│   ├── listing/
│   ├── message/
│   ├── notification/
│   └── report/
├── repositories/
├── dtos/
├── mappers/
└── security/
```

---

## 🗄 Database

**Database:** PostgreSQL  
**Migrations:** Managed by Liquibase

Migration files are located in:
```
src/main/resources/db/changelog/
```

Example migration file: `V14__add_listings_table.sql`

---

## 🤖 ML Integration

The backend communicates with an external FastAPI service for gem price prediction.

**Repository:** [gemstore-ml-service](https://github.com/mnmmusharraf/gemstore-ml-service)

**Configuration:**
```properties
gem-price-api.base-url=http://localhost:8000
gem-price-api.predict-endpoint=/predict
gem-price-api.health-endpoint=/health
```

**Example prediction endpoint:** `POST /api/v1/gems/price/predict`

---

## 📨 Kafka Messaging

Kafka handles the full messaging pipeline including delivery, typing indicators, and status updates.

**Kafka Topics:**
```
gemstore.messages
gemstore.message-status
gemstore.typing
```

**Configuration:**
```properties
spring.kafka.bootstrap-servers=localhost:9092
```

**Event flow:**
```
User sends message → Controller → Kafka Producer
    → Topic: gemstore.messages
        → Kafka Consumer → PostgreSQL
            → WebSocket notification delivered
```

---

## 🔒 Authentication

### JWT
Standard login flow — returns a signed JWT token.
```
Authorization: Bearer <token>
```

### Google OAuth2
```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

---

## 🖼 File Uploads

```properties
app.upload.dir=uploads
spring.servlet.multipart.max-file-size=5MB
```

---

## 📖 API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Apache Kafka
- Python + uvicorn (for ML service)

---

### 1. Clone the repository

```bash
git clone https://github.com/mnmmusharraf/gemstore-backend
cd gemstore-backend
```

### 2. Configure secrets

Create `src/main/resources/secrets.properties`:

```properties
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

### 3. Set up PostgreSQL

```sql
CREATE DATABASE gemstore;
```

### 4. Start Kafka

Ensure Kafka is running on `localhost:9092`.

### 5. Start the ML service

```bash
cd gemstore-ml-service
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 6. Run the backend

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

---

## 🧪 Running Tests

Tests use H2 in-memory DB, disabled Kafka, and mocked external services.

```bash
mvn test
```

**Test coverage includes:**

| Type | Examples |
|---|---|
| Unit Tests | `GemPriceServiceTest`, `ListingServiceTest`, `UserServiceTest` |
| Integration Tests | `AuthControllerIntegrationTest`, `ListingControllerIntegrationTest`, `GemPriceControllerIntegrationTest` |

---

## 🔗 Core API Endpoints

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and receive JWT |
| POST | `/api/v1/auth/password/reset` | Reset password |

### Users

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/users/{id}` | Get user profile |
| PUT | `/api/v1/users/profile` | Update profile |
| POST | `/api/v1/users/follow/{id}` | Follow a user |

### Listings

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/listings` | Create listing |
| GET | `/api/v1/listings` | Search listings |
| GET | `/api/v1/listings/{id}` | Get listing |
| PUT | `/api/v1/listings/{id}` | Update listing |
| DELETE | `/api/v1/listings/{id}` | Delete listing |
| POST | `/api/v1/listings/{id}/like` | Like a listing |
| POST | `/api/v1/listings/{id}/favorite` | Add to favorites |

### Messaging

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/messages` | Send a message |
| GET | `/api/v1/messages/conversation/{id}` | Get conversation |

### Price Prediction

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/gems/price/predict` | Predict gemstone price |
| GET | `/api/v1/gems/price/health` | ML service health check |

**Example prediction request:**
```json
{
  "gemstoneType": "SAPPHIRE",
  "carat": 1.5,
  "color": "BLUE",
  "clarity": "VVS"
}
```

---

## 🗺 System Design

```
Client Layer
      │
      ▼
Controllers  ──  Validate inputs, return DTO responses
      │
      ▼
Service Layer  ──  Business logic, ML integration, messaging workflows
      │
      ▼
Repository Layer  ──  Spring Data JPA queries
      │
      ▼
Database  ──  PostgreSQL
```

---

## 🔮 Future Improvements

- Redis caching layer
- Elasticsearch for listing search
- Docker + Kubernetes deployment
- CD pipeline (GitHub Actions)
- S3 image storage
- Rate limiting

---
