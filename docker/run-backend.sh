#!/bin/bash

JAVA_OPTS=""

if [ "$JAVA_DEBUG" = "true" ]; then
    echo "Debug mode enabled on port 5005"
    JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
fi

exec java $JAVA_OPTS -jar /app/app.jar
