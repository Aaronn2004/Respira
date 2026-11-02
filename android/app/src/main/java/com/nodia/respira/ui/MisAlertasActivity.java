package com.nodia.respira.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.datos.Alerta;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

import java.util.List;

public class MisAlertasActivity extends AppCompatActivity {

    private LinearLayout c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = Ui.pantalla(this, "Mis alertas");
    }

    @Override
    protected void onResume() {
        super.onResume();
        c.removeAllViews();
        Repositorio repo = Repositorio.obtener(this);
        List<Alerta> alertas = repo.alertas();
        if (alertas.isEmpty()) {
            Ui.vacio(c, "No tienes alertas", "Abre un parque en el mapa y pulsa «Alerta».", null, null);
            return;
        }
        LinearLayout lista = Ui.tarjeta(c);
        for (Alerta a : alertas) {
            Zona z = repo.zona(a.zonaId);
            Ui.fila(lista, z == null ? a.zonaId : z.nombre, a.texto(), v -> AlertaActivity.abrir(this, a.zonaId));
        }
        if (!Notificaciones.permitidas(this)) {
            Ui.aviso(c, "Las notificaciones no están permitidas: las alertas no te llegarán. Actívalas en Ajustes.", Nivel.MEDIO);
        }
    }
}
