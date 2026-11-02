package com.nodia.respira.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.BuildConfig;
import com.nodia.respira.datos.Magnitud;

// ==============================================================
// Textos fijos: guía de valores, privacidad y acerca de.
// ==============================================================
public class TextoActivity extends AppCompatActivity {

    public static final String GUIA = "guia";
    public static final String PRIVACIDAD = "privacidad";
    public static final String ACERCA = "acerca";
    private static final String EXTRA = "texto";

    public static void abrir(Context c, String cual) {
        c.startActivity(new Intent(c, TextoActivity.class).putExtra(EXTRA, cual));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String cual = getIntent().getStringExtra(EXTRA);
        if (GUIA.equals(cual)) {
            guia(Ui.pantalla(this, "Qué significan los valores"));
        } else if (PRIVACIDAD.equals(cual)) {
            privacidad(Ui.pantalla(this, "Privacidad"));
        } else {
            acerca(Ui.pantalla(this, "Acerca de"));
        }
    }

    private void guia(LinearLayout c) {
        Ui.parrafo(c, "Cada zona muestra la media de lo que han medido los sensores al pasar por ella en la última hora.");
        for (Magnitud m : Magnitud.values()) {
            LinearLayout card = Ui.tarjeta(c);
            Ui.fila(card, m.nombre + " (" + m.unidad + ")",
                    "Bajo: menos de " + m.formatear(m.limiteMedio) + "\nMedio: de " + m.formatear(m.limiteMedio)
                            + " a " + m.formatear(m.limiteAlto) + "\nAlto: " + m.formatear(m.limiteAlto) + " o más", null);
        }
        LinearLayout est = Ui.tarjeta(c);
        Ui.fila(est, "Antiguo", "El último dato tiene entre 1 y 24 horas. Puede no reflejar cómo está ahora.", null);
        Ui.fila(est, "Sin datos", "Nadie con sensor ha pasado en las últimas 24 horas. No significa que el aire esté limpio.", null);
        Ui.nota(c, "Umbrales orientativos. Ozono: 0,060 ppm ≈ 120 µg/m³ (valor objetivo UE, media de 8 h) y "
                + "0,090 ppm ≈ 180 µg/m³ (umbral de información UE, media de 1 h). Ruido: referencia OMS de 55 dB "
                + "para exteriores de día. Temperatura: valores de ejemplo, no son los umbrales oficiales de AEMET.");
    }

    private void privacidad(LinearLayout c) {
        Ui.parrafo(c, "Qué guardamos y para qué:");
        LinearLayout card = Ui.tarjeta(c);
        Ui.fila(card, "Sin cuenta", "Tus parques y alertas se guardan solo en este móvil.", null);
        Ui.fila(card, "Con cuenta", "Nombre de usuario, correo e ID. Sirven para el préstamo del sensor y para tus puntos.", null);
        Ui.fila(card, "Mediciones", "Cada dato se envía con su ubicación, pero el mapa solo publica medias por zona. Tu recorrido no se publica nunca.", null);
        Ui.fila(card, "Percepciones", "Se muestran 24 h en el mapa sin tu nombre. Puedes borrarlas cuando quieras.", null);
        Ui.fila(card, "Borrar todo", "Más › Mi cuenta › Borrar mi cuenta.", null);
        Ui.nota(c, "Responsable: Ayuntamiento (servicio Respira, desarrollado por Nodia). Texto provisional del proyecto PBIO: no es un aviso legal definitivo.");
    }

    private void acerca(LinearLayout c) {
        Ui.titulo(c, "Respira " + BuildConfig.VERSION_NAME);
        Ui.parrafo(c, "Calidad del aire, temperatura y ruido de tu ciudad por zonas, medidos con sensores móviles.");
        Ui.parrafo(c, "Developed by Nodia · Proyecto de Biometría y Medio Ambiente (GTI, UPV Gandia).");
        Ui.nota(c, "Mapas © Google.");
    }
}
