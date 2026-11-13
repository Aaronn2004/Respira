package com.nodia.respira.ui;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nodia.respira.R;
import com.nodia.respira.datos.Alerta;
import com.nodia.respira.datos.Usuario;
import com.nodia.respira.datos.Zona;

import java.util.List;

// ==============================================================
// Pestaña «Más»: cuenta, alertas, percepciones, ajustes, textos
// legales y unas pruebas (actualizar, probar una alerta, borrar
// lo guardado en el móvil).
// ==============================================================
public class MasFragment extends Pestana {

    @Override
    protected void pintar(LinearLayout c) {
        Ui.titulo(c, "Más");
        Usuario u = repo.usuario();
        LinearLayout cuenta = Ui.tarjeta(c);
        if (u != null) {
            Ui.fila(cuenta, "Mi cuenta", u.nombre + " · ID " + u.id, v -> abrir(CuentaActivity.class));
        } else {
            Ui.fila(cuenta, "Iniciar sesión", "Para llevar un sensor y ganar puntos", v -> abrir(LoginActivity.class));
            Ui.fila(cuenta, "Crear cuenta", null, v -> abrir(RegistroActivity.class));
        }

        // Modo claro / oscuro (mismos colores que la web)
        LinearLayout tema = Ui.tarjeta(c);
        boolean oscuro = Tema.oscuro(requireContext());
        Bloques.interruptor(tema, oscuro ? R.drawable.ic_luna : R.drawable.ic_sol, "Modo oscuro",
                oscuro ? "Activado" : "Desactivado · modo claro", oscuro,
                activado -> Tema.cambiar(requireContext(), activado));

        // Mismo orden que en el visual (prototipo v24)
        LinearLayout lista = Ui.tarjeta(c);
        int mias = repo.misPercepciones().size(), nAlertas = repo.alertas().size();
        Ui.fila(lista, "Mis percepciones", mias + (mias == 1 ? " activa" : " activas"), v -> abrir(MisPercepcionesActivity.class));
        Ui.fila(lista, "Mis alertas", nAlertas + (nAlertas == 1 ? " alerta" : " alertas"), v -> abrir(MisAlertasActivity.class));
        Ui.fila(lista, "Ajustes", "Idioma y notificaciones", v -> abrir(AjustesActivity.class));
        Ui.fila(lista, "Privacidad", "Qué guardamos y cómo borrarlo", v -> TextoActivity.abrir(requireContext(), TextoActivity.PRIVACIDAD));

        LinearLayout info = Ui.tarjeta(c);
        Ui.fila(info, "Qué significan los valores", null, v -> TextoActivity.abrir(requireContext(), TextoActivity.GUIA));
        Ui.fila(info, "Efectos en la salud", "Ozono, calor y ruido", v -> abrir(EfectosActivity.class));
        Ui.fila(info, "Ver la bienvenida otra vez", null, v -> startActivity(
                new Intent(requireContext(), OnboardingActivity.class).putExtra(OnboardingActivity.EXTRA_REPASO, true)));
        Ui.fila(info, "Acerca de Respira", null, v -> TextoActivity.abrir(requireContext(), TextoActivity.ACERCA));

        Ui.etiqueta(c, "Pruebas");
        LinearLayout demo = Ui.tarjeta(c);
        Ui.fila(demo, "Actualizar datos", repo.datosReales() ? "Conectado al servidor de Respira"
                : "Sin conexión con el servidor: se ven datos de demostración", v -> {
            repo.actualizarZonas();
            repo.refrescar(true);
            aviso("Pidiendo los datos al servidor…");
        });
        Ui.fila(demo, "Probar una alerta", "Enseña cómo llega la notificación de tu última alerta", v -> simularAlerta());
        Ui.fila(demo, "Borrar los datos de este móvil", "Cierra la sesión y borra de este móvil favoritos, alertas y la bienvenida (los de tu cuenta siguen en ella)", v ->
                Ui.confirmar(requireContext(), "¿Borrar los datos de este móvil?",
                        "Tu cuenta y tus puntos siguen en el servidor: podrás volver a entrar.", "Borrar", () -> {
                            repo.olvidarEsteMovil();
                            startActivity(new Intent(requireContext(), SplashActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }));
        Ui.nota(c, "Cuentas de prueba: lucia@correo.es / Respira2026 (sensor RSP-4F7K) · marcos@correo.es / Marcos2026");
    }

    private void simularAlerta() {
        List<Alerta> alertas = repo.alertas();
        if (alertas.isEmpty()) {
            aviso("No hay alertas configuradas");
            return;
        }
        Alerta a = alertas.get(alertas.size() - 1);
        Zona z = repo.zona(a.zonaId);
        String titulo = a.magnitud.nombre + ("custom".equals(a.condicion) ? " por encima de tu límite"
                : " en nivel " + a.condicion) + " en " + z.nombre;
        Intent ir = new Intent(requireContext(), ZonaActivity.class).putExtra(ZonaActivity.EXTRA_ZONA, z.id);
        if (!Notificaciones.mostrar(requireContext(), Notificaciones.CANAL_ALERTAS, 100 + Math.abs(z.id.hashCode() % 100),
                titulo, a.magnitud.formatear(z.valor(a.magnitud)) + " " + a.magnitud.unidad + " a las " + ZonaActivity.hora(System.currentTimeMillis() - (z.haceMin == null ? 0 : z.haceMin) * 60000L, false) + ". Toca para ver la zona.", ir)) {
            aviso("No llega: las notificaciones no están permitidas para Respira");
        }
    }

    private void abrir(Class<?> pantalla) {
        startActivity(new Intent(requireContext(), pantalla));
    }

    private void aviso(String txt) {
        Toast.makeText(requireContext(), txt, Toast.LENGTH_LONG).show();
    }
}
