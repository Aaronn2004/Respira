// -*- mode: c++ -*-
// ==============================================================
// Fichero:     Conversiones.h
// Descripción: Cuentas puras (sin hardware), para poder probarlas en el
//              PC: de lecturas del ADC a magnitudes físicas y de
//              magnitudes a los enteros que viajan en el iBeacon.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef CONVERSIONES_H_INCLUIDO
#define CONVERSIONES_H_INCLUIDO

#include <math.h>
#include <stdint.h>

namespace Conversiones {

  // Tipos de medida (byte alto del major del iBeacon). Los mismos que
  // en el Sprint 0, la app y el servidor.
  const uint8_t TIPO_O3 = 11;
  const uint8_t TIPO_TEMPERATURA = 12;
  const uint8_t TIPO_RUIDO = 13;

  const float RUIDO_DB_MIN = 30.0f;   // por debajo, el KY-037 solo ve ruido eléctrico
  const float RUIDO_DB_MAX = 130.0f;

  // --------------------------------------------------------------
  // lectura: R, fondo: R, maximo: R --> voltios() --> R
  // --------------------------------------------------------------
  inline float voltios( float lectura, float fondo, float maximo ) {
    return lectura * fondo / maximo;
  }

  // --------------------------------------------------------------
  // # Convierte nA/ppm y kV/A a V/ppm.
  // sensibilidadNA: R, gananciaKV: R --> voltiosPorPpm() --> R
  // --------------------------------------------------------------
  inline float voltiosPorPpm( float sensibilidadNA, float gananciaKV ) {
    return sensibilidadNA * gananciaKV * 1e-6f;
  }

  // --------------------------------------------------------------
  // # Resta el cero calibrado y satura en cero el ruido que produciría ppm negativas.
  // vGas: R, vRef: R, offsetCero: R, m: R --> ppmO3() --> R
  // --------------------------------------------------------------
  inline float ppmO3( float vGas, float vRef, float offsetCero, float m ) {
    float ppm = ( vGas - ( vRef + offsetCero ) ) / m;
    return ppm < 0.0f ? 0.0f : ppm;
  }

  // --------------------------------------------------------------
  // # Aplica 20·log10 al cociente de amplitudes y acota la estimación a 30..130 dB.
  // vpp: R, vppRef: R, dbRef: R --> decibelios() --> R
  // --------------------------------------------------------------
  inline float decibelios( float vpp, float vppRef, float dbRef ) {
    if ( vpp <= 0.0f || vppRef <= 0.0f ) {
      return RUIDO_DB_MIN;
    }
    float db = dbRef + 20.0f * log10f( vpp / vppRef );
    if ( db < RUIDO_DB_MIN ) return RUIDO_DB_MIN;
    if ( db > RUIDO_DB_MAX ) return RUIDO_DB_MAX;
    return db;
  }

  // --------------------------------------------------------------
  // # Redondea antes de saturar al intervalo del entero transmitido.
  // valor: R, factor: R, minimo: Z, maximo: Z --> aEntero() --> Z
  // --------------------------------------------------------------
  inline int16_t aEntero( float valor, float factor, long minimo, long maximo ) {
    long v = lroundf( valor * factor );
    if ( v < minimo ) v = minimo;
    if ( v > maximo ) v = maximo;
    return (int16_t) v;
  }

  // El minor del iBeacon lleva el valor como entero de 16 bits:
  // --------------------------------------------------------------
  // # El minor transmite milésimas de ppm.
  // ppm: R --> minorO3() --> N
  // --------------------------------------------------------------
  inline int16_t minorO3( float ppm ) { return aEntero( ppm, 1000.0f, 0, 32767 ); }            // ppm x 1000
  // --------------------------------------------------------------
  // # El minor conserva el signo y transmite décimas de grado.
  // c: R --> minorTemperatura() --> Z
  // --------------------------------------------------------------
  inline int16_t minorTemperatura( float c ) { return aEntero( c, 10.0f, -32768, 32767 ); }    // décimas de °C
  // --------------------------------------------------------------
  // # El minor transmite décimas de dB.
  // db: R --> minorRuido() --> N
  // --------------------------------------------------------------
  inline int16_t minorRuido( float db ) { return aEntero( db, 10.0f, 0, 32767 ); }             // décimas de dB

  // --------------------------------------------------------------
  // # El byte alto identifica la magnitud; el bajo identifica el ciclo de medida.
  // tipo: N, contador: N --> major() --> N
  // --------------------------------------------------------------
  inline uint16_t major( uint8_t tipo, uint8_t contador ) {
    return ( (uint16_t) tipo << 8 ) + contador;
  }
}

#endif
