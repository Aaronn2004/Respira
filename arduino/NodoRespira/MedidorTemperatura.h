// -*- mode: c++ -*-
// ==============================================================
// Fichero:     MedidorTemperatura.h
// Descripción: Lee la temperatura del DHT11 (0 a 50 °C, ±2 °C).
//              Librería: «DHT sensor library» de Adafruit (y su
//              dependencia «Adafruit Unified Sensor»).
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef MEDIDOR_TEMPERATURA_H_INCLUIDO
#define MEDIDOR_TEMPERATURA_H_INCLUIDO

#include <DHT.h>
#include "Configuracion.h"

class MedidorTemperatura {
private:
  DHT elDHT { Config::PIN_DHT11, DHT11 };

public:
  // --------------------------------------------------------------
  // iniciar() -->
  // --------------------------------------------------------------
  void iniciar() {
    elDHT.begin();
  }

  // --------------------------------------------------------------
  // medir(), ok: B --> R   (°C)
  // Si el DHT11 no responde (cable suelto, sin pull-up...), ok = false
  // y NO se envía nada: nunca se inventa un valor.
  // --------------------------------------------------------------
  float medir( bool & ok ) {
    float c = elDHT.readTemperature();
    ok = !isnan( c );
    return ok ? c : 0.0f;
  }
};

#endif
