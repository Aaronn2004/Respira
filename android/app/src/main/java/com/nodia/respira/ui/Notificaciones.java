package com.nodia.respira.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.nodia.respira.R;

// ==============================================================
// Notificaciones: canales y envío.
// Las alertas las comprueba la app con los datos reales del servidor
// cada vez que lee las zonas (Repositorio.comprobarAlertas). Con la
// app cerrada no llegan: para eso haría falta push (FCM).
// ==============================================================
public final class Notificaciones {

    public static final String CANAL_ALERTAS = "alertas";
    public static final String CANAL_RECORRIDO = "recorrido";
    public static final String CANAL_PUNTOS = "puntos";

    private Notificaciones() { }

    public static void crearCanales(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        nm.createNotificationChannel(new NotificationChannel(CANAL_ALERTAS,
                c.getString(R.string.canal_alertas), NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CANAL_RECORRIDO,
                c.getString(R.string.canal_recorrido), NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CANAL_PUNTOS,
                c.getString(R.string.canal_puntos), NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static boolean permitidas(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(c).areNotificationsEnabled();
    }

    // --------------------------------------------------------------
    // c, canal, id, titulo, texto, destino: Intent --> mostrar() --> B (se ha mostrado)
    // --------------------------------------------------------------
    @SuppressWarnings("MissingPermission") // se comprueba en permitidas()
    public static boolean mostrar(Context c, String canal, int id, String titulo, String texto, Intent destino) {
        if (!permitidas(c)) {
            return false;
        }
        crearCanales(c);
        PendingIntent pi = PendingIntent.getActivity(c, id, destino,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, canal)
                .setSmallIcon(R.drawable.ic_respira_simbolo)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
