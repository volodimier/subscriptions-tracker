#!/bin/bash

# Check if running in development mode (source code mounted)
if [ -d "/workspace/backend" ]; then
    echo "============================================"
    echo "BACKEND DEVELOPMENT MODE"
    echo "============================================"

    cd /workspace/backend

    GRADLE_OPTS=""

    if [ "$JAVA_DEBUG" = "true" ]; then
        echo "Remote debugging enabled on port 5005"
        GRADLE_OPTS="-Dorg.gradle.jvmargs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    fi

    echo "Starting Spring Boot with hot reload..."
    echo "Changes to Java files will trigger automatic restart"
    echo "============================================"

    # Run with bootRun for hot reload support
    exec gradle bootRun $GRADLE_OPTS --no-daemon -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Production mode - run from pre-built JAR
else
    echo "============================================"
    echo "BACKEND PRODUCTION MODE"
    echo "============================================"

    JAVA_OPTS=""

    if [ "$JAVA_DEBUG" = "true" ]; then
        echo "Remote debugging enabled on port 5005"
        JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    fi

    exec java $JAVA_OPTS -jar /app/app.jar
fi
