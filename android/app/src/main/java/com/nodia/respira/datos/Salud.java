package com.nodia.respira.datos;

// ==============================================================
// Salud: textos de «Qué significan los valores» y de «Efectos en
// la salud» (los mismos que la web). Solo datos: sin Android.
// Fuentes: ozono, RD 102/2011 y guías OMS 2021; calor, Plan Nacional
// de Exceso de Temperaturas (Ministerio de Sanidad); ruido, guías de
// ruido ambiental OMS Europa 2018 y RD 286/2006.
// ==============================================================
public final class Salud {

    // --------------------------------------------------------------
    // -
    //     Salud() -->
    // --------------------------------------------------------------
    private Salud() { }

    // ---------------- guía de valores ----------------

    // --------------------------------------------------------------
    // + m: Magnitud, n: Nivel
    //       -->
    //     significado() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String significado(Magnitud m, Nivel n) {
        return n == Nivel.BAJO ? "Intervalo inferior de la escala de Respira."
                : n == Nivel.MEDIO ? "Intervalo intermedio de la escala de Respira."
                : "Intervalo superior de la escala de Respira.";
    }

    // --------------------------------------------------------------
    // + m: Magnitud
    //       -->
    //     fuenteGuia() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String fuenteGuia(Magnitud m) {
        switch (m) {
            case O3:
                return "Referencia oficial (RD 102/2011): 120 µg/m³ en 8 h y 180 µg/m³ en 1 h. "
                        + "La escala de Respira clasifica medias horarias en ppm.";
            case TEMP:
                return "La escala es orientativa. El Ministerio de Sanidad usa máximas diarias "
                        + "y umbrales distintos por zona de meteosalud.";
            default:
                return "La escala es orientativa. Para tráfico vial la OMS usa 53 dB Lden y 45 dB Lnight, "
                        + "indicadores de exposición a largo plazo.";
        }
    }

    // --------------------------------------------------------------
    // + m: Magnitud, n: Nivel
    //       -->
    //     rango() --x
    //       <--
    //   Text
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

        // --------------------------------------------------------------
        // - gravedad: Z, valor: Text, detalle: Text, efecto: Text
        //       -->
        //     Paso() -->
        // --------------------------------------------------------------
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

        // --------------------------------------------------------------
        // - magnitud: Magnitud, nombre: Text, lema: Text, pasos: [Paso], peor_para: [Text], hacer: Text, fuente: Text
        //       -->
        //     Efecto() -->
        // --------------------------------------------------------------
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
        new Efecto(Magnitud.O3, "Ozono", "Puede irritar las vías respiratorias", new Paso[]{
            new Paso(MOLESTA, "Al respirar", "", "Puede causar tos o irritación de garganta."),
            new Paso(DANA, "Con ejercicio", "", "La respiración rápida aumenta la exposición."),
            new Paso(PELIGRO, "Personas sensibles", "", "Puede agravar el asma y otras enfermedades respiratorias.")},
            new String[]{"Niños", "Mayores", "Asma o EPOC", "Deportistas"},
            "Consulta los avisos oficiales y adapta el esfuerzo al aire libre.",
            "OMS 2021 · RD 102/2011"),
        new Efecto(Magnitud.TEMP, "Calor", "La exposición prolongada aumenta el riesgo", new Paso[]{
            new Paso(MOLESTA, "Con calor", "", "Pueden aparecer calambres, dolor de cabeza o deshidratación."),
            new Paso(DANA, "Con exposición intensa", "", "El calor puede causar agotamiento."),
            new Paso(PELIGRO, "Golpe de calor", "", "La confusión o pérdida de consciencia pueden indicar una emergencia.")},
            new String[]{"Mayores de 65", "Bebés", "Embarazadas", "Enfermos crónicos"},
            "Busca sombra, hidrátate y sigue los avisos de tu zona. Si hay síntomas graves, llama al 112.",
            "Ministerio de Sanidad · Plan nacional de calor"),
        new Efecto(Magnitud.RUIDO, "Ruido", "Los efectos dependen de intensidad y duración", new Paso[]{
            new Paso(MOLESTA, "Por la noche", "", "El ruido puede interrumpir el sueño."),
            new Paso(DANA, "Exposición habitual", "", "El ruido ambiental se asocia con molestias y efectos cardiovasculares."),
            new Paso(PELIGRO, "Sonidos intensos", "", "La exposición intensa puede afectar a la audición.")},
            new String[]{"Niños", "Mayores", "Personas con problemas de sueño o cardiovasculares"},
            "Reduce la exposición cuando puedas y busca zonas tranquilas.",
            "OMS Europa 2018 · Guías de ruido ambiental"),
    };

    public static final String AVISO = "Orientativo: no sustituye al médico. El mapa da medias de la última hora. "
            + "Si hay síntomas graves, llama al 112.";
}
