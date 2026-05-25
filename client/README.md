# FLIP7 Frontend

Frontend del juego FLIP7 construido con React, TypeScript, Vite, TailwindCSS, Zustand y Framer Motion. La interfaz incluye simulación local completa del gameplay para trabajar sin backend real y está preparada para conectarse a una API más adelante.

## Arquitectura

La pantalla principal vive en `src/features/game/pages/GamePage.tsx`, pero la lógica está separada por capas:

- `src/features/game/engine/`: reglas puras del juego, baraja, turnos, puntuación y resolución de rondas.
- `src/features/game/api/`: capa de acceso preparada para backend real.
- `src/features/game/services/`: fachada de servicios que consume la UI.
- `src/features/game/mocks/`: repositorio local determinista para simular respuestas de servidor.
- `src/features/game/store/`: estado global con Zustand.
- `src/features/game/components/`: piezas visuales reutilizables.
- `src/features/game/hooks/`: hooks de lectura y acciones.
- `src/features/game/utils/`: utilidades de presentación y etiquetas en español.

## Juego local

El frontend corre completamente con estado local y una simulación mock del backend. La app soporta:

- 4 jugadores.
- rotación de turnos.
- robar carta.
- plantarse.
- eliminación por carta duplicada.
- cierre automático de ronda.
- resumen de ronda.
- detección de ganador a partir de 200 puntos.

Para pruebas e2e, Playwright puede inyectar estados deterministas con `window.__FLIP7_TEST__`.

## Scripts

Desde `client/`:

- `npm run dev` para levantar Vite en desarrollo.
- `npm run build` para compilar producción.
- `npm run lint` para revisar el código.
- `npm run test:e2e` para ejecutar Playwright.
- `npm run test:e2e:headed` para ejecutar Playwright con navegador visible.

## Tests

La suite de Playwright vive en `tests/game/` y cubre el flujo completo del juego:

- ronda normal.
- eliminación de todos los jugadores.
- duplicado y alerta de eliminación.
- acción de plantarse.
- rotación de turnos.
- detección de ganador.
- reinicio de ronda.

Los tests usan selectores estables con `data-testid` para mantenerlos robustos y fáciles de extender.

## Estructura del proyecto

```text
src/
	features/game/
		api/
		components/
		engine/
		hooks/
		mocks/
		pages/
		services/
		store/
		types/
		utils/
tests/
	fixtures/
	game/
	helpers/
```

## Notas de desarrollo

- El cliente ya está separado del backend.
- No mezcles lógica de juego dentro de los componentes visuales.
- Si luego conectas una API real, la capa de `services` y `api` está lista para reemplazar el mock sin rehacer la UI.
