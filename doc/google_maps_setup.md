# Configuración de Google Maps en Respira

## Estado de la integración

**Proyecto de Google Cloud: `respira-gti`.**

Según la confirmación del responsable de esta configuración, los mapas de Google de **Android y Web ya se han ejecutado correctamente en este equipo con Windows 11**. La dirección Web utilizada es **`http://127.0.0.1:5500`**.

Esa comprobación se comunicó el 13 de noviembre de 2026. La verificación posterior en el móvil de Aitor se detalla a continuación. La carga del mapa base no acredita por sí sola el funcionamiento de los sensores, la API de Respira o la base de datos.

No se incluyen claves API reales. Los valores de los ejemplos son marcadores de posición.

### Comprobación en el móvil de Aitor — 19 de noviembre de 2026

La clave configurada en `android/local.properties` coincide con la incorporada al APK instalado. El SDK de Google Maps devuelve **`Authorization failure`** e indica esta identidad de aplicación:

```text
Paquete: com.nodia.respira
SHA-1: CD:95:9A:49:9C:8C:7F:52:03:50:8A:94:B7:0D:72:17:12:B4:2E:4B
```

El equipo corrigió la autorización en Google Cloud y se comprobó después que la cartografía carga correctamente en el móvil. Para otras compilaciones sigue siendo necesario habilitar **Maps SDK for Android**, mantener la facturación activa y autorizar la pareja paquete + SHA-1 correspondiente.

La API de Respira es independiente del mapa base de Google. Para probar desde un móvil físico se ha sustituido la dirección local del emulador (`10.0.2.2:18862`) por la API publicada en Plesk, comprobando la conexión y el inicio de sesión. Este ajuste está en `local.properties`, que no se publica en Git.

### Zonas compartidas entre Android y Web

Android utiliza `ZonasCosta`, con los mismos ocho identificadores, contornos, anclas y nombres de `web/js/grau-platja.js`. Las respuestas de la API actualizan únicamente las mediciones de los identificadores coincidentes; no sustituyen la geografía por los contornos del prototipo.

En la comprobación del 19 de noviembre, la API publicada todavía devuelve las diez zonas antiguas y ninguna de las ocho costeras. Por eso el mapa muestra las zonas reales con **Sin datos**. La migración del catálogo del servidor está en `servidor/bd/actualizar_zonas_costeras.sql`; la actualización de la app no ejecuta esa migración remota ni traslada lecturas entre zonas diferentes.

## Configuración común en Google Cloud

