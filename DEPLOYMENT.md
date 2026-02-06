# Deployment Guide

## Local Development

Uses an all-in-one Docker container with PostgreSQL, backend, and frontend bundled together.

### Why Dev Container

- **Consistent environment**: No JDK/Node version mismatches
- **Fast onboarding**: Clone repo → run container → done
- **No local dependencies**: Everything runs in Docker
- **Hot reload**: Code changes reflect immediately

### Running Locally

```bash
# Configure environment
cp .env.dev.example .env.dev
# Edit .env.dev with your secrets

# Build and run the dev container
docker compose -f docker-compose.dev.yml up -d

# Access the app
# Frontend: http://localhost:8889
# API: http://localhost:8889/api/v1
```

### Running Tests

```bash
# Run full verification (backend + frontend tests, container health check)
./scripts/verify.sh        # Linux/Mac
./scripts/verify.ps1       # Windows
```

Tests run in containers to ensure consistent results across machines.

### Architecture Difference

Local dev uses a single container for simplicity. Production uses separate services. This is intentional — staging environment catches any integration issues before production.

---

## Production (Railway)

## Overview

The application is deployed as two separate services on Railway:
- **Backend**: Spring Boot API (Nixpacks buildpack)
- **Frontend**: Vue.js static site
- **Database**: Railway managed PostgreSQL

## Database

Use Railway's managed PostgreSQL:
1. Add PostgreSQL plugin to your Railway project
2. Link it to the backend service
3. Railway auto-injects connection variables

## Backend

### Setup

1. Create new Railway service
2. Connect your GitHub repo
3. Set root directory: `/backend`
4. Add environment variables (see below)
5. Deploy

Railway auto-detects Gradle + Spring Boot via Nixpacks - no Dockerfile needed.

### Environment Variables

The backend uses a single `application.yml` with environment variable overrides. No Spring profiles are used.

**Required:**

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET` | Secret for JWT signing (min 32 chars) |

**Optional:**

| Variable | Default | Description |
|----------|---------|-------------|
| `FX_RATE_API_KEY` | *(empty)* | API key for exchange rates |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed origins |
| `SWAGGER_ENABLED` | `true` | Set to `false` to disable Swagger UI in production |
| `LOG_LEVEL_APP` | `INFO` | Logging level for application code |
| `LOG_LEVEL_ROOT` | `WARN` | Root logging level |
| `DEVTOOLS_ENABLED` | `false` | Spring DevTools (hot reload) |
| `JAVA_DEBUG` | `false` | Enable remote debug port 5005 |

**Production recommendation:** Set `SWAGGER_ENABLED=false` to hide API documentation.

### Health Check

The `/actuator/health` endpoint is available for Railway health monitoring.

## Frontend

### Setup

1. Create new Railway service in same project
2. Connect your GitHub repo
3. Set root directory: `/frontend`
4. Configure build settings (see below)
5. Add environment variables
6. Deploy

### Build Settings

| Setting | Value |
|---------|-------|
| Build command | `npm run build` |
| Output directory | `dist` |

### Environment Variables

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | Backend API URL (e.g., `https://api.yourdomain.com`) |

## Custom Domain

Configure custom domains in Railway dashboard for both services:
- Frontend: `yourdomain.com`
- Backend: `api.yourdomain.com`

Update backend CORS configuration to allow your frontend domain.

## Staging Environment

Use a **separate Railway project** for staging to keep it fully isolated from production.

### Setup

1. Create new Railway project (e.g., `subscriptions-tracker-staging`)
2. Follow same setup steps as production for database, backend, and frontend
3. Configure to auto-deploy from a staging branch or pull requests

### Differences from Production

| Aspect | Staging | Production |
|--------|---------|------------|
| Railway project | Separate | Separate |
| Database | Own PostgreSQL instance | Own PostgreSQL instance |
| Domain | `staging.yourdomain.com` | `yourdomain.com` |
| API domain | `api-staging.yourdomain.com` | `api.yourdomain.com` |
| Branch | `staging` or PR deploys | `main` / `master` |
| Resources | Smaller (cost saving) | Scaled for traffic |
| API keys | Test keys if available | Production keys |

### Deployment Flow

```
Feature branch → PR → Staging (auto-deploy) → Verify → Merge to main → Production
```

### Environment Variables

Same variables as production, but with staging-specific values:
- Different `JWT_SECRET`
- Test API keys for `FX_RATE_API_KEY` if available
- `SWAGGER_ENABLED=true` (optional, to allow API testing)
- `VITE_API_BASE_URL=https://api-staging.yourdomain.com`

## Notes

- Railway handles build caching automatically for both services
- To switch backend to Dockerfile later, add `Dockerfile` to `/backend` folder
