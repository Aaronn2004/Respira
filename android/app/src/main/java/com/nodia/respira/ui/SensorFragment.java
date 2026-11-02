package com.nodia.respira.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.ble.ServicioEscuharBeacons;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Recorrido;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ==============================================================
// Pestaña «Sensor»: el voluntario escribe el código RSP-XXXX del
// sensor que le han prestado (con su ID) y empieza el recorrido.
// No se vincula nada: el móvil solo «escucha» al sensor por BLE.
// ==============================================================
public class SensorFragment extends Pestana {

    private String codigoPendiente;
    private String textoCodigo = "";
    private String errorCodigo;

    private final ActivityResultLauncher<String[]> pedirPermisos =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::alResponderPermisos);

    @Override
    protected void pintar(LinearLayout c) {
        Ui.titulo(c, "Mi sensor");
        Usuario u = repo.usuario();
        if (u == null) {
            Ui.necesitaSesion(c, requireActivity(), "Inicia sesión para llevar un sensor",
                    "El sensor se presta en un punto de reparto con tu ID de usuario. Así sabemos a quién darle los puntos.");
            return;
        }
        Recorrido r = repo.recorrido();
        if (r != null && r.enCurso()) {
            pintarRecorrido(c, r);
        } else {
            pintarFormulario(c, u, r);
        }
    }

    // ---------- recorrido en curso ----------
    private void pintarRecorrido(LinearLayout c, Recorrido r) {
        LinearLayout card = Ui.tarjeta(c);
        LinearLayout f = Ui.fila(card, r.codigo, "Sensor prestado · desde las " + r.inicio, null);
        ((TextView) ((LinearLayout) f.getChildAt(0)).getChildAt(0)).setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        if (!ServicioEscuharBeacons.tienePermisoEscanear(requireContext())) {
            Ui.aviso(c, "Envío en pausa: falta el permiso de Bluetooth. El móvil no puede oír al sensor.", Nivel.MEDIO);
        } else if (Recorrido.BUSCANDO.equals(r.estado)) {
            Ui.aviso(c, "Buscando tu sensor… Llévalo cerca del móvil.", Nivel.NINGUNO);
        } else {
            Ui.aviso(c, "Recorrido activo: tu sensor está al alcance.", Nivel.BAJO);
        }

        int[] hoy = repo.hoy();
        LinearLayout datos = Ui.tarjeta(c);
        Ui.valor(datos, "Mediciones enviadas hoy", String.valueOf(r.enviadas));
        Ui.valor(datos, "Última enviada", r.ultima == null ? "—" : r.ultima);
        Ui.valor(datos, "Minutos válidos hoy", hoy[0] + " (" + hoy[1] + " en parques)");
        if (r.pendientes > 0) {
            Ui.valor(datos, "Pendientes de enviar", String.valueOf(r.pendientes));
        }
        LinearLayout pts = Ui.tarjeta(c);
        Ui.valor(pts, "Puntos estimados de hoy (se suman esta noche)", "~" + Reglas.puntosDelDia(hoy[0], hoy[1]));

        Ui.nota(c, "Mientras el recorrido está activo verás una notificación fija: Android lo exige para escuchar en segundo plano.");
        Ui.boton(c, "Terminar recorrido", Ui.BOTON_BORDE, v -> Ui.confirmar(requireContext(),
                "¿Terminar el recorrido?", "El móvil dejará de escuchar al sensor. Los minutos de hoy se guardan.",
                "Terminar", () -> {
                    ServicioEscuharBeacons.detener(requireContext());
                    repo.terminarRecorrido();
                }));
    }

    // ---------- formulario del código ----------
    private void pintarFormulario(LinearLayout c, Usuario u, Recorrido anterior) {
        if (anterior != null && Recorrido.TERMINADO.equals(anterior.estado)) {
            Ui.aviso(c, "Recorrido terminado. Hoy has enviado " + anterior.enviadas + " mediciones. Devuelve el sensor "
                    + anterior.codigo + " en un punto de reparto antes de 24 h.", Nivel.BAJO);
        }
        Ui.parrafo(c, "Recoge un sensor en un punto de reparto dando tu ID " + u.id
                + " y escribe aquí el código que lleva impreso.");

        TextInputLayout campo = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        campo.setHint("Código del sensor (RSP-XXXX)");
        TextInputEditText entrada = new TextInputEditText(campo.getContext());
        entrada.setText(textoCodigo);
        entrada.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        entrada.setTextSize(20);
        entrada.setSingleLine(true);
        entrada.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(8)});
        entrada.setImeOptions(EditorInfo.IME_ACTION_GO);
        campo.addView(entrada);
        campo.setError(errorCodigo);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(requireContext(), 16);
        c.addView(campo, lp);

        Runnable enviar = () -> {
            textoCodigo = entrada.getText() == null ? "" : entrada.getText().toString().trim().toUpperCase(Locale.ROOT);
            errorCodigo = repo.errorCodigo(textoCodigo, hayRed());
            if (errorCodigo != null) {
                campo.setError(errorCodigo);
                return;
            }
            codigoPendiente = textoCodigo;
            if (faltanPermisos().isEmpty()) {
                empezar();
            } else {
                explicarPermisos();
            }
        };
        entrada.setOnEditorActionListener((v, id, e) -> {
            enviar.run();
            return true;
        });
        Ui.boton(c, "Empezar recorrido", Ui.BOTON_LLENO, v -> enviar.run());

        LinearLayout pasos = Ui.tarjeta(c);
        Ui.fila(pasos, "1 · Recoge el sensor", "En un punto de reparto, con tu ID de usuario.", null);
        Ui.fila(pasos, "2 · Escribe su código", "Lo lleva impreso en la carcasa: RSP- y 4 caracteres.", null);
        Ui.fila(pasos, "3 · Pasea con el móvil", "El móvil oye al sensor y envía cada dato con su ubicación. Dentro de los parques cuenta doble.", null);
        Ui.nota(c, "Tu recorrido no se publica: el mapa solo muestra medias por zona.");
    }

    // ---------- permisos ----------
    private List<String> faltanPermisos() {
        List<String> p = new ArrayList<>();
        Context ctx = requireContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            p.add(Manifest.permission.BLUETOOTH_SCAN);
            p.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        p.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            p.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        List<String> faltan = new ArrayList<>();
        for (String s : p) {
            if (ContextCompat.checkSelfPermission(ctx, s) != PackageManager.PERMISSION_GRANTED) {
                faltan.add(s);
            }
        }
        return faltan;
    }

    // Pantalla «Antes de los permisos»: se explica ANTES de que Android pregunte
    private void explicarPermisos() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dos permisos para empezar")
                .setMessage("• Bluetooth («Dispositivos cercanos»): para oír a tu sensor y recibir sus mediciones.\n\n"
                        + "• Ubicación: para saber dónde se ha medido cada dato. Tu recorrido no se publica.\n\n"
                        + "Android te los pedirá a continuación.")
                .setNegativeButton("Ahora no", null)
                .setPositiveButton("Continuar", (d, w) -> pedirPermisos.launch(faltanPermisos().toArray(new String[0])))
                .show();
    }

    private void alResponderPermisos(Map<String, Boolean> res) {
        boolean bt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || (Boolean.TRUE.equals(res.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, tiene(Manifest.permission.BLUETOOTH_SCAN)))
                && Boolean.TRUE.equals(res.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, tiene(Manifest.permission.BLUETOOTH_CONNECT))));
        boolean loc = Boolean.TRUE.equals(res.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,
                tiene(Manifest.permission.ACCESS_FINE_LOCATION)));
        if (bt && loc) {
            empezar(); // las notificaciones son opcionales
            return;
        }
        String falta = !bt ? "el permiso de Bluetooth el móvil no puede oír al sensor"
                : "la ubicación los datos no se pueden situar en el mapa";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Falta un permiso")
                .setMessage("Sin " + falta + ", así que no podemos empezar el recorrido.\n\n"
                        + "Si Android ya no vuelve a preguntar, actívalo en Ajustes › Aplicaciones › Respira › Permisos.")
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Abrir ajustes", (d, w) -> startActivity(new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().getPackageName(), null))))
                .show();
    }

    private boolean tiene(String permiso) {
        return ContextCompat.checkSelfPermission(requireContext(), permiso) == PackageManager.PERMISSION_GRANTED;
    }

    private void empezar() {
        if (codigoPendiente == null) {
            return;
        }
        repo.iniciarRecorrido(codigoPendiente);
        ServicioEscuharBeacons.arrancar(requireContext());
        codigoPendiente = null;
        textoCodigo = "";
        errorCodigo = null;
    }

    private boolean hayRed() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
