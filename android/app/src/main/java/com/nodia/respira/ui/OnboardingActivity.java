package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nodia.respira.R;
import com.nodia.respira.datos.Repositorio;

// ==============================================================
// Bienvenida (solo la primera vez): 3 pasos.
// No hay términos obligatorios aquí: se aceptan al crear la cuenta.
// ==============================================================
public class OnboardingActivity extends AppCompatActivity {

    // true = se abre desde «Más › Ver la bienvenida otra vez»: al terminar solo se cierra
    public static final String EXTRA_REPASO = "repaso";

    private static final String[][] PASOS = {
            {"El aire de tu ciudad, por zonas",
                    "Ozono, temperatura y ruido medidos por sensores que llevan trabajadores públicos, bicis y motos compartidas, autobuses y vecinos voluntarios."},
            {"Lleva un sensor y gana puntos",
                    "Recoge un sensor en un punto de reparto, escribe su código en la app y pasea por los parques. Cada noche sumas puntos para canjear por regalos."},
            {"Cuenta lo que notas",
                    "Comparte cómo percibes el ruido, el calor o el aire de tu parque. Tu recorrido nunca se publica: el mapa solo muestra medias por zona."}};
    private static final int[] ICONOS = {R.drawable.ic_mapa, R.drawable.ic_sensor, R.drawable.ic_parque};

    private int paso = 0;
    private LinearLayout contenido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            paso = savedInstanceState.getInt("paso");
        }
        contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setBackgroundColor(Ui.color(this, R.color.fondo));
        int p = Ui.dp(this, 24);
        contenido.setPadding(p, p, p, p);
        Ui.insets(contenido, true, true);
        setContentView(contenido);
        pintar();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("paso", paso);
    }

    private void pintar() {
        contenido.removeAllViews();

        MaterialButton saltar = Ui.boton(contenido, "Saltar", Ui.BOTON_TEXTO, v -> terminar());
        ((LinearLayout.LayoutParams) saltar.getLayoutParams()).width = LinearLayout.LayoutParams.WRAP_CONTENT;
        ((LinearLayout.LayoutParams) saltar.getLayoutParams()).gravity = Gravity.END;

        LinearLayout centro = new LinearLayout(this);
        centro.setOrientation(LinearLayout.VERTICAL);
        centro.setGravity(Gravity.CENTER);
        contenido.addView(centro, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        ImageView ico = new ImageView(this);
        ico.setImageResource(ICONOS[paso]);
        ico.setColorFilter(Ui.color(this, R.color.primario));
        centro.addView(ico, new LinearLayout.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 120)));

        TextView t = Ui.titulo(centro, PASOS[paso][0]);
        t.setGravity(Gravity.CENTER);
        TextView d = Ui.parrafo(centro, PASOS[paso][1]);
        d.setGravity(Gravity.CENTER);

        TextView puntos = Ui.texto(contenido, (paso + 1) + " de " + PASOS.length, 13, true, R.color.tinta_3);
        puntos.setGravity(Gravity.CENTER);

        boolean ultimo = paso == PASOS.length - 1;
        Ui.boton(contenido, ultimo ? "Empezar" : "Siguiente", Ui.BOTON_LLENO, v -> {
            if (ultimo) {
                terminar();
            } else {
                paso++;
                pintar();
            }
        });
    }

    private void terminar() {
        if (getIntent().getBooleanExtra(EXTRA_REPASO, false)) {
            finish();
            return;
        }
        Repositorio.obtener(this).marcarOnboardingVisto();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
