package com.nodia.respira.ble;

import com.nodia.respira.BuildConfig;

import java.util.List;

// ==============================================================
// LogicaFake (móvil): desde fuera parece la lógica del negocio,
// pero solo hace de cliente REST contra la API de Respira.
// La URL sale de local.properties -> URL_SERVIDOR, por ejemplo
//   https://<tu-dominio>/respira/api/index.php
// ==============================================================
public class LogicaFake {

    public static final String URL_SERVIDOR = BuildConfig.URL_SERVIDOR;

    public interface Resultado {
        // codigo: el HTTP (-1 si no hubo red). 201 guardada, 409 repetida.
        void callback(boolean exito, int codigo, String detalle);
    }

    private final String urlServidor;
    private final PeticionarioREST elPeticionario = new PeticionarioREST();
    // Sesión de quien lleva el sensor: así el servidor sabe a quién darle
    // los puntos (el móvil no puede decirlo: lo comprueba el servidor)
    private volatile String token;

    public void conToken(String token) {
        this.token = token;
    }

    // --------------------------------------------------------------
    // urlServidor: Text --> LogicaFake()
    // --------------------------------------------------------------
    public LogicaFake(String urlServidor) {
        this.urlServidor = urlServidor.endsWith("/") ? urlServidor.substring(0, urlServidor.length() - 1) : urlServidor;
    }

    // --------------------------------------------------------------
    // medicion: Medicion --> guardarMedicion() --> exito, codigo, detalle (por callback)
    // Una repetida (409: ya la mandó otro móvil) también cuenta como entregada.
    // --------------------------------------------------------------
    public void guardarMedicion(Medicion medicion, Resultado resultado) {
        elPeticionario.hacerPeticionREST("POST", urlServidor + "/mediciones", medicion.toJSON(), token,
                (codigo, cuerpo) -> resultado.callback(codigo == 201 || codigo == 409, codigo, cuerpo));
    }

    // --------------------------------------------------------------
    // lista: [Medicion] --> guardarMediciones() --> exito, codigo, detalle
    // (las que quedaron pendientes sin conexión, de una vez)
    // --------------------------------------------------------------
    public void guardarMediciones(List<Medicion> lista, Resultado resultado) {
        StringBuilder sb = new StringBuilder("{\"mediciones\":[");
        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(lista.get(i).toJSON());
        }
        sb.append("]}");
        elPeticionario.hacerPeticionREST("POST", urlServidor + "/mediciones", sb.toString(), token,
                (codigo, cuerpo) -> resultado.callback(codigo == 201, codigo, cuerpo));
    }
}
