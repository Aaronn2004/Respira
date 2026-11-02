package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// ==============================================================
// Iniciar sesión (APP-10). 5 intentos fallidos -> 15 min de bloqueo.
// Sin huella: solo correo y contraseña.
// ==============================================================
public class LoginActivity extends AppCompatActivity {

    private Repositorio repo;
    private TextInputLayout email, clave;
    private TextView errorGlobal;
    private MaterialButton entrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = Repositorio.obtener(this);
        LinearLayout c = Ui.pantalla(this, "Iniciar sesión");
        Ui.titulo(c, "Hola de nuevo");
        Ui.parrafo(c, "Entra para llevar un sensor, ganar puntos y canjearlos.");

        errorGlobal = Ui.aviso(c, "", Nivel.ALTO);
        errorGlobal.setVisibility(TextView.GONE);

        email = Ui.campo(c, "Correo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        clave = Ui.campo(c, "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        entrar = Ui.boton(c, "Entrar", Ui.BOTON_LLENO, v -> entrar());
        Ui.boton(c, "He olvidado mi contraseña", Ui.BOTON_TEXTO,
                v -> startActivity(new Intent(this, RecuperarActivity.class)));
        Ui.boton(c, "¿No tienes cuenta? Crear cuenta", Ui.BOTON_TEXTO, v -> {
            startActivity(new Intent(this, RegistroActivity.class));
            finish();
        });
        comprobarBloqueo();
    }

    private void comprobarBloqueo() {
        long hasta = repo.bloqueadoHasta();
        boolean bloqueado = hasta > 0;
        entrar.setEnabled(!bloqueado);
        if (bloqueado) {
            String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(hasta));
            mostrarError("Has fallado " + Reglas.MAX_INTENTOS_LOGIN + " veces. Por seguridad, el acceso está bloqueado hasta las " + hora + ".");
            entrar.postDelayed(this::comprobarBloqueo, Math.max(1000, hasta - System.currentTimeMillis()));
        }
    }

    private void mostrarError(String txt) {
        errorGlobal.setText(txt);
        errorGlobal.setVisibility(txt == null ? TextView.GONE : TextView.VISIBLE);
    }

    private void entrar() {
        String m = Ui.leer(email).trim();
        String p = Ui.leer(clave);
        email.setError(m.isEmpty() ? "Escribe tu correo."
                : !Reglas.esEmail(m) ? "El correo no tiene un formato válido. Ejemplo: nombre@correo.es" : null);
        clave.setError(p.isEmpty() ? "Escribe tu contraseña." : null);
        if (email.getError() != null || clave.getError() != null) {
            return;
        }
        Repositorio.ResultadoLogin r = repo.iniciarSesion(m, p);
        if (r.error != null) {
            mostrarError(r.error);
            comprobarBloqueo();
            return;
        }
        Toast.makeText(this, "Hola, " + r.usuario.nombre, Toast.LENGTH_SHORT).show();
        finish();
    }
}
