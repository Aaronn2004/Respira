package com.nodia.respira.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.datos.Alerta;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

import java.util.HashMap;
import java.util.Map;

// ==============================================================
// Configurar la alerta de un parque. Como mucho un aviso cada 3 h.
// ==============================================================
// --------------------------------------------------------------
// class AlertaActivity
// # Edición del límite de alerta de una zona y magnitud.
// # AppCompatActivity <--- AlertaActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Entorno = { densidad: R, tema: Text }
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// ResultadoAccion = Completada | Fallo{ mensaje: Text, campos: Map[Text, Text] }
// --------------------------------------------------------------
public class AlertaActivity extends AppCompatActivity {

    private static final String EXTRA_ZONA = "zona";
    private static final String EXTRA_MAGNITUD = "magnitud";
    private static final String[][] CONDICIONES = {
            {"bajo", "Baje a nivel Bajo (buen momento para ir)"},
            {"medio", "Llegue a nivel Medio"},
            {"alto", "Llegue a nivel Alto"},
            {"custom", "Supere un valor que yo elija"}};

    private Repositorio repo;
    private Zona zona;
    private Magnitud magnitud = Magnitud.O3;
    private String condicion = "alto";
    private TextInputLayout valor;
    private RadioGroup grupo;
    private final Map<String, Integer> opciones = new HashMap<>();
    private MaterialButton guardar;
    private MaterialButton eliminar;
    private boolean sincronizada;

