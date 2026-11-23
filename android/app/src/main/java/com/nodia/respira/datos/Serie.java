package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.List;

// # Serie temporal con valores observados, estimaciones y eventos separados por procedencia.
// # Ausente representa la ausencia lógica (null en el código).

// ==============================================================
// Serie: evolución de una magnitud en una zona (GET /zonas/{id}/serie).
//   valores  media de cada tramo (null = sin datos)
//   aprox    huecos cortos (hasta 2 h) aproximados por el servidor;
//            no cuentan en mínimo, media y máximo
//   alertas  ratos seguidos en nivel Alto (o Medio si no hubo Alto)
//   picos    los 3 valores más altos medidos, con su hora
// ==============================================================
public class Serie {

    public static class Evento {
        public final long desde;      // ms (en los picos: el momento)
        public final long hasta;      // ms (en los picos: igual que desde)
        public final double valor;    // máximo del rato o valor del pico
        public final String nivel;
        public final String origen;

        // --------------------------------------------------------------
        // + desde: Z, hasta: Z, valor: R, nivel: Text, origen: Text
        //       -->
        //     Evento() -->
        // --------------------------------------------------------------
        public Evento(long desde, long hasta, double valor, String nivel, String origen) {
            this.origen = origen;
            this.desde = desde;
            this.hasta = hasta;
            this.valor = valor;
            this.nivel = nivel;
        }
    }

    public final Double[] valores;
    public final Double[] aprox;
    public final long desde;          // instante UTC en ms; no es el inicio del día
    public final long hasta;
    public final long pasoMs;
    public final List<Evento> alertas = new ArrayList<>();
    public final List<Evento> picos = new ArrayList<>();
    public final List<Evento> avisos = new ArrayList<>();
    public String[] origen;
    public Double minimo;
    public Double media;
    public Double maximo;
    public boolean conResumen;

    // --------------------------------------------------------------
    // +
    //     tieneSimulacion() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    public boolean tieneSimulacion() {
        if (origen == null) return false;
        for (String o : origen) if ("simulado".equals(o)) return true;
        return false;
    }

    // --------------------------------------------------------------
    // + valores: [R | Ausente], aprox: [R | Ausente] | Ausente
    //       -->
    //     Serie() -->
    // --------------------------------------------------------------
    public Serie(Double[] valores, Double[] aprox) {
        this(valores, aprox, 0, 0, 0);
    }

    public Serie(Double[] valores, Double[] aprox, long desde, long hasta, long pasoMs) {
        // Sin metadatos se conserva la serie, pero no se inventan horas.
        if (desde != 0 || hasta != 0 || pasoMs != 0) {
            if (hasta <= desde || pasoMs <= 0
                    || 1 + (hasta - desde - 1) / pasoMs != valores.length) {
                throw new IllegalArgumentException("Intervalo temporal incompatible con la serie");
            }
        }
        this.valores = valores;
        this.aprox = aprox == null ? new Double[valores.length] : aprox;
        this.desde = desde;
        this.hasta = hasta;
        this.pasoMs = pasoMs;
    }

    public boolean tieneIntervalo() {
        return pasoMs > 0;
    }

    // Cada punto representa la media del intervalo, situada en su centro.
    public long instanteDelTramo(int indice) {
        if (!tieneIntervalo()) throw new IllegalStateException("Serie sin intervalo temporal");
        if (indice < 0 || indice >= valores.length) throw new IndexOutOfBoundsException();
        long inicio = desde + indice * pasoMs;
        return inicio + Math.min(pasoMs, hasta - inicio) / 2;
    }

    public double posicionTemporal(long instante) {
        if (!tieneIntervalo()) throw new IllegalStateException("Serie sin intervalo temporal");
        return (instante - desde) / (double) (hasta - desde);
    }
}
