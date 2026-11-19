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
import com.nodia.respira.datos.ZonasCosta;

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
// --------------------------------------------------------------
// class MapaFragment
// # Mapa público, filtros y aportaciones de la comunidad.
// # Fragment <--- MapaFragment
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Vista = { contenido: Text, visible: B, habilitada: B }
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// Mapa = { centro: Coordenadas, zoom: R }
// Coordenadas = [ R ]_2
// Imagen = { ancho: N, alto: N, pixeles: [ Z ] }
// --------------------------------------------------------------
public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private Repositorio repo;
    private GoogleMap mapa;
    private Magnitud magnitud = Magnitud.O3;
    private String filtroPercepcion = "todos";
    private boolean capaComunidad = false;
    private boolean en3d = false;
    private boolean general = true;
    private boolean encuadreInicial;
    private CameraPosition camaraGuardada;
    private String firmaDibujo;
    private String zonaSeleccionada;
    private final List<EtiquetaZona> etiquetas = new ArrayList<>();

    // Conserva el tamaño real de cada etiqueta para sustituir solapamientos por puntos.
    private static final class EtiquetaZona {
        final Marker marcador;
        final BitmapDescriptor completa, punto;
        final int ancho, alto;

        // --------------------------------------------------------------
        // + marcador: Marcador, imagen: Imagen, icono_punto: Imagen
        //       -->
        //     EtiquetaZona() -->
        // --------------------------------------------------------------
        EtiquetaZona(Marker marcador, Bitmap imagen, BitmapDescriptor punto) {
            this.marcador = marcador;
            this.completa = BitmapDescriptorFactory.fromBitmap(imagen);
            this.punto = punto;
            ancho = imagen.getWidth();
            alto = imagen.getHeight();
        }
    }

    private ChipGroup filtros;
    private TextView leyenda;
    private MaterialButton franja, botonCuenta, boton3d;
    private View cabecera, pie;

    private final Runnable alCambiar = this::refrescar;
    // Datos reales: se piden a la API al abrir el mapa y cada minuto
    private final android.os.Handler temporizador = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable recargarZonas = new Runnable() {
        // --------------------------------------------------------------
        // # effects: solicita zonas y programa la siguiente actualización
        // +
        //   run() -->
        // --------------------------------------------------------------
        @Override
        public void run() {
            if (capaComunidad) refrescar();
            repo.actualizarZonas();
            temporizador.postDelayed(this, 60_000);
        }
    };

    private final ActivityResultLauncher<String[]> pedirUbicacion = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> activarMiUbicacion(true));

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
        if (savedInstanceState != null) {
            general = savedInstanceState.getBoolean("general", true);
            capaComunidad = savedInstanceState.getBoolean("comunidad");
            filtroPercepcion = savedInstanceState.getString("filtro", "todos");
            en3d = savedInstanceState.getBoolean("perspectiva");
            for (Magnitud m : Magnitud.values()) {
                if (m.id.equals(savedInstanceState.getString("magnitud"))) magnitud = m;
            }
            camaraGuardada = savedInstanceState.getParcelable("camara");
        }
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + v: Vista, savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onViewCreated() -->
    // --------------------------------------------------------------
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = Repositorio.obtener(requireContext());
        cabecera = v.findViewById(R.id.cabecera);
        pie = v.findViewById(R.id.pie);
        filtros = v.findViewById(R.id.filtros);
        leyenda = v.findViewById(R.id.leyenda);
        leyenda.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        franja = v.findViewById(R.id.franja);
        botonCuenta = v.findViewById(R.id.cuenta);

        MaterialButtonToggleGroup capa = v.findViewById(R.id.capa);
        capa.check(capaComunidad ? R.id.capa_comunidad : R.id.capa_mediciones);
        capa.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado) {
                capaComunidad = id == R.id.capa_comunidad;
                pintarFiltros();
                refrescar();
            }
        });

        boton3d = v.findViewById(R.id.vista3d);
        boton3d.setOnClickListener(x -> alternar3d());
        pintarPerspectiva();
        v.findViewById(R.id.encuadrar).setOnClickListener(x -> encuadrar());
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

    // --------------------------------------------------------------
    // # effects: actualiza la suscripción al repositorio; solicita datos al servidor; ajusta las tareas pendientes
    // +
    //   onStart() -->
    // --------------------------------------------------------------
    @Override
    public void onStart() {
        super.onStart();
        repo.observar(alCambiar);
        temporizador.post(recargarZonas);
        refrescar();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la suscripción al repositorio; ajusta las tareas pendientes
    // +
    //   onStop() -->
    // --------------------------------------------------------------
    @Override
    public void onStop() {
        repo.dejarDeObservar(alCambiar);
        temporizador.removeCallbacks(recargarZonas);
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
            refrescar();
        }
    }

    // --------------------------------------------------------------
    // # effects: conserva los filtros y la cámara al girar el móvil.
    // + estado: EstadoPantalla
    //       -->
    //     onSaveInstanceState() <--
    // --------------------------------------------------------------
    @Override
    public void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean("general", general);
        estado.putBoolean("comunidad", capaComunidad);
        estado.putBoolean("perspectiva", en3d);
        estado.putString("filtro", filtroPercepcion);
        estado.putString("magnitud", magnitud.id);
        estado.putParcelable("camara", mapa == null ? camaraGuardada : mapa.getCameraPosition());
    }

    // --------------------------------------------------------------
    // # effects: libera referencias a la vista anterior y a su mapa.
    // +
    //     onDestroyView() -->
    // --------------------------------------------------------------
    @Override
    public void onDestroyView() {
        if (mapa != null) camaraGuardada = mapa.getCameraPosition();
        mapa = null;
        cabecera = null;
        pie = null;
        firmaDibujo = null;
        etiquetas.clear();
        filtros = null;
        leyenda = null;
        franja = null;
        botonCuenta = null;
        boton3d = null;
        super.onDestroyView();
    }

    // --------------------------------------------------------------
    // # effects: encuadra las zonas reales, conservando espacio para los controles.
    // -
    //     encuadrar() -->
    // --------------------------------------------------------------
    private void encuadrar() {
        View vista = getView();
        if (mapa == null || vista == null || vista.getWidth() == 0 || vista.getHeight() == 0) return;
        LatLngBounds.Builder limites = new LatLngBounds.Builder();
        for (Zona zona : repo.zonas()) {
            for (List<List<double[]>> poligono : zona.contornos()) {
                for (List<double[]> anillo : poligono) {
                    for (double[] p : anillo) limites.include(new LatLng(p[0], p[1]));
                }
            }
        }
        if (repo.zonas().isEmpty()) return;
        mapa.moveCamera(CameraUpdateFactory.newLatLngBounds(limites.build(), Ui.dp(requireContext(), 24)));
        encuadreInicial = true;
        en3d = false;
        pintarPerspectiva();
    }

    // --------------------------------------------------------------
    // # effects: refleja el estado de perspectiva en su botón.
    // -
    //     pintarPerspectiva() <--
    // --------------------------------------------------------------
    private void pintarPerspectiva() {
        if (boton3d == null) return;
        boton3d.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Ui.color(requireContext(), en3d ? R.color.primario : R.color.superficie)));
        boton3d.setTextColor(Ui.color(requireContext(), en3d ? R.color.primario_tinta : R.color.primario));
        boton3d.setContentDescription(en3d ? "Volver a vista plana" : "Vista en perspectiva");
    }

    // ==============================================================
    // Google Maps listo
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: configura el mapa y consulta la ubicación cuando hay permiso
    // + googleMap: Mapa
    //       -->
    //           onMapReady() -->
    // --------------------------------------------------------------
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (!isAdded() || getView() == null) return;
        mapa = googleMap;
        firmaDibujo = null;
        boolean oscuro = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        mapa.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                oscuro ? R.raw.mapa_oscuro : R.raw.mapa_claro));
        mapa.getUiSettings().setZoomControlsEnabled(false); // usamos nuestros botones + / −
        mapa.getUiSettings().setCompassEnabled(true);
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setMyLocationButtonEnabled(false); // usamos nuestro botón
        mapa.getUiSettings().setTiltGesturesEnabled(true);
        mapa.getUiSettings().setRotateGesturesEnabled(true);
        mapa.setBuildingsEnabled(true);
        mapa.setIndoorEnabled(false);
        mapa.setMinZoomPreference(10f);
        mapa.setOnCameraIdleListener(this::actualizarEtiquetas);
        encuadreInicial = camaraGuardada != null;
        if (camaraGuardada != null) {
            mapa.moveCamera(CameraUpdateFactory.newCameraPosition(camaraGuardada));
        }

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
            if (tag instanceof Long) {          // percepción: su hoja (Editar/Borrar si es tuya)
                mostrarPercepcion((Long) tag);
                return true;
            }
            return false;
        });
        activarMiUbicacion(false);
        ajustarRelleno();
        dibujar();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   ajustarRelleno() -->
    // --------------------------------------------------------------
    private void ajustarRelleno() {
        if (mapa == null || cabecera == null) {
            return;
        }
        int arriba = cabecera.getBottom() + Ui.dp(requireContext(), 4);
        int abajo = pie.getHeight();
        mapa.setPadding(0, arriba, 0, abajo);
        View controles = getView().findViewById(R.id.botones);
        controles.setTranslationY((arriba - abajo) / 2f);
        if (!encuadreInicial && getView().getHeight() > arriba + abajo) encuadrar();
    }

    // ==============================================================
    // Repintado (cabecera, leyenda, franja y capas del mapa)
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   refrescar() -->
    // --------------------------------------------------------------
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
            boolean hayDatos = repo.zonas().stream().anyMatch(z -> general
                    ? z.nivelGeneral() != Nivel.NINGUNO : z.valor(magnitud) != null);
            boolean simulados = repo.zonas().stream().anyMatch(z -> Arrays.stream(Magnitud.values())
                    .anyMatch(m -> "simulado".equals(z.dato(m).origen)));
            String nota = !repo.datosReales() ? "Sin conexión: no se muestran mediciones nuevas"
                    : !hayDatos ? "Sin mediciones disponibles para estas zonas"
                    : simulados ? "Incluye mediciones simuladas" : "Cortes orientativos de Respira";
            leyenda.setText(leyenda(general ? "Estado general · última hora"
                    : magnitud.nombre + " (" + magnitud.unidad + ") · última hora", nota, true));
            Recorrido r = repo.recorrido();
            if (r != null && r.enCurso()) {
                franja.setText("● Enviando datos · " + r.enviadas + " mediciones hoy   Ver ›");
            } else {
                franja.setText("Conectar mi sensor   ›");
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
    // --------------------------------------------------------------
    // # effects: lee los colores de recursos y compone la leyenda
    // - titulo: Text, nota: Text, conEstados: B
    //       -->
    //           leyenda() <--
    //       <--
    //   resultado: Text
    // --------------------------------------------------------------
    private CharSequence leyenda(String titulo, String nota, boolean conEstados) {
        Context c = requireContext();
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        sb.append(titulo, new android.text.style.StyleSpan(Typeface.BOLD), 0);
        sb.append("\n");
        int[][] items = conEstados
                ? new int[][]{{R.color.nivel_bajo, 0}, {R.color.nivel_medio, 1}, {R.color.nivel_alto, 2}, {R.color.nivel_ninguno, 3}}
                : new int[][]{{R.color.nivel_bajo, 0}, {R.color.nivel_medio, 1}, {R.color.nivel_alto, 2}};
        String[] nombres = {"Bajo", "Medio", "Alto", "Sin datos", "Antiguo (1–24 h)"};
        for (int[] it : items) {
            sb.append("●", new android.text.style.ForegroundColorSpan(Ui.color(c, it[0])), 0);
            sb.append(" ").append(nombres[it[1]]).append("   ");
        }
        sb.append("\n");
        sb.append(nota, new android.text.style.ForegroundColorSpan(Ui.color(c, R.color.tinta_3)), 0);
        sb.append(" · ");
        sb.append("© OpenStreetMap", new android.text.style.URLSpan("https://www.openstreetmap.org/copyright"),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   pintarFiltros() -->
    // --------------------------------------------------------------
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
            Chip todos = chip("General", general);
            todos.setOnClickListener(x -> { general = true; refrescar(); });
            for (Magnitud m : Magnitud.values()) {
                Chip chip = chip(m.nombre, !general && m == magnitud);
                chip.setOnClickListener(x -> {
                    general = false;
                    magnitud = m;
                    refrescar();
                });
            }
        }
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - texto: Text, marcado: B
    //       -->
    //           chip() -->
    //       <--
    //   resultado: Vista
    // --------------------------------------------------------------
    private Chip chip(String texto, boolean marcado) {
        Chip c = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        c.setText(texto);
        c.setCheckable(true);
        c.setChecked(marcado);
        filtros.addView(c);
        return c;
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   dibujar() -->
    // --------------------------------------------------------------
    private void dibujar() {
        if (mapa == null || !isAdded()) return;
        StringBuilder firma = new StringBuilder(capaComunidad + ":" + general + ":" + magnitud + ":"
                + filtroPercepcion + ":" + zonaSeleccionada);
        if (capaComunidad) {
            for (Percepcion p : repo.percepciones()) {
                firma.append('|').append(p.id).append(':').append(p.tipo).append(':').append(p.nivel)
                        .append(':').append(p.lat).append(':').append(p.lng).append(':').append(p.cuando)
                        .append(':').append(p.simulada).append(':').append(p.direccion);
            }
        } else {
            for (Zona z : repo.zonas()) {
                firma.append('|').append(z.id);
                for (Magnitud m : Magnitud.values()) {
                    firma.append(':').append(z.valor(m)).append(':').append(z.estado(m))
                            .append(':').append(z.dato(m).origen);
                }
            }
        }
        if (firma.toString().equals(firmaDibujo)) return;
        firmaDibujo = firma.toString();
        etiquetas.clear();
        mapa.clear();
        if (capaComunidad) dibujarPercepciones();
        else dibujarZonas();
        actualizarEtiquetas();
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   dibujarZonas() -->
    // --------------------------------------------------------------
    private void dibujarZonas() {
        Context c = requireContext();
        for (Zona z : repo.zonas()) {
            String estado = general ? z.estado() : z.estado(magnitud);
            Nivel n = general ? z.nivelGeneral() : z.nivel(magnitud);
            int color = Ui.color(c, n.color);
            boolean antiguo = Reglas.ESTADO_ANTIGUO.equals(estado);
            boolean sinDatos = n == Nivel.NINGUNO;
            boolean estimado = "estimado".equals(estado);
            boolean seleccionada = z.id.equals(zonaSeleccionada);
            int borde = ColorUtils.setAlphaComponent(color, antiguo || estimado ? 166 : 235);
            // Mismos niveles y opacidades de la web: la cartografía queda legible bajo cada zona.
            for (List<List<double[]>> poligono : z.contornos()) {
                if (poligono.isEmpty() || poligono.get(0).size() < 3) continue;
                PolygonOptions po = new PolygonOptions()
                        .addAll(anillo(poligono.get(0)))
                        .fillColor(ColorUtils.setAlphaComponent(color, seleccionada ? 140 : sinDatos || estimado ? 43 : 82))
                        .strokeColor(borde).strokeWidth(Ui.dp(c, seleccionada ? 3 : 1.5f))
                        .zIndex(seleccionada ? 2 : 1).clickable(true);
                for (int i = 1; i < poligono.size(); i++) po.addHole(anillo(poligono.get(i)));
                if (estimado) po.strokePattern(Arrays.asList(new com.google.android.gms.maps.model.Dot(), new Gap(Ui.dp(c, 5))));
                else if (antiguo) po.strokePattern(Arrays.asList(new Dash(Ui.dp(c, 6)), new Gap(Ui.dp(c, 4))));
                mapa.addPolygon(po).setTag(z.id);
            }
            double[] centro = z.posicionEtiqueta();
            String valor = general ? n.nombre : sinDatos ? "Sin datos"
                    : magnitud.formatear(z.valor(magnitud)) + " " + magnitud.unidad;
            boolean simulada = general ? Arrays.stream(Magnitud.values())
                    .anyMatch(m -> "simulado".equals(z.dato(m).origen)) : "simulado".equals(z.dato(magnitud).origen);
            String nota = antiguo ? " · antiguo" : estimado ? " · estimado" : simulada && !sinDatos ? " · simulado" : "";
            Bitmap imagen = etiqueta(z.nombreCorto(), valor + nota, color, true);
            Marker etiqueta = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(centro[0], centro[1]))
                    .icon(BitmapDescriptorFactory.fromBitmap(imagen))
                    .title(z.nombre).snippet(valor + nota)
                    .anchor(.5f, .5f).zIndex(seleccionada ? 5 : 3));
            if (etiqueta != null) {
                etiqueta.setTag(z.id);
                etiquetas.add(new EtiquetaZona(etiqueta, imagen, punto("", color)));
            }
        }
    }

    // --------------------------------------------------------------
    // # effects: evita que los nombres se tapen, manteniendo puntos pulsables al alejar.
    // -
    //     actualizarEtiquetas() <--
    // --------------------------------------------------------------
    private void actualizarEtiquetas() {
        if (mapa == null || getView() == null || capaComunidad) return;
        List<RectF> ocupadas = new ArrayList<>();
        List<EtiquetaZona> orden = new ArrayList<>(etiquetas);
        orden.sort((a, b) -> Boolean.compare(zonaSeleccionada != null && zonaSeleccionada.equals(b.marcador.getTag()),
                zonaSeleccionada != null && zonaSeleccionada.equals(a.marcador.getTag())));
        for (EtiquetaZona e : orden) {
            android.graphics.Point p = mapa.getProjection().toScreenLocation(e.marcador.getPosition());
            RectF rect = new RectF(p.x - e.ancho / 2f, p.y - e.alto / 2f,
                    p.x + e.ancho / 2f, p.y + e.alto / 2f);
            boolean libre = ocupadas.stream().noneMatch(otra -> RectF.intersects(rect, otra));
            e.marcador.setIcon(libre ? e.completa : e.punto);
            if (libre) ocupadas.add(rect);
        }
    }

    // --------------------------------------------------------------
    // # El catálogo ya adapta GeoJSON a [latitud, longitud], el orden que espera Maps.
    // - datos: [ Coordenadas ]
    //       -->
    //           anillo() --x
    //       <--
    //   resultado: [ Coordenadas ]
    // --------------------------------------------------------------
    private static List<LatLng> anillo(List<double[]> datos) {
        List<LatLng> puntos = new ArrayList<>();
        for (double[] p : datos) puntos.add(new LatLng(p[0], p[1]));
        return puntos;
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   dibujarPercepciones() -->
    // --------------------------------------------------------------
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
                    .title(p.titulo() + (p.simulada ? " · Simulada" : ""))
                    .snippet(p.direccion + " · " + DateUtils.getRelativeTimeSpanString(p.cuando)));
            if (m != null) {
                m.setTag(p.id); // Long: no abre la ficha de zona
            }
        }
    }

    // ==============================================================
    // Hoja de una percepción: Editar y Borrar solo si es del usuario
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - id: N
    //       -->
    //           mostrarPercepcion() -->
    // --------------------------------------------------------------
    private void mostrarPercepcion(long id) {
        Percepcion p = repo.percepcion(id);
        if (p == null) {
            return;
        }
        Context c = requireContext();
        BottomSheetDialog hoja = new BottomSheetDialog(c);
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.columna(c);
        sv.addView(col);
        Ui.titulo(col, p.titulo());
        Ui.texto(col, p.direccion, 14, false, R.color.tinta_3);
        Ui.parrafo(col, "Publicado " + DateUtils.getRelativeTimeSpanString(p.cuando) + ". Dejará de mostrarse a las 24 h.");
        String detalle = Percepcion.detallesTipo(p.tipo)[p.nivel - 1];
        if (repo.esMia(p)) {
            Ui.nota(col, "Es tu percepción («" + detalle + "»), no una medición.");
            Ui.boton(col, "Editar", Ui.BOTON_BORDE, x -> {
                hoja.dismiss();
                PercepcionActivity.editar(c, p.id);
            });
            Ui.boton(col, "Borrar", Ui.BOTON_LLENO, x -> Ui.confirmar(c, "¿Borrar la percepción?",
                    "Dejará de verse en el mapa.", "Borrar", () -> {
                        repo.borrarPercepcion(p.id, Ui.hecho(c, hoja::dismiss));
                    })).setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.color(c, R.color.nivel_alto)));
        } else if (p.simulada) {
            Ui.nota(col, "Percepción simulada para la demostración.");
        } else {
            Ui.nota(col, "Es la percepción de un vecino («" + detalle + "»), no una medición. "
                    + "Solo quien la publicó puede editarla o borrarla.");
        }
        hoja.setContentView(sv);
        hoja.show();
    }

    // ==============================================================
    // Ficha de una zona (hoja inferior, como en Google Maps)
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - id: N
    //       -->
    //           mostrarZona() -->
    // --------------------------------------------------------------
    private void mostrarZona(String id) {
        Zona z = repo.zona(id);
        if (z == null) {
            return;
        }
        zonaSeleccionada = id;
        dibujar();
        Context c = requireContext();
        BottomSheetDialog hoja = new BottomSheetDialog(c);
        hoja.setOnDismissListener(d -> { zonaSeleccionada = null; dibujar(); });
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.columna(c);
        sv.addView(col);

        Ui.texto(col, z.tipoTexto() + " · " + z.numMediciones + " mediciones en la última hora",
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
        LinearLayout card = Ui.tarjeta(col);
        for (Magnitud m : Magnitud.values()) Ui.datoZona(card, z, m);
        Ui.nota(col, getString(R.string.nota_privacidad_mapa));
        Ui.boton(col, "Ver evolución", Ui.BOTON_LLENO, x -> {
            hoja.dismiss();
            ZonaActivity.abrir(c, z.id);
        });
        Ui.boton(col, "¿Qué significan estos valores?", Ui.BOTON_TEXTO,
                x -> TextoActivity.abrir(c, TextoActivity.GUIA));

        hoja.setContentView(sv);
        hoja.show();
    }

    // ==============================================================
    // Acciones
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   nuevaPercepcion() -->
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // -
    //   alternar3d() -->
    // --------------------------------------------------------------
    private void alternar3d() {
        if (mapa == null) {
            return;
        }
        en3d = !en3d;
        pintarPerspectiva();
        CameraPosition actual = mapa.getCameraPosition();
        CameraPosition nueva = new CameraPosition.Builder()
                .target(actual.target)
                .zoom(en3d ? Math.max(actual.zoom, 16f) : actual.zoom)
                .tilt(en3d ? 60f : 0f)
                .bearing(en3d ? -25f : 0f)
                .build();
        mapa.animateCamera(CameraUpdateFactory.newCameraPosition(nueva), 900, null);
    }

    // --------------------------------------------------------------
    // # effects: consulta los permisos o ajustes del dispositivo
    // - tienePermisoUbicacion() <--
    //       <--
    //   resultado: B
    // --------------------------------------------------------------
    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // --------------------------------------------------------------
    // # effects: configura el mapa y consulta la ubicación cuando hay permiso
    // - centrar: B
    //       -->
    //           activarMiUbicacion() -->
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission") // se comprueba en tienePermisoUbicacion()
    private void activarMiUbicacion(boolean centrar) {
        if (mapa == null || !isAdded()) return;
        if (!tienePermisoUbicacion()) {
            if (centrar) android.widget.Toast.makeText(requireContext(),
                    "Permite el acceso a la ubicación para localizarte en el mapa.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        mapa.setMyLocationEnabled(true);
        if (!centrar) return;
        android.location.Location l = mapa.getMyLocation();
        if (l == null) {
            android.widget.Toast.makeText(requireContext(), "Todavía no se ha obtenido tu ubicación. Inténtalo de nuevo.",
                    android.widget.Toast.LENGTH_SHORT).show();
        } else if (ZonasCosta.enAmbito(l.getLatitude(), l.getLongitude())) {
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(l.getLatitude(), l.getLongitude()), 16f));
        } else {
            android.widget.Toast.makeText(requireContext(), "Tu ubicación está fuera del Grau i Platja, la zona que cubre este mapa.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    // ==============================================================
    // Iconos dibujados a mano (Canvas -> Bitmap)
    // ==============================================================
    // --------------------------------------------------------------
    // # effects: lee el tema y dibuja una imagen para el mapa
    // - nombre: Text, valor: Text, color: Z, parque: B
    //       -->
    //           etiqueta() <--
    //       <--
    //   resultado: Imagen
    // --------------------------------------------------------------
    private Bitmap etiqueta(String nombre, String valor, int color, boolean parque) {
        Context c = requireContext();
        Paint t1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        t1.setTextSize(Ui.dp(c, parque ? 12 : 11));
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        t1.setColor(Ui.color(c, R.color.tinta));
        Paint t2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        t2.setTextSize(Ui.dp(c, 11));
        t2.setTypeface(Typeface.DEFAULT);
        t2.setColor(Ui.color(c, R.color.tinta_2));

        float pad = Ui.dp(c, 7), barra = Ui.dp(c, 3);
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
        return bmp;
    }

    // --------------------------------------------------------------
    // # effects: lee el tema y dibuja una imagen para el mapa
    // - letra: Text, color: Z
    //       -->
    //           punto() <--
    //       <--
    //   resultado: Imagen
    // --------------------------------------------------------------
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
