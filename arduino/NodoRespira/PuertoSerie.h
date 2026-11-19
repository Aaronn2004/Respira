// -*- mode: c++ -*-
// ==============================================================
// Fichero:     PuertoSerie.h
// Descripción: Escritura por el puerto serie USB (depuración y calibración).
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef PUERTO_SERIE_H_INCLUIDO
#define PUERTO_SERIE_H_INCLUIDO

#include <Arduino.h>
#include <Adafruit_TinyUSB.h> // Serial por USB en el nRF52840

// # Centraliza la salida de diagnóstico por USB.
class PuertoSerie {
public:
  // --------------------------------------------------------------
  // # effects: abre el puerto serie USB.
  // + baudios: N
  //     -->
  //         PuertoSerie() --x
  // --------------------------------------------------------------
  PuertoSerie( unsigned long baudios ) {
    Serial.begin( baudios );
  }

  // --------------------------------------------------------------
  // # effects: escribe en el puerto serie.
  // + mensaje: Text | Z | R
  //     -->
  //         escribir() --x
  // --------------------------------------------------------------
  template <typename T>
  void escribir( T mensaje ) {
    Serial.print( mensaje );
  }

  // --------------------------------------------------------------
  // # effects: escribe en el puerto serie con precisión fija.
  // + valor: R, decimales: N
  //     -->
  //         escribir() --x
  // --------------------------------------------------------------
  void escribir( float valor, int decimales ) {
    Serial.print( valor, decimales );
  }
};

#endif
