#!/bin/bash

# Check if running in development mode (source code mounted)
if [ -d "/workspace/frontend" ]; then
    echo "============================================"
    echo "FRONTEND DEVELOPMENT MODE"
    echo "============================================"

    cd /workspace/frontend

    # Install dependencies if node_modules is empty or doesn't exist
    if [ ! -d "node_modules" ] || [ -z "$(ls -A node_modules 2>/dev/null)" ]; then
        echo "Installing npm dependencies..."
        npm install
    fi

    echo "Starting Vite dev server with hot reload..."
    echo "Changes to Vue files will update instantly"
    echo "Dev server: http://localhost:5173"
    echo "Proxied via Nginx: http://localhost:8889"
    echo "============================================"

    # Run Vite dev server with hot reload
    exec npm run dev -- --host 0.0.0.0 --port 5173

# Production mode - nginx serves pre-built files
else
    echo "============================================"
    echo "FRONTEND PRODUCTION MODE"
    echo "============================================"
    echo "Static files served by Nginx from /usr/share/nginx/html"
    echo "============================================"

    # In production, frontend is served by nginx, so we just exit
    exit 0
fi
