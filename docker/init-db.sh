#!/bin/bash
set -e

# Wait for PostgreSQL to be ready
until pg_isready -U postgres -h localhost; do
    echo "Waiting for PostgreSQL to start..."
    sleep 2
done

# Check if database exists
if ! psql -U postgres -lqt | cut -d \| -f 1 | grep -qw "$POSTGRES_DB"; then
    echo "Creating database: $POSTGRES_DB"
    psql -U postgres -c "CREATE DATABASE $POSTGRES_DB;"
fi

# Check if user exists
if ! psql -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$POSTGRES_USER'" | grep -q 1; then
    echo "Creating user: $POSTGRES_USER"
    psql -U postgres -c "CREATE USER $POSTGRES_USER WITH PASSWORD '$POSTGRES_PASSWORD';"
fi

# Grant privileges
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO $POSTGRES_USER;"
psql -U postgres -d "$POSTGRES_DB" -c "GRANT ALL ON SCHEMA public TO $POSTGRES_USER;"
psql -U postgres -d "$POSTGRES_DB" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $POSTGRES_USER;"
psql -U postgres -d "$POSTGRES_DB" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $POSTGRES_USER;"

# Configure PostgreSQL for external connections if requested (for dev environment)
if [ "$EXPOSE_POSTGRES" = "true" ]; then
    echo "Configuring PostgreSQL for external connections..."

    # Allow listening on all interfaces
    echo "listen_addresses = '*'" >> /var/lib/postgresql/data/postgresql.conf

    # Allow connections from any host with password authentication
    echo "host    all             all             0.0.0.0/0               md5" >> /var/lib/postgresql/data/pg_hba.conf

    echo "PostgreSQL configured for external access on port 5432"
fi

echo "Database initialization complete!"
