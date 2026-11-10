package com.nodia.respira;

import android.app.Application;
import android.content.Intent;

import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.ui.Notificaciones;
import com.nodia.respira.ui.Tema;
import com.nodia.respira.ui.ZonaActivity;

// Aplicación: aplica el modo claro u oscuro guardado antes de abrir ninguna
// pantalla y enseña las notificaciones de las alertas del usuario.
public class RespiraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Tema.aplicar(this);
        // Cuando una alerta del usuario se cumple con los datos reales
        Repositorio.obtener(this).alAvisarDeAlerta((a, z) -> {
            String titulo = a.magnitud.nombre + ("custom".equals(a.condicion) ? " por encima de tu límite"
                    : " en nivel " + a.condicion) + " en " + z.nombre;
            Intent ir = new Intent(this, ZonaActivity.class).putExtra(ZonaActivity.EXTRA_ZONA, z.id);
            Notificaciones.mostrar(this, Notificaciones.CANAL_ALERTAS, 100 + Math.abs(z.id.hashCode() % 100), titulo,
                    a.magnitud.formatear(z.valor(a.magnitud)) + " " + a.magnitud.unidad + " a las " + ZonaActivity.hora(System.currentTimeMillis() - (z.haceMin == null ? 0 : z.haceMin) * 60000L, false) + ". Toca para ver la zona.", ir);
        });
    }
}
