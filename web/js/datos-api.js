/* =============================================================
   datos-api.js · La web con los datos REALES de la API de Respira
   -------------------------------------------------------------
   Dirección de la API: js/config.js (RESPIRA_CONFIG.API_URL), p. ej.
     https://TU_DOMINIO/respira/api/index.php

   La API sirve zonas, series y percepciones, además de los datos municipales.

   Panel municipal (#acceso y #panel-…): con API_URL, TODO va al
   servidor (no hay modo demo): inicio de sesión con token (en
   sessionStorage: se borra al cerrar el navegador), sensores,
   préstamos, puntos, regalos, canjes y trabajadores. app.js valida
   los formularios y, al enviarlos, llama a RespiraDatos.accion().
   ============================================================= */
(function () {
  'use strict';

  const URL_API = window.Respira.urlApi;
  const CADA_MS = 60000;
  const CLAVE_SESION = 'respira-sesion';
  const R = () => window.Respira;
  const E = () => R().estado;          // getters/setters de los datos de app.js

  const D = { version: 0, enlaces: Object.create(null) };
  window.RespiraDatos = D;
  D.conApi = () => !!URL_API;

  // ---------------- repintar sin molestar ----------------
  let firmaPintada = '';
  function repintar(forzar) {
    const r = R();
    if (!r) return;
    const a = document.activeElement;
    if (!forzar && a && /^(INPUT|TEXTAREA)$/.test(a.tagName)) return;   // no borrar lo que se escribe
    if (!forzar && r.S.modal) return;
    const firma = D.version;
    if (!forzar && firma === firmaPintada) return;
    firmaPintada = firma;
    r.render(!forzar);
  }

  // ---------------- peticiones ----------------
  let token = null;
  try { token = sessionStorage.getItem(CLAVE_SESION); } catch (_) { /* sin almacenamiento: sin sesión */ }

  class ErrorApi extends Error {
    constructor(codigo, j) {
      super((j && j.error) || (codigo === 0 ? 'Sin conexión con el servidor de Respira. Inténtalo de nuevo.' : 'Error del servidor (' + codigo + ')'));
      this.codigo = codigo;
      this.errores = (j && j.errores) || {};
      if (j && j.campo) this.errores[j.campo] = j.error;
    }
  }

  async function pedir(ruta, metodo = 'GET', cuerpo, publico = false) {
    const tokenPeticion = publico ? null : token;
    let res;
    try {
      const h = { Accept: 'application/json' };
      if (cuerpo !== undefined) h['Content-Type'] = 'application/json';
      if (tokenPeticion) h['X-Respira-Token'] = tokenPeticion;
      res = await fetch(URL_API + ruta, { method: metodo, cache: 'no-store', headers: h,
        signal: AbortSignal.timeout(15000),
        body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo) });
    } catch (e) {
      throw new ErrorApi(0, null);
    }
    let j = null;
    try { j = await res.json(); } catch (_) { /* sin cuerpo JSON */ }
    if (!res.ok) {
      if (res.status === 401 && tokenPeticion && token === tokenPeticion && !/^\/sesiones/.test(ruta)) sesionCaducada();
      throw new ErrorApi(res.status, j);
    }
    return j;
  }

  // ---------------- formatos ----------------
  const MES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
  const MES_LARGO = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
  const dosC = n => String(n).padStart(2, '0');
  const fechaHora = iso => { if (!iso) return null; const d = new Date(iso); return d.getDate() + ' ' + MES[d.getMonth()] + ' ' + dosC(d.getHours()) + ':' + dosC(d.getMinutes()); };
  const fechaDia = f => { if (!f) return ''; const d = new Date(String(f).length <= 10 ? f + 'T12:00' : f); return d.getDate() + ' ' + MES[d.getMonth()]; };

  // ==============================================================
  // DATOS PÚBLICOS
  // ==============================================================

  let idsPuntos = {};
  const fuentes = {
    zonas: { ruta: '/zonas', aplicar: zonas => R().actualizarZonas(zonas) },
    percepciones: { ruta: '/percepciones', aplicar: opiniones => R().actualizarPercepciones(opiniones) },
    ayuntamientos: { ruta: '/ayuntamientos', aplicar: aytos => {
      const { AYTOS } = E();
      AYTOS.splice(0, AYTOS.length, ...aytos.map(a => {
      const d = new Date(a.desde + 'T12:00');
      return { id: a.id, n: a.nombre, zona: a.comarca, desde: MES_LARGO[d.getMonth()] + ' ' + d.getFullYear(), activo: a.activo };
      }));
    } },
    puntos: { ruta: '/puntos-reparto', aplicar: puntos => {
      const { PUNTOS_REPARTO } = E();
      PUNTOS_REPARTO.splice(0, PUNTOS_REPARTO.length, ...puntos.map(p => [p.nombre, p.direccion, p.horario]));
      idsPuntos = Object.fromEntries(puntos.map(p => [p.nombre, p.id]));
    } },
    regalos: { ruta: '/regalos', aplicar: regalos => {
      const { REGALOS_PUBLICOS } = E();
      REGALOS_PUBLICOS.splice(0, REGALOS_PUBLICOS.length, ...regalos.map(aRegalo));
    } },
  };
  D.estadoPublico = nombre => fuentes[nombre];
  D.ayuntamientosVerificados = () => Boolean(fuentes.ayuntamientos.actualizado);
  D.repartoVerificado = () => Boolean(fuentes.puntos.actualizado);

  function cargarFuente(nombre) {
    const fuente = fuentes[nombre];
    if (fuente.pendiente) return fuente.pendiente;
    fuente.pendiente = pedir(fuente.ruta, 'GET', undefined, true).then(datos => {
      if (!Array.isArray(datos)) throw new Error('Respuesta no válida');
      fuente.aplicar(datos);
      fuente.actualizado = Date.now();
      fuente.error = false;
    }).catch(error => {
      fuente.error = true;
      throw error;
    }).finally(() => {
      fuente.pendiente = null;
      D.version++;
      repintar();
    });
    return fuente.pendiente;
  }

  async function cargarPuntos() {
    if (!fuentes.puntos.actualizado || fuentes.puntos.error) await cargarFuente('puntos');
  }

  const series = new Map();
  D.serie = (zona, magnitud, periodo, rango = {}) => {
    const params = new URLSearchParams({ magnitud, periodo: periodo === 'custom' ? 'rango' : periodo });
    if (periodo === 'custom') {
      params.set('desde', rango.desde);
      params.set('hasta', rango.hasta);
    } else if (rango.referencia) params.set('referencia', rango.referencia);
    const ruta = '/zonas/' + encodeURIComponent(zona) + '/serie?' + params;
    if (series.has(ruta)) return series.get(ruta);
    const serie = { estado: 'cargando', datos: null };
    series.set(ruta, serie);
    if (series.size > 40) series.delete(series.keys().next().value);
    pedir(ruta, 'GET', undefined, true).then(datos => {
      serie.datos = window.RespiraSeries.desdeApi(datos);
      serie.estado = 'listo';
    }).catch(error => {
      serie.estado = 'error';
      serie.mensaje = error instanceof ErrorApi ? error.message : 'El servidor devolvió una serie no válida.';
    }).finally(() => {
      if (series.get(ruta) !== serie) return;
      D.version++;
      repintar();
    });
    return serie;
  };

  D.reintentarPublico = nombre => {
    if (nombre === 'serie') {
      series.clear();
      repintar(true);
    } else if (fuentes[nombre]) cargarFuente(nombre).catch(() => {});
  };

  // ==============================================================
  // PANEL MUNICIPAL
  // ==============================================================

  const PANEL = ['admin', 'tecnico', 'reparto'];
  const DATOS_VISTA = {
    panel: ['SENS', 'LOANS'],
    'panel-sensores': ['SENS'],
    'panel-editar': ['SENS'],
    'panel-prestamos': ['SENS', 'LOANS', 'APPUSERS', 'PUNTOS_REPARTO'],
    'panel-puntos': ['RULES'],
    'panel-regalos': ['GIFTS', 'CANJES'],
    'panel-trabajadores': ['STAFF'],
  };
  let datosPanel = {};
  let ultimaCarga = 0;
  D.estadoPanel = (ruta, rol) => {
    if (!URL_API) return null;
    const datos = ruta === 'panel-regalos' && rol === 'reparto' ? [] : DATOS_VISTA[ruta];
    return {
      listo: datos.every(k => datosPanel[k]?.listo),
      error: datos.some(k => datosPanel[k]?.error),
    };
  };

  function reiniciarPanel() {
    datosPanel = {};
    const estado = E();
    for (const clave of ['SENS', 'LOANS', 'APPUSERS', 'GIFTS', 'CANJES', 'STAFF']) estado[clave] = [];
    const S = R().S;
    S.canje = null; S.edit = null; S.altaOk = null;
  }

  function guardarToken(t) {
    token = t;
    try { t ? sessionStorage.setItem(CLAVE_SESION, t) : sessionStorage.removeItem(CLAVE_SESION); } catch (_) { /* nada */ }
  }

  function aUsuarioWeb(u) {
    return { n: u.nombre, e: u.email, r: u.rol, id: u.id, punto: u.punto ? u.punto.nombre : null };
  }

  function sesionCaducada() {
    guardarToken(null);
    const r = R();
    if (r && r.S.user) {
      reiniciarPanel();
      r.S.user = null;
      r.toast('Tu sesión ha caducado. Vuelve a entrar.');
      r.nav('acceso');
    }
  }

  // ---------- traducir los datos del servidor a los de app.js ----------
  // tipo de sensor: la API usa palabras (voluntariado, trabajador, bici, autobus); app.js, códigos cortos
  const TIPO_WEB = { voluntariado: 'vol', trabajador: 'trab', bici: 'bici', autobus: 'bus' };
  const TIPO_API = Object.fromEntries(Object.entries(TIPO_WEB).map(([a, w]) => [w, a]));
  const aSensor = n => ({ code: n.codigo, tipo: TIPO_WEB[n.tipo] || n.tipo, mags: n.magnitudes, est: n.estado, asig: n.asignado_a || '',
    user: n.prestamo ? n.prestamo.usuario : null, desde: n.prestamo ? fechaHora(n.prestamo.desde) : null,
    ult: n.ultima_min, motivo: n.motivo_mant });
  // «No devuelto» = en curso; lim = fecha límite (10 días desde la recogida)
  const aPrestamo = p => ({ id: p.id, code: p.nodo, user: p.usuario || '(cuenta borrada)', ini: fechaHora(p.inicio),
    iniMs: Date.parse(p.inicio), lim: fechaHora(p.fecha_limite), limMs: Date.parse(p.fecha_limite),
    fin: p.estado_devolucion === 'No devuelto' ? null : fechaHora(p.fecha_limite), punto: p.punto || '—', estDev: p.estado_devolucion });
  const aTrabajador = t => ({ n: t.nombre, e: t.email, p: null, r: 'trabajador', id: t.id, emp: t.num_empleado,
    serv: t.servicio, estado: 'activo', alta: fechaDia(t.alta) });
  const aRegalo = g => ({ id: g.id, n: g.nombre, c: g.coste, stock: g.stock });
  const aCanje = c => ({ code: c.codigo, user: c.usuario || '(cuenta borrada)', gift: c.regalo_id, giftN: c.regalo,
    cad: c.caduca, estado: c.estado });

  // Carga lo que necesita el panel según el rol.
  async function cargarPanel() {
    const S = R().S;
    const usuario = S.user;
    if (!usuario || !PANEL.includes(usuario.r)) return;
    const carga = ++ultimaCarga;
    const consultas = [
      ['SENS', pedir('/panel/nodos').then(l => l.map(aSensor))],
      ['LOANS', pedir('/panel/prestamos').then(p => p.abiertos.concat(p.cerrados).map(aPrestamo))],
      ['APPUSERS', pedir('/panel/vecinos').then(l => l.map(u => ({ id: u.id, n: u.nombre })))],
      ['PUNTOS_REPARTO', cargarPuntos()],
    ];
    if (usuario.r === 'admin') consultas.push(
      ['GIFTS', pedir('/regalos').then(l => l.map(aRegalo))],
      ['RULES', pedir('/panel/reglas').then(r => {
        if (![r?.min, r?.mult, r?.max].every(Number.isFinite)) throw new Error('Reglas no disponibles.');
        return r;
      })],
      ['CANJES', pedir('/panel/canjes').then(l => l.map(aCanje))],
      ['STAFF', pedir('/panel/trabajadores').then(l => [{ ...usuario, p: null }, ...l.map(aTrabajador)])],
    );
    const resultados = await Promise.allSettled(consultas.map(([, peticion]) => peticion));
    if (S.user !== usuario || carga !== ultimaCarga) return;
    const estado = E();
    resultados.forEach((resultado, i) => {
      const clave = consultas[i][0];
      const anterior = datosPanel[clave];
      if (resultado.status === 'rejected') {
        datosPanel[clave] = { listo: Boolean(anterior?.listo), error: true };
        console.warn('[Respira] Panel:', resultado.reason?.message);
        return;
      }
      if (clave === 'RULES') {
        const { min, mult, max } = resultado.value;   // fijas en el servidor: solo se enseñan
        estado.RULES = { min, mult, max };
      } else if (clave !== 'PUNTOS_REPARTO') estado[clave] = resultado.value;
      datosPanel[clave] = { listo: true, error: false };
    });
    D.version++;
  }
  D.cargarPanel = cargarPanel;
  D.reintentarPanel = async () => { await cargarPanel(); repintar(true); };

  // Al abrir la web con una sesión guardada (recargar la página del panel)
  async function restaurarSesion() {
    const guardado = token;
    if (!guardado) return;
    try {
      const j = await pedir('/yo');
      if (token !== guardado) return;
      if (!PANEL.includes(j.usuario.rol)) { guardarToken(null); return; }
      reiniciarPanel();
      const usuario = aUsuarioWeb(j.usuario);
      R().S.user = usuario;
      await cargarPanel();
      if (R().S.user !== usuario) return;
      const inicial = (window.__respiraRutaInicial || '').slice(1);
      if (inicial && inicial.startsWith('panel') && location.hash !== '#' + inicial) location.hash = inicial;
      else R().render();
    } catch (e) {
      if (e.codigo !== 401) console.warn('[Respira] No se pudo recuperar la sesión:', e.message);
    }
  }

  D.cerrarSesion = () => {
    if (token) pedir('/sesiones', 'DELETE').catch(() => { /* la sesión caduca sola */ });
    guardarToken(null);
    if (URL_API) reiniciarPanel();
  };

  // undefined = cargando, null = enlace no válido, false = comprobación fallida.
  D.enlace = tok => {
    if (!tok) return null;
    if (!(tok in D.enlaces)) {
      D.enlaces[tok] = undefined;
      pedir('/enlaces/' + encodeURIComponent(tok)).then(v => {
        D.enlaces[tok] = v.estado === 'no_valido' ? null : { n: v.nombre, e: v.email, id: v.id, rol: v.rol, tipo: v.tipo,
          caduca: v.estado === 'caducado' ? 0 : Date.now() + 36e5, token: tok, horas: v.horas };
      }).catch(() => { D.enlaces[tok] = false; }).finally(() => { D.version++; repintar(true); });
    }
    return D.enlaces[tok];
  };
  D.reintentarEnlace = tok => {
    if (D.enlaces[tok] !== false) return;
    delete D.enlaces[tok];
    repintar(true);
  };

  // ---------- errores del servidor --> campos del formulario ----------
  const CAMPOS = {
    prestar: { usuario: 'puser', codigo: 'pcode', punto: 'ppunto' }, devolver: { codigo: 'dcode' },
    sensor: { codigo: 'code', magnitudes: 'mags', asignado_a: 'asig', motivo: 'motivo' },
    baja: { motivo: 'bmot' },
    regalo: { nombre: 'gn', coste: 'gc', stock: 'gs' }, comprobarCanje: { codigo: 'ccode' }, entregar: { codigo: 'ccode' },
    trabajador: { nombre: 'tn', email: 'te', num_empleado: 'temp', servicio: 'ts', clave: 'tp1' }, activar: { clave: 'ap1' },
    recuperar: { email: 'rmail' },
  };
  function mostrarError(accion, e, porDefecto) {
    const S = R().S;
    const m = CAMPOS[accion] || {};
    const er = {};
    const sinCampo = [];
    for (const [clave, mensaje] of Object.entries(e.errores || {})) {
      if (typeof mensaje !== 'string' || !mensaje.trim()) continue;
      if (Object.hasOwn(m, clave)) er[m[clave]] = mensaje;
      else sinCampo.push(mensaje);
    }
    const hayCampos = Object.keys(er).length > 0;
    if (accion === 'trabajador') {
      if (sinCampo.length) er.altaTrabajador = sinCampo.join(' ');
      else if (!hayCampos) er.altaTrabajador = e.message;
    } else if (!hayCampos) {
      if (porDefecto) er[porDefecto] = e.message;
      else R().toast(sinCampo.length ? sinCampo.join(' ') : e.message);
    }
    S.e = er;
    if (hayCampos || er[porDefecto]) S.focusErr = true;
    R().render();
    if (er.altaTrabajador && !hayCampos) document.getElementById('alta-trabajador-error')?.focus();
  }

  // ==============================================================
  // Acciones del panel (las llama app.js después de validar)
  // ==============================================================
  let altaTrabajadorPendiente = null;
  D.altaTrabajadorEnCurso = () => altaTrabajadorPendiente !== null && altaTrabajadorPendiente === R().S.user;

  const ACC = {
    async login({ email, clave }) {
      const S = R().S;
      try {
        const j = await pedir('/sesiones', 'POST', { email, clave, cliente: 'web' });
        guardarToken(j.token);
        reiniciarPanel();
        const usuario = aUsuarioWeb(j.usuario);
        S.user = usuario; S.f = {}; S.e = {};
        await cargarPanel();
        if (S.user !== usuario) return;
        R().toast('Hola, ' + S.user.n.split(' ')[0]);
        R().nav(R().HOME[S.user.r]);
      } catch (e) {
        S.f.pass = '';
        S.e = { login: e.codigo === 401 ? 'Correo o contraseña incorrectos.' : R().esc(e.message) };
        R().render();
      }
    },

    async recuperar({ email }) {
      try {
        await pedir('/recuperaciones', 'POST', { email });
        R().S.recOk = true; R().render();
      } catch (e) { mostrarError('recuperar', e, 'rmail'); }
    },

    async activar({ token: tok, clave }) {
      const S = R().S;
      try {
        const v = D.enlaces[tok];
        const j = await pedir('/enlaces/' + encodeURIComponent(tok), 'POST', { clave });
        S.activOk = j.email; S.activTipo = v && v.tipo; S.activRol = v && v.rol;
        S.f.ap1 = ''; S.f.ap2 = ''; delete D.enlaces[tok];
        R().render();
      } catch (e) { S.f.ap1 = ''; S.f.ap2 = ''; mostrarError('activar', e, 'ap1'); }
    },

    async sensor({ orig, codigo, tipo, mags, asig, mant, motivo }) {
      const S = R().S;
      const cuerpo = { codigo, tipo: TIPO_API[tipo] || tipo, magnitudes: mags, asignado_a: asig || '', mantenimiento: mant, motivo: motivo || '' };
      try {
        const n = orig ? await pedir('/panel/nodos/' + encodeURIComponent(orig), 'PUT', cuerpo)
          : await pedir('/panel/nodos', 'POST', cuerpo);
        await cargarPanel();
        R().toast(orig ? `${n.codigo} guardado` : `${n.codigo} dado de alta${n.tipo === 'voluntariado' ? ': ya se puede prestar' : ''}`);
        S.edit = null; R().nav('panel-sensores');
      } catch (e) {
        if (e.errores && e.errores.tipo) R().toast(e.errores.tipo);
        mostrarError('sensor', e);
      }
    },

    async baja({ codigo, motivo }) {
      const S = R().S;
      try {
        await pedir('/panel/nodos/' + encodeURIComponent(codigo), 'DELETE', { motivo });
        S.modal = null; await cargarPanel(); R().render();
        R().toast(`${codigo} dado de baja. Sus mediciones se conservan.`);
      } catch (e) { mostrarError('baja', e); }
    },

    async prestar({ usuario, codigo, punto }) {
      const S = R().S;
      try {
        await cargarPuntos();
        await pedir('/panel/prestamos', 'POST', { usuario, codigo, punto: idsPuntos[punto] ?? null });
        S.f.puser = ''; S.f.pcode = ''; await cargarPanel(); R().render();
        R().toast(`${codigo} prestado a ${usuario}. Ya puede escribir el código en su app.`);
      } catch (e) { mostrarError('prestar', e); }
    },

    async devolver({ codigo, estado }) {
      const S = R().S;
      try {
        await pedir('/panel/devoluciones', 'POST', { codigo, estado });
        S.f.dcode = ''; S.f.dest = 'Correcto'; await cargarPanel(); R().render();
        R().toast(estado === 'Defectuoso' ? `${codigo} devuelto y en mantenimiento` : `${codigo} devuelto`);
      } catch (e) { mostrarError('devolver', e, 'dcode'); }
    },

    async regalo({ id, nombre, coste, stock }) {
      const S = R().S;
      try {
        if (id) await pedir('/panel/regalos/' + encodeURIComponent(id), 'PUT', { nombre, coste, stock });
        else await pedir('/panel/regalos', 'POST', { nombre, coste, stock });
        S.modal = null; await cargarPanel(); R().render(); R().toast('Catálogo actualizado');
      } catch (e) { mostrarError('regalo', e); }
    },

    async retirar({ id }) {
      try {
        await pedir('/panel/regalos/' + encodeURIComponent(id), 'DELETE');
        await cargarPanel(); R().render(); R().toast('Regalo retirado del catálogo');
      } catch (e) { R().toast(e.message); }
    },

    async comprobarCanje({ codigo }) {
      const S = R().S;
      const usuario = S.user;
      const vigente = () => S.user === usuario && location.hash === '#panel-regalos' && S.f.ccode?.trim().toUpperCase() === codigo;
      try {
        const c = await pedir('/panel/canjes/' + encodeURIComponent(codigo));
        if (!vigente()) return;
        S.canje = aCanje(c); S.e = {}; R().render();
      } catch (e) {
        if (!vigente()) return;
        S.canje = null; mostrarError('comprobarCanje', e, 'ccode');
      }
    },

    async entregar({ codigo }) {
      const S = R().S;
      const usuario = S.user;
      try {
        const c = await pedir('/panel/canjes/' + encodeURIComponent(codigo) + '/entrega', 'POST', {});
        if (S.user !== usuario) return;
        if (S.f.ccode?.trim().toUpperCase() === codigo) {
          S.canje = null; S.f.ccode = '';
        }
        await cargarPanel();
        if (S.user !== usuario) return;
        R().render();
        R().toast(c.stock === 0 ? `Entregado. «${c.regalo}» queda agotado en la app.` : 'Regalo entregado');
      } catch (e) {
        if (S.user !== usuario) return;
        if (location.hash === '#panel-regalos' && S.f.ccode?.trim().toUpperCase() === codigo) mostrarError('entregar', e, 'ccode');
        else R().toast(e.message);
      }
    },

    async trabajador({ nombre, email, num_empleado, servicio, clave }) {
      const S = R().S;
      const usuario = S.user;
      if (usuario?.r !== 'admin' || D.altaTrabajadorEnCurso()) return;
      altaTrabajadorPendiente = usuario;
      R().render();
      let respuesta, error;
      try {
        respuesta = await pedir('/panel/trabajadores', 'POST', { nombre, email, num_empleado, servicio, clave });
        if (S.user !== usuario) return;
        await cargarPanel();
      } catch (e) {
        error = e;
      } finally {
        if (altaTrabajadorPendiente === usuario) altaTrabajadorPendiente = null;
      }
      if (S.user !== usuario) return;
      const enFormulario = location.hash === '#panel-trabajadores';
      S.f.tp1 = ''; S.f.tp2 = '';
      if (error) {
        if (enFormulario) mostrarError('trabajador', error);
        else R().toast(error.message);
        return;
      }
      const t = respuesta.trabajador;
      S.f.tn = ''; S.f.te = ''; S.f.temp = ''; S.f.ts = '';
      if (enFormulario) {
        S.altaOk = { id: t.id, nombre: t.nombre, email: t.email };
        R().render();
      }
      R().toast(`Trabajador ${t.id} dado de alta`);
    },

  };

  D.accion = (nombre, datos) => {
    if (!ACC[nombre]) { console.warn('[Respira] Acción desconocida:', nombre); return; }
    ACC[nombre](datos || {});
  };

  // ==============================================================
  // Ciclo
  // ==============================================================
  async function ciclo() {
    const fijos = await Promise.allSettled(Object.keys(fuentes).map(cargarFuente));
    fijos.filter(x => x.status === 'rejected').forEach(x => console.warn('[Respira] Datos públicos:', x.reason?.message));
    const S = R().S;
    if (S.user && /^panel/.test(location.hash.slice(1)) && !S.modal) await cargarPanel();
    repintar();
  }

  if (URL_API) {
    restaurarSesion();
    ciclo();
    setInterval(() => { if (!document.hidden) ciclo(); }, CADA_MS);
    window.addEventListener('hashchange', () => {
      if (location.hash === '#comunidad') cargarFuente('percepciones').catch(() => {});
    });
    document.addEventListener('visibilitychange', () => { if (!document.hidden) ciclo(); });
    window.addEventListener('focusout', () => setTimeout(() => repintar(), 0));
  }
  repintar(true);
})();
