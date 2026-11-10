package com.nodia.respira.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;

// ==============================================================
// Recuperar contraseña. Por privacidad la respuesta es la misma
// exista o no la cuenta (no se revela qué correos están dados de alta).
// ==============================================================
public class RecuperarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout c = Ui.pantalla(this, "Recuperar contraseña");
        Ui.parrafo(c, "Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva.");
        TextInputLayout email = Ui.campo(c, "Correo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        Ui.boton(c, "Enviar enlace", Ui.BOTON_LLENO, v -> {
            String m = Ui.leer(email).trim();
            email.setError(m.isEmpty() ? "Escribe tu correo." : !Reglas.esEmail(m) ? "El correo no tiene un formato válido." : null);
            if (email.getError() == null) {
                v.setEnabled(false);
                Repositorio.obtener(this).recuperar(m, new Repositorio.Hecho<Void>() {
                    @Override
                    public void ok(Void nada) {
                        c.removeAllViews();
                        Ui.aviso(c, "Si hay una cuenta con " + m + ", te llegará un correo en unos minutos con un enlace "
                                + "(caduca en 1 hora). Revisa también la carpeta de spam.", Nivel.BAJO);
                        Ui.boton(c, "Volver", Ui.BOTON_BORDE, x -> finish());
                    }

                    @Override
                    public void error(String mensaje, Map<String, String> campos) {
                        v.setEnabled(true);
                        email.setError(mensaje);
                    }
                });
            }
        });
    }
}
