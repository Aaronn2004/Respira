package com.nodia.respira.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.nodia.respira.R;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Salud;

// ==============================================================
// Bloques: piezas visuales de la guía de valores y de «Efectos en
// la salud» (mismo diseño que la web): icono con degradado, barra
// de niveles, escalones de gravedad, etiquetas y el interruptor
// de «Modo oscuro».
// ==============================================================
public final class Bloques {

    private Bloques() { }

    // --------------------------------------------------------------
    // magnitud --> icono() --> drawable   y   colores de su degradado
    // --------------------------------------------------------------
    public static int icono(Magnitud m) {
        return m == Magnitud.O3 ? R.drawable.ic_ozono : m == Magnitud.TEMP ? R.drawable.ic_temperatura : R.drawable.ic_ruido;
    }

    private static int[] degradado(Context c, Magnitud m) {
        if (m == Magnitud.O3) return new int[]{Ui.color(c, R.color.grad_o3_ini), Ui.color(c, R.color.grad_o3_fin)};
        if (m == Magnitud.TEMP) return new int[]{Ui.color(c, R.color.grad_temp_ini), Ui.color(c, R.color.grad_temp_fin)};
        return new int[]{Ui.color(c, R.color.grad_ruido_ini), Ui.color(c, R.color.grad_ruido_fin)};
    }

