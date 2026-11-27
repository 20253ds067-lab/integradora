# 🎰 Proyecto Integrador: Casino Móvil & API REST Spring Boot

## 👥 Integrantes del Equipo
* **Ixaya Vélez Reyes** — Matrícula: `20253DS067`
*(Si hay más integrantes en el equipo, favor de agregar sus nombres y matrículas aquí)*

---

## 📱 Descripción de la Solución
Aplicación móvil desarrollada en **Android nativo (Jetpack Compose, Kotlin)** conectada a un servidor backend en **Spring Boot (Java, Maven)** con arquitectura de doble persistencia:
1. **Persistencia Local en SQLite:** Almacenamiento persistente en el dispositivo para usuarios, sesiones activas, historial de banca y amigos/solicitudes, permitiendo funcionamiento fluido aun en modo offline.
2. **Persistencia Remota en MySQL:** Gestión centralizada de jugadores y partidas mediante API REST en Spring Boot.

El casino implementa tres juegos de habilidad y gestión de banca con economía protegida (límite mínimo garantizado de $1 MXN para evitar bloqueos por bancarrota):
* **Ruleta Clásica:** Apuestas a Rojo (2x), Verde 0 (14x) y Negro (2x) con desaceleración física y control por sensor giroscópico.
* **Juego de Dados:** Pronóstico de la suma (<7, =7, >7) activado por agitación física (Shake) con respuesta háptica y filtrado de gravedad.
* **Blackjack 21:** Mano contra el dealer con mazo tipo shoe, reglas en español, doblar, split y sensor de proximidad para plantarse.
* **Ranking y Amigos:** Clasificación general de banca por saldo, podio Top 3, sistema mutuo de solicitudes de amistad y edición de apodos privados.

---

## 📡 Sensores Físicos Integrados (15% de la Rúbrica)
* **Acelerómetro (`Sensor.TYPE_ACCELEROMETER`):** Detección de sacudida física (**Shake**) mediante diferencial dinámico de aceleración $\Delta a = \sqrt{\Delta x^2 + \Delta y^2 + \Delta z^2}$ con cancelación al 100% de la gravedad estática terrestre y ventana de enfriamiento de 800 ms al apostar para lanzar los dados con vibración háptica.
* **Giroscopio (`Sensor.TYPE_GYROSCOPE`):** Detección de velocidad angular y rotación física del dispositivo en tiempo real para girar la ruleta.
* **Sensor de Proximidad (`Sensor.TYPE_PROXIMITY`):** Permite al jugador plantarse en el Blackjack pasando la mano por encima del teléfono sin tocar la pantalla.

---

## 📂 Estructura del Repositorio
* `/app` — Código fuente completo de la aplicación móvil en Android Jetpack Compose.
* `/backend` — Código fuente completo del servidor API REST en Spring Boot (Java, JPA/Hibernate, MySQL).
* `/gradle` y `gradlew` — Scripts de compilación y empaquetado del APK de Android.

---

## 🚀 Instrucciones de Ejecución

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
   *(El servidor iniciará en el puerto `8080`)*

### 2. Aplicación Móvil (Android)
1. Abrir la raíz del proyecto en **Android Studio**.
2. Si se prueba en **emulador**, la conexión se realiza de forma automática a través del alias `http://10.0.2.2:8080/`.
3. Si se prueba en un **dispositivo físico por USB**, ejecutar el túnel ADB en la terminal:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
4. Compilar y ejecutar en Android Studio o generar el APK con:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🌐 Endpoints de la API REST Consumidos por la App
* `GET /jugador` — Obtiene la lista completa de jugadores registrados en el servidor.
* `POST /jugador` — Registra un nuevo jugador con su saldo inicial.
* `POST /jugador/recargarSaldo` — Permite recargar o ajustar saldo desde el servidor.
* `POST /partida/iniciarPartida` — Inicia una nueva partida con la apuesta establecida.
* `POST /partida/realizarTiro` — Procesa el tiro de dados en el servidor.
* `POST /partida/finalizarPartidaManual` — Cierra y liquida la partida.
