#!/bin/bash
set -e

echo "============================================"
echo "Subscription Tracker - Starting All Services"
echo "============================================"

# Ensure directories have correct permissions
chown -R postgres:postgres /var/lib/postgresql/data /run/postgresql
mkdir -p /var/log/postgresql
chown postgres:postgres /var/log/postgresql

# Start PostgreSQL temporarily to initialize the database
echo "Starting PostgreSQL for initialization..."
su - postgres -c "/usr/lib/postgresql/15/bin/pg_ctl -D /var/lib/postgresql/data -l /var/log/postgresql/startup.log start"

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until su - postgres -c "pg_isready"; do
    sleep 2
done

# Initialize database
echo "Initializing database..."
su - postgres -c "POSTGRES_DB='$POSTGRES_DB' POSTGRES_USER='$POSTGRES_USER' POSTGRES_PASSWORD='$POSTGRES_PASSWORD' EXPOSE_POSTGRES='$EXPOSE_POSTGRES' bash /app/init-db.sh"

# Stop PostgreSQL (supervisor will start it properly)
echo "Stopping PostgreSQL for supervisor takeover..."
su - postgres -c "/usr/lib/postgresql/15/bin/pg_ctl -D /var/lib/postgresql/data stop"

# Give it a moment to fully stop
sleep 2

echo "Starting all services via supervisor..."
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
