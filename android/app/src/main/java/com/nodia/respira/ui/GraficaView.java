package com.nodia.respira.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;

import com.nodia.respira.R;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Serie;

import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;

// ==============================================================
// GraficaView: evolución de una magnitud con bandas de nivel.
// Huecos cortos (hasta 2 h) que el servidor aproxima: línea de puntos.
// Huecos largos (null y sin aproximación): franja gris, «sin datos».
// ==============================================================
// --------------------------------------------------------------
// class GraficaView
// # Gráfica que distingue observaciones, simulaciones y aproximaciones.
// # View <--- GraficaView
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Entorno = { densidad: R, tema: Text }
// Atributos = Map[Text, Text]
// Lienzo = { ancho: R, alto: R }
// Escala = { minimo: R, maximo: R, altura: R }
// --------------------------------------------------------------
public class GraficaView extends View {

    private Double[] datos = new Double[0];
    private Double[] aprox = new Double[0];
    private String[] origen;
    private Magnitud magnitud = Magnitud.O3;
    private Serie serie;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<List<ReferenciasGrafica.Etiqueta>> paginas = new ArrayList<>();
    private int paginaReferencias;
    private int totalNotificado = -1;
    private BiConsumer<Integer, Integer> alCambiarReferencias;

    public void alCambiarReferencias(BiConsumer<Integer, Integer> callback) {
        alCambiarReferencias = callback;
        totalNotificado = -1;
        requestLayout();
    }

    public void cambiarReferencias(int cambio) {
        paginaReferencias = Math.max(0, Math.min(Math.max(0, paginas.size() - 1), paginaReferencias + cambio));
        if (alCambiarReferencias != null) alCambiarReferencias.accept(paginaReferencias, paginas.size());
        invalidate();
    }

