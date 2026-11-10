// -*- mode: c++ -*-
// ==============================================================
// Fichero:     NodoRespira.ino
// Descripción: Nodo sensor de Respira (SparkFun Pro nRF52840 Mini).
//              Mide de verdad ozono (SPEC ULPSM-O3), temperatura (DHT11)
//              y ruido (KY-037) y anuncia cada dato como iBeacon para
//              que lo recoja la app.
//
//   Cableado (ver Configuracion.h y doc/arduino.md):
//     SPEC ULPSM-O3  Vgas -> A3   Vref -> A4   VCC -> 3V3   GND -> GND
//     DHT11          DATA -> 6 (pull-up 10k a 3V3)   VCC -> 3V3   GND
//     KY-037         AO -> A2     + -> 3V3 (¡no 5 V!)   G -> GND
//
//   Librerías (Arduino IDE > Gestor de librerías):
//     «DHT sensor library» (Adafruit) + «Adafruit Unified Sensor»
//
//   Monitor serie a 115200 baudios: se ve cada medida y el Vpp del
//   micrófono para calibrar el ruido.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

#include <bluefruit.h>

#undef min
#undef max

#include "LED.h"
#include "PuertoSerie.h"
#include "Configuracion.h"
#include "Conversiones.h"
#include "MedidorO3.h"
#include "MedidorTemperatura.h"
#include "MedidorRuido.h"
#include "Publicador.h"

namespace Globales {
  LED elLED( LED_BUILTIN );
  PuertoSerie elPuerto( 115200 );
  Publicador elPublicador;
  MedidorO3 elOzono;
  MedidorTemperatura laTemperatura;
  MedidorRuido elRuido;
};

// --------------------------------------------------------------
// setup() -->
// --------------------------------------------------------------
void setup() {
  using namespace Globales;
  // No se espera al puerto serie: con batería y sin USB se quedaría parada.
  delay( 1500 );
  elPuerto.escribir( "\nRespira - nodo " );
  elPuerto.escribir( Config::CODIGO_NODO );
  elPuerto.escribir( "\nCalibrando el cero del ozono (2 s, en aire limpio)...\n" );

  elPublicador.encenderEmisora();
  elOzono.iniciar();
  laTemperatura.iniciar();

  elPuerto.escribir( "Offset de cero O3 = " );
  elPuerto.escribir( elOzono.getOffsetCero(), 4 );
  elPuerto.escribir( " V\n---- setup(): fin ----\n" );
  elLED.brillar( 500 );
}

namespace Loop {
  uint8_t contador = 0;
};

// --------------------------------------------------------------
// loop() -->
// mide las tres magnitudes y anuncia cada una TIEMPO_ANUNCIO_MS
// (el mismo contador en las tres: así el servidor sabe que son
// del mismo instante)
// --------------------------------------------------------------
void loop() {
  using namespace Loop;
  using namespace Globales;

  contador++;

  float ppm = elOzono.medir();
  bool tempOk = false;
  float grados = laTemperatura.medir( tempOk );
  float db = elRuido.medir();

  elPuerto.escribir( "\n#" );
  elPuerto.escribir( contador );
  elPuerto.escribir( "  O3 = " );
  elPuerto.escribir( ppm, 3 );
  elPuerto.escribir( " ppm  T = " );
  if ( tempOk ) {
    elPuerto.escribir( grados, 1 );
    elPuerto.escribir( " C" );
  } else {
    elPuerto.escribir( "ERROR DHT11 (no se envía)" );
  }
  elPuerto.escribir( "  Ruido = " );
  elPuerto.escribir( db, 1 );
  elPuerto.escribir( " dB (Vpp = " );
  elPuerto.escribir( elRuido.getUltimoVpp(), 4 );
  elPuerto.escribir( " V)\n" );

  elLED.brillar( 50 );

  elPublicador.publicar( Conversiones::TIPO_O3, Conversiones::minorO3( ppm ), contador, Config::TIEMPO_ANUNCIO_MS );
  if ( tempOk ) {
    elPublicador.publicar( Conversiones::TIPO_TEMPERATURA, Conversiones::minorTemperatura( grados ), contador, Config::TIEMPO_ANUNCIO_MS );
  }
  elPublicador.publicar( Conversiones::TIPO_RUIDO, Conversiones::minorRuido( db ), contador, Config::TIEMPO_ANUNCIO_MS );
}
