// -*- mode: c++ -*-
// ==============================================================
// Fichero:     EmisoraBLE.h
// Descripción: Envoltorio de la pila Bluefruit para emitir anuncios
//              iBeacon (el mismo del Sprint 0).
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef EMISORA_BLE_H_INCLUIDO
#define EMISORA_BLE_H_INCLUIDO

#include <bluefruit.h>

class EmisoraBLE {
private:
  const char * nombreEmisora;
  const uint16_t fabricanteID;
  const int8_t txPower; // dBm. Válidos en el nRF52840: -40,-20,-16,-12,-8,-4,0,2..8

public:
  // --------------------------------------------------------------
  // nombre: Text, fabricante: N, potencia: Z --> EmisoraBLE()
  // --------------------------------------------------------------
  EmisoraBLE( const char * nombre, uint16_t fabricante, int8_t potencia )
    : nombreEmisora( nombre ), fabricanteID( fabricante ), txPower( potencia ) {
  }

  // --------------------------------------------------------------
  // encenderEmisora() -->
  // --------------------------------------------------------------
  void encenderEmisora() {
    Bluefruit.begin();
    detenerAnuncio();
  }

  // --------------------------------------------------------------
  // detenerAnuncio() -->
  // --------------------------------------------------------------
  void detenerAnuncio() {
    if ( estaAnunciando() ) {
      Bluefruit.Advertising.stop();
    }
  }

  // --------------------------------------------------------------
  // estaAnunciando() --> B
  // --------------------------------------------------------------
  bool estaAnunciando() {
    return Bluefruit.Advertising.isRunning();
  }

  // --------------------------------------------------------------
  // uuid: [N]_16, major: N, minor: N, rssi: Z --> emitirAnuncioIBeacon() -->
  // --------------------------------------------------------------
  void emitirAnuncioIBeacon( uint8_t * uuid, uint16_t major, uint16_t minor, int8_t rssi ) {
    detenerAnuncio();

    Bluefruit.Advertising.clearData();
    Bluefruit.ScanResponse.clearData();

    BLEBeacon elBeacon( uuid, major, minor, rssi );
    elBeacon.setManufacturer( fabricanteID );

    Bluefruit.setTxPower( txPower );
    Bluefruit.setName( nombreEmisora );
    Bluefruit.ScanResponse.addName();

    Bluefruit.Advertising.setBeacon( elBeacon );
    Bluefruit.Advertising.restartOnDisconnect( true );
    Bluefruit.Advertising.setInterval( 100, 100 ); // unidades de 0.625 ms

    Bluefruit.Advertising.start( 0 ); // 0 = anunciar hasta que se pare
  }
};

#endif
