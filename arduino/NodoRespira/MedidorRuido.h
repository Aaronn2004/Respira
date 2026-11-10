// -*- mode: c++ -*-
// ==============================================================
// Fichero:     MedidorRuido.h
// Descripción: Ruido con el KY-037 (salida analógica AO). Mide la
//              tensión pico a pico en ventanas cortas y la pasa a dB
//              con el punto de calibración de Configuracion.h.
//              Son dB APROXIMADOS: el KY-037 no es un sonómetro.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef MEDIDOR_RUIDO_H_INCLUIDO
#define MEDIDOR_RUIDO_H_INCLUIDO

#include "Configuracion.h"
#include "Conversiones.h"

class MedidorRuido {
private:
  float ultimoVpp = 0.0f;

  // --------------------------------------------------------------
  // vppVentana() --> R   (V pico a pico durante RUIDO_VENTANA_MS)
  // --------------------------------------------------------------
  float vppVentana() {
    int minimo = 4095, maximo = 0;
    unsigned long inicio = millis();
    while ( millis() - inicio < Config::RUIDO_VENTANA_MS ) {
      int v = analogRead( Config::PIN_RUIDO_AO );
      if ( v < minimo ) minimo = v;
      if ( v > maximo ) maximo = v;
    }
    if ( maximo < minimo ) return 0.0f;
    return Conversiones::voltios( maximo - minimo, Config::ADC_VOLTIOS_FONDO, Config::ADC_MAXIMO );
  }

public:
  // --------------------------------------------------------------
  // medir() --> R   (dB aproximados, 30..130)
  // Media de RUIDO_VENTANAS ventanas en energía (no en dB).
  // --------------------------------------------------------------
  float medir() {
    float sumaCuadrados = 0.0f;
    for ( int i = 0; i < Config::RUIDO_VENTANAS; i++ ) {
      float vpp = vppVentana();
      sumaCuadrados += vpp * vpp;
    }
    ultimoVpp = sqrtf( sumaCuadrados / Config::RUIDO_VENTANAS );
    return Conversiones::decibelios( ultimoVpp, Config::RUIDO_VPP_REF, Config::RUIDO_DB_REF );
  }

  // Para calibrar: el Vpp de la última medida (se escribe por el monitor serie)
  float getUltimoVpp() { return ultimoVpp; }
};

#endif
