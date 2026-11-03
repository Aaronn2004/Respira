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
// legales y los controles de la demo (para enseñar el flujo sin
// tener el servidor ni el sensor delante).
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

        Ui.etiqueta(c, "Controles de la demo");
        LinearLayout demo = Ui.tarjeta(c);
        Ui.fila(demo, "Simular préstamo en el punto de reparto", "Presta el sensor RSP-9C2D a tu cuenta", v -> {
            String cod = repo.simularPrestamo();
            aviso(cod == null ? "Inicia sesión primero: el préstamo se registra con tu ID"
                    : "Punto de reparto: sensor " + cod + " prestado a " + repo.usuario().id);
        });
        Ui.fila(demo, "Simular 40 min de paseo", "15 de ellos dentro de un parque", v -> {
            if (repo.usuario() == null) {
                aviso("Inicia sesión primero");
                return;
            }
            repo.simularPaseo(40, 15);
            aviso("Sumados 40 minutos válidos");
        });
        Ui.fila(demo, "Simular fin del día", "Calcula y suma los puntos de hoy", v -> {
            if (repo.usuario() == null) {
                aviso("Inicia sesión primero");
                return;
            }
            int pts = repo.cerrarDia();
            if (pts < 0) {
                aviso("Hoy no hay minutos válidos: no se suman puntos");
                return;
            }
            Intent ir = new Intent(requireContext(), MainActivity.class).putExtra(MainActivity.EXTRA_PESTANA, R.id.tab_puntos);
            boolean ok = Notificaciones.mostrar(requireContext(), Notificaciones.CANAL_PUNTOS, 2,
                    "Has ganado " + pts + " puntos hoy", "Tu saldo es de " + repo.cuenta().puntos + " puntos.", ir);
            if (!ok) {
                aviso("Has ganado " + pts + " puntos (las notificaciones están desactivadas)");
            }
        });
        Ui.fila(demo, "Simular que salta una alerta", null, v -> simularAlerta());
        Ui.fila(demo, "Reiniciar datos de la demo", "Vuelve a la primera apertura", v ->
                Ui.confirmar(requireContext(), "¿Reiniciar la demo?",
                        "Se borran cuentas nuevas, parques, alertas y puntos de este móvil.", "Reiniciar", () -> {
                            repo.reiniciarDemo();
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
        if (!Notificaciones.mostrar(requireContext(), Notificaciones.CANAL_ALERTAS, 100 + z.id.hashCode() % 100,
                titulo, a.magnitud.formatear(z.valor(a.magnitud)) + " " + a.magnitud.unidad + ". Toca para ver la zona.", ir)) {
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
