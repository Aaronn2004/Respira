# Especificaciones del sprint: qué cubre cada punto y criterios cambiados

Este documento recoge **solo lo que cambia o se añade** en este sprint respecto a las historias de usuario (APP-01…10, WEB-01…04). El resto de criterios siguen como estaban en el Product Backlog.

## Puntos del sprint y dónde están

| Punto | App | Web | Servidor |
|---|---|---|---|
| Efectos nocivos en las especificaciones | Ficha de zona: aviso por magnitud en Medio/Alto + botón «Efectos en la salud» | Ficha de zona del mapa: lo mismo, con enlace a «Cómo funciona» | — |
| Historial: alertas y picos | Ficha de zona › «Alertas y picos» | Historial › tarjeta «Alertas y picos» | `GET /zonas/{id}/serie` › `eventos` |
| Mediciones valoradas, historial de cada zona | Insignia Bajo/Medio/Alto en cada valor y en cada alerta o pico | Igual | Límites en `Logica::LIMITES` |
| Separar «Iniciar sesión» y «Crear cuenta» | Pantallas separadas (Login y Registro) | Solo acceso de trabajadores | `POST /sesiones` · `POST /usuarios` |
| Los trabajadores los registra administración | — | Panel › Trabajadores (enlace de activación por correo y en pantalla) | `POST /panel/trabajadores` (solo admin) |
| Interpolar (aproximado) | Gráfica: línea de puntos | Gráfica: línea de puntos | `serie` › `aprox` |
| Promedio de las mediciones en la info de cada zona | «Media de las últimas 24 h» | «Media de las últimas 24 h» | `GET /zonas` › `media_24h` |
| Portada: parques de mejor a peor, mejor medida | — | Portada › «Los parques ahora» | `GET /zonas` › `mejor` |
| Datos para probarlo todo | — | — | `Simulador.php` (ver `doc/servidor.md`) |

## Efectos en la salud

Cuando una magnitud de la zona está en **Medio** o **Alto**, la ficha de la zona enseña:

- qué te puede pasar: el paso «Molesta» si está en Medio y el paso «Daña» si está en Alto (los mismos textos de la guía de efectos);
- qué hacer;
- quién es más sensible;
- un enlace a la guía completa.

Si todo está en Bajo, no se enseña nada (no hay nada de lo que avisar).

## Criterios cambiados

**APP-03 / APP-04 (ficha de zona en la app).** Se añade:
- Aparece la media de las últimas 24 h de cada magnitud.
- Si alguna magnitud está en Medio o Alto, aparecen sus efectos, qué hacer y un acceso a «Efectos en la salud».
- Debajo de la gráfica aparecen las alertas del periodo (ratos seguidos en Alto, o en Medio si no hubo Alto) y los 3 picos más altos, con su hora.

**APP-03.2 y WEB-02.3 (huecos en la gráfica).** Antes: «los huecos sin datos se ven en gris y no se rellenan». Ahora:
- Un hueco **corto** (hasta 2 h) con datos a los dos lados se aproxima en línea recta y se dibuja con **línea de puntos**. No cuenta para el mínimo, la media ni el máximo.
- Un hueco **largo**, o sin dato a uno de los lados, sigue en **gris** («sin datos») y no se rellena.

**APP-10.3 (notificación de alerta).** La notificación dice la zona, la magnitud, el valor **y la hora** de la medición.

**WEB-02 (historial).** Se añade la tarjeta «Alertas y picos» con los mismos datos que la app.

**WEB-04.3.** Se quita el recuadro «Descarga la app». El criterio pasa a ser: la portada explica qué es Respira y lleva al mapa y a «Cómo funciona»; no enlaza a ninguna descarga mientras la app no esté publicada.

**Inicio de sesión (app y panel).** Se quita el bloqueo de 15 minutos tras 5 contraseñas mal: ahora solo se avisa de «Correo o contraseña incorrectos».

**Préstamos (panel).** Al prestar se elige el punto de recogida. El plazo para devolver el sensor es de 10 días desde la hora exacta de la recogida (por ejemplo, del 10 de enero a las 12:00 al 20 de enero a las 12:00), en cualquier punto de reparto. La devolución se registra como «Correcto» o «Defectuoso» (no se ha entregado en buenas condiciones: el sensor pasa a mantenimiento). Pasada la fecha límite, el panel lo marca «Fuera de plazo» y la app avisa a la persona.

**WEB-04 (portada).** La portada se centra en los parques:
- Sección «Los parques ahora, de mejor a peor»: todos los parques con datos de la última hora, ordenados por su estado general (ver abajo). Cada tarjeta lleva la silueta real del parque (su polígono) con el color del estado general y la **mejor medida** (la más baja) de la última hora de ozono, temperatura y ruido, cada una en verde, amarillo o rojo según su nivel. Al tocarla se abre el parque en el mapa.
- «Qué significa cada color»: pestañas Ozono / Calor / Ruido con la escala, la media de la ciudad, los efectos y qué hacer.
- Sin enlaces a descargas (WEB-04.3).

**WEB-01 (mapa): estado general.** El selector del mapa tiene «General» (por defecto) además de Ozono, Temperatura y Ruido. El estado general de una zona es la media de los niveles de sus medidas (Bajo 0, Medio 1, Alto 2), redondeada: si ninguna medida está en Bajo, la zona no sale en verde; un empate Bajo/Medio sale Medio. Con «General», la etiqueta de la zona dice Bajo, Medio o Alto en lugar de un número.

**Estadísticas del recorrido (app › Mi sensor y Puntos).** Con datos del servidor (`GET /yo` › `recorrido`):
- Durante el recorrido: mediciones enviadas, últimos valores, minutos válidos (y en parques), **distancia andada hoy**, **tiempo con el sensor** (de la primera a la última medición) y **parques por los que has pasado**.
- Al terminar: un **resumen del día** (tiempo, distancia, lecturas, minutos válidos y descartados, puntos provisionales y parques) y la fecha límite para devolver el sensor.
- **Tus últimos recorridos**: los 7 últimos días con sensor, con duración, distancia, lecturas, horas, parques y puntos. El historial de Puntos también enseña la duración y la distancia de cada día.
- La distancia solo suma los tramos válidos: no cuentan los huecos de más de 15 min ni los tramos a velocidad de vehículo.

**Seguimiento de parques (APP-24 a APP-26).** Los parques que sigues y sus alertas se guardan **en la cuenta** (tablas `favorito` y `alerta`) y se recuperan al entrar en cualquier móvil. Sin sesión se guardan en el móvil y, al iniciar sesión, se suman a la cuenta. Al cerrar sesión se quitan del móvil (siguen en la cuenta). Las alertas las comprueba el móvil.

**Mediciones.** Solo se guardan las que traen ubicación, caen dentro de un parque o barrio y están dentro del rango del sensor; el resto se rechazan.

## Datos simulados

Mientras no haya sensores por la calle, el servidor genera datos simulados (`origen = 'simulado'`) para que todo lo anterior se pueda ver y probar:

- Todas las zonas tienen datos, salvo **Oeste**, que sigue «Sin datos» a propósito, y **Estación**, que va 3 h atrasada («Antiguo»). Así se ven los tres estados.
- Los datos reales siempre mandan sobre los simulados.
- Se apaga con `'simulacion' => false` en `config.php`.
