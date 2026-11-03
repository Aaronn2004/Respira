package com.nodia.respira.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.R;
import com.nodia.respira.datos.Salud;

// ==============================================================
// Efectos en la salud («Qué te puede pasar»): para cada magnitud,
// tres escalones (Molesta · Daña · Peligro), a quién afecta más y
// qué hacer. Mismo contenido y diseño que la web.
// ==============================================================
public class EfectosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout c = Ui.pantalla(this, "Efectos en la salud");
        Ui.titulo(c, "Qué te puede pasar");
        Ui.parrafo(c, "Cuanto más sube, más daño. Así afecta cada nivel.");

        for (Salud.Efecto e : Salud.EFECTOS) {
            LinearLayout card = Bloques.tarjetaConCabecera(c, e.magnitud, e.nombre, e.lema, false);
            LinearLayout pasos = new LinearLayout(this);
            pasos.setOrientation(LinearLayout.VERTICAL);
            pasos.setPadding(0, Ui.dp(this, 8), 0, 0);
            for (Salud.Paso p : e.pasos) {
                Bloques.escalon(pasos, p);
            }
            card.addView(pasos);
            Bloques.etiquetas(Bloques.filaIcono(card, R.drawable.ic_personas, R.color.tinta_3), e.peorPara);
            Bloques.consejo(Bloques.filaIcono(card, R.drawable.ic_check, R.color.primario), e.hacer);
            Bloques.pequeno(card, e.fuente, 12);
        }
        Ui.aviso(c, Salud.AVISO, com.nodia.respira.datos.Nivel.NINGUNO);
    }
}
