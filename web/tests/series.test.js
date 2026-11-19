const assert = require('node:assert/strict');
const test = require('node:test');
const { serie, aproximar, resumen, desdeApi } = require('../js/series');

const MINUTO = 60_000;
const HORA = 60 * MINUTO;
const ahora = Date.UTC(2026, 10, 16, 12);
const zona = { id: 'clot', hace: 0, o3: .052, temp: 24.1, ruido: 49 };

test('interpolación lineal de huecos internos y máximo documentado en Android/PHP', () => {
  const mediaHora = HORA / 2;
  assert.deepEqual(aproximar([1, null, 3], mediaHora, 1), [null, 2, null]);
  assert.deepEqual(aproximar([1, null, null, null, 3], mediaHora, 1), [null, 1.5, 2, 2.5, null]);
  assert.deepEqual(aproximar([1, null, null, null, null, 3], mediaHora, 1), [null, 1.4, 1.8, 2.2, 2.6, null]);
  assert.deepEqual(aproximar([1, null, null, null, null, null, 3], mediaHora, 1), Array(7).fill(null));
  assert.deepEqual(aproximar([null, null, 3, null], mediaHora, 1), Array(4).fill(null));
  assert.deepEqual(aproximar([1, null, 3], 3 * HORA, 1), [null, null, null]);
  assert.deepEqual(aproximar([.009, null, .012], mediaHora, 3), [null, .011, null]);
});

test('un mismo rango de demostración cerrado conserva los valores al volver a consultarlo', () => {
  const rango = { desde: '2026-11-10', hasta: '2026-11-12' };
  assert.deepEqual(serie(zona, 'temp', 'custom', { ...rango, ahora }),
    serie(zona, 'temp', 'custom', { ...rango, ahora: ahora + 26 * HORA }));
});

test('la API conserva duración corta, procedencia y estadísticas de las lecturas base', () => {
  const datos = {
    desde: '2026-11-16T00:00:00Z', hasta: '2026-11-16T00:01:30Z', paso_ms: 30000,
    valores: [16, null, 20], aprox: [null, 18, null], origen: ['real', null, 'simulado'],
    resumen: { minimo: 15, media: 18, maximo: 21, horas_sin_datos: 1 / 120 },
    eventos: { avisos: [{ momento: '2026-11-16T00:01:30Z', valor: 20, origen: 'simulado' }] },
  };
  const serie = desdeApi(datos);
  assert.equal(serie.paso, 30000);
  assert.deepEqual(serie.origen, ['real', null, 'simulado']);
  assert.deepEqual(serie.estimados, [null, 18, null]);
  assert.deepEqual(resumen(serie), { min: 15, media: 18, max: 21, horasSinDatos: 1 / 120 });
  assert.equal(serie.avisos[0].momento, Date.parse(datos.hasta));
  assert.throws(() => desdeApi({ ...datos, paso_ms: -30000 }), /Serie no válida/);
});

test('el cálculo de mínimos, medias, máximos y horas sin datos solo usa observados', () => {
  assert.deepEqual(resumen({ observados: [2, 7, null], estimados: [null, null, 99], paso: HORA }), {
    min: 2, media: 4.5, max: 7, horasSinDatos: 1,
  });
  assert.deepEqual(resumen({ observados: [null, null], estimados: [null, null], paso: HORA }), {
    min: null, media: null, max: null, horasSinDatos: 2,
  });
});

test('la última hora reproduce la media de la zona y la serie incluye huecos de ambos tipos', () => {
  const datos = serie(zona, 'temp', '24h', { ahora });
  assert.equal(datos.observados.length, 48);
  assert.equal(datos.paso, HORA / 2);
  assert.deepEqual(datos.observados.slice(-2), [zona.temp, zona.temp]);
  assert.ok(datos.estimados.some(Number.isFinite));
  assert.ok(datos.observados.some((dato, i) => dato === null && datos.estimados[i] === null));
  assert.deepEqual(datos, serie(zona, 'temp', '24h', { ahora }));
  const semanal = serie(zona, 'temp', '7d', { ahora });
  const recientes = datos.observados.slice(-6).filter(Number.isFinite);
  assert.ok(recientes.length >= 3);
  assert.ok(Math.abs(recientes.reduce((s, n) => s + n, 0) / recientes.length - semanal.observados.at(-1)) < .1);
});

test('una lectura antigua conserva su hora y no rellena el tramo derecho', () => {
  const antigua = serie({ ...zona, hace: 94 }, 'o3', '24h', { ahora });
  assert.deepEqual(antigua.observados.slice(-3), [null, null, null]);
  assert.deepEqual(antigua.estimados.slice(-3), [null, null, null]);
  assert.ok(antigua.picos.every(pico => pico.momento <= ahora - 94 * MINUTO));

  for (const sinLectura of [{ ...zona, o3: null }, { ...zona, hace: null }]) {
    const datos = serie(sinLectura, 'o3', '24h', { ahora });
    assert.ok(datos.observados.every(valor => valor === null));
    assert.ok(datos.estimados.every(valor => valor === null));
    assert.deepEqual(datos.alertas, []);
    assert.deepEqual(datos.picos, []);
  }
});

test('los picos usan el valor y el instante de una lectura, las alertas usan sus medias', () => {
  const ruidosa = { ...zona, ruido: 68 };
  const datos = serie(ruidosa, 'ruido', '24h', { ahora });
  assert.ok(datos.alertas.length);
  assert.ok(datos.alertas.every(a => a.nivel === 'alto' && a.max >= 65 && a.desde < a.hasta));
  assert.equal(datos.picos.length, 3);
  assert.ok(datos.picos.every(({ momento, valor }) => momento > datos.inicio && momento <= datos.fin && valor >= 65));
});

test('rangos personalizados utilizan UTC, rechazan fechas inválidas o futuras y limitan a 90 días', () => {
  // --------------------------------------------------------------
  // # Usa el mismo instante de referencia para probar los límites del rango.
  // # Serie es el resultado lógico de serie(); Ausente representa null.
  // desde: Date, hasta: Date --> rango() --> Serie | Ausente
  // --------------------------------------------------------------
  const rango = (desde, hasta) => serie(zona, 'temp', 'custom', { desde, hasta, ahora });
  assert.equal(rango('2026-11-15', '2026-11-14'), null);
  assert.equal(rango('2026-11-16', '2026-11-17'), null);
  assert.equal(rango('2026-02-31', '2026-03-05'), null);
  assert.equal(rango('2026-07-31', '2026-10-29'), null);
  assert.ok(rango('2026-08-01', '2026-10-29'));
  const hoy = rango('2026-11-16', '2026-11-16');
  assert.equal(hoy.inicio, Date.UTC(2026, 10, 16));
  assert.equal(hoy.fin, ahora);
  assert.equal(hoy.paso, 12 * MINUTO);
  assert.equal(hoy.observados.length, 60);
  const pasado = rango('2026-11-10', '2026-11-12');
  assert.equal(pasado.inicio, Date.UTC(2026, 10, 10));
  assert.equal(pasado.fin, Date.UTC(2026, 10, 13) - 1);
  assert.ok(pasado.picos.every(pico => pico.momento <= pasado.fin));
});
