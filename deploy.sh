#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Despliegue automático ==="
echo "Directorio: $PROJECT_DIR"

# Verifica que existe .env (necesario para las variables de docker-compose)
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "ERROR: No existe $PROJECT_DIR/.env"
    echo "Copia .env.example a .env y rellena las contraseñas."
    exit 1
fi

# 1. Pull del código
echo ""
echo ">>> Pulling último commit de master..."
cd "$PROJECT_DIR"
git pull origin master

# 2. Reconstruir SOLO el contenedor de la app (mysql no se toca)
echo ""
echo ">>> Reconstruyendo contenedor de la app..."
docker compose build app

# 3. Levantar SOLO el contenedor de la app
#    (--no-deps evita que compose intente recrear "mysql-db", que ya existe
#     con el volumen figuras_ocultas_mysql_data intacto y la misma red)
echo ""
echo ">>> Reiniciando contenedor de la app..."
docker compose up -d --no-deps app

# 4. Limpiar imágenes Docker huérfanas (no afecta a contenedores en uso)
echo ""
echo ">>> Limpiando imágenes Docker huérfanas..."
docker image prune -f

echo ""
echo "=== Despliegue completado ==="
echo "App: http://localhost:8080"
echo "MySQL: intacto (volumen mysql_data preservado)"
