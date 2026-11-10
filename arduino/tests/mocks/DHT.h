// ==============================================================
// Fichero:     DHT.h  (MOCK de la librería de Adafruit para los tests)
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#pragma once
#include <math.h>
#define DHT11 11
namespace mock { inline float temperaturaDHT = NAN; }
class DHT {
public:
  DHT(int, int) {}
  void begin() {}
  float readTemperature() { return mock::temperaturaDHT; }
};
