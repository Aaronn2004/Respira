package com.nodia.respira.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nodia.respira.R;
import com.nodia.respira.datos.Canje;
import com.nodia.respira.datos.Regalo;
import com.nodia.respira.datos.Repositorio;

import java.util.Map;

// ==============================================================
// Regalos del ayuntamiento. Al canjear se descuentan los puntos y
// se da un código CJ-XXXX para recogerlo en 7 días.
// ==============================================================
// --------------------------------------------------------------
// class RegalosActivity
// # Catálogo de regalos y confirmación de canjes.
// # AppCompatActivity <--- RegalosActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class RegalosActivity extends AppCompatActivity {

    private Repositorio repo;
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
        repo = Repositorio.obtener(this);
        c = Ui.pantalla(this, "Regalos");
        pintar();
        repo.observar(alCambiar);
        repo.refrescar(true);     // saldo y existencias al día
    }

    private final Runnable alCambiar = this::pintar;

    // --------------------------------------------------------------
    // # effects: actualiza la suscripción al repositorio
    // +
    //   onDestroy() -->
    // --------------------------------------------------------------
    @Override
    protected void onDestroy() {
        repo.dejarDeObservar(alCambiar);
        super.onDestroy();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   pintar() -->
    // --------------------------------------------------------------
    private void pintar() {
        c.removeAllViews();
        Repositorio.Cuenta cuenta = repo.cuenta();
        if (cuenta == null) {
            finish();
            return;
        }
        Ui.texto(c, "Tienes " + cuenta.puntos + " puntos", 18, true, R.color.primario);
        for (Regalo g : repo.regalos()) {
            LinearLayout card = Ui.tarjeta(c);
            String estado = g.stock > 0 ? "quedan " + g.stock : "agotado";
            String falta = g.stock <= 0 ? null : cuenta.puntos < g.coste ? "Te faltan " + (g.coste - cuenta.puntos) + " puntos" : null;
            LinearLayout f = Ui.fila(card, g.nombre, g.coste + " puntos · " + estado + (falta == null ? "" : "\n" + falta), null);
            MaterialButton b = new MaterialButton(this);
            b.setText("Canjear");
            b.setAllCaps(false);
            b.setEnabled(g.stock > 0 && cuenta.puntos >= g.coste);
            b.setOnClickListener(v -> confirmar(g, cuenta.puntos));
            f.addView(b);
        }
        Ui.nota(c, "Al canjear recibes un código para recoger el regalo en un punto de reparto en 7 días. Si no lo recoges, te devolvemos los puntos.");
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz y registra la acción del usuario
    // - g: Regalo, puntos: N
    //       -->
    //           confirmar() -->
    // --------------------------------------------------------------
    private void confirmar(Regalo g, int puntos) {
        Ui.confirmar(this, "¿Canjear " + g.nombre + "?",
                "Se descontarán " + g.coste + " puntos (te quedarán " + (puntos - g.coste)
                        + "). Tendrás 7 días para recogerlo en un punto de reparto.", "Canjear", () -> {
                    repo.canjear(g.id, new Repositorio.Hecho<Canje>() {
                        // --------------------------------------------------------------
                        // # effects: actualiza la interfaz Android
                        // + k: Canje
                        //       -->
                        //           ok() -->
                        // --------------------------------------------------------------
                        @Override
                        public void ok(Canje k) {
                            mostrarCodigo(k);
                        }

                        // --------------------------------------------------------------
                        // # effects: actualiza la interfaz Android
                        // + mensaje: Text, campos: Map[Text, Text]
                        //       -->
                        //           error() -->
                        // --------------------------------------------------------------
                        @Override
                        public void error(String mensaje, Map<String, String> campos) {
                            Ui.confirmar(RegalosActivity.this, "No se ha podido canjear", mensaje, "Entendido", () -> { });
                        }
                    });
                });
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - k: Canje
    //       -->
    //           mostrarCodigo() -->
    // --------------------------------------------------------------
    private void mostrarCodigo(Canje k) {
        TextView codigo = new TextView(this);
        codigo.setText(k.codigo);
        codigo.setTextSize(36);
        codigo.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        codigo.setTextColor(Ui.color(this, R.color.primario));
        codigo.setGravity(Gravity.CENTER);
        codigo.setBackgroundResource(R.drawable.fondo_codigo);
        int p = Ui.dp(this, 18);
        codigo.setPadding(p, p, p, p);
        LinearLayout marco = new LinearLayout(this);
        marco.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), 0);
        marco.addView(codigo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Canje hecho: " + k.regalo)
                .setMessage("Enséñalo en el punto de reparto antes del " + k.caduca + ". También lo tienes en «Mis canjes».")
                .setView(marco)
                .setPositiveButton("Hecho", null)
                .show();
    }
}
