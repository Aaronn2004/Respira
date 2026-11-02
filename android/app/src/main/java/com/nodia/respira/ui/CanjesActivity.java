package com.nodia.respira.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.datos.Canje;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Repositorio;

// Mis canjes: códigos de recogida y su estado.
public class CanjesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repositorio.Cuenta cuenta = Repositorio.obtener(this).cuenta();
        LinearLayout c = Ui.pantalla(this, "Mis canjes");
        if (cuenta == null) {
            finish();
            return;
        }
        if (cuenta.canjes.isEmpty()) {
            Ui.vacio(c, "Aún no has canjeado nada", "Cuando canjees un regalo, aquí verás su código de recogida.", null, null);
            return;
        }
        LinearLayout lista = Ui.tarjeta(c);
        for (Canje k : cuenta.canjes) {
            boolean pendiente = "pendiente".equals(k.estado);
            LinearLayout f = Ui.fila(lista, k.regalo, k.codigo + " · " + (pendiente ? "recoger antes del " + k.caduca
                    : "caducado".equals(k.estado) ? "caducado (puntos devueltos)" : "entregado"), null);
            Ui.insignia(f, pendiente ? "Pendiente" : "caducado".equals(k.estado) ? "Caducado" : "Entregado",
                    pendiente ? Nivel.MEDIO : "caducado".equals(k.estado) ? Nivel.NINGUNO : Nivel.BAJO);
        }
    }
}
