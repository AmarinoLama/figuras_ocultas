#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-$HOME/figuras-ocultas}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.modern.yml}"
ENV_FILE="${ENV_FILE:-.env.modern}"

cd "$APP_DIR"

tracked_changes="$(git diff --name-only; git diff --cached --name-only)"
if [[ -n "$tracked_changes" ]]; then
  echo "ERROR: el checkout contiene cambios locales en archivos versionados; se cancela el despliegue." >&2
  printf '%s\n' "$tracked_changes" >&2
  exit 1
fi

unexpected_untracked="$(git status --porcelain --untracked-files=all | awk '$1 == "??" && $2 != ".env" && $2 != ".env.modern" && $2 != "..env.swp" {print}')"
if [[ -n "$unexpected_untracked" ]]; then
  echo "ERROR: hay archivos no versionados inesperados; se cancela el despliegue." >&2
  printf '%s\n' "$unexpected_untracked" >&2
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

if docker ps --format '{{.Names}}' | grep -qx 'figuras-ocultas-app'; then
  echo "==> Deteniendo solo la aplicación legacy; MySQL y su volumen permanecen intactos"
  docker stop figuras-ocultas-app
  docker rm figuras-ocultas-app
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build --remove-orphans

echo "==> Estado de los servicios"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

echo "==> Despliegue completado"
