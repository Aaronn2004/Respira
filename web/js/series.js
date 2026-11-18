/* Series de la demostración web. Los periodos, la interpolación y los
   eventos siguen Logica.php, cuyos resultados consume Android. */
(() => {
  'use strict';

  const MINUTO = 60_000;
  const HORA = 60 * MINUTO;
  const DIA = 24 * HORA;
  const PASO_LECTURA = 15 * MINUTO;
  const PERIODOS = {
    '24h': { tramos: 48, paso: 30 * MINUTO },
    '7d': { tramos: 56, paso: 3 * HORA },
    '30d': { tramos: 60, paso: 12 * HORA },
  };
  const MAGNITUDES = {
    o3: { decimales: 3, medio: 0.060, alto: 0.090, amplitud: 0.015 },
    temp: { decimales: 1, medio: 25, alto: 32, amplitud: 3 },
    ruido: { decimales: 1, medio: 55, alto: 65, amplitud: 7 },
  };

  function fechaUtc(fecha) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return null;
    const ms = Date.parse(fecha + 'T00:00:00Z');
    return Number.isFinite(ms) && new Date(ms).toISOString().slice(0, 10) === fecha ? ms : null;
  }

  function intervalo(periodo, { desde, hasta, ahora }) {
    const predefinido = PERIODOS[periodo];
    if (predefinido) {
      const { tramos, paso } = predefinido;
      return { tramos, paso, fin: ahora, inicio: ahora - tramos * paso };
    }
    if (periodo !== 'custom' && periodo !== 'rango') return null;
    const primerDia = fechaUtc(desde);
    const ultimoDia = fechaUtc(hasta);
    if (primerDia === null || ultimoDia === null || ultimoDia < primerDia
        || ultimoDia > ahora || ultimoDia - primerDia >= 90 * DIA) return null;
    const fin = Math.min(ultimoDia + DIA - 1, ahora);
    if (fin <= primerDia) return null;
    const tramos = 60;
    const paso = (fin - primerDia) / tramos;
    return { tramos, paso, fin, inicio: primerDia };
  }

  // PHP round() redondea los puntos medios alejándose de cero.
  function redondear(valor, decimales) {
    const escala = 10 ** decimales;
    const absoluto = Math.abs(valor) * escala;
    return Math.sign(valor) * Math.round(absoluto + Number.EPSILON * absoluto) / escala;
  }

  // Equivalente a Logica::aproximar: solo huecos internos de hasta dos horas.
  function aproximar(valores, paso, decimales) {
    const estimados = new Array(valores.length).fill(null);
    const maxTramos = Math.floor(2 * HORA / paso);
    let i = 0;
    while (i < valores.length) {
      if (valores[i] !== null) {
        i++;
        continue;
      }
      let siguiente = i + 1;
      while (siguiente < valores.length && valores[siguiente] === null) siguiente++;
      const hueco = siguiente - i;
      if (i > 0 && siguiente < valores.length && hueco <= maxTramos) {
        const anterior = valores[i - 1];
        const cambio = (valores[siguiente] - anterior) / (hueco + 1);
        for (let j = 0; j < hueco; j++) {
          estimados[i + j] = redondear(anterior + cambio * (j + 1), decimales);
        }
      }
      i = siguiente;
    }
    return estimados;
  }

  function lecturasPorTramo(zona, magnitud, modelo, { tramos, inicio, fin }, ahora, cerrado) {
    const grupos = Array.from({ length: tramos }, () => ({ suma: 0, cantidad: 0, max: null, momento: null }));
    if (!Number.isFinite(zona?.[magnitud]) || !Number.isFinite(zona.hace) || zona.hace < 0) return grupos;

    const base = zona[magnitud];
    const semilla = Array.from(zona.id || '').reduce((n, letra) => n + letra.charCodeAt(0), 0);
    const ultimaLectura = cerrado
      ? Math.floor(fin / PASO_LECTURA) * PASO_LECTURA
      : ahora - zona.hace * MINUTO;
    const tramosHastaFin = Math.max(0, Math.ceil((ultimaLectura - fin) / PASO_LECTURA));
    for (let momento = ultimaLectura - tramosHastaFin * PASO_LECTURA; momento > inicio; momento -= PASO_LECTURA) {
      const edad = ultimaLectura - momento;
      const reloj = cerrado ? momento : edad;
      const fase = (reloj + (semilla % 4) * PASO_LECTURA) % DIA;
      const dia = Math.floor(reloj / DIA);
      // Un hueco corto y otro largo por día, y una jornada completa
      // ocasional sin lecturas. La última hora conserva la media del mapa.
      const dentroUltimaHora = !cerrado && edad <= HORA;
      const sinLectura = (fase >= 3 * HORA && fase < 4 * HORA)
        || (fase >= 11 * HORA && fase < 16 * HORA)
        || (dia > 0 && (dia + semilla) % 11 === 0);
      if (!dentroUltimaHora && sinLectura) continue;

      const variacion = Math.sin(fase / DIA * 2 * Math.PI) * .8
        + Math.cos((reloj / PASO_LECTURA + semilla) * .61) * .2;
      const valor = Math.max(0, dentroUltimaHora ? base : base + modelo.amplitud * variacion);
      const indice = Math.ceil((momento - inicio) * tramos / (fin - inicio)) - 1;
      const grupo = grupos[indice];
      grupo.suma += valor;
      grupo.cantidad++;
      if (grupo.max === null || valor > grupo.max) {
        grupo.max = valor;
        grupo.momento = momento;
      }
    }
    return grupos;
  }

  function eventos(observados, grupos, modelo, inicio, paso) {
    const alertas = [];
    for (const nivel of ['alto', 'medio']) {
      const limite = modelo[nivel];
      for (let i = 0; i < observados.length; i++) {
        const valor = observados[i];
        if (valor === null || valor < limite) continue;
        let fin = i + 1;
        let max = valor;
        while (fin < observados.length && observados[fin] !== null && observados[fin] >= limite) {
          max = Math.max(max, observados[fin]);
          fin++;
        }
        alertas.push({ desde: inicio + i * paso, hasta: inicio + fin * paso, max, nivel });
        i = fin - 1;
      }
      if (alertas.length) break;
    }

    const candidatos = grupos.map(({ max, momento }, i) => ({ max, momento, i }))
      .filter(pico => pico.max !== null)
      .sort((a, b) => redondear(b.max, modelo.decimales) - redondear(a.max, modelo.decimales));
    const picos = [];
    const indices = new Set();
    for (const { max, momento, i } of candidatos) {
      if (indices.has(i - 1) || indices.has(i + 1)) continue;
      picos.push({ momento, valor: redondear(max, modelo.decimales) });
      indices.add(i);
      if (picos.length === 3) break;
    }
    return { alertas: alertas.slice(-10), picos };
  }

  function serie(zona, magnitud, periodo, { desde, hasta, ahora = Math.floor(Date.now() / MINUTO) * MINUTO } = {}) {
    const tiempos = intervalo(periodo, { desde, hasta, ahora });
    const modelo = MAGNITUDES[magnitud];
    if (!tiempos || !modelo) return null;
    const { inicio, fin, paso } = tiempos;
    const cerrado = (periodo === 'custom' || periodo === 'rango') && fin < ahora;
    const grupos = lecturasPorTramo(zona, magnitud, modelo, tiempos, ahora, cerrado);
    const observados = grupos.map(g => g.cantidad ? redondear(g.suma / g.cantidad, modelo.decimales) : null);
    const estimados = aproximar(observados, paso, modelo.decimales);
    const origen = observados.map(valor => valor === null ? null : 'simulado');
    return { observados, estimados, origen, inicio, fin, paso, avisos: [],
      ...eventos(observados, grupos, modelo, inicio, paso) };
  }

  function resumen({ observados, paso, estadisticas }) {
    if (estadisticas) return estadisticas;
    const datos = observados.filter(valor => valor !== null);
    return {
      min: datos.length ? Math.min(...datos) : null,
      media: datos.length ? datos.reduce((suma, dato) => suma + dato, 0) / datos.length : null,
      max: datos.length ? Math.max(...datos) : null,
      horasSinDatos: (observados.length - datos.length) * paso / HORA,
    };
  }

  function desdeApi(datos) {
    const { valores, aprox, origen, eventos = {}, resumen: estadisticas } = datos;
    const inicio = Date.parse(datos.desde), fin = Date.parse(datos.hasta);
    const paso = datos.paso_ms ?? datos.paso_min * MINUTO;
    const numeroOpcional = valor => valor === null || Number.isFinite(valor);
    if (!Array.isArray(valores) || !valores.length || !valores.every(numeroOpcional)
        || !Array.isArray(aprox) || aprox.length !== valores.length || !aprox.every(numeroOpcional)
        || !Array.isArray(origen) || origen.length !== valores.length
        || !origen.every((tipo, i) => valores[i] === null ? tipo === null : tipo === 'real' || tipo === 'simulado')
        || !Number.isFinite(inicio) || !Number.isFinite(fin) || fin <= inicio
        || !Number.isFinite(paso) || paso <= 0 || !estadisticas
        || ![estadisticas.minimo, estadisticas.media, estadisticas.maximo].every(numeroOpcional)
        || !Number.isFinite(estadisticas.horas_sin_datos)) {
      throw new Error('Serie no válida');
    }
    const lectura = evento => ({ ...evento, momento: Date.parse(evento.momento) });
    const alertas = (eventos.alertas ?? []).map(a => ({ ...a, desde: Date.parse(a.desde), hasta: Date.parse(a.hasta) }))
      .filter(a => Number.isFinite(a.desde) && Number.isFinite(a.hasta) && Number.isFinite(a.max));
    const picos = (eventos.picos ?? []).map(lectura).filter(p => Number.isFinite(p.momento) && Number.isFinite(p.valor));
    const avisos = (eventos.avisos ?? []).map(lectura).filter(a => Number.isFinite(a.momento) && Number.isFinite(a.valor));
    return {
      observados: valores, estimados: aprox, origen, inicio, fin, paso, alertas, picos, avisos,
      estadisticas: { min: estadisticas.minimo, media: estadisticas.media, max: estadisticas.maximo,
        horasSinDatos: estadisticas.horas_sin_datos },
    };
  }

  const api = { serie, aproximar, resumen, desdeApi, fechaUtc };
  if (typeof module !== 'undefined') module.exports = api;
  else window.RespiraSeries = api;
})();
