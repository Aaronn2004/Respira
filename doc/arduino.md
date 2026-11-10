# Nodo sensor de Respira (Arduino)

Sketch: `arduino/NodoRespira/`. Placa: **SparkFun Pro nRF52840 Mini** con el núcleo *Adafruit nRF52* (el mismo del Sprint 0, versión 1.7.0). Mide **de verdad** (sin valores simulados) y anuncia cada dato como iBeacon.

## Cableado

| Sensor | Pin del sensor | Pin de la placa |
|---|---|---|
| SPEC ULPSM-O3 968-046 (ozono) | Vgas | **A3** |
| | Vref | **A4** |
| | V+ / GND | 3V3 / GND |
| DHT11 (temperatura) | DATA | **6**, con resistencia de 4,7–10 kΩ entre DATA y 3V3 (si tu módulo ya la lleva, no hace falta) |
| | VCC / GND | 3V3 / GND |
| KY-037 (sonido) | AO | **A2** |
| | + / G | **3V3** (¡no 5 V: su salida pasaría de 3,3 V y podría dañar la entrada!) / GND |

Todos los pines se cambian en `Configuracion.h`, junto con el código del nodo (`RSP-4F7K`) y la calibración. El resto del código no se toca.

## Librerías

Arduino IDE › Gestor de librerías: **«DHT sensor library»** (Adafruit) y, cuando lo pregunte, **«Adafruit Unified Sensor»**. *Ahora mismo no las tienes instaladas.* `bluefruit.h` ya viene con el núcleo nRF52.

Placa: Herramientas › Placa › Adafruit nRF52 › *SparkFun Pro nRF52840 Mini*.

## Cómo mide

- **Ozono**: el SPEC da una tensión `Vgas − Vref` proporcional a la concentración. `ppm = (Vgas − (Vref + cero)) / M` (si sale negativo, 0), con `M = sensibilidad (nA/ppm) × 499 kV/A`. Al arrancar se calibra el cero con 50 muestras: **enciéndelo en aire limpio** (exterior, lejos de impresoras o fotocopiadoras); tarda unos 3 s y, al acabar, el LED se enciende medio segundo y el monitor serie muestra el «Offset de cero O3». Cambia `O3_SENSIBILIDAD_NA_PPM` (−42,54) por el número de la etiqueta de **tu** sensor. El sensor necesita calentarse: los primeros minutos tras conectarlo no son fiables.
- **Temperatura**: DHT11 (0–50 °C, ±2 °C). Si la lectura falla, **no se envía nada** (nunca un valor inventado).
- **Ruido**: el KY-037 no da decibelios. Se mide la tensión pico a pico en 8 ventanas de 125 ms y se convierte con un punto de calibración: `dB = DB_REF + 20·log10(Vpp / VPP_REF)`. Para calibrarlo: pon junto al micrófono el móvil con una app de sonómetro y un ruido constante, y copia en `Configuracion.h` los dB del móvil (`RUIDO_DB_REF`) y el `Vpp` que sale en el monitor serie (`RUIDO_VPP_REF`). Son **dB aproximados**, no los de un sonómetro homologado.

## Qué anuncia (iBeacon)

| Campo | Valor |
|---|---|
| UUID (16 bytes) | `RESPIRA-` + código del nodo, p. ej. `RESPIRA-RSP-4F7K` |
| major | `(tipo << 8) + contador` · tipo 11 = O₃, 12 = temperatura, 13 = ruido |
| minor | entero de 16 bits: O₃ en ppm × 1000 · temperatura en °C × 10 **con signo** · ruido en dB × 10 |
| TX power | −53 dBm a 1 m |

Cada magnitud se anuncia 2 s; un juego completo (O₃, temperatura y ruido, con el mismo contador) sale cada ~6 s. La app solo escucha el UUID del sensor que el usuario ha tomado prestado.

## Monitor serie (115200 baudios)

Al arrancar: el código del nodo y el offset de cero del ozono. Después, una línea por medida: `#7  O3 = 0.042 ppm  T = 23.4 C  Ruido = 58.1 dB (Vpp = 0.0712 V)`. Si el DHT11 falla, sale `ERROR DHT11 (no se envía)`.

## Tests

Sin placa, con mocks (`arduino/tests/mocks`):

```bash
g++ -std=c++17 -I arduino/tests/mocks -I arduino/NodoRespira -x c++ arduino/tests/NodoRespiraTest.cpp -o test && ./test
```

26 tests: conversiones (ppm, °C con signo, dB), calibración del ozono, lectura fallida del DHT11 y formato del beacon.
