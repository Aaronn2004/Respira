package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.List;

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
        public final String nivel;    // "alto" | "medio" (en los picos: null)

        public Evento(long desde, long hasta, double valor, String nivel) {
            this.desde = desde;
            this.hasta = hasta;
            this.valor = valor;
            this.nivel = nivel;
        }
    }

    public final Double[] valores;
    public final Double[] aprox;
    public final List<Evento> alertas = new ArrayList<>();
    public final List<Evento> picos = new ArrayList<>();

    public Serie(Double[] valores, Double[] aprox) {
        this.valores = valores;
        this.aprox = aprox == null ? new Double[valores.length] : aprox;
    }
}
