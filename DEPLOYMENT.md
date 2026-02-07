# Deployment Guide

## Local Development

Uses an all-in-one Docker container with PostgreSQL, backend, and frontend bundled together.

```bash
# Configure environment
cp .env.dev.example .env.dev
# Edit .env.dev with your secrets

# Build and run
docker compose -f docker-compose.dev.yml up -d

# Access the app
# Frontend: http://localhost:8889
# API: http://localhost:8889/api/v1
```

### Running Tests

```bash
./scripts/verify.sh        # Linux/Mac
./scripts/verify.ps1       # Windows
```

---

## Railway Deployment

The application is deployed as three services on Railway:
- **PostgreSQL** - Managed database
- **Backend** - Spring Boot API (Railpack builder)
- **Frontend** - Vue.js static site (Railpack builder)

### Architecture

```
                         Railway Project
┌─────────────────────────────────────────────────────────┐
│                                                         │
│   PostgreSQL ◄──private──► Backend ◄──public──► Users  │
│   (no domain)              (port 8080)                  │
│                                                         │
│                            Frontend ◄──public──► Users  │
│                            (port 80)                    │
└─────────────────────────────────────────────────────────┘
```

- Database: No public domain (only backend connects to it internally)
- Backend: Needs public domain (browser calls API directly)
- Frontend: Needs public domain (serves static files to users)

---

## Setup Guide

### Step 1: Create Project

1. Go to [railway.app](https://railway.app) → **New Project**
2. Name it (e.g., `subscriptions-tracker-staging` or `subscriptions-tracker-prod`)

### Step 2: Add PostgreSQL

1. Click **+ New** → **Database** → **PostgreSQL**
2. Railway auto-generates credentials (visible in Variables tab)

### Step 3: Add Backend

1. **+ New** → **GitHub Repo** → Select your repo
2. **Settings → General**:
   - Root Directory: `/backend`
   - Watch Paths: `/backend/**`
3. **Settings → Deploy**:
   - Branch: `staging` or `master` (depending on environment)
4. **Settings → Networking**:
   - Click **Generate Domain**
   - Enter port: `8080`
5. **Variables** → Add all backend variables (see table below)

### Step 4: Add Frontend

1. **+ New** → **GitHub Repo** → Select your repo
2. **Settings → General**:
   - Root Directory: `/frontend`
   - Watch Paths: `/frontend/**`
3. **Settings → Deploy**:
   - Branch: `staging` or `master` (depending on environment)
4. **Settings → Networking**:
   - Click **Generate Domain**
   - Enter port: `80`
5. **Variables** → Add frontend variables (see table below)

### Step 5: Update Backend CORS

After frontend domain is generated, update backend's `CORS_ALLOWED_ORIGINS` with the frontend URL.

### Step 6: Verify

```bash
# Check backend health
curl https://your-backend.up.railway.app/api/v1/actuator/health

# Open frontend in browser
open https://your-frontend.up.railway.app
```

---

## Environment Variables

### Backend Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `SPRING_DATASOURCE_USERNAME` | Yes | `${{Postgres.PGUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRET` | Yes | Secret for JWT signing (generate with `openssl rand -base64 32`) |
| `RAILPACK_JDK_VERSION` | Yes | `17` (project requires Java 17) |
| `CORS_ALLOWED_ORIGINS` | Yes | Frontend URL (e.g., `https://your-frontend.up.railway.app`) |
| `SWAGGER_ENABLED` | No | Enable/disable Swagger UI (default: `true`) |
| `FX_RATE_API_KEY` | No | API key for exchange rates |

**Note:** Use `${{Postgres.VARIABLE}}` syntax to reference the PostgreSQL service variables. Additional debug variables (logging levels, JPA settings) are available in `application.yml` but rarely need overriding.

### Frontend Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_API_BASE_URL` | Yes | Backend URL (e.g., `https://your-backend.up.railway.app`) |
| `PORT` | Yes | `80` (Caddy static server port) |

---

## Configuration Files

Both services use `railway.json` for build configuration:

| File | Purpose |
|------|---------|
| `backend/railway.json` | Gradle build, health check on `/api/v1/actuator/health`, restart policy |
| `frontend/railway.json` | Static site build with `npm install && npm run build` |

---

## Environments

Use **separate Railway projects** for staging and production to keep them fully isolated.

Staging deploys from the `staging` branch and is used for testing before production. You can enable Swagger UI and use test API keys. Railway-generated subdomains work fine for staging.

Production deploys from the `master` branch. Disable Swagger UI for security. Use unique secrets that differ from staging. Configure custom domains for a professional appearance.

### Branching Strategy

```
feature/* ──PR──► staging ──PR──► master
                    │               │
                    ▼               ▼
              Railway Staging   Railway Prod
```

---

## Custom Domains (Optional)

In Railway dashboard → Service → Settings → Networking → Custom Domain:

| Service | Staging | Production |
|---------|---------|------------|
| Frontend | `staging.yourdomain.com` | `yourdomain.com` |
| Backend | `api-staging.yourdomain.com` | `api.yourdomain.com` |

Remember to update `CORS_ALLOWED_ORIGINS` and `VITE_API_BASE_URL` when using custom domains.
