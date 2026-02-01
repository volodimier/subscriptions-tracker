# Subscription Tracker

A full-stack application for tracking personal subscriptions with multi-currency support, automatic payment record generation, and spending analytics.

## Features

- **Subscription Management**: Track Netflix, Spotify, gym memberships, and any recurring services
- **Multi-Currency Support**: Handle subscriptions in USD, EUR, GBP, PLN with automatic FX rate updates
- **Payment History**: Automatic payment record generation with manual entry support
- **Dashboard & Statistics**: Visual spending analytics with charts and projections
- **Service Catalog**: Manage your personal catalog of subscription services

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

### Run with Docker Compose

```bash
# Configure environment
cp .env.example .env
# Edit .env with your POSTGRES_PASSWORD and JWT_SECRET

# Start application
docker compose up -d

# Access at http://localhost
```

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | JWT signing secret (min 32 chars) |
| `FX_RATE_API_KEY` | No | Exchange rate API key |

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
└── docker-compose.yml
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

- **Swagger UI**: `http://localhost/api/v1/swagger-ui.html`
- **OpenAPI Schema**: `http://localhost/api/v1/v3/api-docs` (JSON) or `backend/docs/api-schema.json`

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login
- `GET /api/v1/auth/me` - Get current user

### Subscriptions
- `GET /api/v1/subscriptions` - List subscriptions
- `POST /api/v1/subscriptions` - Create subscription
- `GET /api/v1/subscriptions/{id}` - Get details
- `PUT /api/v1/subscriptions/{id}` - Update
- `PATCH /api/v1/subscriptions/{id}/cancel` - Cancel
- `PATCH /api/v1/subscriptions/{id}/reactivate` - Reactivate

### Services
- `GET /api/v1/services` - List services
- `POST /api/v1/services` - Create service
- `PUT /api/v1/services/{id}` - Update
- `DELETE /api/v1/services/{id}` - Delete

### Payments
- `GET /api/v1/subscriptions/{id}/payments` - List payments
- `POST /api/v1/payments` - Create payment
- `PUT /api/v1/payments/{id}` - Update
- `DELETE /api/v1/payments/{id}` - Delete

### Dashboard
- `GET /api/v1/dashboard/summary` - Spending summary
- `GET /api/v1/dashboard/projection` - Annual projection

## Scheduled Jobs

- **Payment Generator** (Daily 1:00 AM): Creates payment records for due subscriptions
- **FX Rate Refresh** (Daily midnight): Updates exchange rates
