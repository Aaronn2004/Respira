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
  const temaActual = () => document.documentElement.dataset.tema === 'oscuro' ? 'oscuro' : 'claro';

  const PALETAS = {
    claro: {
      terreno: '#eef4f1', manzanas: '#e8efec', parques: '#d4e6dc', agua: '#acd7dd',
      calle: '#ffffff', avenida: '#fcfdfc', borde: '#cbdad7', carretera: '#b3c8c5',
      texto: '#365154', halo: '#f6faf8', textoAgua: '#265968',
    },
    oscuro: {
      terreno: '#1b2a2c', manzanas: '#223438', parques: '#263e35', agua: '#132e39',
      calle: '#3d5255', avenida: '#536d70', borde: '#253b3e', carretera: '#647e80',
      texto: '#ccdfde', halo: '#19282b', textoAgua: '#9ebdcb',
    },
  };

  function estiloCallejero({ terreno, manzanas, parques, agua, calle, avenida, borde, carretera, texto, halo, textoAgua }) {
    return [
      { elementType: 'geometry', stylers: [{ color: terreno }] },
      { elementType: 'labels.text.fill', stylers: [{ color: texto }] },
      { elementType: 'labels.text.stroke', stylers: [{ color: halo }] },
      { featureType: 'landscape.man_made', elementType: 'geometry.fill', stylers: [{ color: manzanas }] },
      { featureType: 'poi.park', elementType: 'geometry.fill', stylers: [{ color: parques }] },
      { featureType: 'poi.business', stylers: [{ visibility: 'off' }] },
      { featureType: 'road', elementType: 'geometry.fill', stylers: [{ color: calle }] },
      { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: borde }] },
      { featureType: 'road.arterial', elementType: 'geometry.fill', stylers: [{ color: avenida }] },
      { featureType: 'road.highway', elementType: 'geometry.fill', stylers: [{ color: carretera }] },
      { featureType: 'water', elementType: 'geometry.fill', stylers: [{ color: agua }] },
      { featureType: 'water', elementType: 'labels.text.fill', stylers: [{ color: textoAgua }] },
    ];
  }

  const contenedor = document.createElement('div');
  contenedor.id = 'gmap';

  let estado = clave && !/pon_aqui/i.test(clave) ? 'pendiente' : 'sin-clave';
  let mapa, ambito, timer, Etiqueta;
  let ultimaFirma, ultimaSeleccion, ultimoAviso, estiloMapa;
  const etiquetasZonas = new Map();
  const contornos = new Map();
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

  function actualizarCallejero() {
    if (mapId) return;
    const estilo = mapa.getMapTypeId() === google.maps.MapTypeId.ROADMAP ? temaActual() : null;
    if (estilo === estiloMapa) return;
    estiloMapa = estilo;
    mapa.setOptions({ styles: estilo ? estiloCallejero(PALETAS[estilo]) : [] });
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
    else {
      estiloMapa = temaActual();
      opciones.styles = estiloCallejero(PALETAS[estiloMapa]);
    }
    mapa = new google.maps.Map(contenedor, opciones);
    mapa.addListener('zoom_changed', () => contenedor.classList.toggle('gmap-detalle', mapa.getZoom() >= 16));
    mapa.addListener('maptypeid_changed', actualizarCallejero);

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
  }

  function actualizarContornos() {
    let cambio = false;
    for (const zona of R().ZONAS) {
      const firma = JSON.stringify(zona.geometry);
      if (contornos.get(zona.id) === firma) continue;
      const anterior = mapa.data.getFeatureById(zona.id);
      if (anterior) mapa.data.remove(anterior);
      const feature = { type: 'Feature', id: zona.id, geometry: zona.geometry, properties: {} };
      mapa.data.addGeoJson({ type: 'FeatureCollection', features: [feature] });
      contornos.set(zona.id, firma);
      cambio = true;
    }
    if (!cambio) return;
    ambito = new google.maps.LatLngBounds();
    mapa.data.forEach(feature => feature.getGeometry().forEachLatLng(p => ambito.extend(p)));
    if (!ultimaFirma) encuadrar();
  }

  function dibujarZonas(enMapa) {
    const { S, ZONAS, zonaMapa, esc, zonaCoincide } = R();
    const visibles = new Set(enMapa ? ZONAS.filter(zonaCoincide).map(z => z.id) : []);
    mapa.data.setStyle(feature => {
      const id = feature.getId();
      const z = R().byZ(id);
      const { l: nivel, estado } = zonaMapa(z);
      const seleccionada = S.sel === id;
      const color = colorNivel(nivel);
      return {
        visible: visibles.has(id),
        strokeColor: color,
        strokeOpacity: seleccionada ? 1 : estado === 'antiguo' || estado === 'estimado' ? .65 : .92,
        strokeWeight: seleccionada ? 4 : 2,
        fillColor: color,
        fillOpacity: seleccionada ? .55 : nivel === 'none' || estado === 'estimado' ? .17 : .32,
        zIndex: seleccionada ? 2 : 1,
      };
    });
    for (const z of ZONAS) {
      const boton = etiquetasZonas.get(z.id).el;
      const zm = zonaMapa(z);
      const seleccionada = S.sel === z.id;
      const estado = { antiguo: ' antigua', estimado: ' estimada' }[zm.estado] || '';
      boton.hidden = !visibles.has(z.id);
      boton.className = 'gm-etq gm-' + zm.l + estado + (seleccionada ? ' sel' : '');
      boton.setAttribute('aria-pressed', String(seleccionada));
      boton.setAttribute('aria-label', z.n + ', ' + zm.texto + (zm.nota ? ', ' + zm.nota : ''));
      boton.innerHTML = `<span class="gm-marca" aria-hidden="true"></span>
        <span class="gm-nombre" aria-hidden="true">${esc(z.etiqueta)}<span class="gm-nivel">${esc(zm.valor)}${zm.nota ? ' · ' + esc(zm.nota) : ''}</span></span>`;
    }
  }

  function dibujarAvisos(mostrar) {
    avisos.forEach(etiqueta => etiqueta.setMap(null));
    const { AVISOS, PERC, S, avOk } = R();
    avisos = mostrar ? AVISOS.filter(avOk).map(a => {
      const el = document.createElement('button');
      el.type = 'button';
      el.dataset.act = 'verPercepcion';
      el.dataset.arg = a.id;
      el.setAttribute('aria-pressed', String(S.avSel === a.id));
      el.className = 'gm-av' + (S.avSel === a.id ? ' sel' : '');
      el.style.background = colorNivel(['', 'bajo', 'medio', 'alto'][a.niv]);
      el.textContent = { ruido: 'R', temp: 'T', aire: 'A' }[a.tipo];
      const nombre = PERC[a.tipo].n + ', ' + PERC[a.tipo].lv[a.niv - 1] + ' · ' + a.dir + (a.simulado ? ' · Simulada' : '');
      el.title = nombre;
      el.setAttribute('aria-label', nombre);
      const etiqueta = new Etiqueta(a.ancla, el);
      etiqueta.setMap(mapa);
      return etiqueta;
    }) : [];
  }

  function actualizar() {
    actualizarCallejero();
    actualizarContornos();
    const { S, byZ, AVISOS, avOk } = R();
    const comunidad = S.layer === 'comunidad';
    const visibles = comunidad ? AVISOS.filter(avOk).map(a => a.id + ':' + a.momento + ':' + a.tipo + ':' + a.niv) : [];
    const firma = [S.layer, S.mag, S.q, S.sel, S.avt, S.avf, S.avSel, document.documentElement.dataset.tema,
      window.RespiraDatos?.version, ...visibles].join('|');
    if (firma !== ultimaFirma) {
      ultimaFirma = firma;
      dibujarZonas(!comunidad);
      dibujarAvisos(comunidad);
    }
    if (!comunidad && S.sel && S.sel !== ultimaSeleccion) {
      mapa.panTo(posicion(byZ(S.sel).ancla));
    }
    if (comunidad && S.avSel && S.avSel !== ultimoAviso) {
      const aviso = R().AVISOS.find(a => a.id === S.avSel);
      if (aviso) mapa.panTo(posicion(aviso.ancla));
    }
    ultimaSeleccion = S.sel;
    ultimoAviso = S.avSel;
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
      R().S.avSel = null;
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
