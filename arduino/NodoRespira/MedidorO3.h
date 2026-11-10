// -*- mode: c++ -*-
// ==============================================================
// Fichero:     MedidorO3.h
// Descripción: Lee el sensor analógico de ozono SPEC ULPSM-O3 968-046
//              (Vgas y Vref) y lo convierte a ppm. Al arrancar hace una
//              calibración de cero, como en las prácticas.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef MEDIDOR_O3_H_INCLUIDO
#define MEDIDOR_O3_H_INCLUIDO

#include "Configuracion.h"
#include "Conversiones.h"

class MedidorO3 {
private:
  float offsetCero = 0.0f;   // Vgas0 - Vref0 en aire limpio (V)
  float m = Conversiones::voltiosPorPpm( Config::O3_SENSIBILIDAD_NA_PPM, Config::O3_GANANCIA_TIA_KV_A );

  float aVoltios( float lectura ) {
    return Conversiones::voltios( lectura, Config::ADC_VOLTIOS_FONDO, Config::ADC_MAXIMO );
  }

public:
  // --------------------------------------------------------------
  // iniciar() -->
  // Calibración de cero: media de N muestras de (Vgas - Vref).
  // OJO: haz el arranque en interior y lejos de impresoras o
  // fotocopiadoras; si calibras en la calle con ozono, ese ozono se
  // resta de todas las medidas siguientes.
  // --------------------------------------------------------------
  void iniciar() {
    analogReadResolution( 12 );
    float suma = 0.0f;
    for ( int i = 0; i < Config::O3_MUESTRAS_CALIBRACION; i++ ) {
      suma += aVoltios( analogRead( Config::PIN_O3_VGAS ) ) - aVoltios( analogRead( Config::PIN_O3_VREF ) );
      delay( 30 );
    }
    offsetCero = suma / Config::O3_MUESTRAS_CALIBRACION;
  }

  float getOffsetCero() { return offsetCero; }

  // --------------------------------------------------------------
  // medir() --> R   (ppm, media de N muestras)
  // --------------------------------------------------------------
  float medir() {
    float sumaGas = 0.0f, sumaRef = 0.0f;
    for ( int i = 0; i < Config::O3_MUESTRAS_MEDIDA; i++ ) {
      sumaGas += analogRead( Config::PIN_O3_VGAS );
      sumaRef += analogRead( Config::PIN_O3_VREF );
      delay( 5 );
    }
    float vGas = aVoltios( sumaGas / Config::O3_MUESTRAS_MEDIDA );
    float vRef = aVoltios( sumaRef / Config::O3_MUESTRAS_MEDIDA );
    return Conversiones::ppmO3( vGas, vRef, offsetCero, m );
  }
};

#endif
