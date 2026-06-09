# FLIP7 Frontend

Frontend de FLIP7 construido con React + TypeScript + Vite.
Este cliente consume el backend real y cubre el flujo completo de Home, lobby multijugador, partida y modo vs IA.

## Stack

- React 19
- TypeScript
- Vite
- Zustand (estado global)
- Axios (HTTP client)
- Framer Motion (animaciones)
- Playwright Test (E2E)
- Cucumber + Playwright (pruebas funcionales BDD)

## Arquitectura frontend

La aplicación usa arquitectura por feature en `src/features/game`.

- `api/`: acceso HTTP y configuración de cliente (`httpClient.ts`, `gameApi.ts`).
- `services/`: capa de fachada para reglas de UI y llamadas de dominio.
- `store/`: estado global y acciones asíncronas (Zustand).
- `pages/`: pantallas principales (`Home`, `MultiplayerPage`, `vsAI`).
- `components/`: componentes visuales reutilizables del juego.
- `hooks/`: hooks de composición para estado y acciones.
- `types/`: contratos TypeScript de estado y entidades.
- `utils/`: utilidades de etiquetas y formateo visual.

## Lógica de la UI (resumen)

- Home: selección de modo de juego.
- Multiplayer: creación/unión a sala y arranque de partida.
- Game view: render de mano, mesa, sidebar, eventos y acciones (`draw`/`stand`).
- VS AI: flujo 1 vs IA consumiendo backend real.
- Alertas/modales: duplicado, cierre de ronda y ganador.

## Estructura de carpetas

```text
client/
├── public/
├── scripts/
│   └── run-functional.mjs
├── src/
│   ├── features/
│   │   └── game/
│   │       ├── api/
│   │       ├── components/
│   │       ├── hooks/
│   │       ├── pages/
│   │       ├── services/
│   │       ├── store/
│   │       ├── types/
│   │       └── utils/
│   ├── App.tsx
│   └── main.tsx
├── tests/
│   ├── game/
│   ├── functional/
│   │   ├── features/
│   │   └── steps/
│   ├── fixtures/
│   └── helpers/
├── playwright.config.ts
├── vite.config.js
└── package.json
```

## Configuración de backend y networking

- El cliente usa `VITE_API_BASE_URL` cuando está definida.
- Si no está definida, usa `'/api/flip'` por defecto.
- En desarrollo, Vite hace proxy de `'/api'` a `http://localhost:8080`.

Esto permite correr frontend local en un puerto y backend en `8080` sin cambiar código.

## Cómo ejecutar la app

Desde `client/`:

```bash
npm install
npm run dev
```

La app queda disponible normalmente en `http://localhost:5173`.

Build de producción:

```bash
npm run build
npm run preview
```

## Scripts disponibles

- `npm run dev`: inicia frontend en desarrollo.
- `npm run build`: genera build de producción.
- `npm run preview`: previsualiza build local.
- `npm run lint`: ejecuta ESLint.
- `npm run test:e2e`: ejecuta Playwright E2E.
- `npm run test:e2e:headed`: E2E con navegador visible.
- `npm run test:e2e:report`: abre reporte HTML E2E generado.
- `npm run test:e2e:full`: ejecuta E2E y abre reporte al final.
- `npm run test:functional`: ejecuta BDD funcional (levanta backend+frontend automáticamente).
- `npm run test:functional:run`: ejecuta solo Cucumber sobre entornos ya levantados.
- `npm run test:functional:report`: abre reporte HTML funcional.

## Cómo ejecutar tests

### E2E (Playwright)

```bash
npm install
npx playwright install
npm run test:e2e
```

Notas:

- La configuración E2E usa `playwright.config.ts`.
- Playwright levanta automáticamente frontend en `http://127.0.0.1:4173`.
- El backend debe estar disponible en `http://localhost:8080` para escenarios que consumen API real.

### Funcionales BDD (Cucumber + Playwright)

```bash
npm run test:functional
```

Este comando:

- Arranca backend (`./gradlew bootRun`) desde `../server`.
- Arranca frontend Vite en `127.0.0.1:4173`.
- Ejecuta features BDD y apaga procesos al finalizar.

## Reportes de tests

- E2E Playwright HTML: `playwright-report/index.html`
- Funcionales BDD HTML: `cucumber-report/index.html`

Comandos para abrir reportes:

```bash
npm run test:e2e:report
npm run test:functional:report
```

## Recomendaciones operativas

- Ejecutar backend antes de correr E2E si no usas el flujo funcional automático.
- Si fallan requests de red, verificar backend en `http://localhost:8080`.
- Mantener Node y npm actualizados para compatibilidad con Vite/Playwright.
