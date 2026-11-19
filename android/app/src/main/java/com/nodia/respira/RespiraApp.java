package com.nodia.respira;

import android.app.Application;
import android.content.Intent;

import com.nodia.respira.ble.EnvioMediciones;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.TrabajoAlertas;
import com.nodia.respira.datos.Zona;
import com.nodia.respira.ui.AccesoSesion;
import com.nodia.respira.ui.Notificaciones;
import com.nodia.respira.ui.Tema;
import com.nodia.respira.ui.ZonaActivity;

// Aplicación: aplica el modo claro u oscuro guardado antes de abrir ninguna
// pantalla y enseña las notificaciones de las alertas del usuario.
// --------------------------------------------------------------
// class RespiraApp
// # Inicialización del tema y las tareas de alertas y envío.
// # Application <--- RespiraApp
// --------------------------------------------------------------
public class RespiraApp extends Application {

    // --------------------------------------------------------------
    // # effects: aplica preferencias, registra avisos y reanuda tareas de envío
    // +
    //   onCreate() -->
    // --------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
        Tema.aplicar(this);
        Repositorio repo = Repositorio.obtener(this);
        new AccesoSesion(this);
        repo.alAvisarDeAlerta((a, z) -> {
            String titulo = a.magnitud.nombre + ("custom".equals(a.condicion) ? " por encima de tu límite"
                    : " en nivel " + a.condicion) + " en " + z.nombre;
            Intent ir = new Intent(this, ZonaActivity.class).putExtra(ZonaActivity.EXTRA_ZONA, z.id);
            Zona.Dato dato = z.dato(a.magnitud);
            String hora = dato.momento == 0 ? "" : " a las " + ZonaActivity.hora(dato.momento, false);
            String origen = "estimado".equals(dato.origen) ? " · Estimado"
                    : "simulado".equals(dato.origen) ? " · Simulado" : "";
            return Notificaciones.mostrar(this, Notificaciones.CANAL_ALERTAS, (z.id + a.magnitud.id).hashCode(),
                    titulo, a.magnitud.formatear(dato.valor) + " " + a.magnitud.unidad + hora + origen, ir);
        });
        TrabajoAlertas.programar(this, repo.necesitaTrabajoAlertas());
        EnvioMediciones.obtener(this).reanudar();
    }
}
