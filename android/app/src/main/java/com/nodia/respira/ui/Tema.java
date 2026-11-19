package com.nodia.respira.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

// ==============================================================
// Tema: modo claro (por defecto) u oscuro, elegido en Más › Modo oscuro.
// Se guarda en el móvil y se aplica al abrir la app (RespiraApp).
// Los colores de cada modo están en values/colors.xml y
// values-night/colors.xml (los mismos que la web).
// ==============================================================
// --------------------------------------------------------------
// class Tema
// # Persistencia y aplicación del tema claro u oscuro.
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Entorno = { densidad: R, tema: Text }
// Preferencias = Map[Text, B]
// --------------------------------------------------------------
public final class Tema {

    private static final String PREFS = "respira_ajustes";
    private static final String CLAVE = "modo_oscuro";

    // --------------------------------------------------------------
    // -
    //   Tema() --x
    // --------------------------------------------------------------
    private Tema() { }

    // --------------------------------------------------------------
    // # effects: abre las preferencias locales de la aplicación
    // - c: Entorno
    //       -->
    //           prefs() --x
    //       <--
    //   resultado: Preferencias
    // --------------------------------------------------------------
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // --------------------------------------------------------------
    // # effects: lee la preferencia local de tema
    // + c: Entorno
    //       -->
    //           oscuro() --x
    //       <--
    //   resultado: B
    // --------------------------------------------------------------
    public static boolean oscuro(Context c) {
        return prefs(c).getBoolean(CLAVE, false);
    }

    // Al arrancar la app
    // --------------------------------------------------------------
    // # effects: lee las preferencias y aplica el tema a la aplicación
    // + c: Entorno
    //       -->
    //           aplicar() --x
    // --------------------------------------------------------------
    public static void aplicar(Context c) {
        AppCompatDelegate.setDefaultNightMode(oscuro(c)
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Desde el interruptor: Android vuelve a crear las pantallas abiertas con los colores nuevos
    // --------------------------------------------------------------
    // # effects: guarda la preferencia local y aplica el tema a las pantallas
    // + c: Entorno, oscuro: B
    //       -->
    //           cambiar() --x
    // --------------------------------------------------------------
    public static void cambiar(Context c, boolean oscuro) {
        prefs(c).edit().putBoolean(CLAVE, oscuro).apply();
        aplicar(c);
    }
}
