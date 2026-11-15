/* =============================================================
   datos-api.js · La web con los datos REALES de la API de Respira
   -------------------------------------------------------------
   Dirección de la API: js/config.js (RESPIRA_CONFIG.API_URL), p. ej.
     https://TU_DOMINIO/respira/api/index.php

   La API carga ayuntamientos y puntos de reparto. La vista pública del
   Grau i Platja usa un escenario local de mediciones simuladas.

   Panel municipal (#acceso y #panel-…): con API_URL, TODO va al
   servidor (no hay modo demo): inicio de sesión con token (en
   sessionStorage: se borra al cerrar el navegador), sensores,
   préstamos, puntos, regalos, canjes y trabajadores. app.js valida
   los formularios y, al enviarlos, llama a RespiraDatos.accion().
   ============================================================= */
(function () {
  'use strict';

  const CFG = window.RESPIRA_CONFIG || {};
  let URL_API = String(CFG.API_URL || '').trim().replace(/\/+$/, '');
  if (/TU_DOMINIO|pon_aqui/i.test(URL_API)) URL_API = '';
  const CADA_MS = 60000;
  const CLAVE_SESION = 'respira-sesion';
  const R = () => window.Respira;
  const E = () => R().estado;          // getters/setters de los datos de app.js

  const D = { version: 0, enlaces: {} };
  window.RespiraDatos = D;
  D.conApi = () => !!URL_API;

  // ---------------- repintar sin molestar ----------------
  let firmaPintada = '';
  function repintar(forzar) {
    const r = R();
    if (!r) return;
    const a = document.activeElement;
    if (!forzar && a && /^(INPUT|SELECT|TEXTAREA)$/.test(a.tagName)) return;   // no borrar lo que se escribe
    if (!forzar && r.S.modal) return;
    const firma = D.version;
    if (!forzar && firma === firmaPintada) return;
    firmaPintada = firma;
    r.render();
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
      this.quedan = j && j.quedan;
    }
  }

  async function pedir(ruta, metodo = 'GET', cuerpo) {
    let res;
    try {
      const h = { Accept: 'application/json' };
      if (cuerpo !== undefined) h['Content-Type'] = 'application/json';
      if (token) h['X-Respira-Token'] = token;
      res = await fetch(URL_API + ruta, { method: metodo, cache: 'no-store', headers: h,
        body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo) });
    } catch (e) {
      throw new ErrorApi(0, null);
    }
    let j = null;
    try { j = await res.json(); } catch (_) { /* sin cuerpo JSON */ }
    if (!res.ok) {
      if (res.status === 401 && token && !/^\/sesiones/.test(ruta)) sesionCaducada();
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

  let fijosCargados = false;
  let idsPuntos = {};
  async function cargarFijos() {
    if (fijosCargados) return;
    try {
      const [aytos, puntos] = await Promise.all([pedir('/ayuntamientos'), pedir('/puntos-reparto')]);
      const { AYTOS, PUNTOS_REPARTO } = E();
      AYTOS.splice(0, AYTOS.length, ...aytos.map(a => {
        const d = new Date(a.desde + 'T12:00');
        return { id: a.id, n: a.nombre, zona: a.comarca, desde: MES_LARGO[d.getMonth()] + ' ' + d.getFullYear(), activo: a.activo };
      }));
      PUNTOS_REPARTO.splice(0, PUNTOS_REPARTO.length, ...puntos.map(p => [p.nombre, p.direccion, p.horario]));
      idsPuntos = Object.fromEntries(puntos.map(p => [p.nombre, p.id]));   // nombre --> id (para prestar)
      fijosCargados = true;
      D.version++;
    } catch (e) {
      console.warn('[Respira] Ayuntamientos o puntos de reparto no disponibles:', e.message || e);
    }
  }

  // ==============================================================
  // PANEL MUNICIPAL
  // ==============================================================

  const PANEL = ['admin', 'tecnico', 'reparto'];

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
      r.S.user = null;
      r.toast('Tu sesión ha caducado. Vuelve a entrar.');
      r.nav('acceso');
    }
  }

  // ---------- traducir los datos del servidor a los de app.js ----------
  const aSensor = n => ({ code: n.codigo, tipo: n.tipo, mags: n.magnitudes, est: n.estado, asig: n.asignado_a || '',
    user: n.prestamo ? n.prestamo.usuario : null, desde: n.prestamo ? fechaHora(n.prestamo.desde) : null,
    ult: n.ultima_min, motivo: n.motivo_mant });
  // «No devuelto» = en curso; lim = fecha límite (10 días desde la recogida)
  const aPrestamo = p => ({ id: p.id, code: p.nodo, user: p.usuario || '(cuenta borrada)', ini: fechaHora(p.inicio),
    iniMs: Date.parse(p.inicio), lim: fechaHora(p.fecha_limite), limMs: Date.parse(p.fecha_limite),
    fin: p.estado_devolucion === 'No devuelto' ? null : fechaHora(p.fecha_limite), punto: p.punto || '—', estDev: p.estado_devolucion });
  const aTrabajador = t => ({ n: t.nombre, e: t.email, p: null, r: 'trabajador', id: t.id, emp: t.num_empleado,
    serv: t.servicio, estado: t.estado === 'activo' ? 'activo' : 'pendiente', alta: fechaDia(t.alta),
    caduca: t.estado === 'caducado' ? 0 : Date.now() + 48 * 36e5, token: null });
  const aRevision = d => ({ id: String(d.id), user: d.usuario, d: fechaDia(d.fecha), min: d.minutos, pts: d.puntos,
    mot: d.motivo, estado: d.estado, razon: d.razon });
  const aRegalo = g => ({ id: g.id, n: g.nombre, c: g.coste, stock: g.stock });
  const aCanje = c => ({ code: c.codigo, user: c.usuario || '(cuenta borrada)', gift: c.regalo_id, giftN: c.regalo,
    cad: c.caduca, estado: c.estado });

  // Carga lo que necesita el panel según el rol.
  // formularios = true: también rellena el formulario de reglas (no se pisa lo que se escribe)
  async function cargarPanel(formularios = false) {
    const S = R().S;
    if (!S.user || !PANEL.includes(S.user.r)) return;
    const rol = S.user.r;
    const t = [];
    t.push(pedir('/panel/nodos').then(l => { E().SENS = l.map(aSensor); }));
    t.push(pedir('/panel/prestamos').then(p => { E().LOANS = p.abiertos.concat(p.cerrados).map(aPrestamo); }));
    t.push(pedir('/panel/vecinos').then(l => { E().APPUSERS = l.map(u => ({ id: u.id, n: u.nombre })); }));
    t.push(pedir('/regalos').then(l => { E().GIFTS = l.map(aRegalo); }));
    if (rol === 'admin') {
      t.push(pedir('/panel/reglas').then(r => { E().RULES = { min: r.min, mult: r.mult, max: r.max };
        if (formularios) S.rules = { min: String(r.desde_manana ? r.desde_manana.min : r.min), mult: String(r.desde_manana ? r.desde_manana.mult : r.mult),
          max: String(r.desde_manana ? r.desde_manana.max : r.max) }; }));
      t.push(pedir('/panel/revisiones').then(l => { E().SOSP = l.map(aRevision); }));
      t.push(pedir('/panel/canjes').then(l => { E().CANJES = l.map(aCanje); }));
      t.push(pedir('/panel/trabajadores').then(l => {
        E().STAFF = [{ n: S.user.n, e: S.user.e, p: null, r: S.user.r }].concat(l.map(aTrabajador));
      }));
    } else {
      E().STAFF = [{ n: S.user.n, e: S.user.e, p: null, r: S.user.r }];
      E().SOSP = []; E().CANJES = [];
    }
    const res = await Promise.allSettled(t);
    res.filter(x => x.status === 'rejected').forEach(x => console.warn('[Respira] Panel:', x.reason && x.reason.message));
    D.version++;
  }
  D.cargarPanel = cargarPanel;

  // Al abrir la web con una sesión guardada (recargar la página del panel)
  async function restaurarSesion() {
    if (!token) return;
    try {
      const j = await pedir('/yo');
      if (!PANEL.includes(j.usuario.rol)) { guardarToken(null); return; }
      R().S.user = aUsuarioWeb(j.usuario);
      await cargarPanel(true);
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
  };

  // Enlace de activación o de contraseña nueva: undefined = cargando, null = no vale
  D.enlace = tok => {
    if (!tok) return null;
    if (!(tok in D.enlaces)) {
      D.enlaces[tok] = undefined;
      pedir('/enlaces/' + encodeURIComponent(tok)).then(v => {
        D.enlaces[tok] = v.estado === 'no_valido' ? null : { n: v.nombre, e: v.email, id: v.id, rol: v.rol, tipo: v.tipo,
          caduca: v.estado === 'caducado' ? 0 : Date.now() + 36e5, token: tok, horas: v.horas };
      }).catch(() => { D.enlaces[tok] = null; }).finally(() => { D.version++; repintar(true); });
    }
    return D.enlaces[tok];
  };

  // ---------- errores del servidor --> campos del formulario ----------
  const CAMPOS = {
    prestar: { usuario: 'puser', codigo: 'pcode', punto: 'ppunto' }, devolver: { codigo: 'dcode' },
    sensor: { codigo: 'code', magnitudes: 'mags', asignado_a: 'asig', motivo: 'motivo' },
    baja: { motivo: 'bmot' }, reglas: { min: 'rmin', mult: 'rmult', max: 'rmax' }, anular: { razon: 'amot' },
    regalo: { nombre: 'gn', coste: 'gc', stock: 'gs' }, comprobarCanje: { codigo: 'ccode' }, entregar: { codigo: 'ccode' },
    trabajador: { nombre: 'tn', email: 'te', num_empleado: 'temp', servicio: 'ts' }, activar: { clave: 'ap1' },
    recuperar: { email: 'rmail' },
  };
  function mostrarError(accion, e, porDefecto) {
    const S = R().S;
    const m = CAMPOS[accion] || {};
    const er = {};
    Object.keys(e.errores || {}).forEach(k => { if (m[k]) er[m[k]] = e.errores[k]; });
    if (!Object.keys(er).length) {
      if (porDefecto) er[porDefecto] = e.message;
      else R().toast(e.message);
    }
    S.e = er;
    if (Object.keys(er).length) S.focusErr = true;
    R().render();
  }

  // ==============================================================
  // Acciones del panel (las llama app.js después de validar)
  // ==============================================================
  const ACC = {
    async login({ email, clave }) {
      const S = R().S;
      try {
        const j = await pedir('/sesiones', 'POST', { email, clave, cliente: 'web' });
        guardarToken(j.token);
        S.user = aUsuarioWeb(j.usuario); S.tries = 0; S.f = {}; S.e = {};
        await cargarPanel(true);
        R().toast('Hola, ' + S.user.n.split(' ')[0]);
        R().nav(R().HOME[S.user.r]);
      } catch (e) {
        S.f.pass = '';
        if (e.codigo === 423) { S.lock = true; S.e = {}; }
        else S.e = { login: e.codigo === 401 ? '<b>Correo o contraseña incorrectos.</b>' + (e.quedan != null
          ? ` Te quedan ${e.quedan} intento${e.quedan === 1 ? '' : 's'} antes del bloqueo temporal.` : '') : R().esc(e.message) };
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
      const cuerpo = { codigo, tipo, magnitudes: mags, asignado_a: asig || '', mantenimiento: mant, motivo: motivo || '' };
      try {
        const n = orig ? await pedir('/panel/nodos/' + encodeURIComponent(orig), 'PUT', cuerpo)
          : await pedir('/panel/nodos', 'POST', cuerpo);
        await cargarPanel();
        R().toast(orig ? `${n.codigo} guardado` : `${n.codigo} dado de alta${n.tipo === 'vol' ? ': ya se puede prestar' : ''}`);
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
        await cargarFijos();
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

    async reglas({ min, mult, max }) {
      try {
        await pedir('/panel/reglas', 'PUT', { min, mult, max });
        await cargarPanel(true); E().RULES = { min: +min, mult: +mult, max: +max }; R().render();
        R().toast('Reglas guardadas: se aplican desde mañana');
      } catch (e) { mostrarError('reglas', e); }
    },

    async revisar({ id, decision, razon }) {
      const S = R().S;
      try {
        const x = await pedir('/panel/revisiones/' + encodeURIComponent(id), 'POST', { decision, razon });
        S.modal = null; await cargarPanel(); R().render();
        R().toast(decision === 'aprobar' ? 'Día aprobado: los puntos se mantienen' : `Puntos anulados: se restan ${x.puntos} a ${x.usuario}`);
      } catch (e) { mostrarError('anular', e); }
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
      try {
        const c = await pedir('/panel/canjes/' + encodeURIComponent(codigo));
        S.canje = aCanje(c); S.e = {}; R().render();
      } catch (e) { S.canje = null; mostrarError('comprobarCanje', e, 'ccode'); }
    },

    async entregar({ codigo }) {
      const S = R().S;
      try {
        const c = await pedir('/panel/canjes/' + encodeURIComponent(codigo) + '/entrega', 'POST', {});
        S.canje = null; S.f.ccode = ''; await cargarPanel(); R().render();
        R().toast(c.stock === 0 ? `Entregado. «${c.regalo}» queda agotado en la app.` : 'Regalo entregado');
      } catch (e) { mostrarError('entregar', e, 'ccode'); }
    },

    async trabajador({ nombre, email, num_empleado, servicio }) {
      const S = R().S;
      try {
        const j = await pedir('/panel/trabajadores', 'POST', { nombre, email, num_empleado, servicio });
        await cargarPanel();
        const t = E().STAFF.find(x => x.id === j.trabajador.id);
        if (t) t.token = j.enlace.split('#activar/')[1];
        S.altaOk = j.trabajador.id; S.f.tn = ''; S.f.te = ''; S.f.temp = ''; S.f.ts = '';
        R().render(); R().toast(`Trabajador ${j.trabajador.id} dado de alta`);
      } catch (e) { mostrarError('trabajador', e); }
    },

    async reenviar({ id }) {
      const S = R().S;
      try {
        const j = await pedir('/panel/trabajadores/' + encodeURIComponent(id) + '/enlace', 'POST', {});
        await cargarPanel();
        const t = E().STAFF.find(x => x.id === id);
        if (t) t.token = j.enlace.split('#activar/')[1];
        S.altaOk = id; R().render();
        R().toast(`Enlace nuevo para ${j.trabajador.email}. El anterior ya no sirve.`);
      } catch (e) { R().toast(e.message); }
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
    await cargarFijos();
    const S = R().S;
    if (S.user && /^panel/.test(location.hash.slice(1)) && !S.modal) await cargarPanel();
    repintar();
  }

  if (URL_API) {
    restaurarSesion();
    ciclo();
    setInterval(() => { if (!document.hidden) ciclo(); }, CADA_MS);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) ciclo(); });
    window.addEventListener('focusout', () => setTimeout(() => repintar(), 0));
  }
  repintar(true);
})();
