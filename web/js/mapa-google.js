/* =============================================================
   mapa-google.js · Mapa de Respira con Google Maps JavaScript API
   -------------------------------------------------------------
   - Se activa solo si js/config.js trae una clave (MAPS_API_KEY).
     Sin clave, o si la clave falla, app.js usa el mapa SVG.
   - Las zonas del prototipo están en un lienzo de 900 x 560; aquí
     se colocan sobre Gandia con una transformación lineal. Son
     zonas de demostración; con la API (datos-api.js) cada zona trae
     su polígono real (z.poligono) y se dibuja ese.
   - El contenedor del mapa se crea UNA vez y se reengancha tras
     cada render(): así no se crea un mapa nuevo (ni se cobra una
     carga nueva) cada vez que cambia el estado.
   ============================================================= */
(function () {
  'use strict';

  const CFG = window.RESPIRA_CONFIG || {};
  const CLAVE = (CFG.MAPS_API_KEY || '').trim();
  const MAP_ID = (CFG.MAP_ID || '').trim();

  // Caja de Gandia que corresponde al lienzo 900 x 560 del prototipo web
  const NORTE = 38.9815, SUR = 38.9540, OESTE = -0.2050, ESTE = -0.1610;
  const geo = (x, y) => ({ lat: NORTE - y / 560 * (NORTE - SUR), lng: OESTE + x / 900 * (ESTE - OESTE) });
  const CENTRO = geo(450, 280);
  // Zoom para que quepan todas las zonas: el mapa estrecho (móvil, panel lateral) se aleja un nivel o dos
  const zoomInicial = () => { const w = (contenedor && contenedor.clientWidth) || window.innerWidth; return w < 820 ? 14 : 15; };

  let estado = CLAVE && !/pon_aqui/i.test(CLAVE) ? 'pendiente' : 'off'; // pendiente | cargando | listo | error | off
  let mapa = null;
  let capas = [];          // polígonos y etiquetas dibujados
  let ultimaFirma = '';
  let ultimaSel = null;
  let Etiqueta = null;     // clase OverlayView (se define al cargar la API)
  const contenedor = document.createElement('div');
  contenedor.id = 'gmap';

  const R = () => window.Respira;
  const css = v => getComputedStyle(document.documentElement).getPropertyValue(v).trim();

  // ---------------- carga de la API ----------------
  function cargar() {
    if (estado !== 'pendiente') return;
    estado = 'cargando';
    window.__respiraMapsListo = () => { estado = 'listo'; R().render(); };
    // Google llama a esto si la clave no vale o no tiene la API activada
    window.gm_authFailure = () => {
      estado = 'error';
      console.warn('[Respira] Google Maps rechazó la clave: se usa el mapa SVG.');
      mapa = null;
      R().render();
    };
    const s = document.createElement('script');
    s.src = 'https://maps.googleapis.com/maps/api/js?key=' + encodeURIComponent(CLAVE)
      + '&v=weekly&language=es&region=ES&loading=async&callback=__respiraMapsListo';
    s.async = true;
    s.onerror = () => { estado = 'error'; R().render(); };
    document.head.appendChild(s);
  }

  // ---------------- creación del mapa ----------------
  function crear() {
    const opciones = {
      center: CENTRO,
      zoom: zoomInicial(),
      minZoom: 12,
      gestureHandling: 'greedy',
      clickableIcons: false,
      // Como en el visual: en pantallas estrechas no caben Mapa/Satélite ni pantalla completa
      mapTypeControl: window.innerWidth >= 700,
      mapTypeControlOptions: { position: google.maps.ControlPosition.TOP_RIGHT },
      streetViewControl: false, // sin Street View (decisión de diseño)
      fullscreenControl: window.innerWidth >= 700,
      zoomControl: true,
    };
    if (MAP_ID) opciones.mapId = MAP_ID; // mapa vectorial: permite inclinar y girar (3D)
    mapa = new google.maps.Map(contenedor, opciones);
    mapa.addListener('click', () => { if (R().S.sel) { R().S.sel = null; R().render(); } });
    definirEtiqueta();
  }

  // Etiqueta HTML anclada a un punto del mapa (no necesita mapId)
  function definirEtiqueta() {
    Etiqueta = class extends google.maps.OverlayView {
      constructor(pos, html, clase, alPulsar, titulo) {
        super();
        this.pos = new google.maps.LatLng(pos.lat, pos.lng);
        this.div = document.createElement(alPulsar ? 'button' : 'div');
        this.div.className = clase;
        this.div.innerHTML = html;
        if (titulo) this.div.title = titulo;
        if (alPulsar) {
          this.div.type = 'button';
          this.div.addEventListener('click', e => { e.stopPropagation(); alPulsar(); });
        }
      }
      onAdd() {
        this.getPanes().overlayMouseTarget.appendChild(this.div);
        google.maps.OverlayView.preventMapHitsAndGesturesFrom(this.div);
      }
      draw() {
        const p = this.getProjection().fromLatLngToDivPixel(this.pos);
        if (p) { this.div.style.left = p.x + 'px'; this.div.style.top = p.y + 'px'; }
      }
      onRemove() { this.div.remove(); }
    };
  }

  // ---------------- geometría de las zonas ----------------
  const cacheContornos = {};
  function contorno(z) {
    const clave = z.id + (z.poligono ? ':api' : '');
    if (cacheContornos[clave]) return cacheContornos[clave];
    let puntos;
    if (z.poligono && z.poligono.length > 2) {
      puntos = z.poligono.map(p => ({ lat: p[0], lng: p[1] }));
    } else if (z.rect) {
      const [x, y, w, h] = z.rect;
      puntos = [geo(x, y), geo(x + w, y), geo(x + w, y + h), geo(x, y + h)];
    } else {
      // Se muestrea el path SVG del prototipo (getPointAtLength) y se pasa a lat/lng
      const ns = 'http://www.w3.org/2000/svg';
      const svg = document.createElementNS(ns, 'svg');
      svg.setAttribute('width', '0'); svg.setAttribute('height', '0');
      svg.style.position = 'absolute';
      const path = document.createElementNS(ns, 'path');
      path.setAttribute('d', z.path);
      svg.appendChild(path); document.body.appendChild(svg);
      const L = path.getTotalLength(), N = 48;
      puntos = [];
      for (let i = 0; i < N; i++) { const p = path.getPointAtLength(L * i / N); puntos.push(geo(p.x, p.y)); }
      svg.remove();
    }
    return (cacheContornos[clave] = puntos);
  }

  // ---------------- dibujo ----------------
  function limpiar() {
    capas.forEach(c => c.setMap(null));
    capas = [];
  }

  function colorNivel(l) {
    return css(l === 'none' ? '--lv-none' : '--lv-' + l) || '#9AA9A2';
  }

  function dibujarZonas() {
    const { S, ZONAS, MAG, nivel, est, fmt, hace, esc, zonaMapa } = R();
    const ordenadas = ZONAS.filter(z => z.tipo === 'barrio').concat(ZONAS.filter(z => z.tipo === 'parque'));
    ordenadas.forEach(z => {
      const parque = z.tipo === 'parque';
      const e = est(z);
      const zm = zonaMapa(z);       // estado general o la magnitud elegida
      const l = zm.l;
      const col = colorNivel(l);
      const sinDatos = l === 'none', antiguo = e === 'old';
      const discontinuo = sinDatos || antiguo || !parque;
      const pol = new google.maps.Polygon({
        paths: contorno(z),
        fillColor: col,
        fillOpacity: parque ? (sinDatos ? .22 : antiguo ? .25 : .42) : (sinDatos ? .10 : .13),
        strokeOpacity: discontinuo ? 0 : 1,       // los discontinuos se pintan con iconos
        strokeColor: col,
        strokeWeight: parque ? 3 : 1.5,
        zIndex: parque ? 2 : 1,
        clickable: true,
        map: mapa,
      });
      pol.addListener('click', () => { S.sel = z.id; R().render(); });
      capas.push(pol);

      if (discontinuo) { // borde discontinuo (sin datos / antiguo / barrio)
        const ring = contorno(z).concat([contorno(z)[0]]);
        const linea = new google.maps.Polyline({
          path: ring, strokeOpacity: 0, clickable: false, zIndex: parque ? 3 : 1, map: mapa,
          icons: [{ icon: { path: 'M 0,-1 0,1', strokeOpacity: parque ? .95 : .5, strokeColor: antiguo ? css('--ink-2') : col, scale: parque ? 3 : 2 }, offset: '0', repeat: parque ? '12px' : '10px' }],
        });
        capas.push(linea);
      }

      const pos = parque ? geo(z.x, z.y) : geo(z.lx + 40, z.ly);
      const valor = zm.valor;
      const html = `<span class="v${sinDatos ? ' none' : ''}" style="${sinDatos ? '' : 'background:' + col}">${valor}</span>${esc(z.n)}${antiguo ? '<span class="reloj" title="Dato antiguo">🕒</span>' : ''}`;
      const titulo = `${z.n}: ${zm.texto} · ${hace(z.hace)}`;
      const et = new Etiqueta(pos, html, 'gm-etq' + (parque ? '' : ' barrio') + (S.sel === z.id ? ' sel' : ''),
        () => { S.sel = z.id; R().render(); }, titulo);
      et.div.setAttribute('aria-label', titulo);
      et.setMap(mapa);
      capas.push(et);
    });
  }

  function dibujarPercepciones() {
    const { AVISOS, PERC, avOk, hace } = R();
    AVISOS.filter(avOk).forEach(a => {
      const col = colorNivel(['', 'bajo', 'medio', 'alto'][a.niv]);
      const letra = a.n ? a.n : { ruido: 'R', temp: 'T', aire: 'A' }[a.tipo];
      const html = letra;
      const titulo = `${PERC[a.tipo].n}: ${PERC[a.tipo].lv[a.niv - 1]}${a.n ? ' · ' + a.n + ' avisos' : ''} · ${a.dir} · ${hace(a.min)}`;
      const et = new Etiqueta(geo(a.x, a.y), html, 'gm-av' + (a.n ? ' grupo' : ''), null, titulo);
      if (!a.n) et.div.style.background = col;
      et.setMap(mapa);
      capas.push(et);
    });
  }

  function dibujar() {
    const { S } = R();
    const tema = document.documentElement.getAttribute('data-tema') || '';
    const datos = window.RespiraDatos ? window.RespiraDatos.version : 0;   // se repinta al llegar datos nuevos
    const firma = [S.layer, S.mag, S.sel, S.avt, S.avf, tema, datos].join('|');
    if (firma !== ultimaFirma) {
      ultimaFirma = firma;
      limpiar();
      if (S.layer === 'comunidad') dibujarPercepciones(); else dibujarZonas();
    }
    // Si se elige una zona desde la lista, el mapa va hacia ella
    if (S.sel && S.sel !== ultimaSel) {
      const z = R().byZ(S.sel);
      if (z) mapa.panTo(z.tipo === 'parque' ? geo(z.x, z.y) : geo(z.lx + 40, z.ly));
    }
    ultimaSel = S.sel;
  }

  // ---------------- API pública para app.js ----------------
  window.RespiraMapa = {
    activo: () => estado !== 'off' && estado !== 'error',

    slotHTML: () => '<div class="gmap-slot"></div>',

    // Solo «vista inicial» y «3D»: el zoom lo ponen los controles de Google
    controlesHTML: () => `<div class="mapctl"><button aria-label="Vista inicial" data-act="zoom" data-arg="reset">⟲</button>${MAP_ID ? `<button class="t3" aria-label="Vista en perspectiva" aria-pressed="${!!(mapa && mapa.getTilt && mapa.getTilt() > 0)}" data-act="tilt">3D</button>` : ''}</div>`,

    montar() {
      const slot = document.querySelector('.gmap-slot');
      if (!slot) return;
      if (estado === 'pendiente' || estado === 'cargando') {
        slot.innerHTML = '<div class="gmap-cargando"><span class="spinner"></span>Cargando Google Maps…</div>';
        cargar();
        return;
      }
      if (estado !== 'listo') return;
      if (!mapa) crear();
      slot.appendChild(contenedor);
      dibujar();
    },

    zoom(a) {
      if (!mapa) return;
      if (a === 'reset') {
        R().S.sel = null;
        mapa.moveCamera({ center: CENTRO, zoom: zoomInicial(), tilt: 0, heading: 0 });
        R().render();
      } else {
        mapa.setZoom(mapa.getZoom() + (a === 'in' ? 1 : -1));
      }
    },

    // Vista en perspectiva (requiere MAP_ID de un mapa vectorial)
    perspectiva() {
      if (!mapa || !MAP_ID) return;
      const inclinado = (mapa.getTilt() || 0) > 0;
      mapa.moveCamera({ tilt: inclinado ? 0 : 60, heading: inclinado ? 0 : -25, zoom: Math.max(mapa.getZoom(), 16.5) });
      R().render();
    },
  };
})();