La persona que administre Google Cloud debe seleccionar `respira-gti`, comprobar que tiene facturación habilitada y habilitar **Maps SDK for Android** y **Maps JavaScript API** en la biblioteca de APIs. Habilitar una API y autorizarla en una clave son pasos distintos. Véanse las guías oficiales de [Android](https://developers.google.com/maps/documentation/android-sdk/get-api-key) y [Web](https://developers.google.com/maps/documentation/javascript/get-api-key).

En **APIs y servicios → Credenciales**, configurar dos claves independientes con estas restricciones:

| Uso | Restricción de aplicación | Restricción de API |
| --- | --- | --- |
| Android | Aplicaciones Android: paquete `com.nodia.respira` y SHA-1 del certificado que firma la aplicación instalada | Solo **Maps SDK for Android** |
| Web local | Sitios web / referentes HTTP: `http://127.0.0.1:5500/*` | Solo **Maps JavaScript API** |

Una clave restringida a Android no sirve como clave Web. Para utilizar también `localhost`, añadir expresamente `http://localhost:5500/*`: es un host diferente de `127.0.0.1`. Autorizar otros puertos o dominios solo cuando se utilicen. Para un despliegue público, usar una clave Web separada y restringida a sus dominios HTTPS. Estas medidas siguen las [recomendaciones de seguridad de Google Maps Platform](https://developers.google.com/maps/api-security-best-practices).

## Android: configuración de cada integrante

### Cómo lee la clave el proyecto

- [`android/app/build.gradle.kts`](../android/app/build.gradle.kts) lee la propiedad `MAPS_API_KEY` de `android/local.properties` y la pasa al marcador de manifiesto del mismo nombre.
- [`AndroidManifest.xml`](../android/app/src/main/AndroidManifest.xml) la recibe en `com.google.android.geo.API_KEY` mediante `${MAPS_API_KEY}`.
- El identificador de aplicación es **`com.nodia.respira`**. La pantalla del mapa se implementa en [`MapaFragment.java`](../android/app/src/main/java/com/nodia/respira/ui/MapaFragment.java).

No hay que escribir la clave directamente en el manifiesto ni en el código Java o Gradle.

### Obtener y registrar el SHA-1

El **SHA-1 es la huella del certificado de firma**, no la clave API. Google utiliza la pareja **paquete + SHA-1** para identificar la aplicación autorizada. Cada integrante suele tener su propio certificado de depuración, por lo que debe registrar su huella aunque el paquete sea el mismo.

Con el entorno Android preparado, abrir una terminal PowerShell en la carpeta `android` y ejecutar:

```powershell
.\gradlew.bat :app:signingReport
```

También se puede ejecutar la tarea `signingReport` desde la ventana Gradle de Android Studio. Localizar la variante **`debug`** y su campo **`SHA1`**; no confundirlo con `SHA-256`. Si todavía no existe el almacén de depuración, realizar primero una compilación de depuración desde Android Studio y repetir la consulta.

Facilitar al administrador únicamente el paquete y la huella necesaria para añadir otra entrada a las aplicaciones Android permitidas, conservando las entradas de los compañeros. No compartir el archivo de firma privado.

Una versión `release` puede tener otro certificado. Si se distribuye mediante Play App Signing, registrar el SHA-1 del **certificado de firma de la aplicación** de Google Play, que puede diferir del certificado de subida. Véase la [guía oficial de certificados y `signingReport`](https://developers.google.com/android/guides/client-auth).

### Preparar la configuración local y abrir el mapa

1. Abrir la carpeta `android` en Android Studio y preparar el SDK requerido por el proyecto. Utilizar un dispositivo con servicios de Google Play o un emulador con imagen Google APIs / Google Play y conexión a Internet.
2. Si no existe `android/local.properties`, partir de [`android/local.properties.example`](../android/local.properties.example). Si ya existe, conservar su contenido, especialmente `sdk.dir` y cualquier configuración del servidor.
3. Añadir o actualizar únicamente esta propiedad con la clave Android autorizada, obtenida por un canal privado del equipo:

   ```properties
   MAPS_API_KEY=pon_aqui_tu_clave_de_maps_sdk_for_android
   ```

4. Completar el registro del SHA-1 de la variante que se va a ejecutar.
5. Sincronizar Gradle y volver a compilar y ejecutar la aplicación. La clave se incorpora durante la compilación; cambiar el archivo no actualiza una aplicación ya instalada.
6. Abrir la pestaña **Mapa** y comprobar que aparecen las calles y la cartografía de Google y que se puede mover y ampliar el mapa.

## Web: configuración de cada integrante

### Cómo lee la clave el proyecto

[`web/index.html`](../web/index.html) carga `js/config.js` antes de [`web/js/mapa-google.js`](../web/js/mapa-google.js). Este último obtiene la clave de `window.RESPIRA_CONFIG.MAPS_API_KEY` y carga Maps JavaScript API.

Si falta la clave o falla Google Maps, la vista avisa y mantiene la lista de zonas disponible.

### Preparar la configuración local y abrir el mapa

1. Si no existe `web/js/config.js`, crear una copia local de [`web/js/config.example.js`](../web/js/config.example.js) con ese nombre. Si ya existe, conservar sus demás propiedades.
2. Dentro del objeto `window.RESPIRA_CONFIG`, sustituir el valor de `MAPS_API_KEY` por la clave Web autorizada. La propiedad de la plantilla tiene este formato:

   ```javascript
   MAPS_API_KEY: 'pon_aqui_tu_clave_de_maps_javascript_api',
   ```

   Es un fragmento del objeto, no el contenido completo del archivo. `MAP_ID` puede permanecer vacío para el mapa básico; el código permite proporcionar un ID de mapa vectorial para las opciones de inclinación y giro. `MAP_ID` no sustituye a la clave API.
3. Servir la carpeta `web` mediante un servidor HTTP local, por ejemplo Live Server de VS Code, con el puerto **5500**. Abrir **`http://127.0.0.1:5500/`** si `web` es la raíz del servidor, o **`http://127.0.0.1:5500/web/`** si se sirve desde la raíz del repositorio.
4. Recargar la página y abrir su vista de mapa. Comprobar que carga la cartografía de Google, con calles y controles de navegación.

Evitar abrir `index.html` mediante `file://`: las restricciones Web necesitan un referente HTTP compatible. La URL, incluido el puerto, debe coincidir con lo autorizado en Google Cloud. Véase la [guía de restricciones para sitios web](https://developers.google.com/maps/api-security-best-practices).

La propiedad `API_URL` configura el servidor de Respira y es independiente de `MAPS_API_KEY`. Mostrar el mapa base de Google no requiere configurar la base de datos local; las mediciones y las demás funciones del proyecto deben verificarse por separado.

## Conservación de las credenciales

El [`.gitignore`](../.gitignore) del repositorio ya excluye `android/local.properties` y `web/js/config.js`. Cada integrante mantiene sus valores localmente; los archivos `.example` y esta documentación deben contener solo marcadores de posición.

No incluir claves reales en commits, incidencias, capturas, mensajes públicos ni registros compartidos. No forzar la incorporación de estos archivos con `git add -f`. Al compartir un error, omitir los valores de las credenciales y las URL de solicitudes que los contengan.

La clave Web es visible para el navegador durante su uso, y la de Android se incorpora a la aplicación. Excluirlas de Git evita publicarlas en el repositorio, pero no reemplaza las restricciones de aplicación y API. Véase la [guía de protección de claves](https://developers.google.com/maps/api-security-best-practices).

## Resolución de problemas

| Síntoma | Comprobación |
| --- | --- |
| Android indica que falta la clave | Revisar localmente la propiedad `MAPS_API_KEY` y recompilar la aplicación. |
| Android muestra un mapa vacío o un fallo de autorización | Comprobar el paquete, el SHA-1 de la aplicación instalada, la API habilitada y las restricciones de la clave Android. |
| Web muestra un aviso en el mapa | Comprobar que se sirve `js/config.js`, que define `window.RESPIRA_CONFIG` y que no conserva el marcador de posición; revisar también posibles fallos de red. |
| `RefererNotAllowedMapError` | Comprobar protocolo, host y puerto, especialmente `127.0.0.1` frente a `localhost`. |
| `ApiNotActivatedMapError` | Habilitar Maps JavaScript API en el proyecto al que pertenece la clave. |
| `BillingNotEnabledMapError` | Revisar la facturación del proyecto correspondiente. |
| Un compañero ve el mapa y otro no | Comparar la variante y el SHA-1 en Android, o la dirección local autorizada en Web, sin intercambiar claves en registros. |

Los fallos de autorización Android se describen en la [guía de inicio del SDK](https://developers.google.com/maps/documentation/android-sdk/start). Para Web, consultar el [catálogo oficial de errores](https://developers.google.com/maps/documentation/javascript/error-messages). Tras habilitar APIs o actualizar restricciones, dejar unos minutos para su propagación y repetir la comprobación.
