package com.nodia.respira.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.R;
import com.nodia.respira.datos.Nivel;

// ==============================================================
// Ui: piezas de interfaz reutilizables (tarjetas, filas, avisos...).
// Las pantallas de listas se construyen con estas piezas en código
// en lugar de un XML por pantalla: así se "repintan" igual que en
// el prototipo cuando cambian los datos.
// ==============================================================
public final class Ui {

    public static final int BOTON_LLENO = 0;
    public static final int BOTON_BORDE = 1;
    public static final int BOTON_TEXTO = 2;

    private Ui() { }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static int color(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    // --------------------------------------------------------------
    // vista: View --> insets()   (Android 15+ dibuja bajo las barras)
    // --------------------------------------------------------------
    public static void insets(View vista, boolean arriba, boolean abajo) {
        final int l = vista.getPaddingLeft(), t = vista.getPaddingTop();
        final int r = vista.getPaddingRight(), b = vista.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(vista, (v, w) -> {
            Insets i = w.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(l + i.left, t + (arriba ? i.top : 0), r + i.right, b + (abajo ? i.bottom : 0));
            return w;
        });
    }

    // --------------------------------------------------------------
    // Pantalla estándar de una Activity: barra con «atrás» + scroll.
    // actividad, titulo --> pantalla() --> contenido: LinearLayout
    // --------------------------------------------------------------
    public static LinearLayout pantalla(AppCompatActivity a, String titulo) {
        LinearLayout raiz = new LinearLayout(a);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(color(a, R.color.fondo));

        MaterialToolbar barra = new MaterialToolbar(a);
        barra.setTitle(titulo);
        barra.setNavigationIcon(R.drawable.ic_atras);
        barra.setNavigationContentDescription("Atrás");
        barra.setNavigationOnClickListener(v -> a.getOnBackPressedDispatcher().onBackPressed());
        raiz.addView(barra, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        LinearLayout contenido = columna(a);
        scroll.addView(contenido);
        raiz.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        insets(raiz, true, true);
        a.setContentView(raiz);
        return contenido;
    }

    public static LinearLayout columna(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, dp(c, 8), p, dp(c, 24));
        return l;
    }

    private static LinearLayout.LayoutParams separado(Context c, int arriba) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, arriba);
        return lp;
    }

    // ---------- textos ----------
    public static TextView texto(ViewGroup padre, String s, float sp, boolean negrita, int colorRes) {
        Context c = padre.getContext();
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color(c, colorRes));
        if (negrita) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        padre.addView(t, separado(c, 8));
        return t;
    }

    public static TextView titulo(ViewGroup padre, String s) {
        return texto(padre, s, 24, true, R.color.tinta);
    }

    public static TextView parrafo(ViewGroup padre, String s) {
        return texto(padre, s, 15, false, R.color.tinta_2);
    }

    public static TextView nota(ViewGroup padre, String s) {
        TextView t = texto(padre, s, 13, false, R.color.tinta_3);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(padre.getContext(), 12);
        return t;
    }

    public static TextView etiqueta(ViewGroup padre, String s) {
        TextView t = texto(padre, s.toUpperCase(), 12, true, R.color.tinta_3);
        t.setLetterSpacing(0.06f);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(padre.getContext(), 18);
        return t;
    }

    // ---------- tarjetas y filas ----------
    public static LinearLayout tarjeta(ViewGroup padre) {
        Context c = padre.getContext();
        MaterialCardView card = new MaterialCardView(c);
        card.setRadius(dp(c, 16));
        card.setCardElevation(0);
        card.setStrokeColor(color(c, R.color.linea));
        card.setStrokeWidth(dp(c, 1));
        card.setCardBackgroundColor(color(c, R.color.superficie));
        LinearLayout dentro = new LinearLayout(c);
        dentro.setOrientation(LinearLayout.VERTICAL);
        card.addView(dentro);
        padre.addView(card, separado(c, 12));
        return dentro;
    }

    // Fila pulsable: título, subtítulo opcional y ">" a la derecha
    public static LinearLayout fila(ViewGroup padre, String titulo, String sub, View.OnClickListener al) {
        Context c = padre.getContext();
        LinearLayout f = new LinearLayout(c);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 16);
        f.setPadding(p, dp(c, 14), p, dp(c, 14));
        f.setMinimumHeight(dp(c, 56));

        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(c);
        t.setText(titulo);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(color(c, R.color.tinta));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textos.addView(t);
        if (sub != null) {
            TextView s = new TextView(c);
            s.setText(sub);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            s.setTextColor(color(c, R.color.tinta_2));
            textos.addView(s);
        }
        f.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (al != null) {
            TextView flecha = new TextView(c);
            flecha.setText("›");
            flecha.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            flecha.setTextColor(color(c, R.color.tinta_3));
            f.addView(flecha);
            TypedValue fondo = new TypedValue();
            c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, fondo, true);
            f.setBackgroundResource(fondo.resourceId);
            f.setOnClickListener(al);
        }
        padre.addView(f);
        return f;
    }

    // Fila de valor: "etiqueta ........ valor"
    public static void valor(ViewGroup padre, String etiqueta, String valor) {
        LinearLayout f = fila(padre, etiqueta, null, null);
        TextView e = (TextView) ((LinearLayout) f.getChildAt(0)).getChildAt(0);
        e.setTypeface(Typeface.DEFAULT);
        TextView v = new TextView(padre.getContext());
        v.setText(valor);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        v.setTextColor(color(padre.getContext(), R.color.tinta));
        f.addView(v);
    }

    public static TextView insignia(ViewGroup padre, String txt, Nivel n) {
        Context c = padre.getContext();
        TextView t = new TextView(c);
        t.setText(txt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(color(c, n == Nivel.NINGUNO ? R.color.tinta_2 : n.color));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(dp(c, 999));
        fondo.setColor(color(c, n.colorSuave));
        t.setBackground(fondo);
        t.setPadding(dp(c, 10), dp(c, 3), dp(c, 10), dp(c, 3));
        padre.addView(t);
        return t;
    }

    // Aviso de color (ok / atención / error)
    public static TextView aviso(ViewGroup padre, String txt, Nivel n) {
        Context c = padre.getContext();
        TextView t = new TextView(c);
        t.setText(txt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(color(c, R.color.tinta));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(dp(c, 14));
        fondo.setColor(color(c, n.colorSuave));
        fondo.setStroke(dp(c, 1), color(c, n.color));
        t.setBackground(fondo);
        int p = dp(c, 14);
        t.setPadding(p, p, p, p);
        padre.addView(t, separado(c, 12));
        return t;
    }

    public static MaterialButton boton(ViewGroup padre, String txt, int estilo, View.OnClickListener al) {
        Context c = padre.getContext();
        int attr = estilo == BOTON_BORDE ? com.google.android.material.R.attr.materialButtonOutlinedStyle
                : estilo == BOTON_TEXTO ? androidx.appcompat.R.attr.borderlessButtonStyle
                : com.google.android.material.R.attr.materialButtonStyle;
        MaterialButton b = new MaterialButton(c, null, attr);
        b.setText(txt);
        b.setAllCaps(false);
        b.setMinHeight(dp(c, 52));
        b.setOnClickListener(al);
        padre.addView(b, separado(c, 10));
        return b;
    }

    // Estado vacío: título + texto + botón opcional
    public static void vacio(ViewGroup padre, String titulo, String texto, String boton, View.OnClickListener al) {
        LinearLayout c = tarjeta(padre);
        int p = dp(padre.getContext(), 20);
        c.setPadding(p, p, p, p);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView t = texto(c, titulo, 19, true, R.color.tinta);
        t.setGravity(Gravity.CENTER);
        TextView x = parrafo(c, texto);
        x.setGravity(Gravity.CENTER);
        if (boton != null) {
            boton(c, boton, BOTON_LLENO, al);
        }
    }

    // Pide iniciar sesión (cuentas: APP-10)
    public static void necesitaSesion(ViewGroup padre, Activity a, String titulo, String texto) {
        LinearLayout c = tarjeta(padre);
        int p = dp(a, 20);
        c.setPadding(p, p, p, p);
        texto(c, titulo, 19, true, R.color.tinta);
        parrafo(c, texto);
        boton(c, "Iniciar sesión", BOTON_LLENO, v -> a.startActivity(new Intent(a, LoginActivity.class)));
        boton(c, "Crear cuenta", BOTON_BORDE, v -> a.startActivity(new Intent(a, RegistroActivity.class)));
    }

    // Campo de texto con etiqueta y mensaje de error (Material)
    public static TextInputLayout campo(ViewGroup padre, String etiqueta, int tipoEntrada) {
        Context c = padre.getContext();
        TextInputLayout campo = new TextInputLayout(c, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        campo.setHint(etiqueta);
        TextInputEditText e = new TextInputEditText(campo.getContext());
        e.setSingleLine(true);          // antes que setInputType: si no, la contraseña se vería
        e.setInputType(tipoEntrada);
        campo.addView(e);
        if ((tipoEntrada & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            campo.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }
        padre.addView(campo, separado(c, 14));
        return campo;
    }

    public static String leer(TextInputLayout campo) {
        return campo.getEditText() == null || campo.getEditText().getText() == null
                ? "" : campo.getEditText().getText().toString();
    }

    public static void confirmar(Context c, String titulo, String mensaje, String ok, Runnable accion) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(ok, (d, w) -> accion.run())
                .show();
    }
}
