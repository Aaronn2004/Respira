package com.nodia.respira.ui;

import android.content.Intent;
import android.widget.LinearLayout;

import com.nodia.respira.R;
import com.nodia.respira.datos.Alerta;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Zona;

import java.util.List;

// ==============================================================
// Pestaña «Parques»: los parques guardados, su estado y su alerta.
// Se guardan solo en el móvil: no hace falta cuenta.
// ==============================================================
public class ParquesFragment extends Pestana {

    @Override
    protected void pintar(LinearLayout c) {
        Ui.titulo(c, "Mis parques");
        List<Zona> favs = repo.favoritos();
        if (favs.isEmpty()) {
            Ui.vacio(c, "Aún no tienes parques",
                    "Abre un parque en el mapa y pulsa «Guardar parque» para verlo aquí de un vistazo.",
                    "Ir al mapa", v -> irA(R.id.tab_mapa));
            return;
        }
        for (Zona z : favs) {
            LinearLayout card = Ui.tarjeta(c);
            LinearLayout f = Ui.fila(card, z.nombre, z.resumen(), v -> ZonaActivity.abrir(requireContext(), z.id));
            String altas = z.magnitudesAltas();
            if (!altas.isEmpty()) {
                Ui.insignia(f, "▲ " + altas, Nivel.ALTO);
            } else if (z.nivel(com.nodia.respira.datos.Magnitud.O3) == Nivel.NINGUNO) {
                Ui.insignia(f, "Sin datos", Nivel.NINGUNO);
            }
            Alerta a = repo.alerta(z.id);
            Ui.fila(card, a != null ? "🔔 Alerta: " + a.texto() : "Sin alerta · Crear", null,
                    v -> AlertaActivity.abrir(requireContext(), z.id));
        }
        Ui.boton(c, "+ Añadir desde el mapa", Ui.BOTON_BORDE, v -> irA(R.id.tab_mapa));
        Ui.boton(c, "Mis alertas", Ui.BOTON_TEXTO, v -> startActivity(new Intent(requireContext(), MisAlertasActivity.class)));
        Ui.nota(c, "Tus parques se guardan solo en este móvil. No necesitas cuenta.");
    }
}
