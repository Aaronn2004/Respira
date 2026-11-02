package com.nodia.respira.ble;

import com.nodia.respira.BuildConfig;

// ==============================================================
// LogicaFake (móvil): desde fuera parece la lógica del negocio,
// pero solo hace de cliente REST contra el servidor (Sprint 0).
// La URL sale de local.properties -> URL_SERVIDOR (ver README).
// ==============================================================
public class LogicaFake {

    public static final String URL_SERVIDOR = BuildConfig.URL_SERVIDOR;

    public interface Resultado {
        void callback(boolean exito, String detalle);
    }

    private final String urlServidor;
    private final PeticionarioREST elPeticionario = new PeticionarioREST();

    // --------------------------------------------------------------
    // urlServidor: Text --> LogicaFake()
    // --------------------------------------------------------------
    public LogicaFake(String urlServidor) {
        this.urlServidor = urlServidor;
    }

    // --------------------------------------------------------------
    // medicion: Medicion --> guardarMedicion() --> exito: B, detalle: Text  (por callback)
    // --------------------------------------------------------------
    public void guardarMedicion(Medicion medicion, Resultado resultado) {
        elPeticionario.hacerPeticionREST("POST", urlServidor + "/medicion", medicion.toJSON(),
                (codigo, cuerpo) -> resultado.callback(codigo == 201, codigo + " " + cuerpo));
    }
}
