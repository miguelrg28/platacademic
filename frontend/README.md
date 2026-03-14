# Frontend

SPA construida con React 19 y Vite 8 para consumir el backend Javalin del mismo proyecto.

## Scripts

- `npm run dev`: levanta Vite con proxy a `http://localhost:7070`.
- `npm run build`: genera `frontend/dist`.
- `npm run build:backend`: genera el build dentro de `src/main/resources/public`.
- `npm run lint`: ejecuta ESLint.

## Flujo recomendado

1. Ejecuta el backend con `./gradlew run`.
2. Ejecuta el frontend con `npm run dev`.
3. Para empaquetar el sistema completo, usa `npm run build:backend`.
