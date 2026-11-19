package com.nodia.respira.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.R;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Usuario;

import java.util.Map;

// ==============================================================
// Crear cuenta (APP-10). Al terminar se muestra el ID (U-XXXXX):
// es lo que el voluntario da en el punto de reparto.
// ==============================================================
// --------------------------------------------------------------
// class RegistroActivity
// # Formulario de creación de cuenta ciudadana.
// # AppCompatActivity <--- RegistroActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class RegistroActivity extends AppCompatActivity {

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
        LinearLayout c = Ui.pantalla(this, "Crear cuenta");
        Ui.titulo(c, "Crea tu cuenta");
        Ui.parrafo(c, "Crea tu cuenta para acceder a Respira, consultar el mapa y participar con un sensor.");

        TextInputLayout nombre = Ui.campo(c, "Nombre de usuario", InputType.TYPE_CLASS_TEXT);
        nombre.setHelperText("De 3 a 20 letras, números, puntos o guiones bajos");
        TextInputLayout email = Ui.campo(c, "Correo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        TextInputLayout clave = Ui.campo(c, "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        clave.setHelperText("Mínimo 8 caracteres");

        MaterialCheckBox acepta = new MaterialCheckBox(this);
        acepta.setText("Acepto los términos de uso y la política de privacidad");
        c.addView(acepta);
        Ui.boton(c, "Leer la política de privacidad", Ui.BOTON_TEXTO,
                v -> TextoActivity.abrir(this, TextoActivity.PRIVACIDAD));
        TextView errorAcepta = Ui.texto(c, "", 13, false, R.color.nivel_alto);
        errorAcepta.setVisibility(TextView.GONE);

        TextView errorGeneral = Ui.texto(c, "", 13, false, R.color.nivel_alto);
        errorGeneral.setVisibility(TextView.GONE);

        MaterialButton crear = Ui.boton(c, "Crear cuenta", Ui.BOTON_LLENO, null);
        crear.setOnClickListener(v -> {
            crear.setEnabled(false);
            errorGeneral.setVisibility(TextView.GONE);
            // Las reglas las comprueba el servidor (nombre y correo no repetidos, etc.)
            repo.registrar(Ui.leer(nombre), Ui.leer(email), Ui.leer(clave), acepta.isChecked(), new Repositorio.Hecho<Usuario>() {
                // --------------------------------------------------------------
                // # effects: actualiza la interfaz Android
                // + u: Usuario
                //       -->
                //           ok() -->
                // --------------------------------------------------------------
                @Override
                public void ok(Usuario u) {
                    nombre.setError(null);
                    email.setError(null);
                    clave.setError(null);
                    errorAcepta.setVisibility(TextView.GONE);
                    new MaterialAlertDialogBuilder(RegistroActivity.this)
                            .setTitle("Cuenta creada")
                            .setMessage("Tu ID es " + u.id + ".\n\nDalo en el punto de reparto cuando recojas un sensor. "
                                    + "Siempre lo tienes en Más › Mi cuenta.")
                            .setCancelable(false)
                            .setPositiveButton("Entendido", (d, w) -> AccesoSesion.abrirPrincipal(RegistroActivity.this))
                            .show();
                }

                // --------------------------------------------------------------
                // # effects: actualiza la interfaz Android
                // + mensaje: Text, err: Map[Text, Text]
                //       -->
                //           error() -->
                // --------------------------------------------------------------
                @Override
                public void error(String mensaje, Map<String, String> err) {
                    crear.setEnabled(true);
                    nombre.setError(err.get("nombre"));
                    email.setError(err.get("email"));
                    clave.setError(err.get("clave"));
                    errorAcepta.setText(err.get("acepta"));
                    errorAcepta.setVisibility(err.containsKey("acepta") ? TextView.VISIBLE : TextView.GONE);
                    if (err.isEmpty()) {
                        errorGeneral.setText(mensaje);
                        errorGeneral.setVisibility(TextView.VISIBLE);
                    }
                }
            });
        });
    }
}
