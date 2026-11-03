package com.nodia.respira.datos;

// ==============================================================
// Salud: textos de «Qué significan los valores» y de «Efectos en
// la salud» (los mismos que la web). Solo datos: sin Android.
// Fuentes: ozono, RD 102/2011 y guías OMS 2021; calor, Plan Nacional
// de Exceso de Temperaturas (Ministerio de Sanidad); ruido, guías de
// ruido ambiental OMS Europa 2018 y RD 286/2006.
// ==============================================================
public final class Salud {

    private Salud() { }

    // ---------------- guía de valores ----------------

    // --------------------------------------------------------------
    // magnitud, nivel --> significado() --> Text (una frase)
    // --------------------------------------------------------------
    public static String significado(Magnitud m, Nivel n) {
        switch (m) {
            case O3:
                return n == Nivel.BAJO ? "Aire sin problemas para nadie."
                        : n == Nivel.MEDIO ? "Quien hace deporte intenso puede notarlo."
                        : "Supera el umbral de información de la UE: evita el esfuerzo al aire libre.";
            case TEMP:
                return n == Nivel.BAJO ? "Temperatura agradable."
                        : n == Nivel.MEDIO ? "Calor: busca sombra y bebe agua."
                        : "Riesgo para mayores, niños y enfermos crónicos.";
            default:
                return n == Nivel.BAJO ? "Ambiente tranquilo."
                        : n == Nivel.MEDIO ? "Ruido de tráfico habitual: molesta y cansa."
                        : "Si es habitual, daña la salud (sueño, tensión, corazón).";
        }
    }

    public static String fuenteGuia(Magnitud m) {
        return m == Magnitud.O3 ? "Referencia europea de 120 y 180 µg/m³ (≈ 0,060 y 0,090 ppm)"
                : "Valores de ejemplo del prototipo, pendientes de fuente oficial";
    }

    // --------------------------------------------------------------
    // magnitud, nivel --> rango() --> Text   p. ej. "< 0.060", "0.060 – 0.090", "≥ 0.090"
    // (mismo criterio que Magnitud.nivel(): el límite ya cuenta como el nivel de arriba)
    // --------------------------------------------------------------
    public static String rango(Magnitud m, Nivel n) {
        String medio = m.formatear(m.limiteMedio), alto = m.formatear(m.limiteAlto);
        return n == Nivel.BAJO ? "< " + medio : n == Nivel.MEDIO ? medio + " – " + alto : "≥ " + alto;
    }

    // ---------------- efectos en la salud («Qué te puede pasar») ----------------

    public static final int MOLESTA = 0;
    public static final int DANA = 1;
    public static final int PELIGRO = 2;
    public static final String[] NOMBRE_PASO = {"Molesta", "Daña", "Peligro"};

    public static final class Paso {
        public final int gravedad;     // MOLESTA, DANA o PELIGRO
        public final String valor;     // p. ej. "0,090 ppm"
        public final String detalle;   // p. ej. "1 h" (puede ser "")
        public final String efecto;

        Paso(int gravedad, String valor, String detalle, String efecto) {
            this.gravedad = gravedad;
            this.valor = valor;
            this.detalle = detalle;
            this.efecto = efecto;
        }
    }

    public static final class Efecto {
        public final Magnitud magnitud;
        public final String nombre;
        public final String lema;
        public final Paso[] pasos;
        public final String[] peorPara;
        public final String hacer;
        public final String fuente;

        Efecto(Magnitud magnitud, String nombre, String lema, Paso[] pasos, String[] peorPara,
               String hacer, String fuente) {
            this.magnitud = magnitud;
            this.nombre = nombre;
            this.lema = lema;
            this.pasos = pasos;
            this.peorPara = peorPara;
            this.hacer = hacer;
            this.fuente = fuente;
        }
    }

    public static final Efecto[] EFECTOS = {
        new Efecto(Magnitud.O3, "Ozono", "Irrita los pulmones las tardes de sol", new Paso[]{
            new Paso(MOLESTA, "0,060 ppm", "8 h", "Te ahogas antes si haces deporte"),
            new Paso(DANA, "0,090 ppm", "1 h", "Ojos y garganta irritados, tos, duele respirar hondo"),
            new Paso(PELIGRO, "0,120 ppm", "1 h", "Crisis de asma o EPOC; los pulmones rinden menos, también en sanos")},
            new String[]{"Niños", "Mayores", "Asma o EPOC", "Deportistas"},
            "Nada de deporte fuera de 12 a 20 h",
            "UE (RD 102/2011) · OMS 2021"),
        new Efecto(Magnitud.TEMP, "Calor", "El cuerpo deja de poder enfriarse", new Paso[]{
            new Paso(MOLESTA, "28–30 °C", "máxima", "Deshidratación, calambres y dolor de cabeza en personas sensibles"),
            new Paso(DANA, "> 20 °C", "de noche", "No descansas: el cuerpo no se recupera y el corazón se fuerza"),
            new Paso(PELIGRO, "> 40 °C", "en el cuerpo", "Golpe de calor: confusión y piel seca y ardiendo. Llama al 112")},
            new String[]{"Mayores de 65", "Bebés", "Embarazadas", "Enfermos crónicos"},
            "Sombra, agua sin esperar a tener sed y evita las horas centrales",
            "Ministerio de Sanidad, plan de calor 2026"),
        new Efecto(Magnitud.RUIDO, "Ruido", "Te daña aunque te acostumbres", new Paso[]{
            new Paso(MOLESTA, "45 dB", "noche", "Duermes peor aunque no llegues a despertarte"),
            new Paso(DANA, "53–65 dB", "día", "Estrés, tensión alta y más riesgo de infarto con los años"),
            new Paso(PELIGRO, "85 dB", "horas", "Pitidos y sordera que no tienen vuelta atrás")},
            new String[]{"Niños", "Mayores", "Tensión alta o corazón"},
            "Duerme en el cuarto más tranquilo y pasea por calles sin tráfico",
            "OMS Europa 2018 · RD 286/2006"),
    };

    public static final String AVISO = "Orientativo: no sustituye al médico. El mapa da medias de la última hora. "
            + "Si hay síntomas graves, llama al 112.";
}
