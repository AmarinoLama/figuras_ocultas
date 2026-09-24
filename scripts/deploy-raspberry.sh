#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-$HOME/figuras-ocultas}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.modern.yml}"
ENV_FILE="${ENV_FILE:-.env.modern}"

cd "$APP_DIR"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: el checkout contiene cambios locales; se cancela el despliegue." >&2
  git status --short >&2
  exit 1
fi

echo "==> Actualizando código desde origin/$DEPLOY_BRANCH"
git fetch origin "$DEPLOY_BRANCH"
git checkout "$DEPLOY_BRANCH"
git pull --ff-only origin "$DEPLOY_BRANCH"

echo "==> Validando configuración de Docker Compose"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet

echo "==> Descargando imágenes y reconstruyendo servicios"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build --remove-orphans

echo "==> Estado de los servicios"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

echo "==> Despliegue completado"
