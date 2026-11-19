// -*- mode: c++ -*-
// ==============================================================
// Fichero:     LED.h
// Descripción: Control del LED de la placa.
// Diseño:      doc/arduino.md
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#ifndef LED_H_INCLUIDO
#define LED_H_INCLUIDO

#include <Arduino.h>

// # Controla el GPIO asignado al LED.
class LED {
private:
  int numeroPin;

public:
  // --------------------------------------------------------------
  // # effects: configura el GPIO como salida y apaga el LED.
  // + pin: N
  //     -->
  //         LED() -->
  // --------------------------------------------------------------
  LED( int pin ) : numeroPin( pin ) {
    pinMode( numeroPin, OUTPUT );
    // --------------------------------------------------------------
    // # effects: desactiva el GPIO del LED.
    // +       apagar() <--
    // --------------------------------------------------------------
    apagar();
  }

  // --------------------------------------------------------------
  // # effects: activa el GPIO del LED.
  // +       encender() <--
  // --------------------------------------------------------------
  void encender() { digitalWrite( numeroPin, HIGH ); }
  void apagar() { digitalWrite( numeroPin, LOW ); }

  // --------------------------------------------------------------
  // # effects: enciende el LED, espera y lo apaga.
  // + tiempoMs: N
  //     -->
  //         brillar() <--
  // --------------------------------------------------------------
  void brillar( long tiempoMs ) {
    encender();
    delay( tiempoMs );
    apagar();
  }
};

#endif
