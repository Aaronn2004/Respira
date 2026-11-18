package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Usuario;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

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
        entrar.setEnabled(false);
        mostrarError(null);
        repo.iniciarSesion(m, p, new Repositorio.Hecho<Usuario>() {
            @Override
            public void ok(Usuario u) {
                if (u.requiereCambioClave) {
                    clave.getEditText().setText("");
                    pedirClaveInicial();
                } else {
                    terminar(u);
                }
            }

            @Override
            public void error(String mensaje, Map<String, String> campos) {
                entrar.setEnabled(true);
                clave.getEditText().setText("");
                mostrarError(mensaje);
                comprobarBloqueo();
            }
        });
    }
    private void terminar(Usuario u) {
        Toast.makeText(this, "Hola, " + u.nombre, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void pedirClaveInicial() {
        LinearLayout c = Ui.columna(this);
        c.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        Ui.parrafo(c, "Crea tu propia contraseña para empezar. Usa al menos 10 caracteres, con letras y números.");
        int tipo = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        TextInputLayout nueva = Ui.campo(c, "Nueva contraseña", tipo);
        TextInputLayout repetir = Ui.campo(c, "Repite la contraseña", tipo);
        TextView error = Ui.aviso(c, "", Nivel.ALTO);
        error.setVisibility(TextView.GONE);
        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Cambia tu contraseña inicial")
                .setView(c)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", (d, w) -> cancelarClave())
                .setOnCancelListener(d -> cancelarClave())
                .create();
        dialogo.show();
        android.widget.Button guardar = dialogo.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        guardar.setOnClickListener(v -> {
            String p = Ui.leer(nueva);
            repetir.setError(p.equals(Ui.leer(repetir)) ? null : "Las contraseñas no coinciden.");
            nueva.setError(null);
            error.setVisibility(TextView.GONE);
            if (repetir.getError() != null) return;
            guardar.setEnabled(false);
            repo.cambiarClaveInicial(p, new Repositorio.Hecho<Usuario>() {
                @Override public void ok(Usuario u) {
                    dialogo.dismiss();
                    terminar(u);
                }

                @Override public void error(String mensaje, Map<String, String> campos) {
                    guardar.setEnabled(true);
                    nueva.setError(campos.get("clave"));
                    if (nueva.getError() == null) {
                        error.setText(mensaje);
                        error.setVisibility(TextView.VISIBLE);
                    }
                }
            });
        });
    }

    private void cancelarClave() {
        repo.cancelarClaveInicial();
        entrar.setEnabled(true);
    }
}
