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
public final class Tema {

    private static final String PREFS = "respira_ajustes";
    private static final String CLAVE = "modo_oscuro";

    private Tema() { }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean oscuro(Context c) {
        return prefs(c).getBoolean(CLAVE, false);
    }

    // Al arrancar la app
    public static void aplicar(Context c) {
        AppCompatDelegate.setDefaultNightMode(oscuro(c)
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Desde el interruptor: Android vuelve a crear las pantallas abiertas con los colores nuevos
    public static void cambiar(Context c, boolean oscuro) {
        prefs(c).edit().putBoolean(CLAVE, oscuro).apply();
        aplicar(c);
    }
}
