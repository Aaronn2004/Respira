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

## Datos simulados

Mientras no haya sensores por la calle, el servidor genera datos simulados (`origen = 'simulado'`) para que todo lo anterior se pueda ver y probar:

- Todas las zonas tienen datos, salvo **Oeste**, que sigue «Sin datos» a propósito, y **Estación**, que va 3 h atrasada («Antiguo»). Así se ven los tres estados.
- Los datos reales siempre mandan sobre los simulados.
- Se apaga con `'simulacion' => false` en `config.php`.
