# Subscription Tracker

A full-stack application for tracking personal subscriptions with multi-currency support, automatic payment record generation, and spending analytics.

## Features

- **Subscription Management**: Track Netflix, Spotify, gym memberships, and any recurring services
- **Multi-Currency Support**: Handle subscriptions in USD, EUR, GBP, PLN with automatic FX rate updates
- **Payment History**: Automatic payment record generation with manual entry support
- **Dashboard & Statistics**: Visual spending analytics with charts and projections
- **Service Catalog**: Manage your personal catalog of subscription services
- **Managed Categories**: Admin-managed category system for organizing services (replaces free-text categories)
- **Two-Factor Authentication**: Optional TOTP-based 2FA with authenticator apps (Google Authenticator, Authy, etc.) and recovery codes
- **Email Verification**: Optional email verification for new accounts with a 7-day grace period (toggle via `EMAIL_VERIFICATION_ENABLED`)

## Tech Stack

| Backend | Frontend |
|---------|----------|
| Java 17, Spring Boot 3.2 | Vue.js 3, TypeScript |
| Spring Security + JWT | Pinia, Vue Router |
| PostgreSQL 15, Flyway | TailwindCSS, Chart.js |

## Quick Start

### Prerequisites
- Docker 20.10+
- (Optional) FX Rate API key from [exchangerate-api.com](https://www.exchangerate-api.com/)

### Run with Docker Compose (Development)

```bash
# Configure environment
cp .env.dev.example .env.dev
# Edit .env.dev with your secrets

# Start development container
docker compose -f docker-compose.dev.yml up -d

# Access at http://localhost:8889
```

### Environment Variables

The backend uses a single `application.yml` with environment variable overrides (no Spring profiles).

**Required:**

| Variable | Description |
|----------|-------------|
| `POSTGRES_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (min 32 chars) |

**Optional:**

| Variable | Default | Description |
|----------|---------|-------------|
| `FX_RATE_API_KEY` | *(empty)* | Exchange rate API key |
| `CORS_ALLOWED_ORIGINS` | `localhost:3000,5173` | Allowed CORS origins |
| `SWAGGER_ENABLED` | `true` | Set `false` in production |
| `LOG_LEVEL_APP` | `INFO` | App logging level |
| `DEVTOOLS_ENABLED` | `false` | Spring DevTools (hot reload) |
| `TOTP_ENCRYPTION_KEY` | *(dev default)* | 2FA secret encryption key (32+ chars, required in production) |
| `EMAIL_VERIFICATION_ENABLED` | `false` | Set `true` to require email verification for new registrations |
| `JWT_REFRESH_TOKEN_PEPPER` | *(required)* | Server-side pepper used when hashing refresh tokens |

See [DEPLOYMENT.md](DEPLOYMENT.md) for full configuration reference.

## Development

### Project Structure

```
subscription-tracker/
├── backend/                 # Spring Boot API
│   ├── src/main/java/       # Application code
│   └── src/test/            # JUnit tests
├── frontend/                # Vue.js SPA
│   ├── src/
│   │   ├── components/      # Vue components
│   │   ├── composables/     # Vue composables
│   │   ├── stores/          # Pinia stores
│   │   ├── services/        # API clients
│   │   ├── utils/           # Utilities
│   │   └── views/           # Page components
│   └── coverage/            # Test coverage reports
├── e2e/                     # Playwright E2E tests
├── scripts/
│   ├── verify.sh            # Test & verify (Linux/macOS)
│   └── verify.ps1           # Test & verify (Windows)
├── docker-compose.dev.yml   # Development container
└── docker-compose.verify.yml # Verification/testing container
```

### Running Tests

**Full verification (tests + coverage + Docker health check):**

```bash
./scripts/verify.sh      # Linux/macOS
.\scripts\verify.ps1     # Windows
```

This runs:
1. Backend tests with JaCoCo coverage
2. Frontend tests with v8 coverage
3. Docker container build and health check

**Individual test commands:**

```bash
# Backend
cd backend && ./gradlew test

# Frontend
cd frontend && npm run test:run

# Frontend with coverage
cd frontend && npm run test:coverage

# E2E (requires app running)
cd e2e && npx playwright test
```

### Coverage Reports

After running `verify.sh`, coverage reports are available at:

| Report | Location |
|--------|----------|
| Backend (JaCoCo) | `backend/build/reports/jacoco/test/html/index.html` |
| Frontend (v8) | `frontend/coverage/index.html` |

## API Documentation

- **Swagger UI**: `http://localhost/api/v1/swagger-ui.html` (disabled in production by default)
- **OpenAPI Schema**: `http://localhost/api/v1/v3/api-docs` (JSON) or `backend/docs/api-schema.json`

## Scheduled Jobs

- **Payment Generator** (Daily 1:00 AM): Creates payment records for due subscriptions
- **FX Rate Refresh** (Daily midnight): Updates exchange rates

## User Roles

The application supports two user roles:

| Role | Capabilities |
|------|--------------|
| **USER** | Manage personal subscriptions, services, and view statistics |
| **ADMIN** | All USER capabilities + manage system categories, view job run history, manually trigger FX rate refresh |

All new users are created with the `USER` role by default.

### Setting Up an Admin User

Admin users must be promoted manually via the database. There is no API endpoint for role escalation for security reasons.

```sql
-- Connect to your PostgreSQL database and run:
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

**Using Docker Compose:**

```bash
docker compose exec db psql -U postgres -d subscriptions_tracker -c \
  "UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';"
```

After promotion, the user must log out and log back in for the role change to take effect.
