# FLIP7 - Proyecto Full Stack

## Descripción General

FLIP7 es una implementación digital del juego de cartas Flip7 desarrollada como proyecto para la asignatura de Calidad de Software.

El proyecto fue construido siguiendo una arquitectura Full Stack compuesta por:

* Backend desarrollado en Java con Spring Boot.
* Frontend desarrollado en React y TypeScript.
* Persistencia de datos mediante base de datos.
* Pruebas automatizadas de distintos niveles.
* Integración real entre frontend y backend.
* Modo multijugador y modo contra Inteligencia Artificial (Ollama).

El objetivo del proyecto no fue únicamente desarrollar un sistema funcional, sino también aplicar principios de ingeniería de software, diseño orientado a objetos, automatización de pruebas y buenas prácticas de desarrollo.

---

# Integrantes

* Luis David Mora
* Erik Valencia
* Deivy Lujan
* Daniel Cabana

---

# Objetivos del Proyecto

Este proyecto busca demostrar conocimientos en:

* Desarrollo Backend.
* Desarrollo Frontend.
* Diseño de APIs REST.
* Persistencia de información.
* Automatización de pruebas.
* Integración entre sistemas.
* Calidad de software.
* Programación orientada a objetos.
* Principios SOLID.
* Arquitecturas mantenibles y escalables.
* Integración de Modelos de Lenguaje (LLMs) locales.

---

# Tecnologías Utilizadas

## Backend

* Java 17
* Spring Boot
* Spring Data JPA
* Gradle
* H2 Database
* PostgreSQL (opcional)
* **Ollama** (Motor de Inteligencia Artificial local)

### Pruebas Backend

* JUnit 5
* Mockito
* MockMvc
* JaCoCo
* PIT Mutation Testing

---

## Frontend

* React 19
* TypeScript
* Vite
* Zustand
* Axios
* Framer Motion

### Pruebas Frontend

* Playwright
* Cucumber
* Playwright BDD

---

# Arquitectura General

El sistema sigue una arquitectura por capas que permite una adecuada separación de responsabilidades.

## Arquitectura Backend

### Controladores (Controller)

Responsables de:

* Recibir peticiones HTTP.
* Validar parámetros de entrada.
* Devolver respuestas HTTP apropiadas.

### Servicios (Service)

Responsables de:

* Aplicar las reglas del juego.
* Gestionar turnos.
* Calcular puntajes.
* Gestionar rondas.
* Gestionar salas.
* Gestionar la IA.

### Dominio (Model)

Contiene las entidades principales:

* Game
* Player
* Deck
* Room
* RoundPlayer

### Persistencia (Repository)

Responsable del acceso a base de datos mediante Spring Data JPA.

---

## Arquitectura de Inteligencia Artificial (Ollama)

El sistema integra un modelo de lenguaje local para la toma de decisiones en tiempo real.

*   **OllamaAiService**: Servicio encargado de la comunicación con la API de Ollama mediante `HttpClient`.
*   **Prompt Engineering**: Se utiliza un `system prompt` estructurado para definir el comportamiento de la IA y asegurar respuestas en formato JSON estricto.
*   **Modelo**: Basado en `qwen2.5:7b`, optimizado mediante un `Modelfile` personalizado (`flip7-ai`).
*   **Flujo de Decisión**:
    1.  El `TurnService` identifica el turno de un jugador controlado por IA.
    2.  Se recopila el estado actual de la partida (cartas en mano, puntos, mazo restante, estado del oponente).
    3.  Se envía el contexto a Ollama.
    4.  La IA devuelve una decisión (`hit` o `stand`) junto con un razonamiento.
    5.  El sistema ejecuta la acción y almacena el razonamiento para visualización en el frontend.

---

## Arquitectura Frontend

El frontend utiliza una arquitectura basada en funcionalidades.

Principales módulos:

* API
* Services
* Store
* Pages
* Components
* Hooks
* Types
* Utils

El frontend es responsable únicamente de:

* Representación visual.
* Interacción con el usuario.
* Experiencia de usuario (incluyendo feedback de la IA).

Toda la lógica del juego se encuentra implementada en el backend.

---

# Funcionalidades Implementadas

## Gestión de Partidas

El sistema permite:

* Crear partidas.
* Crear salas multijugador.
* Unirse a salas existentes.
* Iniciar partidas.
* Consultar estado de partida.

---

## Gestión de Turnos

Se implementa:

* Control de turno activo.
* Rotación de jugadores.
* Validación de acciones permitidas.
* Omisión de jugadores eliminados o plantados.
* **Ejecución automática de turnos de IA**.

---

## Gestión del Mazo

La implementación respeta la distribución oficial de Flip7:

* 1 carta con valor 0.
* 1 carta con valor 1.
* 2 cartas con valor 2.
* 3 cartas con valor 3.
* ...
* 12 cartas con valor 12.

---

## Sistema de Puntajes

Incluye:

* Acumulación de puntos por ronda.
* Conservación de puntaje al plantarse.
* Eliminación por carta repetida.
* Bonificación de 15 puntos por obtener 7 cartas numéricas distintas.

---

## Determinación de Ganador

La partida finaliza cuando un jugador alcanza el puntaje objetivo definido por las reglas del juego.

El ganador es el jugador con la mayor cantidad de puntos.

---

## Persistencia

