package com.nodia.respira.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class EnvioMedicionesTest {
    @Test public void resultadoParcialConservaElIndiceRechazado() throws Exception {
        JSONObject respuesta = new JSONObject("{\"guardadas\":1,\"repetidas\":1,"
                + "\"errores\":[{\"i\":1,\"error\":\"ubicación fuera de zona\"}]}");
        assertEquals(Collections.singleton(1), EnvioMediciones.indicesRechazados(respuesta, 3));
    }

    @Test public void unResultadoIncompletoNoConfirmaElLote() throws Exception {
        JSONObject respuesta = new JSONObject("{\"guardadas\":1,\"repetidas\":0,\"errores\":[]}");
        try {
            EnvioMediciones.indicesRechazados(respuesta, 2);
            fail("Una respuesta incompleta no debe confirmar las lecturas");
        } catch (JSONException esperado) {
            // La cola permanece pendiente si la respuesta no identifica todo el lote.
        }
    }
}
