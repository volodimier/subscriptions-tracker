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
| `CORS_ALLOWED_ORIGINS` | Yes | Frontend URL - must include `https://` (e.g., `https://your-frontend.up.railway.app`) |
| `SWAGGER_ENABLED` | No | Enable/disable Swagger UI (default: `true`) |
| `REGISTRATION_ENABLED` | No | Enable/disable user registration (default: `true`). Set to `false` to prevent new user registrations. |
| `FX_RATE_API_KEY` | No | API key for exchange rates |
| `TOTP_ENCRYPTION_KEY` | **Yes (prod)** | Encryption key for 2FA secrets. Generate with `openssl rand -base64 32`. Must be at least 32 characters. |
| `TOTP_ISSUER` | No | Name shown in authenticator apps (default: `Subscription Tracker`). Use different values for staging/prod. |
| `RESEND_API_KEY` | **Yes (prod)** | API key from [resend.com](https://resend.com) for sending verification emails. |
| `EMAIL_FROM_ADDRESS` | No | Sender email address (default: `noreply@pennywise.app`). Must be a verified domain in Resend. |
| `EMAIL_FROM_NAME` | No | Sender display name (default: `PennyWise`). |
| `EMAIL_VERIFICATION_BASE_URL` | **Yes (prod)** | Frontend URL for verification links (e.g., `https://your-frontend.up.railway.app`). |

**Note:** Use `${{Postgres.VARIABLE}}` syntax to reference the PostgreSQL service variables. Additional debug variables (logging levels, JPA settings) are available in `application.yml` but rarely need overriding.

### Frontend Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_API_BASE_URL` | Yes | Backend URL - must include `https://` and `/api/v1` (e.g., `https://your-backend.up.railway.app/api/v1`) |
| `PORT` | Yes | `80` (Caddy static server port) |

---

## Configuration Files

Both services use `railway.json` for build configuration:

| File | Purpose |
|------|---------|
| `backend/railway.json` | Gradle build, health check on `/api/v1/actuator/health`, restart policy |
| `frontend/railway.json` | Static site build with `npm install && npm run build` |

---

## JVM Memory Tuning

The backend uses optimized JVM flags to minimize RAM usage on Railway's resource-constrained environment:

```
-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=40m -XX:MaxDirectMemorySize=16m -XX:TieredStopAtLevel=1
```

| Flag | Purpose |
|------|---------|
| `-Xmx256m -Xms128m` | Heap memory bounds (max 256MB, initial 128MB) |
| `-XX:+UseSerialGC` | Single-threaded GC with lower memory overhead |
| `-XX:MaxMetaspaceSize=128m` | Cap class metadata memory (Spring Boot needs ~100-120MB) |
| `-XX:ReservedCodeCacheSize=40m` | Cap JIT compiled code cache |
| `-XX:MaxDirectMemorySize=16m` | Cap off-heap NIO buffers |
| `-XX:TieredStopAtLevel=1` | Simpler JIT compilation (less memory, slightly slower peak performance) |

Additionally, Spring's lazy initialization is enabled in `application.yml` to defer bean creation until first use, reducing initial memory footprint.

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

---

## Two-Factor Authentication (2FA)

The application supports optional TOTP-based two-factor authentication using authenticator apps like Google Authenticator, Authy, or 1Password.

### Configuration

Set the `TOTP_ENCRYPTION_KEY` environment variable in production. This key is used to encrypt 2FA secrets at rest using AES-256-GCM.

```bash
# Generate a secure key
openssl rand -base64 32
```

**Important:** Use a different key for each environment (staging vs production). If you lose this key, users will need to reset and re-enable 2FA.

### Features

- **Setup**: Users scan a QR code with their authenticator app
- **Recovery Codes**: 10 single-use backup codes provided during setup
- **Rate Limiting**: 5 TOTP attempts per 30 seconds to prevent brute force

### Account Recovery

If a user loses access to their authenticator app, they can use one of their 10 recovery codes to log in. Users should store recovery codes securely during initial 2FA setup.

---

## Email Verification

New user registrations require email verification. The application uses [Resend](https://resend.com) as the email provider.

### Behavior

- **Grace Period**: New users have 7 days of full access before email verification is required
- **After Grace Period**: Login is blocked until email is verified
- **Account Cleanup**: Unverified accounts are automatically deleted after the grace period expires (daily at 2 AM UTC)
- **Token Expiration**: Verification links expire after 48 hours
- **Rate Limiting**: Users can resend verification emails once every 2 minutes

### Setup

1. Create a free account at [resend.com](https://resend.com)
2. Add and verify your sending domain
3. Generate an API key
4. Set the required environment variables:

```bash
RESEND_API_KEY=re_xxxxxxxxxxxx
EMAIL_VERIFICATION_BASE_URL=https://your-frontend-domain.com
```

### Settings Page Warning

Unverified users see a warning on the Settings page with the exact date their account will be deleted if not verified. This helps ensure users complete verification before losing access.
