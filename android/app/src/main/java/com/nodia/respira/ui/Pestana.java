package com.nodia.respira.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nodia.respira.datos.Repositorio;

// ==============================================================
// Pestana: base de las pestañas de listas (Parques, Sensor, Puntos, Más).
// Se repinta entera cuando cambian los datos del Repositorio.
// ==============================================================
// --------------------------------------------------------------
// class Pestana
// # Base de pestañas que observan y presentan el repositorio.
// # Fragment <--- Pestana
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Vista = { contenido: Text, visible: B, habilitada: B }
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public abstract class Pestana extends Fragment {

    protected LinearLayout contenido;
    protected Repositorio repo;
    private final Runnable alCambiar = this::repintar;

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + container: Vista, savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onCreateView() -->
    //       <--
    //   resultado: Vista
    // --------------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        repo = Repositorio.obtener(requireContext());
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        contenido = Ui.columna(requireContext());
        scroll.addView(contenido);
        return scroll;
    }

    // --------------------------------------------------------------
    // # effects: actualiza la suscripción al repositorio; solicita datos al servidor; actualiza la interfaz
    // +
    //   onStart() -->
    // --------------------------------------------------------------
    @Override
    public void onStart() {
        super.onStart();
        repo.observar(alCambiar);
        repo.refrescar();      // como mucho cada 30 s: cuenta, puntos, regalos y percepciones
        repintar();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la suscripción al repositorio
    // +
    //   onStop() -->
    // --------------------------------------------------------------
    @Override
    public void onStop() {
        repo.dejarDeObservar(alCambiar);
        super.onStop();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + oculto: B
    //       -->
    //           onHiddenChanged() -->
    // --------------------------------------------------------------
    @Override
    public void onHiddenChanged(boolean oculto) {
        super.onHiddenChanged(oculto);
        if (!oculto) {
            repintar();
        }
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   repintar() -->
    // --------------------------------------------------------------
    private void repintar() {
        if (contenido == null || !isAdded()) {
            return;
        }
        contenido.removeAllViews();
        pintar(contenido);
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + c: Vista
    //       -->
    //           pintar() -->
    // --------------------------------------------------------------
    protected abstract void pintar(LinearLayout c);

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + idPestana: N
    //       -->
    //           irA() -->
    // --------------------------------------------------------------
    protected void irA(int idPestana) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).irA(idPestana);
        }
    }
}
