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

class PuertoSerie {
public:
  // baudios: N --> PuertoSerie()
  PuertoSerie( unsigned long baudios ) {
    Serial.begin( baudios );
  }

  // mensaje: Text | Z --> escribir() -->
  template <typename T>
  void escribir( T mensaje ) {
    Serial.print( mensaje );
  }

  // valor: R, decimales: N --> escribir() -->
  void escribir( float valor, int decimales ) {
    Serial.print( valor, decimales );
  }
};

#endif
