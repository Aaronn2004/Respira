// ==============================================================
// Fichero:     Arduino.h  (MOCK para los tests en el PC)
// Descripción: Sustituye a la API de Arduino para compilar y probar el
//              código de la placa con g++. Las entradas analógicas se
//              pueden fijar desde el test (mock::analogico) y millis()
//              avanza 1 ms en cada llamada.
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#pragma once
#include <stdint.h>
#include <math.h>
#include <functional>
#define HIGH 1
#define LOW 0
#define OUTPUT 1
#define LED_BUILTIN 7
#define A0 2
#define A1 3
#define A2 4
#define A3 5
#define A4 28
#define A5 29
namespace mock {
  // valor del ADC (0..4095) por pin; se puede cambiar por una función
  inline std::function<int(int)> analogico = [](int) { return 0; };
  inline unsigned long reloj = 0;
}
inline void pinMode(int, int) {}
inline void digitalWrite(int, int) {}
inline void delay(unsigned long ms) { mock::reloj += ms; }
inline unsigned long millis() { return mock::reloj++; }
inline void analogReadResolution(int) {}
inline int analogRead(int pin) { return mock::analogico(pin); }
struct SerialMock {
  void begin(unsigned long) {}
  operator bool() { return true; }
  template <class T> void print(T) {}
  template <class T> void print(T, int) {}
};
inline SerialMock Serial;
