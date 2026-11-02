package com.nodia.respira.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.R;
import com.nodia.respira.datos.Repositorio;

// ==============================================================
// SplashActivity: animación de carga cada vez que se abre la app.
// El símbolo entra girando, el nombre se desliza y aparece
// «Developed by Nodia». Mientras tanto se cargan los datos.
// ==============================================================
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_MS = 2800;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean listo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View simbolo = findViewById(R.id.simbolo);
        View nombre = findViewById(R.id.nombre);
        View nodia = findViewById(R.id.nodia);

        simbolo.setScaleX(0.4f);
        simbolo.setScaleY(0.4f);
        simbolo.setRotation(-120);
        simbolo.setAlpha(0);
        simbolo.animate().scaleX(1).scaleY(1).rotation(0).alpha(1)
                .setDuration(900).setInterpolator(new OvershootInterpolator(1.4f)).start();

        nombre.setTranslationX(-60);
        nombre.setAlpha(0);
        nombre.animate().translationX(0).alpha(1).setStartDelay(500).setDuration(700).start();

        nodia.animate().alpha(1).setStartDelay(1300).setDuration(700).start();

        // Carga real en segundo plano (lee SharedPreferences)
        new Thread(() -> {
            Repositorio.obtener(getApplicationContext());
            listo = true;
        }).start();

        handler.postDelayed(this::seguir, DURACION_MS);
    }

    private void seguir() {
        if (!listo) { // carga lenta: se queda la barra hasta que termine
            handler.postDelayed(this::seguir, 200);
            return;
        }
        Repositorio repo = Repositorio.obtener(this);
        Class<?> destino = repo.onboardingVisto() ? MainActivity.class : OnboardingActivity.class;
        startActivity(new Intent(this, destino));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
