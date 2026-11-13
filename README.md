# Respira 

Calidad del aire (ozono), temperatura y ruido **por zonas** de la ciudad, medidos con sensores móviles que llevan trabajadores públicos, bicis y motos compartidas, autobuses y vecinos voluntarios (que ganan puntos canjeables).

Producto de **Nodia** · Proyecto de Biometría y Medio Ambiente (GTI, UPV Gandia).

```
Respira/
├── arduino/   Nodo sensor (SparkFun Pro nRF52840 Mini + SPEC O₃ + DHT11 + KY-037) → iBeacon
├── android/   Proyecto de Android Studio «Respira» (Java) → app móvil
├── servidor/  API REST (PHP) + lógica + base de datos (MariaDB, Plesk)
├── web/       Proyecto de VS Code «respira» (HTML/CSS/JS) → web pública + panel municipal
├── doc/       base_de_datos.md · servidor.md · arduino.md
└── README.md

Sensor ──iBeacon──▶ app Android ──POST /mediciones──▶ API (Plesk) ──▶ MariaDB
                                                        │
                    app (mapa, zona, Mi sensor) ◀──GET──┤──GET──▶ web (mapa, historial, comparar, flota)

Cuentas, préstamos, puntos, regalos, canjes y percepciones: también en el servidor
(app y web con sesión por token). Ver doc/servidor.md y doc/base_de_datos.md.
```

## Estado: qué es real y qué es demostración

| Parte | Estado |
|---|---|
| Mapa con Google Maps (app y web) | Real (necesita clave, ver abajo) |
| Medidas del sensor (O₃, temperatura, ruido) | **Reales** (`arduino/`), sin valores simulados. Ruido en dB aproximados (KY-037 calibrado con el móvil) |
| Sensor → app → servidor | **Real**: la app escucha el beacon de SU sensor, añade ubicación y lo envía a `POST /mediciones`; sin red, lo guarda y lo manda en lote después |
| Zonas y sus valores (mapa, ficha de zona, historial, comparar) | **Reales** si la API responde (`GET /zonas`, `/serie`). Si no, datos de demostración **con aviso** |
| Cuentas (app y panel) y sesión | **Servidor**: contraseñas con hash, token de sesión, roles comprobados en el servidor |
| Préstamos, sensores del panel, puntos, revisiones, regalos, canjes, trabajadores | **Servidor** (BD completa). Los puntos los calcula el servidor con las mediciones de quien lleva el sensor |
| Percepciones (capa Comunidad) | **Servidor**: se ven en la app y en la web, 24 h, sin autor |
| Datos de partida | Los del prototipo, cargados en la BD (`esquema.sql`, sección demo). Solo RSP-4F7K manda datos reales |
| Zonas sobre Gandia | Las 9 de la demo (en la BD con sus coordenadas) + **Campus UPV** (rectángulo **aproximado**: compruébalo) |
| Alertas | La app las comprueba con los datos reales al leer las zonas (con la app abierta). Sin push (FCM) no llegan con la app cerrada |
| Favoritos y alertas | Se guardan en el móvil (no en el servidor) |

## App Android (`android/`)

1. Android Studio › **Open** › `Respira/android`.
2. Copia `android/local.properties.example` como `android/local.properties` y rellena:
   - `MAPS_API_KEY` = clave con **Maps SDK for Android** activada.
   - `URL_SERVIDOR` = `https://ablabla.upv.edu.es/respira/api/index.php` (la API de este sprint en Plesk, ver `doc/servidor.md`).
3. Sync Gradle y ejecuta en un móvil real (el emulador no tiene BLE).

Configuración: AGP 9.4.1 · Gradle 9.6.0 · compileSdk/targetSdk 36 · minSdk 28 · Java 11. Si Android Studio pide instalar el SDK 36, acéptalo.

Cuentas de prueba (en la BD de Plesk): `lucia@correo.es` / `Respira2026` (sensor `RSP-4F7K`) · `marcos@correo.es` / `Marcos2026` · trabajador `psanz@ayto.es` / `Trabajo2026!`. La app necesita la API: sin conexión se ve el mapa de demostración, pero no se puede entrar.

Estructura del código (`app/src/main/java/com/nodia/respira/`):

