package com.nodia.respira.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nodia.respira.R;
import com.nodia.respira.datos.Movimiento;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Repositorio;

// ==============================================================
// Pestaña «Puntos»: saldo, puntos de hoy, historial, regalos y canjes.
// ==============================================================
public class PuntosFragment extends Pestana {

    @Override
    protected void pintar(LinearLayout c) {
        Ui.titulo(c, "Mis puntos");
        Repositorio.Cuenta cuenta = repo.cuenta();
        if (cuenta == null) {
            Ui.necesitaSesion(c, requireActivity(), "Inicia sesión para ver tus puntos",
                    "Los puntos se ganan llevando un sensor y se canjean por regalos del ayuntamiento.");
            return;
        }

        LinearLayout saldo = Ui.tarjeta(c);
        ((com.google.android.material.card.MaterialCardView) saldo.getParent())
                .setCardBackgroundColor(Ui.color(requireContext(), R.color.primario));
        int p = Ui.dp(requireContext(), 18);
        saldo.setPadding(p, p, p, p);
        Ui.texto(saldo, "Saldo", 14, false, R.color.primario_tinta);
        TextView n = Ui.texto(saldo, cuenta.puntos + " puntos", 32, true, R.color.primario_tinta);
        n.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        int[] hoy = repo.hoy();
        if (hoy[0] > 0) {
            Ui.aviso(c, "Hoy llevas " + hoy[0] + " minutos válidos: unos "
                    + cuenta.hoyPuntos + " puntos que se sumarán al acabar el día.", Nivel.BAJO);
        }

        Ui.boton(c, "🎁  Ver regalos", Ui.BOTON_LLENO, v -> startActivity(new Intent(requireContext(), RegalosActivity.class)));
        Ui.boton(c, "Mis canjes", Ui.BOTON_BORDE, v -> startActivity(new Intent(requireContext(), CanjesActivity.class)));

        Ui.etiqueta(c, "Historial");
        LinearLayout h = Ui.tarjeta(c);
        if (cuenta.historial.isEmpty()) {
            Ui.fila(h, "Todavía no tienes movimientos", "Lleva un sensor un día y aquí verás tus puntos.", null);
        }
        for (Movimiento m : cuenta.historial) {
            String sub = m.canje != null ? "Canje: " + m.canje
                    : m.minutos + " min válidos · " + m.minutosParque + " en parques"
                    + (m.descartados > 0 ? "\n" + m.descartados + " min descartados: sensor parado, sin datos o a velocidad de vehículo" : "");
            if (m.recorrido != null && !m.recorrido.vacio()) {
                sub += "\n" + m.recorrido.linea();
            }
            if (m.nota != null) {
                sub += "\n" + m.nota;
            }
            LinearLayout f = Ui.fila(h, m.fecha, sub, null);
            Ui.insignia(f, (m.puntos > 0 ? "+" : "") + m.puntos, m.puntos < 0 ? Nivel.ALTO : Nivel.BAJO);
        }
        Ui.nota(c, "Cómo se ganan: 1 punto cada " + cuenta.minPorPunto + " minutos válidos"
                + (cuenta.multParque > 1 ? " y x" + cuenta.multParque + " dentro de los parques" : "")
                + ", hasta " + cuenta.maxDia + " al día. Los calcula el servidor al acabar el día. "
                + "No cuentan los ratos con el sensor parado más de 15 minutos, sin datos ni a velocidad de vehículo.");
    }
}
