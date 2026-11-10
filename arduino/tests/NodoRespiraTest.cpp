// ==============================================================
// Fichero:     NodoRespiraTest.cpp
// Descripción: Tests del nodo sensor en el PC (g++ con mocks de Arduino,
//              Bluefruit y DHT). Prueban las conversiones, los tres
//              medidores con entradas analógicas fijadas y el loop()
//              completo (qué iBeacons salen y con qué major/minor).
//              Ejecutar desde la carpeta arduino:
//   g++ -std=c++17 -I tests/mocks -I NodoRespira -x c++ tests/NodoRespiraTest.cpp -o tests/nodo && tests/nodo
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
#include <cstdio>
#include <cstring>
#include <cmath>
#include "NodoRespira.ino"

static int ok = 0, fallos = 0;
void comprobar( bool c, const char * nombre ) {
  if ( c ) { ok++; printf( "ok     - %s\n", nombre ); }
  else { fallos++; printf( "FALLO  - %s\n", nombre ); }
}
bool cerca( float a, float b, float tol ) { return fabsf( a - b ) <= tol; }
// voltios --> lectura del ADC (referencia 3,6 V, 12 bits)
int adc( float v ) { return (int) lroundf( v * 4095.0f / 3.6f ); }

int main() {
  using namespace Conversiones;

  // ---- Conversiones ----
  comprobar( minorO3( 0.0425f ) == 43, "O3: ppm x 1000 redondeado" );
  comprobar( minorO3( -1.0f ) == 0 && minorO3( 99.0f ) == 32767, "O3: acotado a 0..32767" );
  comprobar( minorTemperatura( 21.46f ) == 215, "temperatura: décimas de grado" );
  comprobar( minorTemperatura( -5.5f ) == -55, "temperatura negativa" );
  comprobar( minorRuido( 63.27f ) == 633, "ruido: décimas de dB" );
  comprobar( major( TIPO_TEMPERATURA, 200 ) == ( 12 << 8 ) + 200, "major = (tipo << 8) + contador" );
  comprobar( cerca( decibelios( 0.10f, 0.10f, 60.0f ), 60.0f, 0.01f ), "dB: en el punto de calibración da el valor de referencia" );
  comprobar( cerca( decibelios( 0.20f, 0.10f, 60.0f ), 66.02f, 0.05f ), "dB: el doble de Vpp son +6 dB" );
  comprobar( decibelios( 0.0f, 0.10f, 60.0f ) == RUIDO_DB_MIN, "dB: sin señal da el mínimo (30)" );
  comprobar( cerca( voltiosPorPpm( -42.54f, 499.0f ), -0.02122746f, 1e-7f ), "sensibilidad del SPEC en V/ppm" );
  comprobar( ppmO3( 1.0f, 1.0f, 0.0f, -0.0212f ) == 0.0f, "O3: sin diferencia, 0 ppm" );

  // ---- MedidorO3 con el ADC simulado ----
  {
    // En la calibración: Vgas - Vref = -0.010 V (offset propio del sensor)
    mock::analogico = []( int pin ) { return pin == Config::PIN_O3_VGAS ? adc( 1.090f ) : adc( 1.100f ); };
    MedidorO3 o3;
    o3.iniciar();
    comprobar( cerca( o3.getOffsetCero(), -0.010f, 0.002f ), "O3: calibración de cero guarda el offset" );
    // Ozono: Vgas baja 0.02123 V más (M negativa) --> 1 ppm
    mock::analogico = []( int pin ) { return pin == Config::PIN_O3_VGAS ? adc( 1.090f - 0.02123f ) : adc( 1.100f ); };
    comprobar( cerca( o3.medir(), 1.0f, 0.05f ), "O3: 21 mV por debajo del cero son ~1 ppm" );
  }

  // ---- MedidorTemperatura (DHT11) ----
  {
    MedidorTemperatura t;
    t.iniciar();
    bool bien = false;
    mock::temperaturaDHT = 21.5f;
    comprobar( t.medir( bien ) == 21.5f && bien, "DHT11: lee la temperatura" );
    mock::temperaturaDHT = NAN;
    t.medir( bien );
    comprobar( !bien, "DHT11: si falla lo dice (no inventa un valor)" );
  }

  // ---- MedidorRuido (KY-037): onda cuadrada de Vpp conocido ----
  {
    MedidorRuido r;
    mock::analogico = []( int ) { static int i = 0; return ( i++ % 2 ) ? adc( 1.65f + 0.05f ) : adc( 1.65f - 0.05f ); };
    float db = r.medir();
    comprobar( cerca( r.getUltimoVpp(), 0.10f, 0.002f ), "ruido: mide 0.10 V pico a pico" );
    comprobar( cerca( db, Config::RUIDO_DB_REF, 0.3f ), "ruido: en el Vpp de calibración da los dB de referencia" );
  }

  // ---- loop() completo ----
  {
    mock::analogico = []( int pin ) {
      static int i = 0;
      if ( pin == Config::PIN_O3_VGAS ) return adc( 1.100f );
      if ( pin == Config::PIN_O3_VREF ) return adc( 1.100f );
      return ( i++ % 2 ) ? adc( 1.75f ) : adc( 1.55f );   // ruido 0.2 Vpp
    };
    mock::temperaturaDHT = 23.4f;
    setup();
    historial.clear();
    loop();
    comprobar( historial.size() == 3, "loop(): anuncia O3, temperatura y ruido" );
    comprobar( memcmp( historial[0].uuid, "RESPIRA-RSP-4F7K", 16 ) == 0, "UUID = RESPIRA- + código del nodo" );
    comprobar( historial[0].major == ( ( 11 << 8 ) + 1 ) && historial[0].minor == 0, "1º O3 (tipo 11, contador 1, 0 ppm)" );
    comprobar( historial[1].major == ( ( 12 << 8 ) + 1 ) && historial[1].minor == 234, "2º temperatura (23.4 °C -> 234)" );
    comprobar( historial[2].major == ( ( 13 << 8 ) + 1 ) && cerca( historial[2].minor / 10.0f, 66.0f, 0.3f ), "3º ruido (~66 dB)" );
    comprobar( historial[0].fabricante == 0x004c, "fabricante Apple (0x004C), obligatorio en iBeacon" );
    comprobar( !ultimoBeacon.anunciando, "al acabar deja de anunciar" );

    mock::temperaturaDHT = NAN;
    historial.clear();
    loop();
    comprobar( historial.size() == 2 && ( historial[1].major >> 8 ) == 13, "si el DHT11 falla no se anuncia temperatura" );
    comprobar( ( historial[0].major & 0xFF ) == 2, "el contador sube en cada vuelta" );
  }

  printf( "\n%d de %d tests correctos\n", ok, ok + fallos );
  return fallos ? 1 : 0;
}
