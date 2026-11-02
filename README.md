# Respira

Calidad del aire (ozono), temperatura y ruido **por zonas** de la ciudad, medidos con sensores móviles que llevan trabajadores públicos, bicis y motos compartidas, autobuses y vecinos voluntarios (que ganan puntos canjeables).

Producto de **Nodia** · Proyecto de Biometría y Medio Ambiente (GTI, UPV Gandia).

```
Respira/
├── android/   Proyecto de Android Studio «Respira» (Java) → app móvil
├── web/       Proyecto de VS Code «respira» (HTML/CSS/JS) → web pública + panel municipal
└── README.md
```

## Estado: qué es real y qué es demostración

| Parte | Estado |
|---|---|
| Mapa con Google Maps (app y web) | Real (necesita clave, ver abajo) |
| Escaneo BLE del sensor y `POST /medicion` | Real: código del Sprint 0 (`ble/`) adaptado a servicio en primer plano |
| Zonas, valores, cuentas, préstamos, puntos, regalos y canjes | **Demostración**: datos fijos del prototipo v17/v7, guardados en el móvil (`SharedPreferences`) o en memoria (web). Falta la API del servidor |
| Zonas sobre Gandia | **Ficticias y aproximadas**: se reemplazarán por los polígonos que dé el servidor |
| Alertas push | Simuladas en local (Más › Controles de la demo). Las reales irán por FCM desde el servidor |

## App Android (`android/`)

1. Android Studio › **Open** › `Respira/android`.
2. Copia `android/local.properties.example` como `android/local.properties` y rellena:
   - `MAPS_API_KEY` = clave con **Maps SDK for Android** activada.
   - `URL_SERVIDOR` = IP del PC con el servidor del Sprint 0 (misma WiFi que el móvil).
3. Sync Gradle y ejecuta en un móvil real (el emulador no tiene BLE).

Configuración: AGP 9.4.1 · Gradle 9.6.0 · compileSdk/targetSdk 36 · minSdk 28 · Java 11. Si Android Studio pide instalar el SDK 36, acéptalo.

Cuentas de prueba: `lucia@correo.es` / `Respira2026` (sensor `RSP-4F7K`) · `marcos@correo.es` / `Marcos2026`.

Estructura del código (`app/src/main/java/com/nodia/respira/`):

- `ble/` · Sprint 0: `ServicioEscuharBeacons`, `TramaIBeacon`, `Medicion`, `PeticionarioREST`, `LogicaFake`, `Utilidades`.
- `datos/` · modelo y `Repositorio` (único punto de acceso a datos; al llegar la API solo cambia esta clase). `Reglas` es lógica pura (estados de zona, puntos, validaciones).
- `ui/` · `SplashActivity` (carga ~3 s, «Developed by Nodia»), `OnboardingActivity`, `MainActivity` con 5 pestañas (Mapa · Parques · Sensor · Puntos · Más) y el resto de pantallas.

## Web (`web/`)

Páginas: portada (`#inicio`, con los ayuntamientos que usan Respira), mapa, comunidad, historial, comparar, cómo funciona, privacidad y panel municipal (`#acceso`).

1. VS Code › **Open Folder** › `Respira/web` (o abre `respira.code-workspace`).
2. Copia `web/js/config.example.js` como `web/js/config.js` y pon `MAPS_API_KEY` (clave con **Maps JavaScript API**). `MAP_ID` (opcional, mapa vectorial) activa el botón 3D: sin él, Google Maps no permite inclinar el mapa.
3. Abre con **Live Server** (extensión recomendada) en `http://127.0.0.1:5500`, o `npm start`.

Sin `config.js`, o si la clave falla, la web usa el mapa SVG del prototipo (sin marca de Google) (y la consola muestra un 404 de `config.js`, que es esperado).

Panel municipal (`#acceso`): `admin@ayto.es` / `Admin2026!` · `tecnico@ayto.es` / `Tecnico2026!` · `reparto@ayto.es` / `Reparto2026!`.

## Claves de Google Maps

- **Nunca** se suben a git: `android/local.properties` y `web/js/config.js` están en `.gitignore`.
- Restringe cada clave en Google Cloud: la de Android a tu paquete `com.nodia.respira` + huella SHA-1; la de la web a tus dominios (HTTP referrers).
- Hace falta tener la facturación activada en el proyecto de Google Cloud, aunque el uso del curso entre en la cuota gratuita.

## Repositorio

```bash
git remote add origin https://github.com/<tu-usuario>/Respira.git
git push -u origin main
```

## Pendiente

- API del servidor para zonas, cuentas, préstamos, puntos y canjes (hoy: demo).
- Enviar lat/lng con cada medición (`POST /medicion` aún es el del Sprint 0).
- Alertas push reales (FCM) y comprobación de umbrales en el servidor.
- Traducciones (valenciano, inglés).
- Compilar y probar en Android Studio: el proyecto se ha escrito sin poder compilarlo.
