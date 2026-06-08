# Flip7 Backend

Backend del juego Flip7 construido con Spring Boot, Java 17 y Gradle.

## Requisitos

- Java 17 instalado y visible para `java`/Gradle.
- Git.
- Conexión a internet para descargar dependencias de Gradle la primera vez.

No necesitas instalar Gradle por separado porque el proyecto usa el wrapper incluido en el repositorio.

### Instalar Java 17

Si `./gradlew clean test` falla con un error de toolchain como el que viste, instala un JDK 17 que Gradle pueda detectar y define `JAVA_HOME` según tu sistema operativo.

#### Windows

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

Después, abre una nueva terminal y verifica:

```powershell
java -version
```

Si hace falta, define `JAVA_HOME` apuntando a la carpeta del JDK 17 instalada y agrega `%JAVA_HOME%\bin` al `Path` del sistema.

Si ya tienes Java 17 instalado, solo asegúrate de que `java -version` muestre la versión 17 antes de seguir.

Si `java -version` sigue mostrando otra versión, revisa qué JDK ve macOS con:

```bash
/usr/libexec/java_home -V
```

#### macOS

```bash
brew install openjdk@17

sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" \
	"/Library/Java/JavaVirtualMachines/openjdk-17.jdk"

echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc

source ~/.zshrc

java -version
```

#### Linux

```bash
sudo apt update
sudo apt install openjdk-17-jdk

echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc

source ~/.bashrc

java -version
```

En distribuciones basadas en Red Hat o Fedora, el paquete puede variar a `java-17-openjdk-devel`.



## ¿Hace falta Docker?

No. En el estado actual del proyecto no hay `Dockerfile` ni `docker-compose` dentro de `/server`.
El backend se arranca directamente con el wrapper de Gradle.

## Base de datos

Por defecto, el backend arranca con **H2 en memoria** para que `bootRun` abra limpio en cada ejecución.
Esto evita bloqueos de archivo y también evita depender de un PostgreSQL remoto para levantar el backend.

Si quieres usar PostgreSQL, puedes crear tu configuración local en
`src/main/resources/application-local.properties` y arrancar con perfil `local`.


## Estructura principal

- `src/main/java/com/flip7/game/GameApplication.java`: punto de entrada de Spring Boot.
- `src/main/java/com/flip7/game/controller`: controladores REST del juego.
- `src/main/java/com/flip7/game/service`: lógica de negocio.
- `src/main/java/com/flip7/game/model`: entidades JPA.
- `src/main/java/com/flip7/game/repository`: repositorios de persistencia.
- `src/main/java/com/flip7/game/DTO`: objetos de transferencia.
- `src/main/java/com/flip7/game/config`: configuración de CORS y seguridad.

## Versión y puertos

- Java: 17
- Gradle Wrapper: 9.4.1
- Spring Boot: 4.0.6
- Puerto por defecto: 8080

## Arranque desde cero

Sigue estos comandos en orden desde la raíz del proyecto.

```bash
cd "/Volumes/Library/University/Calidad de Software/Flip7/server"

java -version
./gradlew --version

chmod +x gradlew

./gradlew clean test
./gradlew bootRun
```

### Arranque opcional con PostgreSQL

Si necesitas arrancar contra PostgreSQL (perfil `local`):

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

`./gradlew bootRun` no termina por diseño: el task se queda en ejecución mientras el servidor está activo.
Si en VS Code ves un porcentaje como 80%, normalmente significa que Spring Boot ya levantó y el proceso
sigue escuchando en `http://localhost:8080`.



## Verificación rápida

Cuando el backend esté arriba, deberías poder consumirlo en:

- `http://localhost:8080/api/flip/game`
- `http://localhost:8080/api/flip/`
- `http://localhost:8080/api/flip/stand`
- `http://localhost:8080/h2-console` (si usas la configuración por defecto)

## Flujo de salas multijugador

El backend ya soporta crear sala, unirse y luego iniciar la partida.

- `POST /api/flip/rooms` crea sala (host)
- `POST /api/flip/rooms/{code}/join` une jugador a sala
- `GET /api/flip/rooms/{code}` consulta estado del lobby
- `POST /api/flip/rooms/{code}/start` inicia la partida con los jugadores de la sala

## Modo 1 vs IA

El backend también expone una partida directa contra la IA local conectada a Ollama.

- `POST /api/flip/games/vs-ai` crea una partida humana vs bot
- El backend consulta `http://localhost:11434` con el modelo `flip7-ai:latest`
- La decisión de la IA se ejecuta en el backend como si fuera un jugador más

Si necesitas cambiar la conexión, ajusta estas propiedades:

- `ollama.base-url`
- `ollama.model`
- `ollama.timeout-ms`

## Notas útiles

- El perfil por defecto usa H2 en memoria para que el backend levante sin dependencias externas.
- CORS está abierto para facilitar el consumo desde el frontend.
- Si usas PostgreSQL, crea/ajusta `src/main/resources/application-local.properties` y arranca con `--spring.profiles.active=local`.