    private final ActivityResultLauncher<String> pedirNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> terminar(ok));

    // --------------------------------------------------------------
    // # effects: abre la pantalla de destino
    // + c: Entorno, zonaId: Text
    //       -->
    //           abrir() --x
    // --------------------------------------------------------------
    public static void abrir(Context c, String zonaId) {
        abrir(c, zonaId, null);
    }

    // --------------------------------------------------------------
    // # effects: abre la pantalla de destino
    // + c: Entorno, zonaId: Text, magnitud: Magnitud
    //       -->
    //           abrir() --x
    // --------------------------------------------------------------
    public static void abrir(Context c, String zonaId, Magnitud magnitud) {
        Intent i = new Intent(c, AlertaActivity.class).putExtra(EXTRA_ZONA, zonaId);
        if (magnitud != null) i.putExtra(EXTRA_MAGNITUD, magnitud.id);
        c.startActivity(i);
    }

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
        zona = repo.zona(getIntent().getStringExtra(EXTRA_ZONA));
        if (zona == null) {
            finish();
            return;
        }
        String magnitudId = getIntent().getStringExtra(EXTRA_MAGNITUD);
        Alerta previa = repo.alerta(zona.id);
        magnitud = magnitudId != null ? Magnitud.desdeId(magnitudId) : previa != null ? previa.magnitud : Magnitud.O3;
        LinearLayout c = Ui.pantalla(this, "Alerta");
        Ui.texto(c, "Parque", 13, false, com.nodia.respira.R.color.tinta_3);
        Ui.texto(c, zona.nombre, 20, true, com.nodia.respira.R.color.tinta);

        Ui.etiqueta(c, "Magnitud");
        ChipGroup chips = new ChipGroup(this);
        chips.setSingleSelection(true);
        chips.setSelectionRequired(true);
        for (Magnitud m : Magnitud.values()) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setText(m.nombre);
            chip.setCheckable(true);
            chip.setChecked(m == magnitud);
            chip.setOnClickListener(v -> mostrarMagnitud(m));
            chips.addView(chip);
        }
        c.addView(chips);

        Ui.etiqueta(c, "Avisarme cuando");
        grupo = new RadioGroup(this);
        for (String[] cond : CONDICIONES) {
            RadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(cond[1]);
            rb.setTag(cond[0]);
            grupo.addView(rb);
            opciones.put(cond[0], rb.getId());
        }
        c.addView(grupo);

        valor = Ui.campo(c, "Valor límite (" + magnitud.unidad + ")", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        grupo.setOnCheckedChangeListener((g, id) -> {
            condicion = (String) g.findViewById(id).getTag();
            valor.setVisibility("custom".equals(condicion) ? View.VISIBLE : View.GONE);
        });
        Ui.nota(c, "Cada magnitud tiene su propia alerta. Como máximo, un aviso por hora y magnitud.");
        guardar = Ui.boton(c, "Guardar alerta", Ui.BOTON_LLENO, v -> guardar());
        eliminar = Ui.boton(c, "Eliminar alerta", Ui.BOTON_TEXTO, v -> eliminar());
        mostrarMagnitud(magnitud);
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - m: Magnitud
    //       -->
    //           mostrarMagnitud() -->
    // --------------------------------------------------------------
    private void mostrarMagnitud(Magnitud m) {
        magnitud = m;
        Alerta alerta = repo.alerta(zona.id, m);
        condicion = alerta == null ? "alto" : alerta.condicion;
        grupo.check(opciones.get(condicion));
        valor.setVisibility("custom".equals(condicion) ? View.VISIBLE : View.GONE);
        valor.setHint("Valor límite (" + m.unidad + ")");
        valor.setHelperText("Rango permitido: " + m.formatear(m.rangoMin) + " – " + m.formatear(m.rangoMax) + " " + m.unidad);
        valor.getEditText().setText(alerta == null ? "" : alerta.valor);
        valor.setError(null);
        eliminar.setVisibility(alerta == null ? View.GONE : View.VISIBLE);
    }

    // --------------------------------------------------------------
    // # effects: valida la entrada, solicita la operación al repositorio y actualiza la interfaz
    // -
    //   guardar() -->
    // --------------------------------------------------------------
    private void guardar() {
        String texto = Ui.leer(valor).trim().replace(',', '.');
        if ("custom".equals(condicion)) {
            String err = Reglas.errorValorAlerta(texto, magnitud);
            valor.setError(err);
            if (err != null) {
                return;
            }
        }
        guardar.setEnabled(false);
        eliminar.setEnabled(false);
        repo.guardarAlerta(new Alerta(zona.id, magnitud, condicion, "custom".equals(condicion) ? texto : null),
                resultado(ok -> {
                    sincronizada = ok;
                    if (Build.VERSION.SDK_INT >= 33 && !Notificaciones.permitidas(this)) {
                        pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        terminar(Notificaciones.permitidas(this));
                    }
                }));
    }

    // --------------------------------------------------------------
    // # effects: valida la entrada, solicita la operación al repositorio y actualiza la interfaz
    // -
    //   eliminar() -->
    // --------------------------------------------------------------
    private void eliminar() {
        guardar.setEnabled(false);
        eliminar.setEnabled(false);
        repo.borrarAlerta(zona.id, magnitud, resultado(ok -> {
            Toast.makeText(this, "Alerta eliminada", Toast.LENGTH_SHORT).show();
            finish();
        }));
    }

    // --------------------------------------------------------------
    // # Las respuestas tardías se ignoran cuando la pantalla ya está terminando.
    // # Contrato de la respuesta futura; se omite el adaptador de callbacks.
    // # effects: prepara el tratamiento de la respuesta y sus errores en la interfaz
    // - resultado() <--
    //       <--
    //   resultado: ResultadoAccion
    // --------------------------------------------------------------
    private Repositorio.Hecho<Boolean> resultado(java.util.function.Consumer<Boolean> realizado) {
        return new Repositorio.Hecho<Boolean>() {
            // --------------------------------------------------------------
            // # effects: actualiza la interfaz Android
            // + servidor: B
            //       -->
            //           ok() -->
            // --------------------------------------------------------------
            @Override public void ok(Boolean servidor) {
                if (!isFinishing() && !isDestroyed()) realizado.accept(servidor);
            }

            // --------------------------------------------------------------
            // # effects: actualiza la interfaz Android
            // + mensaje: Text, campos: Map[Text, Text]
            //       -->
            //           error() -->
            // --------------------------------------------------------------
            @Override public void error(String mensaje, Map<String, String> campos) {
                if (isFinishing() || isDestroyed()) return;
                guardar.setEnabled(true);
                eliminar.setEnabled(true);
                if (campos.containsKey("valor")) valor.setError(campos.get("valor"));
                else Toast.makeText(AlertaActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        };
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - notificacionesOk: B
    //       -->
    //           terminar() -->
    // --------------------------------------------------------------
    private void terminar(boolean notificacionesOk) {
        String guardada = sincronizada ? "Alerta guardada en tu cuenta." : "Alerta guardada en este móvil.";
        Toast.makeText(this, guardada + (notificacionesOk ? "" : " Permite las notificaciones para recibir avisos."),
                Toast.LENGTH_LONG).show();
        finish();
    }
}
