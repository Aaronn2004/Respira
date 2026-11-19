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
// Los parques seguidos se guardan en la cuenta y se conservan en la caché del móvil.
// ==============================================================
// --------------------------------------------------------------
// class ParquesFragment
// # Listado de zonas y sus magnitudes actuales.
// # Pestana <--- ParquesFragment
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Vista = { contenido: Text, visible: B, habilitada: B }
// --------------------------------------------------------------
public class ParquesFragment extends Pestana {

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + c: Vista
    //       -->
    //           pintar() -->
    // --------------------------------------------------------------
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
            } else if (com.nodia.respira.datos.Reglas.ESTADO_SIN_DATOS.equals(z.estado())) {
                Ui.insignia(f, "Sin datos", Nivel.NINGUNO);
            }
            boolean tieneAlerta = false;
            for (Alerta alerta : repo.alertas()) {
                if (!alerta.zonaId.equals(z.id)) continue;
                Ui.fila(card, "🔔 Alerta: " + alerta.texto(), null,
                        v -> AlertaActivity.abrir(requireContext(), z.id, alerta.magnitud));
                tieneAlerta = true;
            }
            if (!tieneAlerta) Ui.fila(card, "Sin alerta · Crear", null,
                    v -> AlertaActivity.abrir(requireContext(), z.id));
        }
        Ui.boton(c, "+ Añadir desde el mapa", Ui.BOTON_BORDE, v -> irA(R.id.tab_mapa));
        Ui.boton(c, "Mis alertas", Ui.BOTON_TEXTO, v -> startActivity(new Intent(requireContext(), MisAlertasActivity.class)));
        Ui.nota(c, repo.usuario() != null
                ? "Tus parques y alertas se guardan en tu cuenta: los verás en cualquier móvil en el que entres."
                : "Tus parques se sincronizan con tu cuenta al iniciar sesión.");
    }
}
