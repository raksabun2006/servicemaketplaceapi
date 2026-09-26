#!/bin/sh
set -e

# If Railway provides DATABASE_URL, convert postgres:// or postgresql:// to jdbc:postgresql://
if [ -n "$DATABASE_URL" ] && [ -z "$SPRING_DATASOURCE_URL" ]; then
    echo "Configuring Spring datasource from DATABASE_URL..."
    JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's/^(postgres|postgresql):\/\//jdbc:postgresql:\/\//')
    export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

echo "Starting Spring Boot application on port ${PORT:-8080}..."
exec java ${JAVA_OPTS} -jar /app/app.jar