    // Icono blanco sobre un cuadrado redondeado con degradado
    public static View iconoDegradado(Context c, Magnitud m, int ladoDp) {
        FrameLayout caja = new FrameLayout(c);
        GradientDrawable fondo = new GradientDrawable(GradientDrawable.Orientation.TL_BR, degradado(c, m));
        fondo.setCornerRadius(Ui.dp(c, 12));
        caja.setBackground(fondo);
        ImageView ic = new ImageView(c);
        ic.setImageResource(icono(m));
        ic.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
        int lado = Ui.dp(c, ladoDp * 0.55f);
        caja.addView(ic, new FrameLayout.LayoutParams(lado, lado, Gravity.CENTER));
        caja.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, ladoDp), Ui.dp(c, ladoDp)));
        return caja;
    }

    // Tarjeta con su cabecera: icono + título + subtítulo. Devuelve el cuerpo.
    public static LinearLayout tarjetaConCabecera(ViewGroup padre, Magnitud m, String titulo, String sub, boolean subMono) {
        Context c = padre.getContext();
        LinearLayout card = Ui.tarjeta(padre);
        int p = Ui.dp(c, 16);
        card.setPadding(p, p, p, p);

        LinearLayout cab = new LinearLayout(c);
        cab.setOrientation(LinearLayout.HORIZONTAL);
        cab.setGravity(Gravity.CENTER_VERTICAL);
        cab.addView(iconoDegradado(c, m, 44));
        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.setPadding(Ui.dp(c, 12), 0, 0, 0);
        TextView t = new TextView(c);
        t.setText(titulo);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Ui.color(c, R.color.tinta));
        textos.addView(t);
        TextView s = new TextView(c);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, subMono ? 12 : 13.5f);
        if (subMono) {
            s.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        }
        s.setTextColor(Ui.color(c, subMono ? R.color.tinta_3 : R.color.tinta_2));
        textos.addView(s);
        cab.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(cab);
        return card;
    }

    // Barra verde / ámbar / roja
    public static void barraNiveles(ViewGroup padre) {
        Context c = padre.getContext();
        LinearLayout barra = new LinearLayout(c);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        int[] colores = {R.color.nivel_bajo, R.color.nivel_medio, R.color.nivel_alto};
        for (int i = 0; i < colores.length; i++) {
            View v = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Ui.dp(c, 999));
            g.setColor(Ui.color(c, colores[i]));
            v.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(c, 7), 1);
            if (i > 0) lp.leftMargin = Ui.dp(c, 3);
            barra.addView(v, lp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 14);
        lp.bottomMargin = Ui.dp(c, 4);
        padre.addView(barra, lp);
    }

    // Línea fina de separación
    public static void separador(ViewGroup padre) {
        Context c = padre.getContext();
        View v = new View(c);
        v.setBackgroundColor(Ui.color(c, R.color.linea));
        padre.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 1)));
    }

    // Fila de un nivel: [insignia]  rango (negrita, mono)  /  frase
    public static void filaNivel(ViewGroup padre, com.nodia.respira.datos.Nivel n, String rango, String frase) {
        Context c = padre.getContext();
        LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding(0, Ui.dp(c, 9), 0, Ui.dp(c, 9));
        FrameLayout hueco = new FrameLayout(c);
        TextView ins = Ui.insignia(hueco, n.nombre, n);
        ins.setGravity(Gravity.CENTER);
        ins.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        f.addView(hueco, new LinearLayout.LayoutParams(Ui.dp(c, 62), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.setPadding(Ui.dp(c, 12), 0, 0, 0);
        TextView r = new TextView(c);
        r.setText(rango);
        r.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        r.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        r.setTextColor(Ui.color(c, R.color.tinta));
        textos.addView(r);
        TextView t = new TextView(c);
        t.setText(frase);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setTextColor(Ui.color(c, R.color.tinta_2));
        textos.addView(t);
        f.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        padre.addView(f);
    }

    // Texto pequeño gris (fuente de una tarjeta)
    public static TextView pequeno(ViewGroup padre, String s, int margenArribaDp) {
        TextView t = Ui.texto(padre, s, 11.5f, false, R.color.tinta_3);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = Ui.dp(padre.getContext(), margenArribaDp);
        return t;
    }

    // Muestra de color del estado del dato (como la leyenda del mapa)
    public static void filaEstado(ViewGroup padre, int fondoRes, boolean trama, String titulo, String texto) {
        Context c = padre.getContext();
        LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
        View muestra = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(c, 6));
        if (trama) {
            g.setColor(0x00000000);
            g.setStroke(Ui.dp(c, 1.5f), Ui.color(c, R.color.nivel_ninguno), Ui.dp(c, 3), Ui.dp(c, 2));
        } else {
            g.setColor(Ui.color(c, fondoRes));
        }
        muestra.setBackground(g);
        LinearLayout.LayoutParams lm = new LinearLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22));
        lm.topMargin = Ui.dp(c, 1);
        f.addView(muestra, lm);
        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.setPadding(Ui.dp(c, 10), 0, 0, 0);
        TextView t = new TextView(c);
        t.setText(titulo);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Ui.color(c, R.color.tinta));
        textos.addView(t);
        TextView x = new TextView(c);
        x.setText(texto);
        x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        x.setTextColor(Ui.color(c, R.color.tinta_2));
        textos.addView(x);
        f.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        padre.addView(f);
    }

    // --------------------------------------------------------------
    // Escalón de «Efectos en la salud»: banda de color a la izquierda,
    // valor (+ detalle) y el efecto con su gravedad encima.
    // --------------------------------------------------------------
    public static void escalon(ViewGroup padre, Salud.Paso paso) {
        Context c = padre.getContext();
        int color = Ui.color(c, paso.gravedad == Salud.MOLESTA ? R.color.ef_molesta
                : paso.gravedad == Salud.DANA ? R.color.ef_dana : R.color.ef_peligro);

        LinearLayout e = new LinearLayout(c);
        e.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(Ui.dp(c, 12));
        fondo.setColor(ColorUtils.setAlphaComponent(color, paso.gravedad == Salud.PELIGRO ? 41 : 26));
        e.setBackground(fondo);
        e.setClipToOutline(true);

        View banda = new View(c);
        banda.setBackgroundColor(color);
        e.addView(banda, new LinearLayout.LayoutParams(Ui.dp(c, 4), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout dentro = new LinearLayout(c);
        dentro.setOrientation(LinearLayout.HORIZONTAL);
        dentro.setGravity(Gravity.CENTER_VERTICAL);
        dentro.setPadding(Ui.dp(c, 10), Ui.dp(c, 9), Ui.dp(c, 11), Ui.dp(c, 9));

        LinearLayout valor = new LinearLayout(c);
        valor.setOrientation(LinearLayout.VERTICAL);
        TextView v = new TextView(c);
        v.setText(paso.valor);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        v.setTextColor(Ui.color(c, R.color.tinta));
        v.setMaxLines(1);
        valor.addView(v);
        if (!paso.detalle.isEmpty()) {
            TextView d = new TextView(c);
            d.setText(paso.detalle);
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            d.setTextColor(Ui.color(c, R.color.tinta_3));
            valor.addView(d);
        }
        dentro.addView(valor, new LinearLayout.LayoutParams(Ui.dp(c, 84), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout txt = new LinearLayout(c);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(Ui.dp(c, 10), 0, 0, 0);
        TextView g = new TextView(c);
        g.setText(Salud.NOMBRE_PASO[paso.gravedad].toUpperCase());
        g.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        g.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        g.setLetterSpacing(0.08f);
        g.setTextColor(color);
        txt.addView(g);
        TextView ef = new TextView(c);
        ef.setText(paso.efecto);
        ef.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        ef.setTextColor(Ui.color(c, R.color.tinta));
        txt.addView(ef);
        dentro.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        e.addView(dentro, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 6);
        padre.addView(e, lp);
    }

    // Icono pequeño + contenido (personas → etiquetas; ✓ → consejo)
    public static LinearLayout filaIcono(ViewGroup padre, int icono, int tinte) {
        Context c = padre.getContext();
        LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        ImageView ic = new ImageView(c);
        ic.setImageResource(icono);
        ic.setImageTintList(ColorStateList.valueOf(Ui.color(c, tinte)));
        LinearLayout.LayoutParams li = new LinearLayout.LayoutParams(Ui.dp(c, 16), Ui.dp(c, 16));
        li.topMargin = Ui.dp(c, 4);
        li.rightMargin = Ui.dp(c, 8);
        f.addView(ic, li);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 12);
        padre.addView(f, lp);
        return f;
    }

    public static void etiquetas(ViewGroup fila, String[] textos) {
        Context c = fila.getContext();
        ChipGroup grupo = new ChipGroup(c);
        grupo.setChipSpacingHorizontal(Ui.dp(c, 5));
        grupo.setChipSpacingVertical(Ui.dp(c, 5));
        for (String s : textos) {
            TextView t = new TextView(c);
            t.setText(s);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t.setTextColor(Ui.color(c, R.color.tinta_2));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Ui.dp(c, 999));
            g.setColor(Ui.color(c, R.color.superficie_2));
            t.setBackground(g);
            t.setPadding(Ui.dp(c, 9), Ui.dp(c, 3), Ui.dp(c, 9), Ui.dp(c, 3));
            grupo.addView(t);
        }
        fila.addView(grupo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }

    public static void consejo(ViewGroup fila, String s) {
        Context c = fila.getContext();
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Ui.color(c, R.color.tinta));
        fila.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }

    // --------------------------------------------------------------
    // Fila con interruptor (Más › Modo oscuro). Toda la fila es pulsable.
    // --------------------------------------------------------------
    public interface AlCambiar {
        void cambiar(boolean activado);
    }

    public static void interruptor(ViewGroup padre, int icono, String titulo, String sub, boolean activado, AlCambiar al) {
        Context c = padre.getContext();
        LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(c, 16);
        f.setPadding(p, Ui.dp(c, 12), p, Ui.dp(c, 12));
        f.setMinimumHeight(Ui.dp(c, 56));

        ImageView ic = new ImageView(c);
        ic.setImageResource(icono);
        ic.setImageTintList(ColorStateList.valueOf(Ui.color(c, R.color.tinta_2)));
        LinearLayout.LayoutParams li = new LinearLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22));
        li.rightMargin = Ui.dp(c, 14);
        f.addView(ic, li);

        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(c);
        t.setText(titulo);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(Ui.color(c, R.color.tinta));
        textos.addView(t);
        TextView s = new TextView(c);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        s.setTextColor(Ui.color(c, R.color.tinta_2));
        textos.addView(s);
        f.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialSwitch sw = new MaterialSwitch(c);
        sw.setChecked(activado);
        sw.setContentDescription(titulo);
        sw.setOnCheckedChangeListener((b, on) -> al.cambiar(on));
        f.addView(sw);

        TypedValue fondo = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, fondo, true);
        f.setBackgroundResource(fondo.resourceId);
        f.setOnClickListener(v -> sw.toggle());
        padre.addView(f);
    }
}
