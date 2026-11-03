package com.nodia.respira;

import android.app.Application;

import com.nodia.respira.ui.Tema;

// Aplicación: aplica el modo claro u oscuro guardado antes de abrir ninguna pantalla.
public class RespiraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Tema.aplicar(this);
    }
}
