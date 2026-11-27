# Proyecto Integrador: Casino Movil & API REST Spring Boot

## Integrantes del Equipo
* **Ixaya Velez Reyes** — Matricula: `20253DS067`
* **Jose Elias Hurtado Hernandez** — Matricula: `20253DS078`
* **Carlos Apreza Gutierrez** — Matricula: `20253DS088`

---

## Descripcion de la Solucion
Aplicacion movil desarrollada en **Android nativo (Jetpack Compose, Kotlin)** conectada a un servidor backend en **Spring Boot (Java, Maven)** con arquitectura de doble persistencia:
1. **Persistencia Local en SQLite:** Almacenamiento persistente en el dispositivo para usuarios, sesiones activas, historial de banca y amigos/solicitudes, permitiendo funcionamiento fluido aun en modo offline.
2. **Persistencia Remota en MySQL:** Gestion centralizada de jugadores y partidas mediante API REST en Spring Boot.

El casino implementa tres juegos de habilidad y gestion de banca con economia protegida (limite minimo garantizado de $1 MXN para evitar bloqueos por bancarrota):
* **Ruleta Clasica:** Apuestas a Rojo (2x), Verde 0 (14x) y Negro (2x) con desaceleracion fisica y control por sensor giroscopico.
* **Juego de Dados:** Pronostico de la suma (<7, =7, >7) activado por agitacion fisica (Shake) con respuesta haptica y filtrado de gravedad.
* **Blackjack 21:** Mano contra el dealer con mazo tipo shoe, reglas en español, doblar, split y sensor de proximidad para plantarse.
* **Ranking y Amigos:** Clasificacion general de banca por saldo, podio Top 3, sistema mutuo de solicitudes de amistad y edicion de apodos privados.

---

## Sensores Fisicos Integrados (15% de la Rubrica)
* **Acelerometro (`Sensor.TYPE_ACCELEROMETER`):** Deteccion de sacudida fisica (**Shake**) mediante diferencial dinamico de aceleracion con cancelacion al 100% de la gravedad estatica terrestre y ventana de enfriamiento de 800 ms al apostar para lanzar los dados con vibracion haptica.
* **Giroscopio (`Sensor.TYPE_GYROSCOPE`):** Deteccion de velocidad angular y rotacion fisica del dispositivo en tiempo real para girar la ruleta.
* **Sensor de Proximidad (`Sensor.TYPE_PROXIMITY`):** Permite al jugador plantarse en el Blackjack pasando la mano por encima del telefono sin tocar la pantalla.

---

## Estructura del Repositorio
* `/app` — Codigo fuente completo de la aplicacion movil en Android Jetpack Compose.
* `/backend` — Codigo fuente completo del servidor API REST en Spring Boot (Java, JPA/Hibernate, MySQL).
* `/gradle` y `gradlew` — Scripts de compilacion y empaquetado del APK de Android.

---

## Instrucciones de Ejecucion

### 1. Servidor Backend (Spring Boot)
1. Abrir la carpeta `backend` en IntelliJ IDEA, VS Code o terminal.
2. Configurar la base de datos MySQL en `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/casino_db?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=root
   ```
3. Ejecutar el backend desde terminal:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   *(El servidor iniciara en el puerto `8080`)*

### 2. Aplicacion Movil (Android)
1. Abrir la raiz del proyecto en **Android Studio**.
2. Si se prueba en **emulador**, la conexion se realiza de forma automatica a traves del alias `http://10.0.2.2:8080/`.
3. Si se prueba en un **dispositivo fisico por USB**, ejecutar el tunel ADB en la terminal:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
4. Compilar y ejecutar en Android Studio o generar el APK con:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Endpoints de la API REST Consumidos por la App
* `GET /jugador` — Obtiene la lista completa de jugadores registrados en el servidor.
* `POST /jugador` — Registra un nuevo jugador con su saldo inicial.
* `POST /jugador/recargarSaldo` — Permite recargar o ajustar saldo desde el servidor.
* `POST /partida/iniciarPartida` — Inicia una nueva partida con la apuesta establecida.
* `POST /partida/realizarTiro` — Procesa el tiro de dados en el servidor.
* `POST /partida/finalizarPartidaManual` — Cierra y liquida la partida.
