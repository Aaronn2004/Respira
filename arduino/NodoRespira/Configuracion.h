// -*- mode: c++ -*-
// ==============================================================
// Fichero:     Configuracion.h
// Descripción: Todo lo que hay que tocar para montar un nodo: código
//              del sensor, pines y calibración. El resto del código no
//              se toca.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef CONFIGURACION_H_INCLUIDO
#define CONFIGURACION_H_INCLUIDO

namespace Config {

  // ---- Identidad del nodo ----
  // Tiene que existir en la tabla nodo de la BD (esquema.sql) y es el
  // código que escribe el voluntario en la app. UUID del beacon =
  // "RESPIRA-" + CODIGO_NODO (16 caracteres justos).
  const char CODIGO_NODO[] = "RSP-4F7K";

  // ---- Pines (SparkFun Pro nRF52840 Mini: pin N de Arduino = P0.N) ----
  // SPEC ULPSM-O3 968-046: Vgas -> A3, Vref -> A4 (Vtemp -> A5, no se usa)
  const int PIN_O3_VGAS = A3;
  const int PIN_O3_VREF = A4;
  // DHT11: DATA -> pin 6, con resistencia de 4,7 a 10 kΩ entre DATA y 3V3
  // (si tu módulo DHT11 ya la trae en la placa, no hace falta)
  const int PIN_DHT11 = 6;
  // KY-037: AO (salida analógica) -> A2. ALIMÉNTALO A 3V3, NO A 5 V:
  // a 5 V su salida pasaría de 3,3 V y podría dañar la entrada de la placa.
  const int PIN_RUIDO_AO = A2;

  // ---- ADC ----
  // El ADC del nRF52840 con la referencia por defecto mide de 0 a 3,6 V
  // en 12 bits (0..4095).
  const float ADC_VOLTIOS_FONDO = 3.6f;
  const float ADC_MAXIMO = 4095.0f;

  // ---- Ozono (SPEC ULPSM-O3 968-046) ----
  // Sensibilidad del sensor (código de la etiqueta, nA/ppm) x ganancia del
  // TIA (kV/A). M = -42.54 nA/ppm * 499 kV/A = -0.02122746 V/ppm.
  // CAMBIA -42.54 por el código impreso en TU sensor.
  const float O3_SENSIBILIDAD_NA_PPM = -42.54f;
  const float O3_GANANCIA_TIA_KV_A = 499.0f;
  // Calibración de cero al arrancar: N muestras en aire limpio
  const int O3_MUESTRAS_CALIBRACION = 50;
  const int O3_MUESTRAS_MEDIDA = 20;

  // ---- Ruido (KY-037) ----
  // El KY-037 NO da decibelios: da una tensión que sube con el sonido.
  // Se convierte con un punto de calibración:
  //   dB = RUIDO_DB_REF + 20·log10( Vpp / RUIDO_VPP_REF )
  // Cómo calibrar: pon el móvil con una app de sonómetro junto al
  // micrófono, haz un ruido constante (música, un ventilador), apunta los
  // dB del móvil en RUIDO_DB_REF y el «Vpp» que sale por el monitor serie
  // en RUIDO_VPP_REF. Gira el potenciómetro del KY-037 lo mínimo para que
  // en silencio el LED L2 quede apagado.
  const float RUIDO_DB_REF = 60.0f;   // dB del sonómetro en la calibración
  const float RUIDO_VPP_REF = 0.10f;  // Vpp medido en ese momento (V)
  const unsigned long RUIDO_VENTANA_MS = 125; // 1/8 s, como un sonómetro "rápido"
  const int RUIDO_VENTANAS = 8;               // se promedian 8 ventanas = 1 s

  // ---- Envío ----
  // Cada magnitud se anuncia este tiempo; con tres magnitudes, sale un
  // juego completo de datos cada ~6 s.
  const long TIEMPO_ANUNCIO_MS = 2000;
  const int8_t POTENCIA_TX_DBM = 4;
  const int8_t RSSI_A_1_METRO = -53;
}

#endif
