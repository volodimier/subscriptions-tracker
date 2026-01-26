# ============================================================
# Subscription Tracker - All-in-One Docker Container
# Contains: PostgreSQL 15, Spring Boot Backend, Vue.js Frontend
# ============================================================

# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend
FROM gradle:8-jdk17 AS backend-build
WORKDIR /app/backend
COPY backend/build.gradle backend/settings.gradle ./
COPY backend/src ./src
RUN gradle clean build -x test --no-daemon && \
    mv build/libs/*-SNAPSHOT.jar build/libs/app.jar

# Stage 3: Final Runtime Image
FROM ubuntu:22.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Add PostgreSQL apt repository and install dependencies
RUN apt-get update && apt-get install -y \
    gnupg2 \
    lsb-release \
    curl \
    && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /usr/share/keyrings/postgresql-keyring.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/postgresql-keyring.gpg] http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    postgresql-15 \
    nginx \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Create app directories
RUN mkdir -p /app /var/log/supervisor /run/postgresql /var/lib/postgresql/data

# Set up PostgreSQL
RUN chown -R postgres:postgres /var/lib/postgresql /run/postgresql
USER postgres
RUN /usr/lib/postgresql/15/bin/initdb -D /var/lib/postgresql/data
USER root

# Copy built frontend to nginx
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html

# Copy built backend JAR
COPY --from=backend-build /app/backend/build/libs/app.jar /app/app.jar

# Copy configuration files
COPY docker/nginx.conf /etc/nginx/sites-available/default
COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY docker/init-db.sh /app/init-db.sh
COPY docker/start.sh /app/start.sh
COPY docker/run-backend.sh /app/run-backend.sh

# Make scripts executable
RUN chmod +x /app/init-db.sh /app/start.sh /app/run-backend.sh

# Environment variables with defaults (secrets must be provided at runtime)
ENV POSTGRES_DB=subscription_tracker \
    POSTGRES_USER=subscription_user \
    SPRING_PROFILES_ACTIVE=docker

# Expose ports
# 80 - nginx (frontend + API proxy)
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost/api/v1/auth/me 2>/dev/null || curl -f http://localhost/ || exit 1

# Start all services via supervisor
CMD ["/app/start.sh"]
