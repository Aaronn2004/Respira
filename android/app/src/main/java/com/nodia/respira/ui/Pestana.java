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
public abstract class Pestana extends Fragment {

    protected LinearLayout contenido;
    protected Repositorio repo;
    private final Runnable alCambiar = this::repintar;

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

    @Override
    public void onStart() {
        super.onStart();
        repo.observar(alCambiar);
        repintar();
    }

    @Override
    public void onStop() {
        repo.dejarDeObservar(alCambiar);
        super.onStop();
    }

    @Override
    public void onHiddenChanged(boolean oculto) {
        super.onHiddenChanged(oculto);
        if (!oculto) {
            repintar();
        }
    }

    private void repintar() {
        if (contenido == null || !isAdded()) {
            return;
        }
        contenido.removeAllViews();
        pintar(contenido);
    }

    protected abstract void pintar(LinearLayout c);

    protected void irA(int idPestana) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).irA(idPestana);
        }
    }
}
