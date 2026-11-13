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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nodia.respira.ble.ServicioEscuharBeacons;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Movimiento;
import com.nodia.respira.datos.Recorrido;
import com.nodia.respira.datos.ResumenRecorrido;
import com.nodia.respira.datos.Repositorio;
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
        // plazo del préstamo: 10 días desde la recogida, en cualquier punto de reparto
        long hasta = repo.prestamoHasta();
        if (hasta > 0) {
            String cuando = ZonaActivity.hora(hasta, true);
            if (hasta < System.currentTimeMillis()) {
                Ui.aviso(c, "Se ha pasado el plazo para devolver el sensor (" + cuando + "). Llévalo cuanto antes a cualquier punto de reparto.", Nivel.ALTO);
            } else {
                Ui.nota(c, "Devuelve el sensor antes del " + cuando + " en cualquier punto de reparto.");
            }
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
        for (Magnitud m : Magnitud.values()) {
            Double v = repo.ultimoValor(m.id);
            Ui.valor(datos, m.nombre + " (último dato)", v == null ? "—" : m.formatear(v) + " " + m.unidad);
        }
        Ui.valor(datos, "Minutos válidos hoy", hoy[0] + " (" + hoy[1] + " en parques)");
        Repositorio.Cuenta cuentaHoy = repo.cuenta();
        ResumenRecorrido rh = cuentaHoy == null ? null : cuentaHoy.hoyRecorrido;
        if (rh != null && !rh.vacio()) {
            Ui.valor(datos, "Distancia andada hoy", ResumenRecorrido.distanciaTexto(rh.distanciaM));
            Ui.valor(datos, "Tiempo con el sensor hoy", ResumenRecorrido.duracionTexto(rh.duracionMin)
                    + " (" + rh.inicio + " a " + rh.fin + ")");
            if (!rh.parques.isEmpty()) {
                Ui.valor(datos, "Parques por los que has pasado", String.join(", ", rh.parques));
            }
        }
        if (r.pendientes > 0) {
            Ui.valor(datos, "Pendientes de enviar", String.valueOf(r.pendientes));
        }
        LinearLayout pts = Ui.tarjeta(c);
        Repositorio.Cuenta cu = repo.cuenta();
        Ui.valor(pts, "Puntos de hoy (provisionales: se suman al acabar el día)", "~" + (cu == null ? 0 : cu.hoyPuntos));

        Ui.nota(c, "Mientras el recorrido está activo verás una notificación fija: Android lo exige para escuchar en segundo plano.");
        Ui.boton(c, "Terminar recorrido", Ui.BOTON_BORDE, v -> Ui.confirmar(requireContext(),
                "¿Terminar el recorrido?", "El móvil dejará de escuchar al sensor. Los minutos de hoy se guardan.",
                "Terminar", () -> {
                    ServicioEscuharBeacons.detener(requireContext());
                    repo.terminarRecorrido();
                }));
    }

    // ---------- resumen al terminar y recorridos de otros días ----------
    // Datos del servidor (Puntos.php): lo que cuenta es lo que llegó al servidor
    private void pintarResumen(LinearLayout c, Recorrido terminado) {
        Repositorio.Cuenta cu = repo.cuenta();
        ResumenRecorrido rh = cu == null ? null : cu.hoyRecorrido;
        long hasta = repo.prestamoHasta();
        Ui.aviso(c, "Recorrido terminado. Devuelve el sensor " + terminado.codigo + " en cualquier punto de reparto"
                + (hasta > 0 ? " antes del " + ZonaActivity.hora(hasta, true) : "") + ".", Nivel.BAJO);
        LinearLayout card = Ui.tarjeta(c);
        Ui.etiqueta(card, "Resumen de hoy");
        if (rh == null || rh.vacio()) {
            Ui.valor(card, "Mediciones enviadas", String.valueOf(terminado.enviadas));
            Ui.nota(card, "El resto del resumen aparece cuando las mediciones llegan al servidor.");
            return;
        }
        int[] hoy = repo.hoy();
        Ui.valor(card, "Tiempo con el sensor", ResumenRecorrido.duracionTexto(rh.duracionMin) + " (" + rh.inicio + " a " + rh.fin + ")");
        Ui.valor(card, "Distancia andada", ResumenRecorrido.distanciaTexto(rh.distanciaM));
        Ui.valor(card, "Lecturas recibidas", String.valueOf(rh.lecturas));
        Ui.valor(card, "Minutos válidos", hoy[0] + " (" + hoy[1] + " en parques)");
        if (hoy[2] > 0) {
            Ui.valor(card, "Minutos descartados", hoy[2] + " (parado, sin datos o en vehículo)");
        }
        Ui.valor(card, "Puntos de hoy (se suman al acabar el día)", "~" + cu.hoyPuntos);
        if (!rh.parques.isEmpty()) {
            Ui.valor(card, "Parques por los que has pasado", String.join(", ", rh.parques));
        }
    }

    private void pintarRecorridosAnteriores(LinearLayout c) {
        Repositorio.Cuenta cu = repo.cuenta();
        if (cu == null) {
            return;
        }
        int n = 0;
        LinearLayout lista = null;
        for (Movimiento m : cu.historial) {
            if (m.canje != null || m.recorrido == null || m.recorrido.vacio()) {
                continue;
            }
            if (lista == null) {
                Ui.etiqueta(c, "Tus últimos recorridos");
                lista = Ui.tarjeta(c);
            }
            String sub = m.recorrido.linea() + "\n" + m.recorrido.inicio + " a " + m.recorrido.fin
                    + (m.recorrido.parques.isEmpty() ? "" : " · " + String.join(", ", m.recorrido.parques));
            LinearLayout f = Ui.fila(lista, m.fecha, sub, null);
            Ui.insignia(f, "+" + m.puntos, Nivel.BAJO);
            if (++n == 7) {
                break;
            }
        }
    }

    // ---------- formulario del código ----------
    private void pintarFormulario(LinearLayout c, Usuario u, Recorrido anterior) {
        if (anterior != null && Recorrido.TERMINADO.equals(anterior.estado)) {
            pintarResumen(c, anterior);
        }
        String prestado = repo.sensorPrestado();
        if (u.esTrabajador()) {
            Ui.parrafo(c, "Escribe el código del sensor que te ha asignado tu servicio (lo lleva impreso).");
        } else if (prestado != null) {
            Ui.parrafo(c, "Tienes prestado el sensor " + prestado + ". Escribe su código para empezar.");
            if (textoCodigo.isEmpty()) {
                textoCodigo = prestado;
            }
        } else {
            Ui.parrafo(c, "Recoge un sensor en un punto de reparto dando tu ID " + u.id
                    + " y escribe aquí el código que lleva impreso.");
        }

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

        MaterialButton boton = Ui.boton(c, "Empezar recorrido", Ui.BOTON_LLENO, null);
        Runnable enviar = () -> {
            textoCodigo = entrada.getText() == null ? "" : entrada.getText().toString().trim().toUpperCase(Locale.ROOT);
            boton.setEnabled(false);
            campo.setError(null);
            // El servidor comprueba que ese sensor está prestado a esta cuenta
            repo.comprobarSensor(textoCodigo, hayRed(), new Repositorio.Hecho<String>() {
                @Override
                public void ok(String codigo) {
                    errorCodigo = null;
                    if (!isAdded()) {
                        return;
                    }
                    boton.setEnabled(true);
                    codigoPendiente = codigo;
                    if (faltanPermisos().isEmpty()) {
                        empezar();
                    } else {
                        explicarPermisos();
                    }
                }

                @Override
                public void error(String mensaje, Map<String, String> campos) {
                    errorCodigo = mensaje;
                    if (isAdded()) {
                        boton.setEnabled(true);
                        campo.setError(mensaje);
                    }
                }
            });
        };
        entrada.setOnEditorActionListener((v, id, e) -> {
            enviar.run();
            return true;
        });
        boton.setOnClickListener(v -> enviar.run());

        LinearLayout pasos = Ui.tarjeta(c);
        Ui.fila(pasos, "1 · Recoge el sensor", "En un punto de reparto, con tu ID de usuario.", null);
        Ui.fila(pasos, "2 · Escribe su código", "Lo lleva impreso en la carcasa: RSP- y 4 caracteres.", null);
        Ui.fila(pasos, "3 · Pasea con el móvil", "El móvil oye al sensor y envía cada dato con su ubicación. Dentro de los parques cuenta doble.", null);
        Ui.nota(c, "Tu recorrido no se publica: el mapa solo muestra medias por zona.");
        pintarRecorridosAnteriores(c);
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
