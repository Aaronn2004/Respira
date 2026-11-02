package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

// Ajustes: notificaciones (las gestiona Android) e idioma.
public class AjustesActivity extends AppCompatActivity {

    private LinearLayout c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = Ui.pantalla(this, "Ajustes");
    }

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
