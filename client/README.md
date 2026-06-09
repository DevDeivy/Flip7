# FLIP7 Frontend

Frontend de FLIP7 construido con React, TypeScript y Vite.
La aplicación consume un backend real vía HTTP y soporta flujo completo de Home, lobby multijugador y partida.

## Stack

- React 19
- TypeScript
- Vite
- Zustand (estado global)
- Axios (cliente HTTP)
- Framer Motion (animaciones)
- Playwright Test (E2E)

## Arquitectura

La lógica del frontend está organizada por capas dentro de `src/features/game/`:

- `api/`: integración HTTP con backend real (`gameApi.ts`, `httpClient.ts`).
- `services/`: fachada de negocio que consume la UI.
- `store/`: estado global (Zustand) y acciones asíncronas.
- `pages/`: pantallas (`Home.tsx`, `MultiplayerPage.tsx`, `vsAI.tsx`).
- `components/`: componentes visuales reutilizables.
- `types/`: contratos TypeScript de datos y estado.
- `utils/`: etiquetas y utilidades de presentación.

## Scripts

Desde `client/`:

- `npm run dev`: iniciar frontend en desarrollo.
- `npm run build`: build de producción.
- `npm run lint`: análisis estático.
- `npm run test:e2e`: ejecutar suite E2E de Playwright.
- `npm run test:e2e:headed`: ejecutar E2E con navegador visible.
- `npm run test:e2e:report`: abrir reporte HTML ya generado.
- `npm run test:e2e:full`: ejecutar pruebas y abrir reporte al final.

## Playwright E2E (Frontend)

La configuración está en `playwright.config.ts` con:

- `retries` (en CI)
- `trace: on-first-retry`
- `screenshot: only-on-failure`
- `video: retain-on-failure`
- `reporter: html`

Ubicación de pruebas:

```text
tests/
	fixtures/
	game/
	helpers/
```

## Cómo generar reporte de pruebas frontend

1. Asegura dependencias e instalación de navegadores Playwright:

```bash
npm install
npx playwright install
```

2. Levanta el backend real en `http://localhost:8080`.

3. Ejecuta pruebas E2E frontend:

```bash
npm run test:e2e
```

4. Abre el reporte HTML:

```bash
npm run test:e2e:report
```

El reporte se genera en `playwright-report/`.

## Ejecución rápida (todo en uno)

```bash
npm run test:e2e:full
```

## Nota importante

Estas pruebas validan frontend consumiendo backend real. Si el backend no está activo o no responde en `localhost:8080`, los escenarios E2E fallarán.
