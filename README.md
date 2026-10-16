# SplitTrip — Backend

REST API for **SplitTrip**, an app to split shared expenses among friends (trips, dinners, shared homes) and settle up with the fewest possible payments.

# Collaborators

Juan Esteban Rodriguez

## Tech stack

- Java 21 · Spring Boot 4.1 · Maven
- Spring Web MVC, Spring Data JPA, Bean Validation
- Spring Security with stateless JWT (HS256) and BCrypt
- H2 in-memory database (development)
- Spring Mail for verification and password-reset emails
- springdoc-openapi (Swagger UI)

## Features

- Sign up with email verification, login with optional "remember me", and password reset
- Password policy: 8–72 characters, with at least one uppercase letter, one number and one special character
- Trips with a category (`TRIP`, `DINNER`, `HOME`, `PARTY`); the creator is added as a participant automatically
- Expenses split evenly among selected participants; leftover cents are distributed so shares always add up to the total
- Per-participant balances: amount paid, fair share, net balance
- Suggested settlements using a greedy algorithm (at most n − 1 transfers)
- Each trip is visible only to its owner
- Errors returned as RFC 9457 Problem Details

## Getting started

Requirements: Java 21.

```bash
./mvnw spring-boot:run      # starts on http://localhost:7986
./mvnw test                 # runs the test suite
```

- Swagger UI: http://localhost:7986/swagger-ui.html
- Manual requests: `requests.http`, for the VS Code REST Client extension

> The H2 database lives in memory, so all data is lost on restart.

## Configuration

Main settings are in `src/main/resources/application.yml`:

| Property | Default | Purpose |
|---|---|---|
| `server.port` | `7986` | API port |
| `app.frontend-url` | `http://localhost:5173` | Base URL used in email links |
| `app.cors.allowed-origins` | `http://localhost:5173` | Allowed frontend origins |
| `app.jwt.secret` | dev secret (env `APP_JWT_SECRET`) | JWT signing key, at least 32 characters |
| `app.jwt.expiration` / `remember-me-expiration` | `24h` / `30d` | Token lifetimes |

### Sending real emails (optional)

If `spring.mail.*` is not configured, verification and reset links are **printed in the console** instead of being emailed.

To send real emails, create `src/main/resources/application-local.yml`. This file is git-ignored.

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-app-account@gmail.com
    password: your-app-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
app:
  mail:
    from: your-app-account@gmail.com
```

Then run the app with that profile:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

## API overview

All endpoints are under `/api`. Everything except the auth endpoints requires `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Create an account and send a verification email |
| POST | `/auth/verify-email` | Verify the email with a token |
| POST | `/auth/resend-verification` | Resend the verification link |
| POST | `/auth/login` | Log in and get a JWT |
| POST | `/auth/forgot-password` | Send a password reset link |
| POST | `/auth/reset-password` | Set a new password with a token |
| GET | `/auth/me` | Current user |
| GET / POST | `/trips` | List / create trips |
| GET | `/trips/{id}` | Trip detail |
| POST | `/trips/{id}/participants` | Add a participant |
| GET / POST | `/trips/{id}/expenses` | List / create expenses |
| DELETE | `/trips/{id}/expenses/{expenseId}` | Delete an expense |
| GET | `/trips/{id}/balances` | Balances per participant |
| GET | `/trips/{id}/settlements` | Suggested transfers |

## Project structure

```
src/main/java/com/splittrip/
├── config/       OpenAPI configuration
├── controller/   REST endpoints
├── dto/          Request and response records
├── exception/    API exceptions and global handler
├── model/        JPA entities
├── repository/   Spring Data repositories
├── security/     Security config, JWT, password policy
└── service/      Business logic (SettlementCalculator holds the core math)
```

## Roadmap

- PostgreSQL for persistent data
- Deployment (Docker)
- Trip sharing and invitations
- Uneven splits (percentages or fixed amounts)
- Edit and delete trips and participants

## Related

Frontend: [SplitTrip_Frontend](https://github.com/JuanEstebanRodriguez2021/SplitTrip_Frontend)
