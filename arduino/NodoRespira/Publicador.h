// -*- mode: c++ -*-
// ==============================================================
// Fichero:     Publicador.h
// Descripción: Publica cada medida como un anuncio iBeacon:
//   UUID  = "RESPIRA-" + código del nodo   (16 bytes ASCII)
//   major = (tipo << 8) + contador         (11 O3, 12 temperatura, 13 ruido)
//   minor = valor entero de 16 bits        (O3 ppm x 1000, °C x 10, dB x 10)
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef PUBLICADOR_H_INCLUIDO
#define PUBLICADOR_H_INCLUIDO

#include <string.h>
#include "Configuracion.h"
#include "Conversiones.h"
#include "EmisoraBLE.h"

class Publicador {
private:
  uint8_t beaconUUID[16];
  EmisoraBLE laEmisora { "Respira", 0x004c, Config::POTENCIA_TX_DBM };

public:
  // --------------------------------------------------------------
  // Publicador()   (UUID = "RESPIRA-" + CODIGO_NODO)
  // --------------------------------------------------------------
  Publicador() {
    memcpy( beaconUUID, "RESPIRA-", 8 );
    memcpy( beaconUUID + 8, Config::CODIGO_NODO, 8 );
  }

  const uint8_t * getUUID() { return beaconUUID; }

  void encenderEmisora() {
    laEmisora.encenderEmisora();
  }

  // --------------------------------------------------------------
  // tipo: N, valor: Z, contador: N, tiempoMs: N --> publicar() -->
  // anuncia durante tiempoMs y deja de anunciar
  // --------------------------------------------------------------
  void publicar( uint8_t tipo, int16_t valor, uint8_t contador, long tiempoMs ) {
    laEmisora.emitirAnuncioIBeacon( beaconUUID, Conversiones::major( tipo, contador ),
                                    (uint16_t) valor, Config::RSSI_A_1_METRO );
    delay( tiempoMs );
    laEmisora.detenerAnuncio();
  }
};

#endif
