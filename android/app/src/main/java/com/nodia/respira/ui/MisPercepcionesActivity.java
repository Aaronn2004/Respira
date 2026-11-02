package com.nodia.respira.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.datos.Percepcion;
import com.nodia.respira.datos.Repositorio;

import java.util.List;

public class MisPercepcionesActivity extends AppCompatActivity {

    private LinearLayout c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = Ui.pantalla(this, "Mis percepciones");
        pintar();
    }

    private void pintar() {
        c.removeAllViews();
        Repositorio repo = Repositorio.obtener(this);
        if (!repo.haySesion()) {
            Ui.necesitaSesion(c, this, "Inicia sesión", "Tus percepciones van asociadas a tu cuenta para que puedas borrarlas.");
            return;
        }
        List<Percepcion> mias = repo.misPercepciones();
        if (mias.isEmpty()) {
            Ui.vacio(c, "Aún no has compartido nada", "En el mapa, capa «Comunidad», pulsa «+ Contar lo que noto».", null, null);
            return;
        }
        LinearLayout lista = Ui.tarjeta(c);
        for (Percepcion p : mias) {
            String cuando = DateUtils.getRelativeTimeSpanString(p.cuando).toString();
            LinearLayout f = Ui.fila(lista, p.titulo(), p.direccion + " · " + cuando, null);
            Ui.boton(f, "Borrar", Ui.BOTON_TEXTO, v -> Ui.confirmar(this, "¿Borrar esta percepción?",
                    "Desaparecerá del mapa.", "Borrar", () -> {
                        repo.borrarPercepcion(p.id);
                        pintar();
                    })).setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }
}