El sistema almacena:

* Ganador de cada partida.
* Puntajes obtenidos por cada jugador.
* Resultados históricos de rondas.

---

# Funcionalidades Adicionales

## Cartas Especiales

Se implementaron las siguientes cartas especiales:

* Freeze
* Flip Three
* Second Chance
* Modificadores de puntaje (+2, +4, +6, +8, +10)
* Multiplicador x2

Cada carta incluye:

* Lógica de negocio.
* Integración frontend.
* Persistencia cuando aplica.
* Pruebas automatizadas.

---

## Inteligencia Artificial

El proyecto incluye un modo de juego contra IA potenciado por **Ollama**.

La IA:

* Analiza el estado actual de la partida.
* Evalúa riesgo y recompensa basándose en el mazo restante y puntos actuales.
* Toma decisiones automáticas (`Hit` o `Stand`).
* Proporciona un razonamiento textual de su decisión.
* Participa como un jugador adicional en tiempo real.

---

# Estructura General del Proyecto

```text 
Flip7/ 
│ 
├── agent/ 
│   └── Modelfile           # Configuración del modelo de Ollama 
│ 
├── client/ 
│   ├── src/ 
│   ├── tests/ 
│   ├── package.json 
│   └── playwright.config.ts 
│ 
├── server/ 
│   ├── src/main/java 
│   ├── src/test/java 
│   ├── build.gradle 
│   └── gradlew 
│ 
└── README.md 
``` 

---

# Cómo Ejecutar el Proyecto

## 1. Configurar Ollama (IA)

Es necesario tener instalado [Ollama](https://ollama.com/) en el sistema.

1.  Descargar el modelo base:
    ```bash
    ollama pull qwen2.5:7b
    ```
2.  Crear el modelo personalizado para Flip7:
    ```bash
    cd agent
    ollama create flip7-ai -f Modelfile
    ```

## 2. Ejecutar Backend

Desde la carpeta `server`:

```bash 
./gradlew bootRun 
``` 

El backend quedará disponible en: 

```text 
http://localhost:8080 
``` 

--- 

## 3. Ejecutar Frontend 

Desde la carpeta `client`: 

```bash 
npm install 
npm run dev 
``` 

El frontend quedará disponible en: 

```text 
http://localhost:5173 
``` 

--- 

# Ejecución de Pruebas 

## Backend 

Ejecutar todos los tests: 

```bash 
./gradlew test 
``` 

Generar reporte de cobertura: 

```bash 
./gradlew jacocoTestReport 
``` 

Verificar cobertura mínima: 

```bash 
./gradlew jacocoTestCoverageVerification 
``` 

Ejecutar mutation testing: 

```bash 
./gradlew pitest 
``` 

--- 

## Frontend 

Instalar Playwright: 

```bash 
npx playwright install 
``` 

Ejecutar pruebas E2E: 

```bash 
npm run test:e2e 
``` 

Ejecutar pruebas funcionales: 

```bash 
npm run test:functional 
``` 

--- 

# Estrategia de Testing 

## Backend 

### Pruebas Unitarias 

Validan: 

* Reglas del juego. 
* Gestión de turnos. 
* Puntajes. 
* Validaciones. 
* Lógica de cartas. 
* Determinación de ganador. 

### Pruebas de Integración 

Validan: 

* Endpoints REST. 
* Flujo completo de la API. 
* Persistencia. 
* Integración entre capas. 
* **Mocks de Ollama para pruebas de IA**.

--- 

## Frontend 

### Playwright 

Escenarios implementados: 

* Flujo normal de ronda. 
* Eliminación por carta repetida. 
* Jugadores eliminados. 
* Rotación de turnos. 
* Acción de plantarse. 
* Reinicio de ronda. 
* Flujo de ganador. 

### Pruebas Funcionales BDD 

Validan el comportamiento completo de la aplicación utilizando el backend real. 

--- 

# Cumplimiento de la Rúbrica 

El proyecto cumple con los requisitos establecidos: 

✅ Backend desarrollado en Java. 

✅ Frontend web funcional. 

✅ Integración real Frontend ↔ Backend. 

✅ Persistencia de información. 

✅ API REST. 

✅ Automatización de pruebas. 

✅ Pruebas unitarias. 

✅ Pruebas de integración. 

✅ Pruebas funcionales Playwright. 

✅ Aplicación de principios SOLID. 

✅ Programación Orientada a Objetos. 

✅ Arquitectura mantenible. 

✅ Código fuente documentado. 

✅ README de ejecución. 

✅ Colección de endpoints. 

--- 

# Posibles Mejoras Futuras 

* Ranking global de jugadores. 
* Sistema de autenticación. 
* Estadísticas avanzadas. 
* Historial visual de partidas. 
* Multijugador online distribuido. 
* **Optimización de prompts y uso de modelos más ligeros para móviles**.
* **Diferentes personalidades de IA (Agresiva vs Conservadora)**.

--- 

# Conclusión 

FLIP7 fue desarrollado siguiendo buenas prácticas de ingeniería de software, integrando backend, frontend, persistencia y pruebas automatizadas. La inclusión de Inteligencia Artificial mediante Ollama añade una capa de modernidad y desafío al proyecto, demostrando la versatilidad de las arquitecturas actuales para integrar modelos LLM locales en aplicaciones tradicionales.
