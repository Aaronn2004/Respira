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

// MedidorTemperatura <>--- DHT
// # Encapsula las lecturas del DHT11 y comunica si son válidas.
class MedidorTemperatura {
private:
  DHT elDHT { Config::PIN_DHT11, DHT11 };

public:
  // --------------------------------------------------------------
  // # effects: inicializa el sensor DHT11.
  // +       iniciar() -->
  // --------------------------------------------------------------
  void iniciar() {
    elDHT.begin();
  }

  // --------------------------------------------------------------
  // # ok es una salida lógica: si falla el sensor, temperatura vale cero y no debe anunciarse.
  // # effects: lee el DHT11 y actualiza su estado de lectura.
  // +       medir() -->
  //     <--
  //     (temperatura: R, ok: B)
  // --------------------------------------------------------------
  float medir( bool & ok ) {
    float c = elDHT.readTemperature();
    ok = !isnan( c );
    return ok ? c : 0.0f;
  }
};

#endif
