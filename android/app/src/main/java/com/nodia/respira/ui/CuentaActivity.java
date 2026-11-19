package com.nodia.respira.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nodia.respira.R;
import com.nodia.respira.ble.ServicioEscuharBeacons;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Usuario;

// ==============================================================
// Mi cuenta: ID para el punto de reparto, cerrar sesión, borrar cuenta.
// ==============================================================
// --------------------------------------------------------------
// class CuentaActivity
// # Datos de cuenta y cierre de sesión.
// # AppCompatActivity <--- CuentaActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class CuentaActivity extends AppCompatActivity {

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onCreate() -->
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repositorio repo = Repositorio.obtener(this);
        Usuario u = repo.usuario();
        LinearLayout c = Ui.pantalla(this, "Mi cuenta");
        if (u == null) {
            finish();
            return;
        }
        LinearLayout id = Ui.tarjeta(c);
        id.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 20);
        id.setPadding(p, p, p, p);
        Ui.texto(id, u.esTrabajador() ? "Tu ID de trabajador municipal" : "Tu ID para el punto de reparto", 14, false, R.color.tinta_2);
        TextView t = Ui.texto(id, u.id, 34, true, R.color.primario);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setBackgroundResource(R.drawable.fondo_codigo);
        t.setPadding(p, p / 2, p, p / 2);

        LinearLayout datos = Ui.tarjeta(c);
        Ui.valor(datos, "Usuario", u.nombre);
        Ui.valor(datos, "Correo", u.email);
        Repositorio.Cuenta cuenta = repo.cuenta();
        Ui.valor(datos, "Puntos", String.valueOf(cuenta == null ? 0 : cuenta.puntos));

        Ui.boton(c, "Cerrar sesión", Ui.BOTON_BORDE, v -> {
            ServicioEscuharBeacons.detener(this);
            repo.cerrarSesion();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            finish();
        });
        if (u.esTrabajador()) {
            Ui.nota(c, "Tu cuenta de trabajador la gestiona el ayuntamiento: para darla de baja, habla con tu servicio.");
            return;
        }
        if (repo.sensorPrestado() != null) {
            Ui.nota(c, "Devuelve el sensor prestado en un punto de reparto antes de borrar tu cuenta.");
        }
        Ui.boton(c, "Borrar mi cuenta", Ui.BOTON_TEXTO, v -> {
            if (repo.sensorPrestado() != null) {
                Toast.makeText(this, "Devuelve el sensor prestado antes de borrar tu cuenta.", Toast.LENGTH_LONG).show();
                return;
            }
            Ui.confirmar(this, "¿Borrar tu cuenta?",
                    "Se borrarán tu cuenta, tus percepciones y tu historial. Perderás "
                            + (cuenta == null ? 0 : cuenta.puntos) + " puntos sin canjear. No se puede deshacer.",
                    "Borrar cuenta", () -> repo.borrarCuenta(Ui.hecho(this, () -> {
                        ServicioEscuharBeacons.detener(this);
                        Toast.makeText(this, "Cuenta borrada", Toast.LENGTH_SHORT).show();
                        finish();
                    })));
        });
    }
}
