# Figuras Ocultas moderno

Nueva arquitectura preparada para Raspberry Pi:

- `frontend/`: Angular standalone + Nginx.
- `backend/`: FastAPI + SQLAlchemy.
- `docker-compose.modern.yml`: frontend, API y MySQL en una red privada.
- La API usa las tablas existentes y no ejecuta migraciones destructivas.

## Desarrollo local

```bash
cd backend
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Para el frontend:

```bash
cd frontend
npm install
npm start
```

## Raspberry Pi con acceso desde Internet

La forma recomendada es publicar únicamente el frontend mediante Caddy y HTTPS. FastAPI y MySQL quedan dentro de la red Docker; MySQL solo escucha en localhost.

1. Compra o usa un dominio y crea un registro DNS `A` apuntando a tu IP pública de fibra. Añade un registro `AAAA` solo si has confirmado que la Raspberry y el router aceptan conexiones IPv6 entrantes; un `AAAA` mal configurado puede hacer que algunos móviles fallen antes de probar IPv4.
2. Comprueba si tienes CGNAT. Si la WAN del router está en `100.64.0.0/10`, `10.0.0.0/8`, `172.16.0.0/12` o `192.168.0.0/16`, el port-forwarding no bastará: solicita IP pública al operador o usa Tailscale/Cloudflare Tunnel.
3. Reserva una IP fija para la Raspberry en el router.
4. Configura NAT/port forwarding:
   - TCP `80` del router → Raspberry:`80`.
   - TCP `443` del router → Raspberry:`443`.
   - No publiques `3306`, `3307`, `8000` ni `8080`.
5. Copia `.env.modern.example` a `.env.modern`, establece un dominio real, email y secretos únicos.
6. Arranca la pila:

```bash
docker compose --env-file .env.modern -f docker-compose.modern.yml up -d --build
```

Caddy solicitará y renovará automáticamente el certificado TLS de Let's Encrypt cuando el DNS y los puertos sean correctos. La aplicación quedará disponible en `https://TU_DOMINIO`; la documentación estará en `https://TU_DOMINIO/api/docs`.

Para comprobar el caso específico de los móviles desde otra red, desactiva el Wi-Fi del teléfono y prueba `https://TU_DOMINIO/api/health`. Si no responde, revisa el DNS (`A`/`AAAA`), CGNAT y la redirección TCP 80/443; si responde pero la web no carga, revisa la consola del navegador y los logs de Caddy.

### Firewall de la Raspberry

Activa el firewall y permite SSH solo desde tu red de administración, además de HTTP/HTTPS:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow from 192.168.1.0/24 to any port 22 proto tcp
sudo ufw enable
```

No uses esos comandos si tu acceso SSH depende de otra subred; adapta la regla antes de activarlo para no perder acceso.

### Si tienes IP dinámica

Configura DDNS en el router o usa un proveedor DDNS. El registro DNS debe actualizarse cuando cambie la IP pública.

### Si hay CGNAT

Usa una de estas opciones:

- Solicitar al ISP una IPv4 pública.
- Cloudflare Tunnel, sin abrir puertos entrantes.
- Tailscale para acceso privado desde tus dispositivos.


## Despliegue automático desde GitHub Actions

El repositorio incluye `.github/workflows/deploy-raspberry.yml`. Cada `push` a `migration/angular-fastapi` valida el código en GitHub y después ejecuta el despliegue en un runner ARM64 instalado en la Raspberry. El runner permanece activo mediante systemd de usuario y `loginctl enable-linger`, por lo que vuelve a arrancar después de reinicios.

El runner está instalado en `/home/aman/actions-runner-arm64` con las etiquetas `self-hosted`, `linux`, `ARM64` y `raspberry-pi`. El checkout de despliegue está en `/home/aman/figuras_ocultas`; el script hace `git pull --ff-only`, valida Compose, descarga imágenes y reconstruye la pila.

### Preparación inicial en la Raspberry

Deja en la Raspberry el `.env.modern` real (no se sube a Git):

```bash
cd ~/figuras_ocultas
cp .env.modern.example .env.modern
# Edita .env.modern con secretos reales
docker compose --env-file .env.modern -f docker-compose.modern.yml up -d --build
```

Con el runner autoalojado ya no hacen falta `RASPBERRY_HOST`, `RASPBERRY_USER`, `RASPBERRY_SSH_KEY` ni `RASPBERRY_APP_DIR` en GitHub. No guardes `.env.modern`, claves SSH ni contraseñas en el repositorio. El checkout de la Raspberry debe estar limpio para que `git pull --ff-only` no falle.

### Cloudflare

La Raspberry tenía un **Quick Tunnel** (`cloudflared tunnel --url http://localhost:8080`). Aunque el servicio systemd está habilitado y reinicia el proceso, los Quick Tunnels no ofrecen una URL estable ni garantía de disponibilidad; por eso no son una solución permanente. Para una URL fija hay que crear un túnel nombrado en una cuenta Cloudflare y configurarlo como servicio con un token o credenciales del túnel. No se puede completar esa parte sin acceso a la cuenta, dominio y token de Cloudflare.

El workflow despliega la rama actual `migration/angular-fastapi`. Cuando la aplicación pase a producción en `main`, cambia esa rama en los dos lugares del workflow (`on.push.branches` y `DEPLOY_BRANCH`) y en el checkout inicial.

## Importante

El login conserva temporalmente la comparación de contraseñas existente para no romper los usuarios actuales. Antes de producción conviene migrar contraseñas a hashes con una tarea explícita y hacer backup de MySQL. La API y el frontend actuales incluyen login, dashboard, catálogo de cartas, imágenes, compras, inventario, historial, gestión de alumnos, insignias, perfil y configuración. Antes de usarlo contra producción, prueba todos los flujos con una copia de la BD.
