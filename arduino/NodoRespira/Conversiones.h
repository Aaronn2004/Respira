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
  // sensibilidad: R (nA/ppm), ganancia: R (kV/A) --> voltiosPorPpm() --> R (V/ppm)
  // --------------------------------------------------------------
  inline float voltiosPorPpm( float sensibilidadNA, float gananciaKV ) {
    return sensibilidadNA * gananciaKV * 1e-6f;
  }

  // --------------------------------------------------------------
  // vGas: R, vRef: R, offsetCero: R, m: R (V/ppm) --> ppmO3() --> R >= 0
  //   ppm = (Vgas - (Vref + offsetCero)) / M
  // (los valores negativos por ruido cerca de 0 se dejan en 0)
  // --------------------------------------------------------------
  inline float ppmO3( float vGas, float vRef, float offsetCero, float m ) {
    float ppm = ( vGas - ( vRef + offsetCero ) ) / m;
    return ppm < 0.0f ? 0.0f : ppm;
  }

  // --------------------------------------------------------------
  // vpp: R, vppRef: R, dbRef: R --> decibelios() --> R (30..130)
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
  // valor: R, factor: R, minimo: Z, maximo: Z --> aEntero() --> Z (redondeado y acotado)
  // --------------------------------------------------------------
  inline int16_t aEntero( float valor, float factor, long minimo, long maximo ) {
    long v = lroundf( valor * factor );
    if ( v < minimo ) v = minimo;
    if ( v > maximo ) v = maximo;
    return (int16_t) v;
  }

  // El minor del iBeacon lleva el valor como entero de 16 bits:
  inline int16_t minorO3( float ppm ) { return aEntero( ppm, 1000.0f, 0, 32767 ); }            // ppm x 1000
  inline int16_t minorTemperatura( float c ) { return aEntero( c, 10.0f, -32768, 32767 ); }    // décimas de °C
  inline int16_t minorRuido( float db ) { return aEntero( db, 10.0f, 0, 32767 ); }             // décimas de dB

  // --------------------------------------------------------------
  // tipo: N, contador: N --> major() --> N   (tipo << 8) + contador
  // --------------------------------------------------------------
  inline uint16_t major( uint8_t tipo, uint8_t contador ) {
    return ( (uint16_t) tipo << 8 ) + contador;
  }
}

#endif
