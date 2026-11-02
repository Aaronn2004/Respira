package com.nodia.respira.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;

import com.nodia.respira.R;
import com.nodia.respira.datos.Magnitud;

// ==============================================================
// GraficaView: evolución de una magnitud con bandas de nivel.
// Los huecos (null) se pintan en gris: «no pasó ningún sensor».
// No se rellenan ni se interpolan.
// ==============================================================
public class GraficaView extends View {

    private Double[] datos = new Double[0];
    private Magnitud magnitud = Magnitud.O3;
    private boolean dia = true;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GraficaView(Context c) {
        super(c);
    }

    public GraficaView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void mostrar(Double[] datos, Magnitud m, boolean dia) {
        this.datos = datos;
        this.magnitud = m;
        this.dia = dia;
        setContentDescription("Gráfica de " + m.nombre);
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int ancho = MeasureSpec.getSize(w);
        setMeasuredDimension(ancho, Ui.dp(getContext(), 210));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (datos.length < 2) {
            return;
        }
        Context c = getContext();
        float w = getWidth(), h = getHeight();
        float x0 = Ui.dp(c, 40), y0 = h - Ui.dp(c, 24), arriba = Ui.dp(c, 10), dcha = w - Ui.dp(c, 8);

        double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE;
        for (Double v : datos) {
            if (v != null) {
                mn = Math.min(mn, v);
                mx = Math.max(mx, v);
            }
        }
        if (mn == Double.MAX_VALUE) {
            return;
        }
        if (magnitud == Magnitud.TEMP) {
            mn -= 2;
            mx = Math.max(mx, magnitud.limiteMedio) + 2;
        } else {
            mn = Math.min(mn, magnitud.limiteMedio * 0.6);
            mx = Math.max(mx, magnitud.limiteAlto * 1.08);
        }
        final double min = mn, max = mx;
        java.util.function.DoubleUnaryOperator Y = v -> y0 - (v - min) / (max - min) * (y0 - arriba);
        java.util.function.IntToDoubleFunction X = i -> x0 + i * (dcha - x0) / (datos.length - 1);

        // bandas de nivel
        banda(canvas, x0, dcha, Y, min, magnitud.limiteMedio, Ui.color(c, R.color.nivel_bajo_suave));
        banda(canvas, x0, dcha, Y, magnitud.limiteMedio, magnitud.limiteAlto, Ui.color(c, R.color.nivel_medio_suave));
        banda(canvas, x0, dcha, Y, magnitud.limiteAlto, max, Ui.color(c, R.color.nivel_alto_suave));

        // umbrales
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 1));
        p.setColor(Ui.color(c, R.color.tinta_3));
        p.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 4), Ui.dp(c, 4)}, 0));
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Ui.color(c, R.color.tinta_3));
        txt.setTextSize(Ui.dp(c, 10));
        txt.setTextAlign(Paint.Align.RIGHT);
        for (double t : new double[]{magnitud.limiteMedio, magnitud.limiteAlto}) {
            if (t > min && t < max) {
                float y = (float) Y.applyAsDouble(t);
                canvas.drawLine(x0, y, dcha, y, p);
                canvas.drawText(magnitud.formatear(t), x0 - Ui.dp(c, 4), y + Ui.dp(c, 4), txt);
            }
        }
        p.setPathEffect(null);

        // huecos sin datos
        Paint gris = new Paint();
        gris.setColor(Ui.color(c, R.color.superficie_2));
        txt.setTextAlign(Paint.Align.CENTER);
        int ini = -1;
        for (int i = 0; i <= datos.length; i++) {
            boolean hueco = i < datos.length && datos[i] == null;
            if (hueco && ini < 0) {
                ini = i;
            } else if (!hueco && ini >= 0) {
                float a = (float) X.applyAsDouble(Math.max(ini, 0)), b = (float) X.applyAsDouble(i - 1);
                float medio = (float) ((X.applyAsDouble(1) - X.applyAsDouble(0)) / 2);
                canvas.drawRect(a - medio, arriba, b + medio, y0, gris);
                if (i - 1 - ini > 3) {
                    canvas.drawText("sin datos", (a + b) / 2, y0 / 2, txt);
                }
                ini = -1;
            }
        }

        // línea y área
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 2.4f));
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(Ui.color(c, R.color.primario));
        Path tramo = null;
        for (int i = 0; i < datos.length; i++) {
            if (datos[i] == null) {
                if (tramo != null) {
                    canvas.drawPath(tramo, p);
                }
                tramo = null;
                continue;
            }
            float x = (float) X.applyAsDouble(i), y = (float) Y.applyAsDouble(datos[i]);
            if (tramo == null) {
                tramo = new Path();
                tramo.moveTo(x, y);
            } else {
                tramo.lineTo(x, y);
            }
        }
        if (tramo != null) {
            canvas.drawPath(tramo, p);
        }
        Double ultimo = datos[datos.length - 1];
        if (ultimo != null) {
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle((float) X.applyAsDouble(datos.length - 1), (float) Y.applyAsDouble(ultimo), Ui.dp(c, 5), p);
        }

        // eje X
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 1));
        p.setColor(Ui.color(c, R.color.linea));
        canvas.drawLine(x0, y0, dcha, y0, p);
        String[] et = dia ? new String[]{"00:00", "06:00", "12:00", "18:00", "ahora"}
                : new String[]{"lun", "mié", "vie", "dom", "hoy"};
        for (int i = 0; i < et.length; i++) {
            txt.setTextAlign(i == 0 ? Paint.Align.LEFT : i == et.length - 1 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(et[i], x0 + i * (dcha - x0) / 4, h - Ui.dp(c, 6), txt);
        }
    }

    private void banda(Canvas canvas, float x0, float x1, java.util.function.DoubleUnaryOperator Y,
                       double a, double b, int color) {
        float ya = (float) Y.applyAsDouble(a), yb = (float) Y.applyAsDouble(b);
        if (yb < ya) {
            Paint f = new Paint();
            f.setColor(color);
            canvas.drawRect(x0, yb, x1, ya, f);
        }
    }
}
