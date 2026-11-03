package com.nodia.respira.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nodia.respira.BuildConfig;
import com.nodia.respira.R;
import com.nodia.respira.datos.DatosDemo;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Percepcion;
import com.nodia.respira.datos.Recorrido;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Usuario;
import com.nodia.respira.datos.Zona;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ==============================================================
// Pestaña «Mapa» con Google Maps SDK for Android (APP-01..03).
//  - Capa «Mediciones»: parques y barrios coloreados por nivel de
//    la magnitud elegida (media de la última hora).
//      · Antiguo (1-24 h): borde discontinuo y color apagado.
//      · Sin datos (> 24 h): gris con borde discontinuo.
//  - Capa «Comunidad»: percepciones de vecinos de las últimas 24 h.
// ==============================================================
public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private Repositorio repo;
    private GoogleMap mapa;
    private Magnitud magnitud = Magnitud.O3;
    private String filtroPercepcion = "todos";
    private boolean capaComunidad = false;
    private boolean en3d = false;

    private ChipGroup filtros;
    private TextView leyenda;
    private MaterialButton franja, botonCuenta, boton3d;
    private View cabecera, pie;

    private final Runnable alCambiar = this::refrescar;

    private final ActivityResultLauncher<String[]> pedirUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> activarMiUbicacion(true));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = Repositorio.obtener(requireContext());
        cabecera = v.findViewById(R.id.cabecera);
        pie = v.findViewById(R.id.pie);
        filtros = v.findViewById(R.id.filtros);
        leyenda = v.findViewById(R.id.leyenda);
        franja = v.findViewById(R.id.franja);
        botonCuenta = v.findViewById(R.id.cuenta);

        MaterialButtonToggleGroup capa = v.findViewById(R.id.capa);
        capa.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado) {
                capaComunidad = id == R.id.capa_comunidad;
                pintarFiltros();
                refrescar();
            }
        });

        boton3d = v.findViewById(R.id.vista3d);
        boton3d.setOnClickListener(x -> alternar3d());
        v.findViewById(R.id.acercar).setOnClickListener(x -> {
            if (mapa != null) mapa.animateCamera(CameraUpdateFactory.zoomIn());
        });
        v.findViewById(R.id.alejar).setOnClickListener(x -> {
            if (mapa != null) mapa.animateCamera(CameraUpdateFactory.zoomOut());
        });
        v.findViewById(R.id.ubicacion).setOnClickListener(x -> {
            if (tienePermisoUbicacion()) {
                activarMiUbicacion(true);
            } else {
                pedirUbicacion.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        });

        // Al cambiar el tamaño de la cabecera o el pie, el mapa deja hueco
        // para que el logo de Google y los controles no queden tapados.
        View.OnLayoutChangeListener ajustar = (vv, l, t, r, b, ol, ot, or, ob) -> ajustarRelleno();
        cabecera.addOnLayoutChangeListener(ajustar);
        pie.addOnLayoutChangeListener(ajustar);

        pintarFiltros();

        if (!BuildConfig.HAY_CLAVE_MAPS) {
            v.findViewById(R.id.sin_clave).setVisibility(View.VISIBLE);
        }
        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapa);
        if (smf != null) {
            smf.getMapAsync(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        repo.observar(alCambiar);
        refrescar();
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
            refrescar();
        }
    }

    // ==============================================================
    // Google Maps listo
    // ==============================================================
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        // En modo oscuro, Google Maps no se oscurece solo: se le pasa un estilo (res/raw/mapa_oscuro.json)
        if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            mapa.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapa_oscuro));
        }
        mapa.getUiSettings().setZoomControlsEnabled(false); // usamos nuestros botones + / −
        mapa.getUiSettings().setCompassEnabled(true);
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setMyLocationButtonEnabled(false); // usamos nuestro botón
        mapa.getUiSettings().setTiltGesturesEnabled(true);
        mapa.getUiSettings().setRotateGesturesEnabled(true);
        mapa.setBuildingsEnabled(true);
        mapa.setIndoorEnabled(false);
        mapa.setMinZoomPreference(12f);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(DatosDemo.LAT_CENTRO, DatosDemo.LNG_CENTRO), 14.4f));

        mapa.setOnPolygonClickListener(p -> {
            Object tag = p.getTag();
            if (tag instanceof String) {
                mostrarZona((String) tag);
            }
        });
        mapa.setOnMarkerClickListener(m -> {
            Object tag = m.getTag();
            if (tag instanceof String) {        // etiqueta de una zona
                mostrarZona((String) tag);
                return true;
            }
            return false;                        // percepción: globo de información
        });
        activarMiUbicacion(false);
        ajustarRelleno();
        dibujar();
    }

    private void ajustarRelleno() {
        if (mapa == null || cabecera == null) {
            return;
        }
        int arriba = cabecera.getBottom() + Ui.dp(requireContext(), 4);
        int abajo = pie.getHeight();
        mapa.setPadding(0, arriba, 0, abajo);
    }

    // ==============================================================
    // Repintado (cabecera, leyenda, franja y capas del mapa)
    // ==============================================================
    private void refrescar() {
        if (!isAdded() || getView() == null) {
            return;
        }
        Usuario u = repo.usuario();
        botonCuenta.setText(u == null ? "Entrar" : u.nombre.substring(0, 1).toUpperCase());
        botonCuenta.setOnClickListener(x -> startActivity(new Intent(requireContext(),
                u == null ? LoginActivity.class : CuentaActivity.class)));

        if (capaComunidad) {
            leyenda.setText(leyenda("Percepciones de vecinos · 24 h", "R ruido · T temperatura · A aire · No son mediciones", false));
            franja.setText("+ Contar lo que noto aquí");
            franja.setOnClickListener(x -> nuevaPercepcion());
        } else {
            leyenda.setText(leyenda(magnitud.abreviatura + " (" + magnitud.unidad + ") · última hora",
                    "Parques con contorno; barrios en tono suave", true));
            Recorrido r = repo.recorrido();
            if (r != null && r.enCurso()) {
                franja.setText("● Enviando datos · " + r.enviadas + " mediciones hoy   Ver ›");
            } else {
                franja.setText("¿Llevas un sensor? Escribe su código   ›");
            }
            franja.setOnClickListener(x -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).irA(R.id.tab_sensor);
                }
            });
        }
        dibujar();
    }

    // --------------------------------------------------------------
    // Leyenda como la del visual: título en negrita, puntos de color
    // por nivel y una nota en gris.
    // --------------------------------------------------------------
    private CharSequence leyenda(String titulo, String nota, boolean conEstados) {
        Context c = requireContext();
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        sb.append(titulo, new android.text.style.StyleSpan(Typeface.BOLD), 0);
        sb.append("   ");
        int[][] items = conEstados
                ? new int[][]{{R.color.nivel_bajo, 0}, {R.color.nivel_medio, 1}, {R.color.nivel_alto, 2}, {R.color.nivel_ninguno, 3}, {R.color.tinta_2, 4}}
                : new int[][]{{R.color.nivel_bajo, 0}, {R.color.nivel_medio, 1}, {R.color.nivel_alto, 2}};
        String[] nombres = {"Bajo", "Medio", "Alto", "Sin datos", "Antiguo (1–24 h)"};
        for (int[] it : items) {
            sb.append("●", new android.text.style.ForegroundColorSpan(Ui.color(c, it[0])), 0);
            sb.append(" ").append(nombres[it[1]]).append("   ");
        }
        sb.append("\n");
        sb.append(nota, new android.text.style.ForegroundColorSpan(Ui.color(c, R.color.tinta_3)), 0);
        return sb;
    }

    private void pintarFiltros() {
        filtros.removeAllViews();
        if (capaComunidad) {
            String[][] tipos = {{"todos", "Todas"}, {"ruido", "Ruido"}, {"temp", "Temp."}, {"aire", "Aire"}};
            for (String[] t : tipos) {
                Chip chip = chip(t[1], t[0].equals(filtroPercepcion));
                chip.setOnClickListener(x -> {
                    filtroPercepcion = t[0];
                    dibujar();
                });
            }
        } else {
            for (Magnitud m : Magnitud.values()) {
                Chip chip = chip(m.nombre, m == magnitud);
                chip.setOnClickListener(x -> {
                    magnitud = m;
                    refrescar();
                });
            }
        }
    }

    private Chip chip(String texto, boolean marcado) {
        Chip c = new Chip(requireContext(), null, com.google.android.material.R.attr.chipChoiceStyle);
        c.setText(texto);
        c.setCheckable(true);
        c.setChecked(marcado);
        filtros.addView(c);
        return c;
    }

    private void dibujar() {
        if (mapa == null || !isAdded()) {
            return;
        }
        mapa.clear();
        if (capaComunidad) {
            dibujarPercepciones();
        } else {
            dibujarZonas();
        }
    }

    private void dibujarZonas() {
        Context c = requireContext();
        for (Zona z : repo.zonas()) {
            String estado = z.estado();
            Nivel n = z.nivel(magnitud);
            int color = Ui.color(c, n.color);
            boolean antiguo = Reglas.ESTADO_ANTIGUO.equals(estado);
            boolean sinDatos = Reglas.ESTADO_SIN_DATOS.equals(estado);
            if (antiguo) {
                color = ColorUtils.blendARGB(color, Ui.color(c, R.color.nivel_ninguno), 0.55f);
            }
            int alfaRelleno = z.esParque() ? (sinDatos ? 70 : antiguo ? 90 : 140) : (sinDatos ? 25 : 45);

            List<LatLng> puntos = new ArrayList<>();
            for (double[] p : z.contorno) {
                puntos.add(new LatLng(p[0], p[1]));
            }
            PolygonOptions po = new PolygonOptions()
                    .addAll(puntos)
                    .fillColor(ColorUtils.setAlphaComponent(color, alfaRelleno))
                    .strokeColor(z.esParque() ? color : ColorUtils.setAlphaComponent(color, 160))
                    .strokeWidth(Ui.dp(c, z.esParque() ? 2.5f : 1.5f))
                    .zIndex(z.esParque() ? 2 : 1)
                    .clickable(true);
            if (antiguo || sinDatos || !z.esParque()) {
                List<PatternItem> patron = Arrays.asList(new Dash(Ui.dp(c, 8)), new Gap(Ui.dp(c, 6)));
                po.strokePattern(patron);
            }
            Polygon poly = mapa.addPolygon(po);
            poly.setTag(z.id);

            // Etiqueta con el nombre y el valor
            double[] centro = z.posicionEtiqueta();
            String linea2 = sinDatos ? "Sin datos" : magnitud.formatear(z.valor(magnitud)) + " " + magnitud.unidad
                    + (antiguo ? " · antiguo" : "");
            Marker m = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(centro[0], centro[1]))
                    .icon(etiqueta(z.nombre, linea2, color, z.esParque()))
                    .anchor(0.5f, 0.5f)
                    .zIndex(z.esParque() ? 4 : 3));
            if (m != null) {
                m.setTag(z.id);
            }
        }
    }

    private void dibujarPercepciones() {
        Context c = requireContext();
        for (Percepcion p : repo.percepciones()) {
            if (!"todos".equals(filtroPercepcion) && !filtroPercepcion.equals(p.tipo)) {
                continue;
            }
            String letra = "temp".equals(p.tipo) ? "T" : "aire".equals(p.tipo) ? "A" : "R";
            Marker m = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(p.lat, p.lng))
                    .icon(punto(letra, Ui.color(c, p.comoNivel().color)))
                    .anchor(0.5f, 0.5f)
                    .title(p.titulo())
                    .snippet(p.direccion + " · " + DateUtils.getRelativeTimeSpanString(p.cuando)));
            if (m != null) {
                m.setTag(p.id); // Long: no abre la ficha de zona
            }
        }
    }

    // ==============================================================
    // Ficha de una zona (hoja inferior, como en Google Maps)
    // ==============================================================
    private void mostrarZona(String id) {
        Zona z = repo.zona(id);
        if (z == null) {
            return;
        }
        Context c = requireContext();
        BottomSheetDialog hoja = new BottomSheetDialog(c);
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.columna(c);
        sv.addView(col);

        String estado = z.estado();
        boolean sinDatos = Reglas.ESTADO_SIN_DATOS.equals(estado);
        Ui.texto(col, (z.esParque() ? "Parque" : "Barrio") + " · " + z.numMediciones + " mediciones en la última hora",
                13, false, R.color.tinta_3);
        Ui.titulo(col, z.nombre);

        if (z.esParque()) {
            boolean fav = repo.esFavorito(z.id);
            Ui.boton(col, fav ? "★ Guardado" : "☆ Guardar parque", fav ? Ui.BOTON_LLENO : Ui.BOTON_BORDE, x -> {
                repo.alternarFavorito(z.id);
                hoja.dismiss();
                mostrarZona(id);
            });
            Ui.boton(col, "🔔 Alerta", Ui.BOTON_BORDE, x -> {
                hoja.dismiss();
                AlertaActivity.abrir(c, z.id);
            });
        }
        if (Reglas.ESTADO_ANTIGUO.equals(estado)) {
            Ui.aviso(col, "La última medición es " + Reglas.haceTexto(z.haceMin)
                    + ". Puede no reflejar cómo está ahora.", Nivel.MEDIO);
        }
        LinearLayout card = Ui.tarjeta(col);
        for (Magnitud m : Magnitud.values()) {
            Double v = z.valor(m);
            boolean sin = v == null || sinDatos;
            LinearLayout f = Ui.fila(card, m.nombre + (sin ? "" : " · " + m.formatear(v) + " " + m.unidad),
                    sin ? "Nadie con sensor ha pasado por aquí" : "Media · última medición " + Reglas.haceTexto(z.haceMin), null);
            Ui.insignia(f, sin ? "Sin datos" : Reglas.ESTADO_ANTIGUO.equals(estado) ? "Antiguo" : m.nivel(v).nombre,
                    sin || Reglas.ESTADO_ANTIGUO.equals(estado) ? Nivel.NINGUNO : m.nivel(v));
        }
        Ui.nota(col, getString(R.string.nota_privacidad_mapa));
        MaterialButton evol = Ui.boton(col, "Ver evolución", Ui.BOTON_LLENO, x -> {
            hoja.dismiss();
            ZonaActivity.abrir(c, z.id);
        });
        evol.setEnabled(!sinDatos);
        Ui.boton(col, "¿Qué significan estos valores?", Ui.BOTON_TEXTO,
                x -> TextoActivity.abrir(c, TextoActivity.GUIA));

        hoja.setContentView(sv);
        hoja.show();
    }

    // ==============================================================
    // Acciones
    // ==============================================================
    private void nuevaPercepcion() {
        if (!repo.haySesion()) {
            Ui.confirmar(requireContext(), "Inicia sesión", "Para compartir lo que notas necesitas una cuenta "
                    + "(así puedes borrar tus percepciones cuando quieras).", "Iniciar sesión",
                    () -> startActivity(new Intent(requireContext(), LoginActivity.class)));
            return;
        }
        LatLng centro = mapa != null ? mapa.getCameraPosition().target
                : new LatLng(DatosDemo.LAT_CENTRO, DatosDemo.LNG_CENTRO);
        PercepcionActivity.abrir(requireContext(), centro.latitude, centro.longitude);
    }

    private void alternar3d() {
        if (mapa == null) {
            return;
        }
        en3d = !en3d;
        // Pulsado: fondo de color primario (como el botón 3D del visual)
        boton3d.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Ui.color(requireContext(), en3d ? R.color.primario : R.color.primario_suave)));
        boton3d.setTextColor(Ui.color(requireContext(), en3d ? R.color.primario_tinta : R.color.primario));
        CameraPosition actual = mapa.getCameraPosition();
        CameraPosition nueva = new CameraPosition.Builder()
                .target(actual.target)
                .zoom(en3d ? Math.max(actual.zoom, 16f) : actual.zoom)
                .tilt(en3d ? 60f : 0f)
                .bearing(en3d ? -25f : 0f)
                .build();
        mapa.animateCamera(CameraUpdateFactory.newCameraPosition(nueva), 900, null);
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission") // se comprueba en tienePermisoUbicacion()
    private void activarMiUbicacion(boolean centrar) {
        if (mapa == null || !isAdded() || !tienePermisoUbicacion()) {
            return;
        }
        mapa.setMyLocationEnabled(true);
        if (centrar) {
            android.location.Location l = mapa.getMyLocation();
            LatLngBounds gandia = new LatLngBounds(new LatLng(38.950, -0.200), new LatLng(38.985, -0.165));
            if (l != null && gandia.contains(new LatLng(l.getLatitude(), l.getLongitude()))) {
                mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(l.getLatitude(), l.getLongitude()), 16f));
            } else {
                android.widget.Toast.makeText(requireContext(),
                        "Los datos de demostración son de Gandia: el mapa se queda allí.",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==============================================================
    // Iconos dibujados a mano (Canvas -> Bitmap)
    // ==============================================================
    private BitmapDescriptor etiqueta(String nombre, String valor, int color, boolean parque) {
        Context c = requireContext();
        Paint t1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        t1.setTextSize(Ui.dp(c, parque ? 12 : 11));
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        t1.setColor(Ui.color(c, R.color.tinta));
        Paint t2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        t2.setTextSize(Ui.dp(c, 11));
        t2.setTypeface(Typeface.MONOSPACE);
        t2.setColor(Ui.color(c, R.color.tinta_2));

        float pad = Ui.dp(c, 8), barra = Ui.dp(c, 4);
        float w = Math.max(t1.measureText(nombre), t2.measureText(valor)) + pad * 2 + barra;
        float h = t1.getTextSize() + t2.getTextSize() + pad * 1.6f;
        Bitmap bmp = Bitmap.createBitmap((int) Math.ceil(w), (int) Math.ceil(h), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(ColorUtils.setAlphaComponent(Ui.color(c, R.color.superficie), parque ? 245 : 215));
        RectF r = new RectF(0, 0, w, h);
        float radio = Ui.dp(c, 8);
        cv.drawRoundRect(r, radio, radio, fondo);
        Paint lateral = new Paint(Paint.ANTI_ALIAS_FLAG);
        lateral.setColor(color);
        cv.drawRoundRect(new RectF(0, 0, barra * 2, h), radio, radio, lateral);
        cv.drawRect(barra, 0, barra * 2, h, fondo);
        cv.drawText(nombre, barra + pad, pad * 0.6f + t1.getTextSize(), t1);
        cv.drawText(valor, barra + pad, pad * 0.6f + t1.getTextSize() + t2.getTextSize() + Ui.dp(c, 2), t2);
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private BitmapDescriptor punto(String letra, int color) {
        Context c = requireContext();
        int d = Ui.dp(c, 28);
        Bitmap bmp = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        cv.drawCircle(d / 2f, d / 2f, d / 2f, p);
        p.setColor(color);
        cv.drawCircle(d / 2f, d / 2f, d / 2f - Ui.dp(c, 2.5f), p);
        p.setColor(Color.WHITE);
        p.setTextSize(Ui.dp(c, 13));
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        cv.drawText(letra, d / 2f, d / 2f + Ui.dp(c, 4.5f), p);
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }
}
