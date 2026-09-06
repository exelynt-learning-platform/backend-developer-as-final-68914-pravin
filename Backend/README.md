# Resource Booking System

A RESTful API for managing resource reservations with JWT-based authentication and role-based access control.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security + JWT (jjwt 0.12.3)
- PostgreSQL
- JPA / Hibernate
- Swagger / OpenAPI (springdoc 2.3)
- Maven

## Getting Started

### Prerequisites

- **JDK 17** (required — see note below)
- Maven 3.8+
- PostgreSQL 13+ **or** Docker

> ⚠️ **Use JDK 17.** This project uses Lombok, which does not run on newer JDKs (e.g. JDK 24/25) and fails with
> `java.lang.ExceptionInInitializerError` / `com.sun.tools.javac.code.TypeTag :: UNKNOWN`.
> If you build in **IntelliJ**, set the Project SDK to **17** via *File → Project Structure → Project → SDK*,
> and *Settings → Build Tools → Maven → Runner → JRE → Use Project SDK*.

### Database Setup

**Option A — Docker (recommended, no install needed):**

```bash
docker run -d --name booking-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=booking_db \
  -p 5432:5432 \
  -v booking_pgdata:/var/lib/postgresql/data \
  postgres:16-alpine
```

This matches the app's default connection settings out of the box. Stop/start later with
`docker stop booking-postgres` / `docker start booking-postgres`.

**Option B — Existing PostgreSQL install:**

```sql
CREATE DATABASE booking_db;
```

The schema is created automatically on first run (`ddl-auto: update`) — no manual table setup required.

### Environment Variables

| Variable        | Default                         | Description               |
|-----------------|---------------------------------|---------------------------|
| `DB_URL`        | `jdbc:postgresql://localhost:5432/booking_db` | JDBC connection URL |
| `DB_USERNAME`   | `postgres`                      | Database username         |
| `DB_PASSWORD`   | `postgres`                      | Database password         |
| `JWT_SECRET`    | *(see application.yml)*         | Base64-encoded JWT secret |
| `JWT_EXPIRATION`| `86400000`                      | Token expiry in ms (24h)  |
| `PORT`          | `8080`                          | Server port               |

> **Note:** For production, always set `JWT_SECRET` to a strong, randomly generated base64 value (minimum 256 bits).

### Running the Application

```bash
# Clone or extract the project
cd resource-booking

# Build
mvn clean package -DskipTests

# Run
java -jar target/resource-booking-1.0.0.jar

# Or with custom env
DB_URL=jdbc:postgresql://localhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypassword \
JWT_SECRET=myBase64Secret \
java -jar target/resource-booking-1.0.0.jar
```

Or run directly with Maven:

```bash
mvn spring-boot:run
```

### Running Tests

```bash
mvn test
```

Tests use an in-memory H2 database and do not require a running PostgreSQL instance.

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

## Seed Users

The application seeds the following users on first startup:

| Username | Password  | Role  |
|----------|-----------|-------|
| `admin`  | `admin123` | ADMIN |
| `john`   | `user123`  | USER  |
| `jane`   | `user123`  | USER  |

## API Endpoints

### Authentication

| Method | Endpoint         | Auth | Description     |
|--------|-----------------|------|-----------------|
| POST   | `/api/auth/login` | None | Login and get JWT |

**Login request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Login response:**
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN"
}
```

### Resources

| Method | Endpoint              | Auth        | Role        |
|--------|----------------------|-------------|-------------|
| GET    | `/api/resources`      | JWT         | ADMIN, USER |
| GET    | `/api/resources/{id}` | JWT         | ADMIN, USER |
| POST   | `/api/resources`      | JWT         | ADMIN only  |
| PUT    | `/api/resources/{id}` | JWT         | ADMIN only  |
| DELETE | `/api/resources/{id}` | JWT         | ADMIN only  |

### Reservations

| Method | Endpoint                  | Auth | Role                              |
|--------|--------------------------|------|-----------------------------------|
| GET    | `/api/reservations`       | JWT  | ADMIN (all), USER (own only)      |
| GET    | `/api/reservations/{id}`  | JWT  | ADMIN (any), USER (own only)      |
| POST   | `/api/reservations`       | JWT  | ADMIN, USER                       |
| PUT    | `/api/reservations/{id}`  | JWT  | ADMIN (any), USER (own only)      |
| DELETE | `/api/reservations/{id}`  | JWT  | ADMIN only                        |

#### Reservation Query Parameters

| Parameter  | Type             | Description                          |
|------------|-----------------|--------------------------------------|
| `status`   | `PENDING \| CONFIRMED \| CANCELLED` | Filter by status |
| `minPrice` | Decimal          | Minimum price filter                 |
| `maxPrice` | Decimal          | Maximum price filter                 |
| `page`     | Integer (0-based)| Page number (default: 0)             |
| `size`     | Integer          | Page size (default: 10)              |
| `sortBy`   | String           | Field to sort by (default: createdAt)|
| `sortDir`  | `asc \| desc`   | Sort direction (default: desc)       |

#### Create Reservation

USER identity is taken from the JWT token — the `userId` field is not accepted in the request body.

```json
{
  "resourceId": 1,
  "startTime": "2026-10-01T09:00:00",
  "endTime": "2026-10-01T11:00:00",
  "price": 150.00,
  "notes": "Team meeting"
}
```

## Authorization Rules

- **ADMIN**: Full CRUD on resources and reservations. Can view all reservations.
- **USER**: Read-only access to resources. Can create reservations (identity from JWT). Can view and update only their own reservations. Cannot delete reservations.

## Error Responses

All error responses follow this structure:

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": {
    "name": "Resource name is required"
  },
  "timestamp": "2026-09-01T10:00:00"
}
```

| HTTP Status | Scenario                                  |
|-------------|-------------------------------------------|
| 400         | Validation errors, invalid input          |
| 401         | Missing/invalid/expired JWT token         |
| 403         | Insufficient permissions                  |
| 404         | Resource or reservation not found         |
| 500         | Unexpected server error                   |

## Project Structure

```
src/main/java/com/booking/
├── BookingApplication.java
├── DataSeeder.java
├── config/
│   ├── SecurityConfig.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ResourceController.java
│   └── ReservationController.java
├── dto/
│   ├── request/
│   └── response/
├── entity/
│   ├── User.java
│   ├── Resource.java
│   ├── Reservation.java
│   └── enums/
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── service/
└── specification/
    └── ReservationSpecification.java
```
