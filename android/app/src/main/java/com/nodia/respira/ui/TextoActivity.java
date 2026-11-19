package com.nodia.respira.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.BuildConfig;
import com.nodia.respira.R;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Salud;

// ==============================================================
// Textos fijos: guía de valores, privacidad y acerca de.
// ==============================================================
// --------------------------------------------------------------
// class TextoActivity
// # Textos de ayuda, privacidad y presentación.
// # AppCompatActivity <--- TextoActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Entorno = { densidad: R, tema: Text }
// Vista = { contenido: Text, visible: B, habilitada: B }
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class TextoActivity extends AppCompatActivity {

    public static final String GUIA = "guia";
    public static final String PRIVACIDAD = "privacidad";
    public static final String ACERCA = "acerca";
    private static final String EXTRA = "texto";

    // --------------------------------------------------------------
    // # effects: abre la pantalla de destino
    // + c: Entorno, cual: Text
    //       -->
    //           abrir() --x
    // --------------------------------------------------------------
    public static void abrir(Context c, String cual) {
        c.startActivity(new Intent(c, TextoActivity.class).putExtra(EXTRA, cual));
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onCreate() -->
    // --------------------------------------------------------------
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

    // Mismo contenido y diseño que «Qué significan los valores» de la web
    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - c: Vista
    //       -->
    //           guia() -->
    // --------------------------------------------------------------
    private void guia(LinearLayout c) {
        Ui.parrafo(c, "Cada zona se colorea según la media de la última hora.");
        for (Magnitud m : Magnitud.values()) {
            LinearLayout card = Bloques.tarjetaConCabecera(c, m, m.nombre,
                    (m == Magnitud.O3 ? m.abreviatura + " · " : "") + m.unidad, true);
            Bloques.barraNiveles(card);
            Nivel[] niveles = {Nivel.BAJO, Nivel.MEDIO, Nivel.ALTO};
            for (int k = 0; k < niveles.length; k++) {
                if (k > 0) {
                    Bloques.separador(card);
                }
                Bloques.filaNivel(card, niveles[k], Salud.rango(m, niveles[k]), Salud.significado(m, niveles[k]));
            }
            Bloques.pequeno(card, Salud.fuenteGuia(m), 6);
        }

        LinearLayout est = Ui.tarjeta(c);
        int p = Ui.dp(this, 16);
        est.setPadding(p, p, p, p);
        TextView titulo = Ui.etiqueta(est, "Estado del dato");
        ((LinearLayout.LayoutParams) titulo.getLayoutParams()).topMargin = 0;
        Bloques.filaEstado(est, R.color.nivel_bajo, false, "Reciente", "Medido en la última hora.");
        Bloques.filaEstado(est, R.color.tinta_2, false, "Antiguo", "Medido hace entre 1 y 24 horas. Se muestra con su hora.");
        Bloques.filaEstado(est, 0, true, "Sin datos", "Nadie con sensor ha pasado en 24 horas. Trama gris: no significa «todo bien».");
        Bloques.filaEstado(est, R.color.primario, true, "Estimado", "Valor aproximado. En el mapa usa un contorno punteado y en la gráfica una línea de puntos.");
        Bloques.filaEstado(est, R.color.tinta_3, false, "Simulado", "Valor de demostración generado por el servidor.");
        Bloques.pequeno(est, "Parques con contorno; barrios en tono suave. Los sensores de bajo coste no sustituyen a las "
                + "estaciones ni a los avisos oficiales.", 8);

        LinearLayout ir = Ui.tarjeta(c);
        Ui.fila(ir, "Efectos en la salud", "Efectos de la exposición y referencias sanitarias",
                v -> startActivity(new Intent(this, EfectosActivity.class)));
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - c: Vista
    //       -->
    //           privacidad() -->
    // --------------------------------------------------------------
    private void privacidad(LinearLayout c) {
        Ui.parrafo(c, "Qué guardamos y para qué:");
        LinearLayout card = Ui.tarjeta(c);
        Ui.fila(card, "Acceso", "Necesitas iniciar sesión para utilizar Respira.", null);
        Ui.fila(card, "Tu cuenta", "Nombre de usuario, correo e ID, para el préstamo del sensor y tus puntos. Los parques que sigues y sus alertas se guardan en tu cuenta.", null);
        Ui.fila(card, "Mediciones", "Cada dato se envía con su ubicación, pero el mapa solo publica medias por zona. Tu recorrido no se publica nunca.", null);
        Ui.fila(card, "Percepciones", "Se muestran 24 h en el mapa sin tu nombre. Puedes borrarlas cuando quieras.", null);
        Ui.fila(card, "Borrar todo", "Más › Mi cuenta › Borrar mi cuenta.", null);
        Ui.nota(c, "Responsable: Ayuntamiento (servicio Respira, desarrollado por Nodia). Texto provisional del proyecto PBIO: no es un aviso legal definitivo.");
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - c: Vista
    //       -->
    //           acerca() -->
    // --------------------------------------------------------------
    private void acerca(LinearLayout c) {
        Ui.titulo(c, "Respira " + BuildConfig.VERSION_NAME);
        Ui.parrafo(c, "Calidad del aire, temperatura y ruido de tu ciudad por zonas, medidos con sensores móviles.");
        Ui.parrafo(c, "Developed by Nodia · Proyecto de Biometría y Medio Ambiente (GTI, UPV Gandia).");
        Ui.nota(c, "Mapas © Google.");
    }
}
