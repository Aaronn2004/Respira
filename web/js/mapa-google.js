/* Google Maps como mapa base. Los contornos de Respira se cargan en WGS84. */
(function () {
  'use strict';

  const { MAPS_API_KEY = '', MAP_ID = '' } = window.RESPIRA_CONFIG || {};
  const clave = MAPS_API_KEY.trim();
  const mapId = MAP_ID.trim();
  const R = () => window.Respira;
  const css = v => getComputedStyle(document.documentElement).getPropertyValue(v).trim();
  const posicion = ([lng, lat]) => ({ lat, lng });
  const colorNivel = nivel => css(nivel === 'none' ? '--lv-none' : '--lv-' + nivel);

  const contenedor = document.createElement('div');
  contenedor.id = 'gmap';

  let estado = clave && !/pon_aqui/i.test(clave) ? 'pendiente' : 'sin-clave';
  let mapa, ambito, timer, Etiqueta;
  let ultimaFirma, ultimaSeleccion;
  const etiquetasZonas = new Map();
  let avisos = [];

  function fallo() {
    clearTimeout(timer);
    estado = 'error';
    R().render();
  }

  function cargar() {
    estado = 'cargando';
    window.gm_authFailure = fallo;
    window.__respiraMapsListo = () => {
      clearTimeout(timer);
      if (estado !== 'cargando') return;
      estado = 'listo';
      R().render();
    };
    const script = document.createElement('script');
    const params = new URLSearchParams({
      key: clave, language: 'es', region: 'ES', v: 'weekly', loading: 'async',
      callback: '__respiraMapsListo',
    });
    script.src = 'https://maps.googleapis.com/maps/api/js?' + params;
    script.async = true;
    script.onerror = fallo;
    timer = setTimeout(fallo, 12000);
    document.head.append(script);
  }

  function encuadrar() {
    mapa.fitBounds(ambito, { top: 36, bottom: 36, left: 32, right: 32 });
  }

  function crear() {
    const opciones = {
      center: { lat: 39.002, lng: -0.165 },
      zoom: 14,
      isFractionalZoomEnabled: true,
      gestureHandling: 'cooperative',
      clickableIcons: false,
      streetViewControl: false,
      mapTypeControl: window.innerWidth >= 700,
      mapTypeControlOptions: { position: google.maps.ControlPosition.TOP_RIGHT },
      fullscreenControl: window.innerWidth >= 700,
      zoomControl: true,
      restriction: {
        latLngBounds: { north: 39.026, south: 38.981, west: -0.187, east: -0.139 },
      },
    };
    if (mapId) opciones.mapId = mapId;
    mapa = new google.maps.Map(contenedor, opciones);
    mapa.data.addGeoJson(window.RespiraGrauPlatja);
    mapa.addListener('zoom_changed', () => contenedor.classList.toggle('gmap-detalle', mapa.getZoom() >= 16));

    ambito = new google.maps.LatLngBounds();
    mapa.data.forEach(feature => feature.getGeometry().forEachLatLng(p => ambito.extend(p)));
    mapa.data.addListener('click', ({ feature }) => R().seleccionarZona(feature.getId()));
    mapa.addListener('click', () => {
      if (R().S.sel) R().seleccionarZona(null);
    });

    Etiqueta = class extends google.maps.OverlayView {
      constructor(ancla, elemento) {
        super();
        this.pos = new google.maps.LatLng(posicion(ancla));
        this.el = elemento;
      }
      onAdd() {
        this.getPanes().overlayMouseTarget.append(this.el);
        google.maps.OverlayView.preventMapHitsAndGesturesFrom(this.el);
      }
      draw() {
        const punto = this.getProjection().fromLatLngToDivPixel(this.pos);
        if (!punto) return;
        this.el.style.left = punto.x + 'px';
        this.el.style.top = punto.y + 'px';
      }
      onRemove() { this.el.remove(); }
    };

    for (const zona of R().ZONAS) {
      const boton = document.createElement('button');
      boton.type = 'button';
      boton.dataset.act = 'sel';
      boton.dataset.arg = zona.id;
      const etiqueta = new Etiqueta(zona.ancla, boton);
      etiqueta.setMap(mapa);
      etiquetasZonas.set(zona.id, etiqueta);
    }
    encuadrar();
  }

  function dibujarZonas(enMapa) {
    const { S, ZONAS, zonaMapa, est, esc, zonaCoincide } = R();
    const visibles = new Set(enMapa ? ZONAS.filter(zonaCoincide).map(z => z.id) : []);
    mapa.data.setStyle(feature => {
      const id = feature.getId();
      const z = R().byZ(id);
      const nivel = zonaMapa(z).l;
      const seleccionada = S.sel === id;
      const color = colorNivel(nivel);
      return {
        visible: visibles.has(id),
        strokeColor: color,
        strokeOpacity: seleccionada ? 1 : .92,
        strokeWeight: seleccionada ? 4 : 2,
        fillColor: color,
        fillOpacity: seleccionada ? .55 : nivel === 'none' ? .17 : .32,
        zIndex: seleccionada ? 2 : 1,
      };
    });
    for (const z of ZONAS) {
      const boton = etiquetasZonas.get(z.id).el;
      const zm = zonaMapa(z);
      const seleccionada = S.sel === z.id;
      const antigua = est(z) === 'old';
      boton.hidden = !visibles.has(z.id);
      boton.className = 'gm-etq gm-' + zm.l + (seleccionada ? ' sel' : '');
      boton.setAttribute('aria-pressed', String(seleccionada));
      boton.setAttribute('aria-label', z.n + ', ' + zm.texto + (antigua ? ', dato antiguo' : ''));
      boton.innerHTML = `<span class="gm-marca" aria-hidden="true"></span>
        <span class="gm-nombre" aria-hidden="true">${esc(z.etiqueta)}<span class="gm-nivel">${esc(zm.l === 'none' ? 'Sin datos' : zm.valor)}</span></span>`;
    }
  }

  function dibujarAvisos(mostrar) {
    avisos.forEach(etiqueta => etiqueta.setMap(null));
    const { AVISOS, PERC, avOk } = R();
    avisos = mostrar ? AVISOS.filter(avOk).map(a => {
      const el = document.createElement('div');
      el.className = 'gm-av';
      el.style.background = colorNivel(['', 'bajo', 'medio', 'alto'][a.niv]);
      el.textContent = { ruido: 'R', temp: 'T', aire: 'A' }[a.tipo];
      el.title = PERC[a.tipo].n + ' · ' + a.dir;
      const etiqueta = new Etiqueta(a.ancla, el);
      etiqueta.setMap(mapa);
      return etiqueta;
    }) : [];
  }

  function actualizar() {
    const { S, byZ } = R();
    const comunidad = S.layer === 'comunidad';
    const firma = [S.layer, S.mag, S.q, S.sel, S.avt, S.avf, document.documentElement.dataset.tema].join('|');
    if (firma !== ultimaFirma) {
      ultimaFirma = firma;
      dibujarZonas(!comunidad);
      dibujarAvisos(comunidad);
    }
    if (!comunidad && S.sel && S.sel !== ultimaSeleccion) {
      mapa.panTo(posicion(byZ(S.sel).ancla));
    }
    ultimaSeleccion = S.sel;
  }

  window.RespiraMapa = {
    controlesHTML: () => estado === 'listo'
      ? `<div class="mapctl"><button type="button" aria-label="Volver a la vista inicial" title="Volver a la vista inicial" data-act="zoom" data-arg="reset">⟲</button>${mapId ? '<button type="button" class="t3" aria-label="Vista en perspectiva" data-act="tilt">3D</button>' : ''}</div>`
      : '',

    montar() {
      const destino = document.querySelector('.gmap-slot');
      if (!destino) return;
      if (estado === 'pendiente') cargar();
      if (estado !== 'listo') {
        const mensajes = {
          cargando: 'Cargando Google Maps…',
          'sin-clave': 'Google Maps requiere configuración. Puedes consultar las zonas en la lista.',
          error: 'No se ha podido cargar Google Maps. Puedes consultar las zonas en la lista.',
        };
        destino.innerHTML = `<p class="gmap-estado" role="status">${mensajes[estado]}</p>`;
        return;
      }
      if (contenedor.parentNode !== destino) destino.replaceChildren(contenedor);
      if (!mapa) crear();
      actualizar();
    },

    reset() {
      if (!mapa) return;
      R().S.sel = null;
      encuadrar();
      R().render();
    },

    perspectiva() {
      if (!mapa || !mapId) return;
      const inclinado = (mapa.getTilt() || 0) > 0;
      mapa.moveCamera({ tilt: inclinado ? 0 : 60, heading: inclinado ? 0 : -25, zoom: Math.max(mapa.getZoom(), 16.5) });
    },
  };
})();
