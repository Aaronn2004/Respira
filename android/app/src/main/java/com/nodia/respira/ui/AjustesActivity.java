package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

// Ajustes: notificaciones (las gestiona Android) e idioma.
// --------------------------------------------------------------
// class AjustesActivity
// # Preferencias y acceso al cambio de tema.
// # AppCompatActivity <--- AjustesActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class AjustesActivity extends AppCompatActivity {

    private LinearLayout c;

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onCreate() -->
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = Ui.pantalla(this, "Ajustes");
    }

    // --------------------------------------------------------------
    // # effects: actualiza el ciclo de vida de la pantalla
    // +
    //   onResume() -->
    // --------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        c.removeAllViews();
        LinearLayout card = Ui.tarjeta(c);
        Ui.fila(card, "Notificaciones", Notificaciones.permitidas(this) ? "Permitidas" : "Desactivadas: las alertas no te llegarán",
                v -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        Ui.fila(card, "Idioma", "Castellano (sigue el idioma del sistema)",
                v -> startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS)));
        Ui.nota(c, "Pendiente: traducciones al valenciano y al inglés (values-ca, values-en).");
    }
}
