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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.datos.Alerta;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

// ==============================================================
// Configurar la alerta de un parque. Como mucho un aviso cada 3 h.
// ==============================================================
public class AlertaActivity extends AppCompatActivity {

    private static final String EXTRA_ZONA = "zona";
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

    private final ActivityResultLauncher<String> pedirNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> terminar(ok));

    public static void abrir(Context c, String zonaId) {
        c.startActivity(new Intent(c, AlertaActivity.class).putExtra(EXTRA_ZONA, zonaId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = Repositorio.obtener(this);
        zona = repo.zona(getIntent().getStringExtra(EXTRA_ZONA));
        if (zona == null) {
            finish();
            return;
        }
        Alerta previa = repo.alerta(zona.id);
        if (previa != null) {
            magnitud = previa.magnitud;
            condicion = previa.condicion;
        }
        LinearLayout c = Ui.pantalla(this, "Alerta");
        Ui.texto(c, "Parque", 13, false, com.nodia.respira.R.color.tinta_3);
        Ui.texto(c, zona.nombre, 20, true, com.nodia.respira.R.color.tinta);

        Ui.etiqueta(c, "Magnitud");
        ChipGroup chips = new ChipGroup(this);
        chips.setSingleSelection(true);
        chips.setSelectionRequired(true);
        for (Magnitud m : Magnitud.values()) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipChoiceStyle);
            chip.setText(m.nombre);
            chip.setCheckable(true);
            chip.setChecked(m == magnitud);
            chip.setOnClickListener(v -> {
                magnitud = m;
                valor.setHint("Valor límite (" + m.unidad + ")");
                valor.setHelperText("Rango permitido: " + m.formatear(m.rangoMin) + " – " + m.formatear(m.rangoMax) + " " + m.unidad);
            });
            chips.addView(chip);
        }
        c.addView(chips);

        Ui.etiqueta(c, "Avisarme cuando");
        RadioGroup grupo = new RadioGroup(this);
        for (String[] cond : CONDICIONES) {
            RadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(cond[1]);
            rb.setTag(cond[0]);
            grupo.addView(rb);
            if (cond[0].equals(condicion)) {
                grupo.check(rb.getId());
            }
        }
        c.addView(grupo);

        valor = Ui.campo(c, "Valor límite (" + magnitud.unidad + ")", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        valor.setHelperText("Rango permitido: " + magnitud.formatear(magnitud.rangoMin) + " – "
                + magnitud.formatear(magnitud.rangoMax) + " " + magnitud.unidad);
        if (previa != null && previa.valor != null && valor.getEditText() != null) {
            valor.getEditText().setText(previa.valor);
        }
        valor.setVisibility("custom".equals(condicion) ? View.VISIBLE : View.GONE);
        grupo.setOnCheckedChangeListener((g, id) -> {
            condicion = (String) g.findViewById(id).getTag();
            valor.setVisibility("custom".equals(condicion) ? View.VISIBLE : View.GONE);
        });

        Ui.nota(c, "Te avisamos como mucho una vez cada 3 horas por parque.");
        Ui.boton(c, "Guardar alerta", Ui.BOTON_LLENO, v -> guardar());
        if (previa != null) {
            Ui.boton(c, "Eliminar alerta", Ui.BOTON_TEXTO, v -> {
                repo.borrarAlerta(zona.id);
                Toast.makeText(this, "Alerta eliminada", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    private void guardar() {
        String texto = Ui.leer(valor).trim().replace(',', '.');
        if ("custom".equals(condicion)) {
            String err = Reglas.errorValorAlerta(texto, magnitud);
            valor.setError(err);
            if (err != null) {
                return;
            }
        }
        repo.guardarAlerta(new Alerta(zona.id, magnitud, condicion, "custom".equals(condicion) ? texto : null));
        if (Build.VERSION.SDK_INT >= 33 && !Notificaciones.permitidas(this)) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            terminar(Notificaciones.permitidas(this));
        }
    }

    private void terminar(boolean notificacionesOk) {
        Toast.makeText(this, notificacionesOk ? "Alerta guardada. Te avisaremos."
                : "Alerta guardada, pero no te llegará hasta que permitas las notificaciones.", Toast.LENGTH_LONG).show();
        finish();
    }
}
