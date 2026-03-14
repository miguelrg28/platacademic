# Sistema de Gestion y Control de Eventos Academicos

Aplicacion full stack con backend en Javalin 7 + Hibernate + H2 y frontend en React 19 + Vite 8. El sistema cubre autenticacion por sesion, gestion de eventos por roles, inscripciones, QR, asistencia y estadisticas visuales.

## Arquitectura

- `src/main/java/edu/pucmm/icc352/events/domain`: entidades y reglas del negocio.
- `src/main/java/edu/pucmm/icc352/events/application`: casos de uso y seguridad.
- `src/main/java/edu/pucmm/icc352/events/infrastructure`: persistencia, H2, Flyway y QR.
- `src/main/java/edu/pucmm/icc352/events/web`: API HTTP JSON.
- `frontend/src/app`: shell principal de React.
- `frontend/src/components`: modulos de UI por rol y por caso de uso.
- `frontend/src/api`: cliente Fetch con sesion compartida.

## Stack utilizado

- JDK 25
- Gradle 9
- Javalin `7.0.1`
- Hibernate ORM
- H2 en modo servidor
- Flyway
- React `19.2.4`
- Vite `8.0.0`
- Chart.js
- html5-qrcode
- Docker multi-stage

## Variables de entorno del backend

- `APP_PORT`: puerto HTTP de Javalin. Default `7070`.
- `DB_HOST`: host del servidor H2. Default `localhost`.
- `DB_PORT`: puerto TCP de H2. Default `9092`.
- `DB_PATH`: ruta del archivo H2. Default `./data/academic-events`.
- `DB_USERNAME`: usuario H2. Default `sa`.
- `DB_PASSWORD`: password H2. Default vacio.
- `DB_ALLOW_REMOTE_CONNECTIONS`: habilita `-tcpAllowOthers` en H2. Default `false`.
- `ADMIN_USERNAME`: usuario bootstrap. Default `admin`.
- `ADMIN_PASSWORD`: password bootstrap. Default `Admin123!`.
- `ADMIN_FULL_NAME`: nombre bootstrap.
- `ADMIN_EMAIL`: email bootstrap.

## Desarrollo local

### Backend

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew run
```

### Frontend con Vite

```bash
cd frontend
npm install
npm run dev
```

El frontend usa proxy a `http://localhost:7070`, así que consume la misma API y la misma sesión del backend.

### Build del frontend para el backend

```bash
cd frontend
npm run build:backend
```

Ese comando genera la SPA en `src/main/resources/public`, que luego Javalin sirve de forma estática.

## Endpoints principales

### Autenticacion

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Eventos e inscripciones

- `GET /api/events`
- `GET /api/events/{eventId}`
- `GET /api/events/managed`
- `GET /api/my/registrations`
- `POST /api/events`
- `PUT /api/events/{eventId}`
- `POST /api/events/{eventId}/publish`
- `POST /api/events/{eventId}/unpublish`
- `POST /api/events/{eventId}/cancel`
- `POST /api/events/{eventId}/registrations`
- `DELETE /api/events/{eventId}/registrations/me`
- `GET /api/events/{eventId}/registrations/me/qr`
- `GET /api/events/{eventId}/registrations/me/qr/image`
- `GET /api/events/{eventId}/registrations`
- `POST /api/events/{eventId}/attendance/scan`
- `GET /api/events/{eventId}/summary`

### Administracion

- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}/blocked`
- `PUT /api/admin/users/{userId}/organizer-role`
- `GET /api/admin/events`
- `DELETE /api/admin/events/{eventId}`

## Docker

```bash
docker compose up --build
```

El `Dockerfile`:

- compila el frontend con Node,
- copia el build al classpath del backend,
- empaqueta la aplicacion Java con Gradle,
- y deja un contenedor final solo con runtime.

Para TLS en `docker-compose`, coloca:

- certificado en `deploy/certs/fullchain.pem`
- llave privada en `deploy/certs/privkey.pem`

Nginx redirige automaticamente `80 -> 443`.

## Verificacion realizada

- `npm run lint`
- `npm run build`
- `GRADLE_USER_HOME=.gradle-home ./gradlew test`
- arranque local del backend con Javalin, Flyway, Hibernate y H2
