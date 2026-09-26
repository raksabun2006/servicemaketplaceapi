#!/bin/sh
set -e

# If Railway provides DATABASE_URL, extract components and export Spring datasource variables
if [ -n "$DATABASE_URL" ]; then
    echo "Processing DATABASE_URL from environment..."
    CLEAN_URL=$(echo "$DATABASE_URL" | sed -E 's/^(postgres|postgresql):\/\///')
    
    # Extract user:pass if present
    case "$CLEAN_URL" in
        *@*)
            USER_PASS=$(echo "$CLEAN_URL" | cut -d'@' -f1)
            HOST_PORT_DB=$(echo "$CLEAN_URL" | cut -d'@' -f2-)
            DB_USER=$(echo "$USER_PASS" | cut -d':' -f1)
            DB_PASS=$(echo "$USER_PASS" | cut -d':' -f2-)
            ;;
        *)
            HOST_PORT_DB="$CLEAN_URL"
            DB_USER=""
            DB_PASS=""
            ;;
    esac

    # Extract host, port, db
    HOST_PORT=$(echo "$HOST_PORT_DB" | cut -d'/' -f1)
    DB_NAME=$(echo "$HOST_PORT_DB" | cut -d'/' -f2- | cut -d'?' -f1)

    case "$HOST_PORT" in
        *:*)
            DB_HOST=$(echo "$HOST_PORT" | cut -d':' -f1)
            DB_PORT=$(echo "$HOST_PORT" | cut -d':' -f2)
            ;;
        *)
            DB_HOST="$HOST_PORT"
            DB_PORT="5432"
            ;;
    esac

    if [ -n "$DB_HOST" ] && [ -n "$DB_NAME" ]; then
        export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
        [ -n "$DB_USER" ] && export SPRING_DATASOURCE_USERNAME="$DB_USER"
        [ -n "$DB_PASS" ] && export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
        echo "Configured datasource for: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
    fi
fi

echo "Starting Spring Boot application on port ${PORT:-8080}..."
exec java ${JAVA_OPTS} -jar /app/app.jar