    private Paint textoReferencia() {
        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setTextSize(Ui.dp(getContext(), 10));
        return texto;
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + c: Entorno
    //       -->
    //           GraficaView() -->
    // --------------------------------------------------------------
    public GraficaView(Context c) {
        super(c);
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + c: Entorno, a: Atributos
    //       -->
    //           GraficaView() -->
    // --------------------------------------------------------------
    public GraficaView(Context c, AttributeSet a) {
        super(c, a);
    }

    // --------------------------------------------------------------
    // # Una aproximación ausente o de distinta longitud se representa con huecos.
    // # effects: actualiza la interfaz Android
    // + serie: Serie, m: Magnitud
    //       -->
    //           mostrar() -->
    // --------------------------------------------------------------
    public void mostrar(Serie serie, Magnitud m) {
        this.serie = serie;
        this.origen = serie.origen;
        this.datos = serie.valores;
        this.aprox = serie.aprox == null || serie.aprox.length != datos.length ? new Double[datos.length] : serie.aprox;
        this.magnitud = m;
        paginaReferencias = 0;
        totalNotificado = -1;
        StringBuilder descripcion = new StringBuilder("Gráfica de " + m.nombre + ". Línea: medias por intervalo.");
        for (GraficaHistorica.Pico pico : GraficaHistorica.picos(serie)) {
            descripcion.append(" ").append(pico.etiqueta).append(": ")
                    .append(m.formatear(pico.evento.valor)).append(" ").append(m.unidad)
                    .append(", ").append(ZonaActivity.hora(pico.evento.desde, true)).append(".");
        }
        if (serie.tieneIntervalo()) {
            for (Serie.Evento alerta : serie.alertas) {
                if ("alto".equals(alerta.nivel) && alerta.desde < serie.hasta && alerta.hasta > serie.desde
                        && alerta.desde < alerta.hasta) {
                    descripcion.append(" En Alto: ").append(ZonaActivity.franja(
                            Math.max(serie.desde, alerta.desde), Math.min(serie.hasta, alerta.hasta))).append(".");
                }
            }
        }
        setContentDescription(descripcion.toString());
        requestLayout();
        invalidate();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + w: Z, h: Z
    //       -->
    //           onMeasure() -->
    // --------------------------------------------------------------
    @Override
    protected void onMeasure(int w, int h) {
        int ancho = MeasureSpec.getSize(w);
        double util = ancho - Ui.dp(getContext(), 48);
        Paint texto = textoReferencia();
        paginas = serie == null ? new ArrayList<>() : ReferenciasGrafica.paginas(
                ReferenciasGrafica.eventos(serie, util, Ui.dp(getContext(), 48)),
                util, texto::measureText, Ui.dp(getContext(), 4));
        paginaReferencias = Math.min(paginaReferencias, Math.max(0, paginas.size() - 1));
        setMeasuredDimension(ancho, Ui.dp(getContext(), paginas.isEmpty() ? 230 : 342));
        if (totalNotificado != paginas.size()) {
            totalNotificado = paginas.size();
            post(() -> {
                if (alCambiarReferencias != null) alCambiarReferencias.accept(paginaReferencias, paginas.size());
            });
        }
    }

    // --------------------------------------------------------------
    // # Las aproximaciones no sustituyen los valores observados ni unen huecos largos.
    // # effects: actualiza la interfaz Android
    // + canvas: Lienzo
    //       -->
    //           onDraw() -->
    // --------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (datos.length < 2) {
            return;
        }
        Context c = getContext();
        float w = getWidth(), h = getHeight();
        // Solo se usa el recorte de las franjas; ya no se dibujan rótulos En Alto arriba.
        List<GraficaHistorica.Alto> altos = GraficaHistorica.altos(serie, 1, 1);
        float x0 = Ui.dp(c, 40), y0 = h - Ui.dp(c, paginas.isEmpty() ? 38 : 150),
                arriba = Ui.dp(c, 10), dcha = w - Ui.dp(c, 8);
        if (dcha <= x0) return;
        double[] limites = GraficaHistorica.limites(serie, magnitud);
        if (limites == null) return;
        final double min = limites[0], max = limites[1];
        java.util.function.DoubleUnaryOperator Y = v -> y0 - (v - min) / (max - min) * (y0 - arriba);
        java.util.function.IntToDoubleFunction X = i -> x0 + (dcha - x0)
                * (serie.tieneIntervalo() ? serie.posicionTemporal(serie.instanteDelTramo(i))
                : (i + 0.5) / datos.length);

        // bandas de nivel
        banda(canvas, x0, dcha, Y, min, magnitud.limiteMedio, Ui.color(c, R.color.nivel_bajo_suave));
        banda(canvas, x0, dcha, Y, magnitud.limiteMedio, magnitud.limiteAlto, Ui.color(c, R.color.nivel_medio_suave));
        banda(canvas, x0, dcha, Y, magnitud.limiteAlto, max, Ui.color(c, R.color.nivel_alto_suave));

        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Ui.color(c, R.color.tinta_3));
        txt.setTextSize(Ui.dp(c, 10));
        txt.setTextAlign(Paint.Align.RIGHT);
        float anchoEtiqueta = Math.max(txt.measureText("00:00 +00"), txt.measureText("00/00")) + Ui.dp(c, 12);
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(serie, dcha - x0, anchoEtiqueta);

        // huecos sin datos
        Paint gris = new Paint();
        gris.setColor(Ui.color(c, R.color.superficie_2));
        txt.setTextAlign(Paint.Align.CENTER);
        int ini = -1;
        for (int i = 0; i <= datos.length; i++) {
            boolean hueco = i < datos.length && datos[i] == null && aprox[i] == null;
            if (hueco && ini < 0) {
                ini = i;
            } else if (!hueco && ini >= 0) {
                float a = x0 + (dcha - x0) * (float) (serie.tieneIntervalo()
                        ? serie.posicionTemporal(serie.desde + ini * serie.pasoMs) : ini / (double) datos.length);
                float b = x0 + (dcha - x0) * (float) (serie.tieneIntervalo()
                        ? serie.posicionTemporal(Math.min(serie.hasta, serie.desde + i * serie.pasoMs)) : i / (double) datos.length);
                canvas.drawRect(a, arriba, b, y0, gris);
                if (i - 1 - ini > 3) {
                    canvas.drawText("sin datos", (a + b) / 2, (arriba + y0) / 2, txt);
                }
                ini = -1;
            }
        }

        // Fondo temporal Alto sobre los fondos de nivel/huecos, debajo de todas las líneas.
        Paint fondoAlto = new Paint();
        fondoAlto.setColor(Ui.color(c, R.color.nivel_alto));
        fondoAlto.setAlpha(28);
        for (GraficaHistorica.Alto alto : altos) {
            float a = x0 + (float) alto.inicio * (dcha - x0);
            float b = x0 + (float) alto.fin * (dcha - x0);
            canvas.drawRect(a, arriba, b, y0, fondoAlto);
        }

        // Guías temporales sobre los fondos, de arriba abajo y con el mismo instante que la etiqueta.
        Paint guia = new Paint(Paint.ANTI_ALIAS_FLAG);
        int tinta = Ui.color(c, R.color.tinta_3);
        int grisGuia = (Color.red(tinta) + Color.green(tinta) + Color.blue(tinta)) / 3;
        guia.setColor(Color.rgb(grisGuia, grisGuia, grisGuia));
        guia.setAlpha(180);
        guia.setStyle(Paint.Style.STROKE);
        guia.setStrokeWidth(Ui.dp(c, 1.2f));
        guia.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 4), Ui.dp(c, 4)}, 0));
        for (GraficaHistorica.Marca marca : marcas) {
            float x = x0 + (float) marca.posicion * (dcha - x0);
            canvas.drawLine(x, arriba, x, y0, guia);
        }

        Paint bordeAlto = new Paint(Paint.ANTI_ALIAS_FLAG);
        bordeAlto.setColor(Ui.color(c, R.color.nivel_alto));
        bordeAlto.setAlpha(170);
        bordeAlto.setStyle(Paint.Style.STROKE);
        bordeAlto.setStrokeWidth(Ui.dp(c, 1));
        bordeAlto.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 3), Ui.dp(c, 3)}, 0));
        for (GraficaHistorica.Alto alto : altos) {
            float a = x0 + (float) alto.inicio * (dcha - x0);
            float b = x0 + (float) alto.fin * (dcha - x0);
            canvas.drawLine(a, arriba, a, y0, bordeAlto);
            canvas.drawLine(b, arriba, b, y0, bordeAlto);
        }

        // Referencias de la página actual: la proyección vertical conserva el instante del evento.
        List<ReferenciasGrafica.Etiqueta> referencias = paginas.isEmpty()
                ? new ArrayList<>() : paginas.get(paginaReferencias);
        Paint proyeccion = new Paint(Paint.ANTI_ALIAS_FLAG);
        proyeccion.setStrokeWidth(Ui.dp(c, 1));
        proyeccion.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 2), Ui.dp(c, 3)}, 0));
        for (ReferenciasGrafica.Etiqueta etiqueta : referencias) {
            ReferenciasGrafica.Referencia r = etiqueta.referencia;
            proyeccion.setColor(Ui.color(c, r.pico ? R.color.primario : R.color.nivel_alto));
            proyeccion.setAlpha(130);
            float desdeY = arriba;
            if (r.pico) {
                for (GraficaHistorica.Pico pico : GraficaHistorica.picos(serie)) {
                    if (pico.etiqueta.equals(r.id)) desdeY = (float) Y.applyAsDouble(pico.evento.valor);
                }
            }
            for (double posicion : r.posiciones) {
                float x = x0 + (float) posicion * (dcha - x0);
                canvas.drawLine(x, desdeY, x, y0, proyeccion);
            }
        }

        // Umbrales originales, encima de los fondos temporales para conservar su legibilidad.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 1));
        p.setColor(Ui.color(c, R.color.tinta_3));
        p.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 4), Ui.dp(c, 4)}, 0));
        txt.setTextAlign(Paint.Align.RIGHT);
        for (double t : new double[]{magnitud.limiteMedio, magnitud.limiteAlto}) {
            if (t > min && t < max) {
                float y = (float) Y.applyAsDouble(t);
                canvas.drawLine(x0, y, dcha, y, p);
                canvas.drawText(magnitud.formatear(t), x0 - Ui.dp(c, 4), y + Ui.dp(c, 4), txt);
            }
        }
        p.setPathEffect(null);

        // línea y área
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 2.4f));
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(Ui.color(c, R.color.primario));
        DashPathEffect trazoSimulado = new DashPathEffect(new float[]{Ui.dp(c, 8), Ui.dp(c, 4)}, 0);
        for (int i = 0; i < datos.length; i++) {
            if (datos[i] == null) continue;
            float x = (float) X.applyAsDouble(i), y = (float) Y.applyAsDouble(datos[i]);
            if (i > 0 && datos[i - 1] != null) {
                p.setPathEffect(simulado(i) || simulado(i - 1) ? trazoSimulado : null);
                canvas.drawLine((float) X.applyAsDouble(i - 1), (float) Y.applyAsDouble(datos[i - 1]), x, y, p);
            } else if (i == datos.length - 1 || datos[i + 1] == null) {
                p.setPathEffect(null);
                canvas.drawCircle(x, y, Ui.dp(c, 2), p);
            }
        }
        p.setPathEffect(null);

        // huecos cortos aproximados: de punto real a punto real, en línea de puntos
        p.setStrokeWidth(Ui.dp(c, 2f));
        p.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 2), Ui.dp(c, 4)}, 0));
        for (int i = 0; i < datos.length; i++) {
            if (aprox[i] == null || datos[i] != null) {
                continue;
            }
            int a = i - 1;
            int b = i;
            while (b < datos.length && datos[b] == null && aprox[b] != null) {
                b++;
            }
            Path pa = new Path();
            if (a >= 0 && datos[a] != null) {
                pa.moveTo((float) X.applyAsDouble(a), (float) Y.applyAsDouble(datos[a]));
            } else {
                pa.moveTo((float) X.applyAsDouble(i), (float) Y.applyAsDouble(aprox[i]));
            }
            for (int k = i; k < b; k++) {
                pa.lineTo((float) X.applyAsDouble(k), (float) Y.applyAsDouble(aprox[k]));
            }
            if (b < datos.length && datos[b] != null) {
                pa.lineTo((float) X.applyAsDouble(b), (float) Y.applyAsDouble(datos[b]));
            }
            canvas.drawPath(pa, p);
            i = b - 1;
        }
        p.setPathEffect(null);

        Double ultimo = datos[datos.length - 1];
        if (ultimo != null) {
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle((float) X.applyAsDouble(datos.length - 1), (float) Y.applyAsDouble(ultimo), Ui.dp(c, 5), p);
        }

        // Rótulos junto a los picos, con conectores cortos que terminan en su coordenada exacta.
        if (serie.tieneIntervalo()) {
            Paint marcador = new Paint(Paint.ANTI_ALIAS_FLAG);
            marcador.setColor(Ui.color(c, R.color.primario));
            marcador.setStrokeWidth(Ui.dp(c, 1.5f));
            Paint rotulo = new Paint(Paint.ANTI_ALIAS_FLAG);
            rotulo.setColor(Ui.color(c, R.color.primario));
            rotulo.setTextSize(Ui.dp(c, 11));
            rotulo.setTextAlign(Paint.Align.CENTER);
            List<ReferenciasGrafica.RotuloPico> rotulos = ReferenciasGrafica.rotulosPicos(serie,
                    dcha - x0, y0 - arriba, min, max, rotulo.measureText("P3") + Ui.dp(c, 6),
                    Ui.dp(c, 16), Ui.dp(c, 7));
            for (ReferenciasGrafica.RotuloPico r : rotulos) {
                marcador.setAlpha(180);
                canvas.drawLine(x0 + (float) r.enlaceX, arriba + (float) r.enlaceY,
                        x0 + (float) r.x, arriba + (float) r.y, marcador);
            }
            Paint fondo = new Paint();
            fondo.setColor(Ui.color(c, R.color.superficie));
            for (ReferenciasGrafica.RotuloPico r : rotulos) {
                float a = x0 + (float) r.izquierda, b = arriba + (float) r.arriba;
                canvas.drawRoundRect(a, b, a + (float) r.ancho, b + (float) r.alto, Ui.dp(c, 3), Ui.dp(c, 3), fondo);
                canvas.drawText(r.pico.etiqueta, a + (float) r.ancho / 2,
                        b + (float) r.alto / 2 - (rotulo.ascent() + rotulo.descent()) / 2, rotulo);
            }
            // Los puntos se dibujan al final: ningún conector tapa el centro de otro pico.
            for (GraficaHistorica.Pico pico : GraficaHistorica.picos(serie)) {
                float x = x0 + (float) pico.posicion * (dcha - x0);
                float y = (float) Y.applyAsDouble(pico.evento.valor);
                marcador.setAlpha(255);
                marcador.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(x, y, Ui.dp(c, 4), marcador);
                marcador.setStyle(Paint.Style.FILL);
                canvas.drawCircle(x, y, Ui.dp(c, 1.5f), marcador);
            }
        }

        dibujarReferencias(canvas, referencias, x0, dcha, y0);

        // eje X
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(c, 1));
        p.setColor(Ui.color(c, R.color.linea));
        canvas.drawLine(x0, y0, dcha, y0, p);
        if (serie.tieneIntervalo()) {
            txt.setTextAlign(Paint.Align.CENTER);
            for (GraficaHistorica.Marca marca : marcas) {
                float x = x0 + (float) marca.posicion * (dcha - x0);
                float mitad = Math.max(txt.measureText(marca.fecha), txt.measureText(marca.hora)) / 2;
                float etiquetaX = Math.max(x0 + mitad, Math.min(dcha - mitad, x));
                Paint fondo = new Paint();
                fondo.setColor(Ui.color(c, R.color.superficie));
                canvas.drawRect(etiquetaX - mitad - Ui.dp(c, 2), y0 + Ui.dp(c, 7),
                        etiquetaX + mitad + Ui.dp(c, 2), y0 + Ui.dp(c, 35), fondo);
                canvas.drawText(marca.fecha, etiquetaX, y0 + Ui.dp(c, 18), txt);
                canvas.drawText(marca.hora, etiquetaX, y0 + Ui.dp(c, 32), txt);
            }
        } else {
            txt.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Horario no disponible", (x0 + dcha) / 2, h - Ui.dp(c, 6), txt);
        }
    }

    private void dibujarReferencias(Canvas canvas, List<ReferenciasGrafica.Etiqueta> referencias,
                                    float x0, float dcha, float y0) {
        Context c = getContext();
        Paint enlace = new Paint(Paint.ANTI_ALIAS_FLAG);
        enlace.setStrokeWidth(Ui.dp(c, 1));
        enlace.setAlpha(170);
        // Primero todos los conectores, luego las cajas: no atraviesan los textos.
        for (ReferenciasGrafica.Etiqueta e : referencias) {
            enlace.setColor(Ui.color(c, e.referencia.pico ? R.color.primario : R.color.nivel_alto));
            enlace.setAlpha(150);
            float a = x0 + (float) e.izquierda, b = a + (float) e.ancho;
            float top = y0 + Ui.dp(c, 46 + e.fila * 50);
            for (double posicion : e.referencia.posiciones) {
                float realX = x0 + (float) posicion * (dcha - x0);
                float textoX = Math.max(a + Ui.dp(c, 3), Math.min(b - Ui.dp(c, 3), realX));
                canvas.drawLine(realX, y0, textoX, top, enlace);
                canvas.drawCircle(realX, y0, Ui.dp(c, 2), enlace);
            }
        }
        Paint fondo = new Paint();
        fondo.setColor(Ui.color(c, R.color.superficie_2));
        Paint texto = textoReferencia();
        texto.setTextAlign(Paint.Align.CENTER);
        for (ReferenciasGrafica.Etiqueta e : referencias) {
            float a = x0 + (float) e.izquierda, b = a + (float) e.ancho;
            float top = y0 + Ui.dp(c, 46 + e.fila * 50);
            canvas.drawRoundRect(a, top, b, top + Ui.dp(c, 40), Ui.dp(c, 3), Ui.dp(c, 3), fondo);
            texto.setColor(Ui.color(c, e.referencia.pico ? R.color.primario : R.color.nivel_alto));
            float mayor = 1;
            texto.setTextSize(Ui.dp(c, 10));
            for (String linea : e.referencia.lineas) mayor = Math.max(mayor, texto.measureText(linea));
            texto.setTextSize(Ui.dp(c, 10) * Math.min(1, Math.max(1, b - a - Ui.dp(c, 8)) / mayor));
            for (int i = 0; i < e.referencia.lineas.length; i++) {
                canvas.drawText(e.referencia.lineas[i], (a + b) / 2, top + Ui.dp(c, 12 + i * 12), texto);
            }
        }
    }

    // --------------------------------------------------------------
    // - i: N
    //       -->
    //           simulado() <--
    //       <--
    //   resultado: B
    // --------------------------------------------------------------
    private boolean simulado(int i) {
        return origen != null && i < origen.length && "simulado".equals(origen[i]);
    }

    // --------------------------------------------------------------
    // # effects: dibuja una banda sobre el lienzo recibido
    // - canvas: Lienzo, x0: R, x1: R, Y: Escala, a: R, b: R, color: Z
    //       -->
    //           banda() --x
    // --------------------------------------------------------------
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