- `ble/` · `ServicioEscuharBeacons` (escucha solo el UUID `RESPIRA-<código>` del sensor prestado, añade lat/lng, RSSI y portador, descarta repetidas por contador y guarda hasta 500 pendientes sin red), `TramaIBeacon`, `Medicion` (trama → JSON de la API), `LogicaFake` (`POST /mediciones`, una o en lote), `PeticionarioREST`, `Utilidades`.
- `datos/` · modelo, `Repositorio` (único punto de acceso a datos: guarda una copia de lo del servidor y responde por callback a lo que cambia algo) y `ApiRespira` (cliente de toda la API con el token de la sesión). El mapa pide las zonas al abrirse y cada minuto (y comprueba las alertas); la ficha de zona pide su gráfica; «Mi sensor» pregunta al servidor si el sensor es tuyo y muestra el último valor de cada magnitud; Puntos, Regalos, Canjes y Mis percepciones son los del servidor. `Reglas` es lógica pura (estados de zona, validaciones).
- `ui/` · `SplashActivity` (carga ~3 s, «Developed by Nodia»), `OnboardingActivity`, `MainActivity` con 5 pestañas (Mapa · Parques · Sensor · Puntos · Más) y el resto de pantallas.
  - `Tema` + `RespiraApp`: modo claro (por defecto) u oscuro, en Más › Modo oscuro. Colores en `values/colors.xml` y `values-night/colors.xml` (los mismos que la web). En oscuro, Google Maps usa `res/raw/mapa_oscuro.json`.
  - `TextoActivity` (guía «Qué significan los valores») y `EfectosActivity` («Efectos en la salud»), con las piezas de `Bloques` y los textos de `datos/Salud`.
  - Percepciones: al tocar una en el mapa sale su hoja; si es tuya, **Editar** (tipo y nivel; conserva lugar y hora) y **Borrar**. También en Más › Mis percepciones. Máximo 3 por hora y cuenta. Demo: Lucía tiene una en C/ Mayor, 43.

## Web (`web/`)

Páginas: portada (`#inicio`, con los ayuntamientos que usan Respira), mapa, comunidad, historial, comparar, cómo funciona, privacidad y panel municipal (`#acceso`).

1. VS Code › **Open Folder** › `Respira/web` (o abre `respira.code-workspace`).
2. Copia `web/js/config.example.js` como `web/js/config.js` y pon `MAPS_API_KEY` (clave con **Maps JavaScript API**) y `API_URL` (`https://ablabla.upv.edu.es/respira/api/index.php`). Con `API_URL`, `js/datos-api.js` pinta los datos reales; sin ella, la demo con un aviso. `MAP_ID` (opcional, mapa vectorial) activa el botón 3D: sin él, Google Maps no permite inclinar el mapa.
3. Abre con **Live Server** (extensión recomendada) en `http://127.0.0.1:5500`, o `npm start`.

Diseño: fondo blanco con botón de modo oscuro en la cabecera (se recuerda en el navegador). En la ficha de cada zona del mapa, el botón «?» de cada medición abre la guía «Qué significan los valores»; en «Cómo funciona» están también la guía y «Qué te puede pasar» (efectos en la salud).

Sin `config.js`, o si la clave falla, la web usa el mapa SVG del prototipo (sin marca de Google) (y la consola muestra un 404 de `config.js`, que es esperado).

## Claves de Google Maps

- **Nunca** se suben a git: `android/local.properties` y `web/js/config.js` están en `.gitignore`.
- Restringe cada clave en Google Cloud: la de Android a tu paquete `com.nodia.respira` + huella SHA-1; la de la web a tus dominios (HTTP referrers).
- Hace falta tener la facturación activada en el proyecto de Google Cloud, aunque el uso del curso entre en la cuota gratuita.

## Repositorio

```bash
git remote add origin https://github.com/<tu-usuario>/Respira.git
git push -u origin main
```

## Servidor y base de datos (`servidor/`)

Pasos para Plesk (BD y carpeta nuevas, sin tocar el Sprint 0) en **`doc/servidor.md`**; tablas y campos en **`doc/base_de_datos.md`**; montaje, librerías y calibración del sensor en **`doc/arduino.md`**.

Resumen: BD `respira` → importar `servidor/bd/esquema.sql` (o `actualizar_desde_v1.sql` para conservar mediciones) → subir `servidor/api`, `servidor/logica` y `web` a `httpdocs/respira/` → `api/config.php` (nunca a git) → probar `…/respira/api/index.php/estado`.

## Tests

| Parte | Cómo | Tests |
|---|---|---|
| Arduino | `g++ -std=c++17 -I arduino/tests/mocks -I arduino/NodoRespira -x c++ arduino/tests/NodoRespiraTest.cpp -o t && ./t` | 26 |
| Lógica PHP | `LogicaTest`, `CuentasTest`, `PuntosTest`, `ComunidadFlotaTest` (`php servidor/tests/…`) | 15 + 12 + 11 + 13 |
| API REST | `php servidor/tests/ApiTest.php` | 23 |
| Simulación, huecos, alertas y picos | `php servidor/tests/SimuladorTest.php` | 5 |
| Android (JUnit) | Android Studio › `app/src/test` › Run | 16 |

## Pendiente

- Comprobar el contorno del Campus UPV y calibrar el ruido y el cero del ozono con el sensor montado.
- Alertas push reales (FCM): hoy las alertas solo saltan con la app abierta.
- Correo: depende de que Plesk pueda enviar (recuperar contraseña, activar trabajadores).
- Cambiar las contraseñas de demostración antes de un uso real.
- Traducciones (valenciano, inglés).
- Compilar con Gradle y probar en un móvil: el Java compila con `javac` contra `android.jar` 36 y las librerías del proyecto, pero no se ha hecho un build de Gradle completo (recursos y manifest) ni se ha probado en un móvil con el sensor.
