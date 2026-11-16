/* =============================================================
   Respira Web (portada + web pública + panel municipal). JS vanilla.
   Generado desde el prototipo web v35 que se ve en Claude (diseño de la versión B).
   - Fondo blanco con botón de modo oscuro (data-tema en <html>, localStorage «respira-tema»).
   - El mapa usa Google Maps JavaScript API si hay clave en js/config.js (js/mapa-google.js).
     Sin clave, usa el mapa SVG de demostración.
   - Datos: si js/config.js trae API_URL, js/datos-api.js los pide a la API
     (mapa, gráficas, comunidad) y el panel municipal funciona contra el
     servidor (sesión, sensores, préstamos, puntos, regalos, trabajadores).
     Sin API_URL, se usan los datos de DEMOSTRACIÓN de abajo.
   ============================================================= */
(function(){
"use strict";
const GMAP=()=>window.RespiraMapa;
/* ---------- marca: Respira (producto) y Nodia (empresa) ---------- */
const RW=`<g transform="translate(55 1.9) scale(0.457)"><g transform="translate(-6.60 74.40) scale(0.10000)"><path d="M66 0V-254V-525H185L186 -346H205Q213 -413 232.5 -455.0Q252 -497 287.0 -517.0Q322 -537 374 -537Q383 -537 393.5 -536.5Q404 -536 418 -533L412 -380Q397 -387 379.0 -390.0Q361 -393 347 -393Q308 -393 280.0 -375.0Q252 -357 234.5 -323.5Q217 -290 211 -242V0Z M734.0 14Q669.0 14 618.5 -4.0Q568.0 -22 533.0 -56.0Q498.0 -90 479.5 -140.0Q461.0 -190 461.0 -253Q461.0 -315 478.5 -367.5Q496.0 -420 530.0 -458.5Q564.0 -497 613.0 -518.0Q662.0 -539 724.0 -539Q784.0 -539 831.0 -519.5Q878.0 -500 909.5 -461.0Q941.0 -422 955.5 -365.5Q970.0 -309 966.0 -236L554.0 -232V-311L883.0 -314L832.0 -274Q839.0 -327 825.0 -361.0Q811.0 -395 784.5 -411.0Q758.0 -427 726.0 -427Q688.0 -427 659.0 -407.0Q630.0 -387 614.0 -348.5Q598.0 -310 598.0 -256Q598.0 -171 635.5 -131.0Q673.0 -91 734.0 -91Q762.0 -91 781.5 -98.5Q801.0 -106 813.5 -118.0Q826.0 -130 833.5 -145.0Q841.0 -160 846.0 -175L971.0 -148Q962.0 -111 944.5 -81.5Q927.0 -52 898.0 -30.5Q869.0 -9 828.5 2.5Q788.0 14 734.0 14Z M1271.0 14Q1218.0 14 1175.0 4.0Q1132.0 -6 1101.0 -25.0Q1070.0 -44 1051.0 -70.5Q1032.0 -97 1026.0 -130L1136.0 -175Q1140.0 -153 1158.0 -134.0Q1176.0 -115 1206.5 -103.5Q1237.0 -92 1280.0 -92Q1326.0 -92 1351.0 -105.5Q1376.0 -119 1376.0 -144Q1376.0 -163 1362.0 -174.0Q1348.0 -185 1321.0 -193.0Q1294.0 -201 1256.0 -208Q1219.0 -217 1180.5 -227.0Q1142.0 -237 1109.5 -255.0Q1077.0 -273 1056.5 -302.5Q1036.0 -332 1036.0 -378Q1036.0 -426 1062.0 -462.0Q1088.0 -498 1138.0 -518.5Q1188.0 -539 1260.0 -539Q1325.0 -539 1374.0 -521.5Q1423.0 -504 1454.5 -471.5Q1486.0 -439 1497.0 -394L1380.0 -355Q1376.0 -379 1360.5 -397.0Q1345.0 -415 1319.5 -424.0Q1294.0 -433 1260.0 -433Q1217.0 -433 1193.5 -419.0Q1170.0 -405 1170.0 -382Q1170.0 -363 1185.5 -351.0Q1201.0 -339 1229.0 -331.5Q1257.0 -324 1295.0 -316Q1335.0 -308 1373.0 -297.5Q1411.0 -287 1441.5 -270.0Q1472.0 -253 1490.0 -225.0Q1508.0 -197 1508.0 -153Q1508.0 -101 1480.0 -63.5Q1452.0 -26 1399.0 -6.0Q1346.0 14 1271.0 14Z M1595.0 168V-258V-525H1714.0L1715.0 -379L1733.0 -378Q1742.0 -431 1764.5 -467.0Q1787.0 -503 1823.0 -521.0Q1859.0 -539 1906.0 -539Q1976.0 -539 2026.0 -504.5Q2076.0 -470 2102.5 -407.5Q2129.0 -345 2129.0 -260Q2129.0 -181 2105.0 -119.5Q2081.0 -58 2033.5 -22.0Q1986.0 14 1915.0 14Q1865.0 14 1830.5 -5.0Q1796.0 -24 1773.5 -58.5Q1751.0 -93 1736.0 -141H1717.0Q1723.0 -114 1728.0 -86.0Q1733.0 -58 1736.0 -31.5Q1739.0 -5 1739.0 20V168ZM1865.0 -101Q1901.0 -101 1926.0 -121.0Q1951.0 -141 1964.5 -177.0Q1978.0 -213 1978.0 -259Q1978.0 -309 1964.0 -344.5Q1950.0 -380 1924.0 -400.0Q1898.0 -420 1862.0 -420Q1830.0 -420 1807.0 -406.0Q1784.0 -392 1768.5 -369.0Q1753.0 -346 1746.0 -319.5Q1739.0 -293 1739.0 -269V-250Q1739.0 -231 1744.0 -210.5Q1749.0 -190 1760.0 -170.5Q1771.0 -151 1786.0 -135.5Q1801.0 -120 1821.0 -110.5Q1841.0 -101 1865.0 -101Z M2223.0 0V-525H2367.0V0ZM2296.0 -604Q2254.0 -604 2231.5 -621.5Q2209.0 -639 2209.0 -673Q2209.0 -708 2231.5 -726.0Q2254.0 -744 2296.0 -744Q2339.0 -744 2361.5 -726.0Q2384.0 -708 2384.0 -673Q2384.0 -640 2361.5 -622.0Q2339.0 -604 2296.0 -604Z M2490.0 0V-254V-525H2609.0L2610.0 -346H2629.0Q2637.0 -413 2656.5 -455.0Q2676.0 -497 2711.0 -517.0Q2746.0 -537 2798.0 -537Q2807.0 -537 2817.5 -536.5Q2828.0 -536 2842.0 -533L2836.0 -380Q2821.0 -387 2803.0 -390.0Q2785.0 -393 2771.0 -393Q2732.0 -393 2704.0 -375.0Q2676.0 -357 2658.5 -323.5Q2641.0 -290 2635.0 -242V0Z M3037.0 14Q2993.0 14 2959.0 -3.5Q2925.0 -21 2906.0 -54.0Q2887.0 -87 2887.0 -136Q2887.0 -179 2903.5 -208.0Q2920.0 -237 2952.0 -255.0Q2984.0 -273 3031.0 -285.0Q3078.0 -297 3138.0 -306Q3170.0 -311 3190.5 -315.5Q3211.0 -320 3221.0 -330.5Q3231.0 -341 3231.0 -361Q3231.0 -389 3211.0 -409.0Q3191.0 -429 3148.0 -429Q3119.0 -429 3094.5 -419.0Q3070.0 -409 3052.5 -389.0Q3035.0 -369 3027.0 -338L2900.0 -377Q2912.0 -418 2934.5 -448.0Q2957.0 -478 2989.0 -498.5Q3021.0 -519 3062.0 -529.0Q3103.0 -539 3151.0 -539Q3228.0 -539 3276.5 -514.5Q3325.0 -490 3349.0 -438.5Q3373.0 -387 3373.0 -306V-216Q3373.0 -181 3374.5 -145.0Q3376.0 -109 3378.5 -72.5Q3381.0 -36 3384.0 0H3257.0Q3253.0 -25 3249.5 -57.5Q3246.0 -90 3244.0 -123H3226.0Q3212.0 -85 3186.0 -54.0Q3160.0 -23 3122.5 -4.5Q3085.0 14 3037.0 14ZM3098.0 -90Q3117.0 -90 3136.5 -97.0Q3156.0 -104 3174.5 -116.5Q3193.0 -129 3208.5 -148.0Q3224.0 -167 3234.0 -191L3232.0 -275L3255.0 -270Q3237.0 -257 3213.0 -249.5Q3189.0 -242 3163.5 -238.0Q3138.0 -234 3113.0 -229.0Q3088.0 -224 3068.5 -216.0Q3049.0 -208 3037.5 -194.5Q3026.0 -181 3026.0 -157Q3026.0 -126 3046.0 -108.0Q3066.0 -90 3098.0 -90Z"/></g></g>`;
const SYM=(p='#0A6C74',a='#5FC4C0')=>`<path d="M20.35 3.32A21 21 0 1 0 44.68 27.65" fill="none" stroke="${a}" stroke-width="4.4" stroke-linecap="round"/><path d="M26.26 11.20A13 13 0 1 0 36.80 21.74" fill="none" stroke="${p}" stroke-width="4.4" stroke-linecap="round"/><circle cx="24" cy="24" r="6" fill="${p}"/>`;
const LOGO=(h=30,word='var(--ink)',p,a)=>`<svg role="img" aria-label="Respira" viewBox="0 0 207 48" height="${h}" width="${Math.round(h*207/48)}" style="flex-shrink:0;display:block">${SYM(p,a)}<g fill="${word}">${RW}</g></svg>`;
const ICON=h=>`<svg role="img" aria-label="Icono de Respira" viewBox="0 0 48 48" width="${h}" height="${h}" style="flex-shrink:0;display:block"><rect width="48" height="48" rx="11" fill="#0A6C74"/><g transform="translate(7.1 7.1) scale(0.703)">${SYM('#FFFFFF','#5FC4C0')}</g></svg>`;
const NODIA=(h=16,c='currentColor')=>`<svg aria-hidden="true" viewBox="0 0 48 48" width="${h}" height="${h}" style="color:${c};flex-shrink:0;display:inline-block;vertical-align:middle"><path d="M10 40V14L36 40V16" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="40" r="5.5" fill="currentColor"/><circle cx="10" cy="14" r="5.5" fill="currentColor"/><circle cx="36" cy="40" r="5.5" fill="currentColor"/><circle cx="36" cy="14" r="10" fill="none" stroke="#7FD3B2" stroke-width="2.2" opacity=".75"/><circle cx="36" cy="14" r="6" fill="#7FD3B2"/></svg>`;

const sv=(p,s=20,w=2)=>`<svg aria-hidden="true" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round">${p}</svg>`;
const I={x:sv('<path d="M6 6l12 12M18 6L6 18"/>'),menu:sv('<path d="M4 6h16M4 12h16M4 18h16"/>',22),search:sv('<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',18),
 dl:sv('<path d="M12 4v11M7 10l5 5 5-5M5 20h14"/>',18),alert:sv('<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18v.01"/>'),info:sv('<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v.01"/>'),
 check:sv('<path d="M5 12l5 5 9-10"/>',16,3),err:sv('<circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16v.01"/>',16),wrench:sv('<path d="M14 6a4 4 0 0 0 5 5l-9 9-3-3 9-9a4 4 0 0 1-2-2z"/>',16),
 grid:sv('<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',18),
 pin:sv('<path d="M12 21s7-6.5 7-12a7 7 0 0 0-14 0c0 5.5 7 12 7 12z"/><circle cx="12" cy="9" r="2.5"/>',18),shield:sv('<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/>',18),
 doc:sv('<path d="M6 3h9l4 4v14H6z"/><path d="M14 3v5h5M9 13h7M9 17h7"/>',18),bell:sv('<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10 21a2 2 0 0 0 4 0"/>',18),
 users:sv('<circle cx="9" cy="8" r="3.5"/><path d="M2 20c1-4 4-6 7-6s6 2 7 6"/><circle cx="17" cy="9" r="2.5"/><path d="M17 14c2.5 0 4.5 1.5 5 4.5"/>',18),
 bt:sv('<path d="M7 7l10 10-5 5V2l5 5L7 17"/>',26),phone:sv('<rect x="7" y="2" width="10" height="20" rx="2.5"/><path d="M11 18h2"/>',26),server:sv('<rect x="3" y="4" width="18" height="7" rx="2"/><rect x="3" y="13" width="18" height="7" rx="2"/><path d="M7 7.5h.01M7 16.5h.01"/>',26),
 mapi:sv('<path d="M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3z"/><path d="M9 3v15M15 6v15"/>',26),sensor:sv('<circle cx="12" cy="12" r="3"/><path d="M6.3 6.3a8 8 0 0 0 0 11.4M17.7 6.3a8 8 0 0 1 0 11.4M3.5 3.5a12 12 0 0 0 0 17M20.5 3.5a12 12 0 0 1 0 17"/>',26)};
I.coin=sv('<circle cx="12" cy="12" r="9"/><path d="M14.6 9.4C14.1 8.5 13.1 8 12 8c-1.5 0-2.6.8-2.6 2s1.1 1.6 2.6 2 2.6.8 2.6 2-1.1 2-2.6 2c-1.2 0-2.2-.5-2.7-1.5M12 6.5V8M12 16v1.5"/>',18);
I.gift=sv('<rect x="3" y="8" width="18" height="13" rx="2"/><path d="M3 12h18M12 8v13M12 8C10 4 6 4 6 6.5S12 8 12 8zm0 0c2-4 6-4 6-1.5S12 8 12 8z"/>',18);
I.swap=sv('<path d="M7 7h11l-3-3M17 17H6l3 3"/>',18);
I.walk=sv('<circle cx="13" cy="4" r="2"/><path d="M9 21l2-6 3 3v3M7 12l3-4 4 2 3 4M10 8l-1 5"/>',26);
I.bus=sv('<rect x="4" y="3" width="16" height="14" rx="3"/><path d="M4 10h16M7 21v-4M17 21v-4M8 14h.01M16 14h.01"/>',26);
I.bike=sv('<circle cx="6" cy="16" r="3.5"/><circle cx="18" cy="16" r="3.5"/><path d="M6 16l4-8h5l3 8M10 8l2 8h-6M14 5h3"/>',26);
I.broom=sv('<path d="M14 3l-4 9M6 13h8l2 8H4z"/>',26);
I.box=sv('<path d="M3 7l9-4 9 4v10l-9 4-9-4z"/><path d="M3 7l9 4 9-4M12 11v10"/>',18);
I.list=sv('<path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01"/>',18);

/* ---------- datos ---------- */
const TODAY=(d=>d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0'))(new Date());
const MAG={o3:{n:'Ozono',ab:'O₃',u:'ppm'},temp:{n:'Temperatura',ab:'T',u:'°C'},ruido:{n:'Ruido',ab:'dB',u:'dB'}};
const LIM={o3:[0.060,0.090],temp:[25,32],ruido:[55,65]};
/* ---------- efectos en la salud (mismo texto en web y app) ---------- */
const EF_ICO={o3:'<path d="M3 8h11a3 3 0 1 0-3-3"/><path d="M3 12h15a3 3 0 1 1-3 3"/><path d="M3 16h7"/>',temp:'<path d="M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0z"/><path d="M12 10v7"/>',ruido:'<path d="M4 9.5h3.5L12 5v14l-4.5-4.5H4z"/><path d="M15.5 9a4 4 0 0 1 0 6"/><path d="M18.5 6.5a7.5 7.5 0 0 1 0 11"/>'};

/* efectos en la salud: escalera Molesta · Daña · Peligro */
const EF_B=[
 {k:'o3',n:'Ozono',lema:'Irrita los pulmones las tardes de sol',pasos:[
   ['medio','0,060 ppm','8 h','Te ahogas antes si haces deporte'],
   ['alto','0,090 ppm','1 h','Ojos y garganta irritados, tos, duele respirar hondo'],
   ['grave','0,120 ppm','1 h','Crisis de asma o EPOC; los pulmones rinden menos, también en sanos']],
  peor:['Niños','Mayores','Asma o EPOC','Deportistas'],hacer:'Si hay un aviso oficial por ozono, sigue las recomendaciones de las autoridades',
  fuente:'UE (RD 102/2011) · OMS 2021'},
 {k:'temp',n:'Calor',lema:'El cuerpo deja de poder enfriarse',pasos:[
   ['medio','28–30 °C','máxima','Deshidratación, calambres y dolor de cabeza en personas sensibles'],
   ['alto','> 20 °C','de noche','No descansas: el cuerpo no se recupera y el corazón se fuerza'],
   ['grave','> 40 °C','en el cuerpo','Golpe de calor: confusión y piel seca y ardiendo. Llama al 112']],
  peor:['Mayores de 65','Bebés','Embarazadas','Enfermos crónicos'],hacer:'Sombra, agua sin esperar a tener sed y evita las horas centrales',
  fuente:'Ministerio de Sanidad, plan de calor 2026'},
 {k:'ruido',n:'Ruido',lema:'Te daña aunque te acostumbres',pasos:[
   ['medio','45 dB','noche','Duermes peor aunque no llegues a despertarte'],
   ['alto','53–65 dB','día','Estrés, tensión alta y más riesgo de infarto con los años'],
   ['grave','85 dB','horas','Pitidos y sordera que no tienen vuelta atrás']],
  peor:['Niños','Mayores','Tensión alta o corazón'],hacer:'Duerme en el cuarto más tranquilo y pasea por calles sin tráfico',
  fuente:'OMS Europa 2018 · RD 286/2006'}];
const EF_NV={medio:'Molesta',alto:'Daña',grave:'Peligro'};
const EF_ICO_B={hacer:'<path d="M20 6L9 17l-5-5"/>',peor:'<circle cx="9" cy="7" r="3"/><path d="M3 21v-1a6 6 0 0 1 12 0v1"/><path d="M16 3.5a3 3 0 0 1 0 6"/><path d="M21 21v-1a6 6 0 0 0-4-5.6"/>'};
const efectosB=()=>`<div class="efb-grid">${EF_B.map(e=>`<article class="efb">
 <header class="efb-top"><span class="efb-ico efb-${e.k}">${sv(EF_ICO[e.k],24,1.9)}</span><div><h3>${e.n}</h3><p>${e.lema}</p></div></header>
 <ol class="efb-pasos">${e.pasos.map(([nv,v,t,txt])=>`<li class="efb-p efb-${nv}"><span class="efb-v"><b>${v}</b>${t?`<small>${t}</small>`:''}</span><span class="efb-txt"><em>${EF_NV[nv]}</em>${txt}</span></li>`).join('')}</ol>
 <div class="efb-pie"><div class="efb-fila">${sv(EF_ICO_B.peor,16,2)}<span class="efb-chips">${e.peor.map(p=>`<span>${p}</span>`).join('')}</span></div>
 <div class="efb-fila efb-hacer">${sv(EF_ICO_B.hacer,16,2.4)}<b>${e.hacer}</b></div></div>
 <span class="efb-fuente">${e.fuente}</span></article>`).join('')}</div>`;

/* guía de valores (equivale a «Qué significan los valores» de la app) */
const GUIA_TXT={
 o3:{bajo:'Intervalo inferior de la escala de Respira.',medio:'Intervalo intermedio de la escala de Respira.',alto:'Intervalo superior de la escala de Respira.',fuente:'Referencias del RD 102/2011: 120 µg/m³ en 8 h y umbral de información de 180 µg/m³ en 1 h. El mapa clasifica medias horarias simuladas.'},
 temp:{bajo:'Temperatura agradable.',medio:'Calor: busca sombra y bebe agua.',alto:'Riesgo para mayores, niños y enfermos crónicos.',fuente:'Valores de ejemplo del prototipo, pendientes de fuente oficial'},
 ruido:{bajo:'Ambiente tranquilo.',medio:'Ruido de tráfico habitual: molesta y cansa.',alto:'Si es habitual, daña la salud (sueño, tensión, corazón).',fuente:'Valores de ejemplo del prototipo, pendientes de fuente oficial'}};
const guiaRango=(k,i)=>{const l=LIM[k];return i===0?'&lt; '+fmt(l[0],k):i===1?fmt(l[0],k)+' a menos de '+fmt(l[1],k):'≥ '+fmt(l[1],k)};
const guiaHTML=()=>`<div class="gv-grid">${Object.keys(MAG).map(k=>`<article class="gv" data-k="${k}">
 <header class="gv-top"><span class="efb-ico efb-${k}">${sv(EF_ICO[k],22,1.9)}</span><div><h3>${MAG[k].n}</h3><span class="gv-u">${k==='o3'?MAG[k].ab+' · ':''}${MAG[k].u}</span></div></header>
 <div class="gv-barra" aria-hidden="true"><i class="lv-b"></i><i class="lv-m"></i><i class="lv-a"></i></div>
 <ul class="gv-niv">${['bajo','medio','alto'].map((n,i)=>`<li><span class="badge b-${n}">${NIV[n]}</span><b class="gv-r">${guiaRango(k,i)}</b><span class="gv-t">${GUIA_TXT[k][n]}</span></li>`).join('')}</ul>
 <span class="gv-fuente">${GUIA_TXT[k].fuente}</span></article>`).join('')}</div>
 <div class="gv-estado"><b class="ef-t">Estado del dato</b><div class="gv-est">${[
  ['real','Reciente','Medido en la última hora.'],
  ['ant','Antiguo','Medido hace entre 1 y 24 horas. Se muestra con su hora.'],
  ['sin','Sin datos','Han pasado más de 24 horas desde la última medición, o la zona aún no tiene mediciones.'],
  ['estimado','Estimado','La línea de puntos une mediciones a ambos lados de huecos de hasta dos horas. Si cada intervalo dura más de dos horas, se puede estimar un intervalo.']].map(([c,t,d])=>`<div><i class="gv-sw gv-${c}" aria-hidden="true"></i><span><b>${t}</b>${d}</span></div>`).join('')}</div>
 <p class="gv-nota">Los valores del mapa se presentan como medias por zona. Los sensores de bajo coste no sustituyen a las estaciones ni a los avisos oficiales.</p></div>`;
const EF_AVISO_B='Los efectos dependen de la duración y del contexto de la exposición. Las medias horarias simuladas del mapa son orientativas. Si hay síntomas graves, llama al 112.';
const efectosHTML=efectosB;

const NIV={bajo:'Bajo',medio:'Medio',alto:'Alto'};
const PERC={ruido:{n:'Ruido',lv:['Bajo','Medio','Alto']},temp:{n:'Temperatura',lv:['Agradable','Calor','Mucho calor']},aire:{n:'Aire',lv:['Limpio','Regular','Malo']}};
const antiguedadAviso=a=>Math.max(0,Math.floor((Date.now()-a.momento)/60000));
const avOk=a=>antiguedadAviso(a)<1440&&(!S.avf||a.niv===S.avf)&&(S.avt==='todos'||a.tipo===S.avt);
const nivel=(m,v)=>v==null?'none':v<LIM[m][0]?'bajo':v<LIM[m][1]?'medio':'alto';
// Estado general de una zona: media de los niveles de sus tres medidas (Bajo 0, Medio 1, Alto 2), redondeada.
// Si ninguna medida está en Bajo, la zona no puede salir en verde.
const nivelGeneral=z=>{const c={bajo:0,medio:1,alto:2};const ls=['o3','temp','ruido'].map(k=>nivel(k,z[k])).filter(l=>l in c);if(!ls.length)return 'none';return ['bajo','medio','alto'][Math.round(ls.reduce((a,l)=>a+c[l],0)/ls.length)]};
const fmt=(v,m)=>v==null?'—':m==='o3'?(+v).toFixed(3):m==='temp'?(+v).toFixed(1):String(Math.round(v));
const FORMATO_RUIDO=new Intl.NumberFormat('es-ES',{maximumFractionDigits:1});
const fmtSerie=(v,m)=>m==='ruido'&&v!=null?FORMATO_RUIDO.format(v):fmt(v,m);
const hace=m=>m==null?'sin datos':m<60?`hace ${m} min`:m<1440?`hace ${Math.round(m/60)} h`:`hace ${Math.round(m/1440)} d`;
const esc=s=>String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
// Escenario local de demostración, indexado por los identificadores del GeoJSON.
const MEDICIONES_COSTA={
 clot:[8,28,0.052,24.1,49], baladre:[12,21,0.064,24.6,51],
 castell:[17,16,0.049,26.5,59], ermita:[94,23,0.058,24.6,58],
 oller:[11,12,0.047,24.3,52], 'platja-nord':[5,36,0.069,28.7,68],
 venecia:[21,10,0.047,24.2,53], marenys:[null,0,null,null,null],
};
const ZONAS=window.RespiraGrauPlatja.features.map(({id,properties,geometry})=>{
 const [hace,nm,o3,temp,ruido]=MEDICIONES_COSTA[id];
 return {id,n:properties.nombre,etiqueta:properties.etiqueta,tipo:properties.tipo,ancla:properties.ancla,
  geometry,hace,nm,o3,temp,ruido};
});
const TIPOS_ZONA={parque:['Parque','Parques y plazas'],playa:['Playa','Playas']};
// Lo que pinta el mapa en cada zona según lo elegido: 'gen' (estado general) o una magnitud
const zonaMapa=z=>{const e=est(z);if(S.mag==='gen'){const l=e==='none'?'none':nivelGeneral(z);return {l,valor:l==='none'?'–':NIV[l],texto:l==='none'?'sin datos':'estado general '+NIV[l].toLowerCase()}}
 const v=z[S.mag],l=e==='none'||v==null?'none':nivel(S.mag,v);
 if(l==='none')return {l,valor:'–',texto:'sin datos'};
 const valor=fmtSerie(v,S.mag);
 return {l,valor,texto:valor+' '+MAG[S.mag].u+' · '+NIV[l]};
};
const est=z=>z.hace==null||z.hace>1440?'none':z.hace>60?'old':'ok';
const byZ=id=>ZONAS.find(z=>z.id===id);
const AVISOS=[
 {id:'a1',zona:'clot',tipo:'temp',niv:2,min:45},
 {id:'a2',zona:'platja-nord',tipo:'ruido',niv:3,min:24},
 {id:'a3',zona:'oller',tipo:'aire',niv:1,min:160},
 {id:'a4',zona:'castell',tipo:'ruido',niv:1,min:220},
 {id:'a5',zona:'venecia',tipo:'aire',niv:2,min:75},
].map(a=>({...a,momento:Date.now()-a.min*60000,dir:byZ(a.zona).n,ancla:byZ(a.zona).ancla}));
const PUNTOS_REPARTO=[['Biblioteca Central','Pl. Mayor, 2','L–V 9:00–20:00'],['Centro Cívico Norte','Av. Norte, 15','L–S 10:00–14:00'],['Oficina de Turismo','Pl. de la Estación, 1','Todos los días 9:00–19:00']];
const TIPOS={vol:'Voluntariado',trab:'Trabajador público',bici:'Bici o moto compartida',bus:'Autobús'};
const TIPO_IC={vol:I.walk,trab:I.broom,bici:I.bike,bus:I.bus};
let APPUSERS=[{id:'U-48213',n:'lucia'},{id:'U-11902',n:'marcos'},{id:'U-30577',n:'ana.v'}];
let SENS=[
 {code:'RSP-4F7K',tipo:'vol',mags:['o3','temp','ruido'],est:'prestado',user:'U-48213',ult:6,bat:78},
 {code:'RSP-2B9Q',tipo:'vol',mags:['o3','temp','ruido'],est:'prestado',user:'U-11902',ult:1530,bat:22,desde:'1 nov 10:05'},
 {code:'RSP-7H3M',tipo:'vol',mags:['o3','temp','ruido'],est:'disponible',ult:1300,bat:95},
 {code:'RSP-5K8P',tipo:'vol',mags:['o3','temp'],est:'disponible',ult:null,bat:100},
 {code:'RSP-9C2D',tipo:'vol',mags:['o3','temp','ruido'],est:'disponible',ult:2900,bat:64},
 {code:'RSP-X3QR',tipo:'vol',mags:['o3','temp'],est:'mant',ult:4300,bat:null,motivo:'Carcasa rota al devolverlo'},
 {code:'RSP-L1MP',tipo:'trab',asig:'Limpieza · Brigada de parques 2',mags:['o3','temp','ruido'],est:'activo',ult:3,bat:55},
 {code:'RSP-M7NT',tipo:'trab',asig:'Mantenimiento de jardines',mags:['o3','temp'],est:'activo',ult:45,bat:18},
 {code:'RSP-B12A',tipo:'bus',asig:'Línea 2 · Bus 214',mags:['o3','temp','ruido'],est:'activo',ult:1,bat:null},
 {code:'RSP-B05C',tipo:'bus',asig:'Línea 5 · Bus 231',mags:['o3','temp','ruido'],est:'activo',ult:1700,bat:null},
 {code:'RSP-V08E',tipo:'bici',asig:'Bici compartida · Estación 8',mags:['o3','temp'],est:'activo',ult:30,bat:41}
];
const SENSORES_DEMO_ACTIVOS=SENS.filter(s=>s.est==='activo'||s.est==='prestado').length;
const byCode=c=>SENS.find(s=>s.code===c);
let LOANS=[{code:'RSP-4F7K',user:'U-48213',ini:'2 nov 09:12',fin:null,punto:'Biblioteca Central'},{code:'RSP-2B9Q',user:'U-11902',ini:'1 nov 10:05',fin:null,punto:'Centro Cívico Norte'},{code:'RSP-7H3M',user:'U-48213',ini:'31 oct 17:40',fin:'1 nov 09:02',punto:'Biblioteca Central',estDev:'Correcto'}];
const REGLAS_DEMO={min:5,mult:2,max:100};
let RULES={...REGLAS_DEMO};
let SOSP=[{id:'d1',user:'U-30577',d:'1 nov',min:520,pts:100,mot:'Llega al máximo diario',estado:'pend'},{id:'d2',user:'U-11902',d:'1 nov',min:180,pts:58,mot:'Sensor 3 h sin moverse en el mismo punto (descontado)',estado:'pend'},{id:'d3',user:'U-48213',d:'31 oct',min:310,pts:91,mot:'Llega al 90 % del máximo',estado:'ok'}];
const REGALOS_DEMO=[{id:'g1',n:'Bolsa de tela',c:100,stock:0},{id:'g2',n:'Gorra Respira',c:150,stock:12},{id:'g3',n:'Cantimplora',c:250,stock:8},{id:'g4',n:'Mochila del ayuntamiento',c:600,stock:3}];
let GIFTS=REGALOS_DEMO.map(regalo=>({...regalo}));
const CADUCIDAD_CANJE_DEMO=new Date(Date.parse(TODAY+'T00:00:00Z')+7*864e5).toISOString().slice(0,10);
let CANJES=[{code:'CJ-8R2T',user:'U-48213',gift:'g2',cad:CADUCIDAD_CANJE_DEMO,estado:'pendiente'},{code:'CJ-2K9L',user:'U-11902',gift:'g3',cad:'2026-10-30',estado:'pendiente'},{code:'CJ-5P7Q',user:'U-30577',gift:'g2',cad:'2026-11-04',estado:'entregado'}];
const CUENTAS_PANEL_PRUEBA=[
 {n:'Ana García',e:'admin@ayto.es',p:'Admin2026!',r:'admin'},
 {n:'Luis Pérez',e:'tecnico@ayto.es',p:'Tecnico2026!',r:'tecnico'},
 {n:'Marta Ruiz',e:'reparto@ayto.es',p:'Reparto2026!',r:'reparto',punto:'Biblioteca Central'},
];
let STAFF=CUENTAS_PANEL_PRUEBA.map(cuenta=>({...cuenta}));
/* Trabajadores municipales (portadores de sensor). Rol 'trabajador': NO entra en el panel.
   La cuenta se activa con un enlace de un solo uso que caduca a las 48 h; administración nunca
   conoce ni fija su contraseña. En la versión real el token solo existe en el servidor (hash). */
STAFF.push(
 {n:'Pablo Sanz Gil',e:'psanz@ayto.es',p:'Trabajo2026!',r:'trabajador',id:'T-0001',emp:'104233',serv:'Limpieza viaria',estado:'activo',alta:'28 oct',por:'admin@ayto.es'},
 {n:'Elena Mora Vidal',e:'emora@ayto.es',p:null,r:'trabajador',id:'T-0002',emp:'104587',serv:'Parques y jardines',estado:'pendiente',alta:'3 nov',por:'admin@ayto.es',token:null,caduca:Date.now()-36e5});
const SERVICIOS=['Limpieza viaria','Parques y jardines','Mantenimiento urbano','Policía local','Movilidad','Otro servicio municipal'];
const TRAB=()=>STAFF.filter(x=>x.r==='trabajador');
const nuevoToken=()=>{const a=new Uint8Array(24);crypto.getRandomValues(a);return Array.from(a,b=>b.toString(16).padStart(2,'0')).join('')};
const ACTIV_H=48;
const ROL={admin:'Administración',tecnico:'Técnico',reparto:'Punto de reparto',trabajador:'Trabajador municipal'};

/* ---------- estado y rutas ---------- */
const S={tabIni:'o3',iniVisto:false,layer:'zonas',mag:'gen',sel:null,q:'',hist:{id:ZONAS[0].id,mag:'o3',per:'24h',desde:'',hasta:''},cmp:{a:ZONAS[0].id,b:ZONAS[1].id,mag:'o3',per:'24h'},avf:0,avt:'todos',avSel:null,
 user:null,f:{},e:{},modal:null,edit:null,fT:'todos',fE:'todos',fQ:'',recOk:false,canje:null,rules:{...RULES}};
const PUB=['inicio','mapa','comunidad','historial','comparar','como-funciona','privacidad'];
const PAN=['panel','panel-sensores','panel-editar','panel-prestamos','panel-puntos','panel-regalos','panel-trabajadores'];
const ACCESS={admin:PAN,tecnico:['panel','panel-sensores','panel-editar','panel-prestamos'],reparto:['panel-prestamos','panel-regalos'],trabajador:[]};
const HOME={admin:'panel',tecnico:'panel',reparto:'panel-prestamos'};
function route(){const h=location.hash.slice(1);if(h==='valores'||h==='voluntariado')return 'como-funciona';if(h.startsWith('activar/')){S.tok=h.slice(8);return 'activar'}return [...PUB,...PAN,'acceso','recuperar'].includes(h)?h:'inicio'}
function nav(r){S.e={};S.modal=null;if(location.hash.slice(1)===r)render();else location.hash=r;window.scrollTo({top:0,behavior:'instant'})}
function seccionPortada(){
 const destino=document.getElementById(location.hash.slice(1));
 return destino?.matches('.tv > section, .tv#inicio, .tv > section h2[id]')?destino:null;
}
function desplazarASeccion(){
 const destino=seccionPortada()||document.getElementById(location.hash.slice(1));
 if(destino?.matches('.tv#inicio, .tv > section, main section[id], .tv section h2[id]')){
  destino.scrollIntoView({behavior:'instant'});
  return destino;
 }
 return null;
}
function transicionVista(actualizar){
 if(document.startViewTransition&&!matchMedia('(prefers-reduced-motion: reduce)').matches){
  document.startViewTransition(actualizar);
 }else actualizar();
}
addEventListener('hashchange',()=>{
 if(seccionPortada())return;
 transicionVista(()=>{
  S.e={};S.modal=null;
  if(!location.hash.startsWith('#activar/')||location.hash.slice(9)!==S.tok){
   S.activOk=null;S.activRol=null;S.activTipo=null;S.f.ap1='';S.f.ap2='';
  }
  if(!location.hash.startsWith('#panel-trabajadores'))S.altaOk=null;
  render();
  const seccion=desplazarASeccion();
  if(route()!=='inicio'){
   if(!seccion)window.scrollTo({top:0,behavior:'instant'});
   enfocarSeccion(seccion?.querySelector('h2,h3')||$r.querySelector('main h1'));
  }
 });
});
let tT;function toast(m){const t=document.getElementById('toast');t.innerHTML=`<div class="t">${esc(m)}</div>`;clearTimeout(tT);tT=setTimeout(()=>t.innerHTML='',3800)}
const err=k=>S.e[k]?`<span class="err" id="e-${k}">${I.err}${S.e[k]}</span>`:'';
const inv=k=>S.e[k]?'aria-invalid="true" aria-describedby="e-'+k+'"':'';
const badge=(m,v)=>{const l=nivel(m,v);return l==='none'?'<span class="badge b-none">Sin datos</span>':`<span class="badge b-${l}">${NIV[l]}</span>`};
const fecha=d=>d.split('-').reverse().slice(0,2).join('/');

const barsIco=(n,s=18)=>{const c=['','var(--lv-bajo)','var(--lv-medio)','var(--lv-alto)'][n];return `<span style="display:inline-flex;align-items:flex-end;gap:2px;height:${s}px">${[7,12,18].map((h,i)=>`<i style="width:4px;border-radius:2px;height:${h*s/18}px;background:${i<n?c:'var(--surface-3)'}"></i>`).join('')}</span>`};

/* ---------- gráfica ---------- */
/* ---------- web pública ---------- */
function header(cur){
 /* Misma barra que la portada: marca, píldora de enlaces, botones y menú móvil */
 const com=S.layer==='comunidad';
 const activo=h=>h==='comunidad'?cur==='mapa'&&com:h==='mapa'?(cur==='mapa'&&!com)||['historial','comparar'].includes(cur):cur===h;
 const links=TV_LINKS.map(([h,n])=>`<a class="tv-link" href="#${h}"${activo(h)?' aria-current="page"':''}>${n}</a>`).join('');
 const accesoActual=['acceso','recuperar','activar'].includes(cur)?' aria-current="page"':'';
 return `<header class="tv tvh public-nav"><nav class="tv-nav" aria-label="Principal">
  <a class="tv-marca" href="#inicio" aria-label="Respira, inicio"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">${SYM()}</svg>Respira</a>
  <div class="tv-pill" data-tv-desktop>${links}</div>
  ${botonTema(true)}
  <button type="button" class="tv-burger" data-tv-mobile aria-controls="tvh-menu" aria-expanded="false" aria-label="Abrir menú">${TV_BURGER(false)}</button>
  <div class="tv-auth" data-tv-desktop>${botonTema(false)}<a class="btn sm sec" href="#acceso"${accesoActual}>Acceso ayuntamiento</a></div>
 </nav><div id="tvh-menu" class="tv-panel" data-tv-mobile inert>${links}<div class="tv-panel-pie"><a class="btn sm sec" href="#acceso"${accesoActual}>Ayuntamiento</a></div></div></header>`}
const footer=()=>`<footer class="wrap"><div class="inner"><span style="display:inline-flex;gap:8px;align-items:center;flex-wrap:wrap">${LOGO(20)}<span>· un servicio de</span>${NODIA(15,'var(--ink)')}<b style="letter-spacing:.08em;color:var(--ink)">NODIA</b></span><span>Proyecto de Biometría · GTI · UPV · <a href="#privacidad">Privacidad</a></span></div></footer>`;
const zonaOpts=sel=>Object.entries(TIPOS_ZONA).map(([t,[,plural]])=>{
 const opciones=ZONAS.filter(z=>z.tipo===t&&est(z)!=='none')
  .map(z=>`<option value="${z.id}" ${z.id===sel?'selected':''}>${esc(z.n)}</option>`).join('');
 return `<optgroup label="${plural}">${opciones}</optgroup>`;
}).join('');

const normalizarZona=texto=>texto.toLowerCase().normalize('NFD').replace(/\p{Diacritic}|[’']/gu,'').trim();
const zonaCoincide=z=>normalizarZona(z.n).includes(normalizarZona(S.q));

function seleccionarZona(id){
 const anterior=S.sel;
 S.sel=byZ(id)?.id||null;
 render();
 if(S.sel)document.getElementById('zona-detalle')?.focus();
 else if(anterior)document.getElementById('zona-'+anterior)?.focus({preventScroll:true});
}

function detalleZona(z){
 if(!z)return '';
 const estado=est(z);
 const medias24=Object.entries(MAG).map(([k,m])=>[m,resumenSerie(modeloSerie(z,k,'24h')).media,k]).filter(([,valor])=>valor!=null);
 return `<section class="mapa-detalle" id="zona-detalle" aria-labelledby="zona-nombre" tabindex="-1">
  <div class="mapa-detalle-cab">
   <div><span class="hint">${TIPOS_ZONA[z.tipo][0]}</span><h2 id="zona-nombre">${esc(z.n)}</h2></div>
   <button type="button" class="ib" data-act="sel" data-arg="" aria-label="Cerrar detalle">${I.x}</button>
  </div>
  ${estado==='none'?'<p class="note">Esta zona no tiene mediciones en el escenario simulado.</p>':`
   <p class="hint">${z.nm} mediciones simuladas · ${hace(z.hace)} ${estado==='old'?'· Dato antiguo':'· Media de la última hora'}</p>
   <div>${Object.entries(MAG).map(([k,m])=>`<div class="vrow"><span>${m.n}</span>
    <b>${fmtSerie(z[k],k)} <small>${m.u}</small></b>${badge(k,z[k])}
    <button type="button" class="gv-q" data-act="aValores" data-arg="${k}" aria-label="Qué significa el valor de ${m.n.toLowerCase()}">?</button>
   </div>`).join('')}</div>`}
  ${medias24.length?`<p class="hint">Media de las últimas 24 h · ${medias24.map(([m,v,k])=>m.n+' '+fmtSerie(v,k)+' '+m.u).join(' · ')}</p>`:''}
  ${estado!=='none'?`<details class="mapa-efectos"><summary>Efectos en la salud</summary>
   <p>La guía explica cómo influyen la magnitud y la duración de la exposición.</p>
   <a href="#valores">Consultar la guía de valores y efectos</a>
  </details>`:''}
  <div class="mapa-detalle-acciones">
   <button type="button" class="btn sm" data-act="toHist" data-arg="${z.id}" ${estado==='none'?'disabled':''}>Ver historial</button>
   <button type="button" class="btn sm sec" data-act="toCmp" data-arg="${z.id}" ${estado==='none'?'disabled':''}>Comparar</button>
  </div>
 </section>`;
}

function listaZonas(){
 const lista=ZONAS.filter(zonaCoincide);
 const fila=z=>{
  const zm=zonaMapa(z);
  const estado=est(z);
  const valor=zm.l==='none'?'Sin datos':S.mag==='gen'?zm.valor:zm.texto;
  return `<button type="button" id="zona-${z.id}" class="srow" data-act="sel" data-arg="${z.id}" aria-pressed="${S.sel===z.id}">
   <span class="mapa-punto gm-${zm.l}" aria-hidden="true"></span>
   <span class="mapa-fila-texto"><b>${esc(z.n)}</b>${estado==='old'?'<span class="hint">Dato antiguo</span>':''}</span>
   <span class="mapa-fila-valor">${valor}</span>
  </button>`;
 };
 return `
  <div class="field">
   <label for="q" class="mapa-label">Buscar zona</label>
   <div class="mapa-buscar"><span aria-hidden="true">${I.search}</span><input class="input" id="q" type="search" data-bind="q" data-live="1" value="${esc(S.q)}" placeholder="Parque, plaza o playa" maxlength="60" autocomplete="off"></div>
  </div>
  ${detalleZona(S.sel&&byZ(S.sel))}
  ${lista.length?Object.entries(TIPOS_ZONA).map(([t,[,plural]])=>{
   const grupo=lista.filter(z=>z.tipo===t);
   return grupo.length?`<section><h2 class="mapa-label">${plural}</h2><div class="slist">${grupo.map(fila).join('')}</div></section>`:'';
  }).join(''):`<p class="note" role="status">No hay zonas para «${esc(S.q.trim())}».</p>`}`;
}

function vMapa(){
 const sensores=S.layer==='zonas';
 const magnitudes=[['gen','General'],...Object.entries(MAG).map(([k,m])=>[k,m.n])];
 const filtrosComunidad=[['todos','Todos'],['ruido','Ruido'],['temp','Temperatura'],['aire','Aire']];
 const nivelesComunidad=S.avt==='todos'?['Bajo','Medio','Alto']:PERC[S.avt].lv;
 const aviso=AVISOS.filter(avOk);
 const opciones=(lista,accion,actual,grupo)=>`<div class="chips mapa-chips" role="group" aria-label="${grupo}">
  ${lista.map(([k,n])=>`<button type="button" class="chip" aria-pressed="${actual===k}" data-act="${accion}" data-arg="${k}">${n}</button>`).join('')}
 </div>`;
 return header('mapa')+`<main class="mapa-pagina wrap">
  <div class="mapa-encabezado">
   <div><h1>${sensores?'Mapa del Grau i Platja':'Comunidad del Grau i Platja'}</h1>
    <p>${sensores?'Ozono, temperatura y ruido en parques, plazas y playas del litoral de Gandia.':'Percepciones de ruido, calor y calidad del aire en el litoral de Gandia.'}</p></div>
   <span class="mapa-simulacion">${sensores?'Mediciones simuladas':'Percepciones simuladas'}</span>
  </div>
  <div class="mapa-filtros">
   <nav class="mapa-capas" aria-label="Capas del mapa">
    <a href="#mapa"${sensores?' aria-current="page"':''}>Sensores</a>
    <a href="#comunidad"${sensores?'':' aria-current="page"'}>Comunidad</a>
   </nav>
   ${sensores?opciones(magnitudes,'mag',S.mag,'Magnitud'):
    opciones(filtrosComunidad,'avt',S.avt,'Tipo de percepción')+opciones([[0,'Cualquier nivel'],...nivelesComunidad.map((n,i)=>[i+1,n])],'avf',S.avf,'Nivel de percepción')}
   ${sensores?`<button type="button" class="btn sec mapa-saltar" aria-controls="mapa-lista" data-act="buscarZona" aria-label="Buscar zona">${I.search}<span>Buscar zona</span></button>`:''}
  </div>
  <div class="mapview">
   <div class="mapa-cartografia">
    <div class="mapwrap" role="region" aria-label="Cartografía del Grau i Platja">
     <div class="gmap-slot"></div>${GMAP().controlesHTML()}
    </div>
    <div class="mapa-pie">
     <div class="mapa-leyenda" aria-label="Leyenda">
      <strong>${sensores?(S.mag==='gen'?'Estado general':MAG[S.mag].n+' · '+MAG[S.mag].u):'Intensidad percibida'}</strong>
      <span><i class="mapa-punto gm-bajo"></i>${sensores?'Bajo':nivelesComunidad[0]}</span>
      <span><i class="mapa-punto gm-medio"></i>${sensores?'Medio':nivelesComunidad[1]}</span>
      <span><i class="mapa-punto gm-alto"></i>${sensores?'Alto':nivelesComunidad[2]}</span>
      ${sensores?`<span><i class="mapa-punto gm-old"></i>Antiguo</span><span><i class="mapa-punto gm-none"></i>Sin datos</span><button type="button" class="gv-abrir" data-act="guia">Qué significan los niveles</button>`:''}
     </div>
     <a class="mapa-fuente" href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">Contornos © colaboradores de OpenStreetMap</a>
    </div>
   </div>
   <aside class="side" id="mapa-lista" aria-label="${sensores?'Zonas':'Percepciones'}">
    ${sensores?listaZonas():`<section><h2 class="mapa-label">Percepciones recientes</h2>
     <p class="hint">Las percepciones se publican desde la app Respira y se muestran durante 24 h. Aquí puedes explorar ejemplos simulados.</p>
     ${aviso.length?`<ul class="mapa-avisos">${aviso.map(a=>`<li><button type="button" class="srow" id="aviso-${a.id}" data-act="verPercepcion" data-arg="${a.id}" aria-pressed="${S.avSel===a.id}">${barsIco(a.niv)}<div><b>${esc(a.dir)}</b><span>${PERC[a.tipo].n} · ${PERC[a.tipo].lv[a.niv-1]}</span></div><span class="hint">${hace(antiguedadAviso(a))}</span></button></li>`).join('')}</ul>`:'<p class="note" role="status">No hay percepciones con estos filtros.</p>'}
    </section>`}
   </aside>
  </div>
 </main>`;
}

const PERIODOS_SERIE=[['24h','24 h'],['7d','7 días'],['30d','30 días']];
const modeloSerie=(zona,mag,per,rango)=>window.RespiraSeries.serie(zona,mag,per,rango);
const resumenSerie=serie=>window.RespiraSeries.resumen(serie);
const HORA_SERIE=new Intl.DateTimeFormat('es-ES',{hour:'2-digit',minute:'2-digit'});
const HORA_SERIE_UTC=new Intl.DateTimeFormat('es-ES',{hour:'2-digit',minute:'2-digit',timeZone:'UTC'});
const FECHA_SERIE=new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short'});
const FECHA_SERIE_UTC=new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short',timeZone:'UTC'});
const MOMENTO_SERIE=new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'});
const MOMENTO_SERIE_UTC=new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit',timeZone:'UTC'});
const momentoSerie=(ms,per)=>(per==='custom'?MOMENTO_SERIE_UTC:MOMENTO_SERIE).format(ms);
const horasSerie=valor=>new Intl.NumberFormat('es-ES',{maximumFractionDigits:2}).format(valor)+' h';

function tramos(valores,incluir){
 const grupos=[];
 let inicio=null;
 for(let i=0;i<=valores.length;i++){
  if(i<valores.length&&incluir(valores[i],i)){if(inicio===null)inicio=i;}
  else if(inicio!==null){grupos.push([inicio,i-1]);inicio=null;}
 }
 return grupos;
}

function graficaSeries(series,mag,per){
 const [medio,alto]=LIM[mag],unidad=MAG[mag].u;
 const datos=series.flatMap(({observados,estimados})=>[...observados,...estimados]).filter(v=>v!=null);
 const base=Math.min(medio,...datos),techo=Math.max(alto,...datos);
 const margen=(techo-base)*.12;
 const min=Math.max(0,base-margen),max=techo+margen;
 const fuente=series[0],n=fuente.observados.length;
 let formatoEje=per==='24h'?HORA_SERIE:FECHA_SERIE;
 if(per==='custom'){
  const mismoDia=FECHA_SERIE_UTC.format(fuente.inicio)===FECHA_SERIE_UTC.format(fuente.fin);
  formatoEje=mismoDia?HORA_SERIE_UTC:FECHA_SERIE_UTC;
 }
 const x=i=>(i+.5)/n*1000;
 const y=v=>(max-v)/(max-min)*300;
 const linea=puntos=>puntos.map(([i,v],j)=>(j?'L':'M')+x(i).toFixed(2)+' '+y(v).toFixed(2)).join(' ');
 const bandas=[[min,medio,'bajo'],[medio,alto,'medio'],[alto,max,'alto']];
 const seriesSvg=series.map((s,j)=>{
  const color=j?'var(--sim)':'var(--primary)';
  const trazos=tramos(s.observados,v=>v!=null).map(([a,b])=>{
   if(a===b)return `<circle cx="${x(a)}" cy="${y(s.observados[a])}" r="3" fill="${color}"/>`;
   const puntos=s.observados.slice(a,b+1).map((v,i)=>[a+i,v]);
   return `<path d="${linea(puntos)}" stroke="${color}" ${j?'stroke-dasharray="8 4"':''}/>`;
  });
  const estimados=tramos(s.estimados,v=>v!=null).map(([a,b])=>{
   const puntos=[[a-1,s.observados[a-1]],...s.estimados.slice(a,b+1).map((v,i)=>[a+i,v]),[b+1,s.observados[b+1]]];
   return `<path d="${linea(puntos)}" stroke="${color}" stroke-dasharray="${j?'2 3 2 6':'2 4'}" opacity=".82"/>`;
  });
  return trazos.join('')+estimados.join('');
 }).join('');
 const sinDatos=tramos(fuente.observados,(_,i)=>series.every(s=>s.observados[i]==null&&s.estimados[i]==null));
 return `<div class="serie-grafica" aria-label="Gráfica de ${MAG[mag].n} en ${unidad}">
  <div class="serie-valores" aria-hidden="true">${[alto,medio].map(v=>`<span style="top:${y(v)/3}%">${fmtSerie(v,mag)}</span>`).join('')}</div>
  <div class="serie-dibujo">
   <svg viewBox="0 0 1000 300" preserveAspectRatio="none" role="img" aria-label="Evolución de ${MAG[mag].n} (${unidad}). Consulta los datos en la tabla de la gráfica.">
    ${bandas.map(([a,b,nivel])=>`<rect width="1000" y="${y(b)}" height="${y(a)-y(b)}" fill="var(--lv-${nivel}-soft)"/>`).join('')}
    ${sinDatos.map(([a,b])=>`<rect class="serie-hueco" x="${a/n*1000}" width="${(b-a+1)/n*1000}" height="300"/>`).join('')}
    ${[medio,alto].map(v=>`<line class="serie-umbral" x2="1000" y1="${y(v)}" y2="${y(v)}"/>`).join('')}
    <g class="serie-trazos">${seriesSvg}</g>
   </svg>
   <div class="serie-eje">${[0,.25,.5,.75,1].map(p=>`<span>${formatoEje.format(fuente.inicio+(fuente.fin-fuente.inicio)*p)}</span>`).join('')}</div>
  </div>
 </div>`;
}

function tablaSerie(series,nombres,mag,per){
 const letra=i=>series.length===1?'':' · '+(i===0?'A':'B');
 const s=series[0];
 return `<details class="serie-tabla">
  <summary>Ver datos de la gráfica</summary>
  <div class="serie-tabla-scroll" tabindex="0" aria-label="Datos de la gráfica, tabla desplazable"><table>
   <thead><tr><th scope="col">Fin del intervalo</th>${series.map((_,i)=>`<th scope="col">${esc(nombres[i])}${letra(i)} (${MAG[mag].u})</th>`).join('')}</tr></thead>
   <tbody>${s.observados.map((_,i)=>`<tr><th scope="row">${momentoSerie(s.inicio+(i+1)*s.paso,per)}</th>
    ${series.map(ds=>`<td>${ds.observados[i]!=null?fmtSerie(ds.observados[i],mag):ds.estimados[i]!=null?fmtSerie(ds.estimados[i],mag)+' <small>Estimado</small>':'Sin datos'}</td>`).join('')}
   </tr>`).join('')}</tbody>
  </table></div>
 </details>`;
}

function panelSerie(series,nombres,mag,per){
 const comparar=series.length===2;
 const hayDatos=series.some(s=>s.observados.some(v=>v!=null));
 return `<section class="serie-panel" aria-labelledby="serie-titulo">
  <div class="serie-panel-cab"><h2 id="serie-titulo">${MAG[mag].n} <small>${MAG[mag].u}</small></h2>
   ${comparar?`<div class="serie-comparacion">
    <span><i class="serie-muestra" aria-hidden="true"></i>A · ${esc(nombres[0])}</span>
    <span><i class="serie-muestra serie-b" aria-hidden="true"></i>B · ${esc(nombres[1])}</span>
   </div>`:''}
  </div>
  ${hayDatos?graficaSeries(series,mag,per):'<p class="note">No hay mediciones en este periodo.</p>'}
  <ul class="serie-leyenda" aria-label="Leyenda de la gráfica">
   ${Object.entries(NIV).map(([k,n])=>`<li><i class="mapa-punto gm-${k}" aria-hidden="true"></i>${n}</li>`).join('')}
   <li><i class="serie-muestra serie-estimada" aria-hidden="true"></i>Estimación</li>
   <li><i class="serie-muestra serie-ausente" aria-hidden="true"></i>Hueco sin datos</li>
  </ul>
  <p class="hint">Los huecos cortos con datos a ambos lados se estiman con una línea de puntos. Los cálculos usan solo las mediciones sin interpolar.</p>
  ${tablaSerie(series,nombres,mag,per)}
 </section>`;
}

function alertasSerie(serie,mag,per){
 const {alertas,picos}=serie;
 const m=MAG[mag];
 return `<section class="serie-panel serie-eventos" aria-labelledby="serie-eventos">
  <h2 id="serie-eventos">Alertas y picos</h2>
  <h3>Periodos en nivel ${alertas.length?NIV[alertas[0].nivel]:'Medio o Alto'}</h3>
  ${alertas.length?`<ul>${alertas.slice().reverse().map(a=>`<li><b>${m.n} · ${fmtSerie(a.max,mag)} ${m.u}</b><span>${momentoSerie(a.desde,per)} – ${momentoSerie(a.hasta,per)}</span></li>`).join('')}</ul>`
   :'<p class="hint">Ningún intervalo en nivel Medio o Alto.</p>'}
  <h3>Valores más altos</h3>
  ${picos.length?`<ul>${picos.map(p=>`<li><b>${m.n} · ${fmtSerie(p.valor,mag)} ${m.u}</b><span>${momentoSerie(p.momento,per)}</span></li>`).join('')}</ul>`
   :'<p class="hint">Sin picos registrados en este periodo.</p>'}
 </section>`;
}

function selectorSerie(id,titulo,clave,opciones,attrs=''){
 return `<div class="field"><label class="mapa-label" for="${id}">${titulo}</label><select class="input" id="${id}" data-bind="${clave}" data-rr="1" ${attrs}>${opciones}</select></div>`;
}
const opcionesSerie=(lista,valor)=>lista.map(([k,n])=>`<option value="${k}" ${valor===k?'selected':''}>${n}</option>`).join('');
const filtrosSerie=(lista,activo,accion,label)=>`<div class="field">
 <span class="mapa-label">${label}</span>
 <div class="chips mapa-chips" role="group" aria-label="${label}">${lista.map(([k,n])=>`<button type="button" class="chip" aria-pressed="${activo===k}" data-act="${accion}" data-arg="${k}">${n}</button>`).join('')}</div>
</div>`;

function vHist(){
 const h=S.hist,z=byZ(h.id);
 const rango=h.per==='custom'?h.rango:null;
 const datos=h.per==='custom'&&!rango?null:modeloSerie(z,h.mag,h.per,rango||undefined);
 const resumen=datos&&resumenSerie(datos),mag=MAG[h.mag];
 return header('historial')+`<main class="page wrap serie-pagina"><div class="inner">
  <nav class="crumb" aria-label="Ruta"><a href="#mapa">Mapa</a> <span aria-hidden="true">›</span> <span aria-current="page">Historial</span></nav>
  <div class="mapa-encabezado">
   <div><h1>${esc(z.n)}</h1><p>Historial de mediciones · ${TIPOS_ZONA[z.tipo][0]}</p></div>
   <button type="button" class="btn sec" data-act="toCmp" data-arg="${z.id}">${I.swap} Comparar con otra zona</button>
  </div>
  <section class="serie-filtros" aria-label="Filtros del historial">
   ${selectorSerie('h-id','Zona','hist.id',zonaOpts(h.id))}
   ${filtrosSerie(Object.entries(MAG).map(([k,m])=>[k,m.n]),h.mag,'hmag','Magnitud')}
   ${filtrosSerie([...PERIODOS_SERIE,['custom','Entre dos fechas']],h.per,'hper','Periodo')}
   ${h.per==='custom'?`<form id="f-rango" class="serie-rango" novalidate>
    <div class="field"><label for="h-desde">Desde (UTC)</label><input class="input" type="date" id="h-desde" data-bind="hist.desde" value="${esc(h.desde)}" ${inv('desde')}>${err('desde')}</div>
    <div class="field"><label for="h-hasta">Hasta (UTC)</label><input class="input" type="date" id="h-hasta" data-bind="hist.hasta" value="${esc(h.hasta)}" ${inv('hasta')}>${err('hasta')}</div>
    <button class="btn" type="submit">Aplicar rango</button>
    <span class="hint">Hasta 90 días. Los datos empiezan el 01/06/2026.</span>
   </form>`:''}
  </section>
  <div class="serie-contexto"><span class="mapa-simulacion">Mediciones simuladas</span>
   ${datos?`<span>${momentoSerie(datos.inicio,h.per)} – ${momentoSerie(datos.fin,h.per)}${h.per==='custom'?' (UTC)':''}</span>`:''}</div>
  ${datos?`
  <dl class="serie-resumen" aria-label="Resumen del periodo">
   ${[['Mínimo',resumen.min],['Media',resumen.media],['Máximo',resumen.max]].map(([etiqueta,v])=>`<div><dt>${etiqueta}</dt><dd>${fmtSerie(v,h.mag)} ${v==null?'':`<small>${mag.u}</small>`}</dd></div>`).join('')}
   <div><dt>Tiempo sin mediciones</dt><dd>${horasSerie(resumen.horasSinDatos)}</dd></div>
  </dl>
  <div class="serie-resultados">${panelSerie([datos],[z.n],h.mag,h.per)}${alertasSerie(datos,h.mag,h.per)}</div>`
  :'<p class="note" role="status">Elige las fechas y pulsa Aplicar rango para consultar el historial.</p>'}
 </div></main>`+footer();
}

function vCmp(){
 const c=S.cmp,A=byZ(c.a),B=byZ(c.b),iguales=c.a===c.b;
 let resultado='';
 if(!iguales){
  const reloj={ahora:Math.floor(Date.now()/60000)*60000};
  const datos=[modeloSerie(A,c.mag,c.per,reloj),modeloSerie(B,c.mag,c.per,reloj)];
  const resumenes=datos.map(resumenSerie),unidad=MAG[c.mag].u;
  resultado=`<div class="serie-resultados">${panelSerie(datos,[A.n,B.n],c.mag,c.per)}
   <section class="serie-panel" aria-labelledby="serie-res-comparacion">
    <h2 id="serie-res-comparacion">Resumen del periodo</h2>
    <table class="serie-contraste">
    <thead><tr><th scope="col">Indicador</th><th scope="col" abbr="${esc(A.n)}">A</th><th scope="col" abbr="${esc(B.n)}">B</th></tr></thead>
    <tbody>${[['Mínimo','min'],['Media','media'],['Máximo','max'],['Sin mediciones','horasSinDatos']].map(([label,k])=>`<tr><th scope="row">${label}</th>${resumenes.map(r=>`<td>${k==='horasSinDatos'?horasSerie(r[k]):r[k]==null?'—':fmtSerie(r[k],c.mag)+' '+unidad}</td>`).join('')}</tr>`).join('')}</tbody>
    </table>
    <p class="hint">El tiempo sin mediciones incluye los intervalos estimados.</p>
   </section>
  </div>`;
 }
 return header('comparar')+`<main class="page wrap serie-pagina"><div class="inner">
  <nav class="crumb" aria-label="Ruta"><a href="#mapa">Mapa</a> <span aria-hidden="true">›</span> <a href="#historial">Historial</a> <span aria-hidden="true">›</span> <span aria-current="page">Comparar</span></nav>
  <div class="mapa-encabezado"><div><h1>Comparar zonas</h1><p>Consulta la misma magnitud y el mismo periodo en dos lugares.</p></div></div>
  <section class="serie-filtros" aria-label="Filtros de la comparación">
   ${selectorSerie('c-a','Zona A · línea continua','cmp.a',zonaOpts(c.a))}
   ${selectorSerie('c-b','Zona B · línea discontinua','cmp.b',zonaOpts(c.b),iguales?'aria-invalid="true" aria-describedby="comparar-error"':'')}
   ${selectorSerie('c-m','Magnitud','cmp.mag',opcionesSerie(Object.entries(MAG).map(([k,m])=>[k,m.n]),c.mag))}
   ${selectorSerie('c-p','Periodo','cmp.per',opcionesSerie(PERIODOS_SERIE,c.per))}
   <button type="button" class="btn sec" data-act="swap">${I.swap} Intercambiar</button>
  </section>
  <div class="serie-contexto"><span class="mapa-simulacion">Mediciones simuladas</span></div>
  ${iguales?`<div class="alert warn" id="comparar-error" role="alert">${I.alert}<span>Has elegido la misma zona dos veces. Cambia la zona B para poder comparar.</span></div>`:resultado}
 </div></main>`+footer();
}

/* ---------- portada ---------- */
const AYTOS=[{id:'gandia',n:'Gandia',zona:'La Safor · Comunitat Valenciana',desde:'junio 2026',activo:true}];
const TV_LINKS=[['mapa','Mapa'],['comunidad','Comunidad'],['como-funciona','Cómo funciona'],['privacidad','Privacidad']];
const ORDEN_NIVEL={bajo:0,medio:1,alto:2};
function mediaCosta(m){
 const valores=ZONAS.filter(z=>est(z)==='ok'&&z[m]!=null).map(z=>z[m]);
 return valores.length?valores.reduce((a,b)=>a+b,0)/valores.length:null;
}
function escalaPos(k,v){
 const [bajo,alto]=LIM[k];
 const tramo=v<bajo?v/bajo:v<alto?1+(v-bajo)/(alto-bajo):2+Math.min(1,(v-alto)/(alto-bajo));
 return Math.max(1,Math.min(99,tramo/3*100));
}
function puntuacionParque(z){
 const magnitudes=Object.keys(MAG).filter(k=>z[k]!=null);
 return magnitudes.reduce((suma,k)=>suma+z[k]/LIM[k][0],0)/magnitudes.length;
}
function siluetaZona(z){
 const anillos=(z.geometry.type==='Polygon'?[z.geometry.coordinates]:z.geometry.coordinates).flat();
 const cosLat=Math.cos(z.ancla[1]*Math.PI/180);
 const puntos=anillos.map(r=>r.map(([lng,lat])=>[(lng-z.ancla[0])*cosLat,-(lat-z.ancla[1])]));
 const xs=puntos.flat().map(p=>p[0]),ys=puntos.flat().map(p=>p[1]);
 const minX=Math.min(...xs),minY=Math.min(...ys),ancho=Math.max(...xs)-minX,alto=Math.max(...ys)-minY;
 const escala=Math.min(860/ancho,460/alto),x=(1000-ancho*escala)/2,y=(560-alto*escala)/2;
 return puntos.map(r=>'M'+r.map(([lng,lat])=>((lng-minX)*escala+x).toFixed(1)+' '+((lat-minY)*escala+y).toFixed(1)).join('L')+'Z').join(' ');
}
const TV_BURGER=open=>`<svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true"><path d="${open?'M18 6 6 18M6 6l12 12':'M3 6h18M3 12h18M3 18h18'}" style="fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round"/></svg>`;
const iconoParque=pausa=>sv(pausa?'<path d="m8 5 12 7-12 7z"/>':'<path d="M9 6v12m6-12v12"/>',18);
const etiquetaParque=pausa=>pausa?'Reanudar animación del parque':'Pausar animación del parque';
function vInicio(){
 const origen='Datos simulados del Grau i Platja';
 const pausa=TV.pausaEn!==null;
 const aytosVerificados=window.RespiraDatos?.ayuntamientosVerificados();
 const MED=[['o3','Ozono','Se forma con el sol y el tráfico. Irrita ojos y vías respiratorias, sobre todo las tardes de verano.'],['temp','Temperatura','Las calles sin sombra acumulan calor. Medirla por zonas muestra dónde faltan árboles y refugios climáticos.'],['ruido','Ruido','El tráfico y el ocio nocturno afectan al descanso y a la salud. Se mide en decibelios.']];
 const PORT=[['Personal municipal','Las brigadas de limpieza y de jardines lo llevan en su ruta de trabajo.'],['Bicis y motos compartidas','El vehículo envía datos mientras circula.'],['Autobuses urbanos','Cubren las avenidas principales todo el día.'],['Vecinos voluntarios','Recogen un sensor en un punto de reparto, pasean y ganan puntos.']];
 const parques=ZONAS.filter(z=>z.tipo==='parque'&&est(z)==='ok'&&nivelGeneral(z)!=='none')
  .sort((a,b)=>ORDEN_NIVEL[nivelGeneral(a)]-ORDEN_NIVEL[nivelGeneral(b)]||puntuacionParque(a)-puntuacionParque(b)||a.n.localeCompare(b.n,'es'));
 const links=TV_LINKS.map(([h,n])=>`<a class="tv-link" href="#${h}">${n}</a>`).join('');
 return `<div class="tv${S.iniVisto?' tv-sin-entrada':''}" id="inicio">
 <section class="tv-hero" id="tv-root" aria-label="Respira">
  <div class="tv-parallax" id="tv-parallax">
   <canvas class="tv-media" id="tv-parque" data-tv-anim aria-hidden="true"></canvas>
  </div>
  <div class="tv-scrim" data-tv-anim></div>
  <div class="tv-col">
   <nav class="tv-nav" data-tv-anim aria-label="Principal">
    <a class="tv-marca" href="#inicio" aria-label="Respira, inicio"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">${SYM()}</svg>Respira</a>
    <div class="tv-pill" data-tv-desktop>${links}</div>
    ${botonTema(true)}
    <button type="button" class="tv-burger" id="tv-burger" data-tv-mobile aria-controls="tv-mobile-menu" aria-expanded="false" aria-label="Abrir menú">${TV_BURGER(false)}</button>
    <div class="tv-auth" data-tv-desktop>${botonTema(false)}<a class="btn sm sec" href="#acceso">Acceso ayuntamiento</a></div>
   </nav>
   <div id="tv-mobile-menu" class="tv-panel" data-tv-mobile inert>${links}<div class="tv-panel-pie"><a class="btn sm sec" href="#acceso">Ayuntamiento</a></div></div>
   <main class="tv-main">
    <p class="tv-proposito">Calidad ambiental urbana</p>
    <h1 class="tv-h1">El aire de tus parques, medido calle a calle</h1>
    <ul class="tv-magnitudes" aria-label="Qué medimos">${Object.entries(MAG).map(([k,m])=>`<li>${sv(EF_ICO[k],19,1.9)}${m.n}</li>`).join('')}</ul>
    <p class="tv-p">Ozono, temperatura y ruido de cada parque y barrio en la última hora, medidos por vecinos con sensores. Demostración con datos simulados.</p>
    <div class="tv-ctas"><a class="btn tv-cta" href="#mapa">${I.mapi} Ver el mapa</a><a class="tv-voluntario" href="#voluntariado">Hazte voluntario</a></div>
   </main>
   <div class="tv-explorar">
    <button type="button" class="tv-pausa" data-act="pausaParque" aria-label="${etiquetaParque(pausa)}" title="${etiquetaParque(pausa)}">${iconoParque(pausa)}</button>
    <a href="#parques" aria-label="Bajar a la sección de parques">${sv('<path d="m6 9 6 6 6-6"/>',20)}</a>
   </div>
  </div>
 </section>

 <section class="tv-sec" id="parques" aria-labelledby="portada-parques"><div class="tv-in">
  <div class="tv-head rv">
   <h2 id="portada-parques">Los parques ahora, de mejor a peor</h2>
   <p>Ordenados por su estado general en el mapa. La etiqueta de nivel resume el ozono, la temperatura y el ruido de cada parque.</p>
   <p class="tv-datos" role="status">${origen}</p>
  </div>
  <ul class="pq-leyenda" aria-label="Leyenda de niveles">
   ${Object.entries(NIV).map(([clave,nombre])=>`<li><i class="pq-punto pq-punto-${clave}" aria-hidden="true"></i>${nombre}</li>`).join('')}
   <li class="pq-umbrales"><a class="pq-todos" href="#colores">Ver umbrales por medición</a></li>
  </ul>
  ${parques.length?'':'<p class="note">Ahora mismo ningún parque tiene datos de la última hora.</p>'}
  <div class="pq-fila">${parques.map(z=>{
   const nivelZona=nivelGeneral(z);
   return `<button type="button" class="pq" data-act="verZona" data-arg="${esc(z.id)}">
   <span class="pq-cab"><b>${esc(z.n)}</b><span class="badge b-${nivelZona}">Nivel ${NIV[nivelZona].toLowerCase()}</span></span>
   <svg class="pq-sil" viewBox="0 0 1000 560" preserveAspectRatio="xMidYMid meet" aria-hidden="true"><path d="${esc(siluetaZona(z))}" class="pq-g-${nivelZona}"/></svg>
   <span class="pq-vals">${Object.keys(MAG).map(k=>`<span><small>${MAG[k].n}</small>${z[k]==null?'<span>Sin datos</span>':`<b class="pq-v-${nivel(k,z[k])}">${fmt(z[k],k)}</b> <small>${MAG[k].u}</small>`}</span>`).join('')}</span>
   <span class="pq-nota">Media simulada de la última hora</span>
  </button>`}).join('')}</div>
  <p class="tv-parques-enlace"><a class="pq-todos" href="#mapa">Ver todos en el mapa</a></p>
 </div></section>

 <section class="tv-sec tv-alt" id="colores"><div class="tv-in">
  <div class="tv-head rv"><h2>Qué significa cada color</h2><p>Los mismos límites en la web, la app y el panel municipal. Elige qué quieres consultar.</p></div>
  <div class="ic-tabs rv" role="tablist" aria-label="Qué se mide">${EF_B.map(e=>`<button type="button" role="tab" id="ict-${e.k}" aria-controls="icp-${e.k}" aria-selected="${S.tabIni===e.k}" tabindex="${S.tabIni===e.k?0:-1}" data-act="tabIni" data-arg="${e.k}">${e.n}</button>`).join('')}</div>
  ${EF_B.map(e=>{const k=e.k,v=mediaCosta(k),l=LIM[k],txt=(MED.find(m=>m[0]===k)||[])[2]||'';return `<div class="ic-panel" role="tabpanel" id="icp-${k}" aria-labelledby="ict-${k}"${S.tabIni===k?'':' hidden'}>
   <div><h3>${e.lema}</h3><p class="tv-mut">${txt}</p>
    <div class="ic-escala" aria-hidden="true"><span class="b"></span><span class="m"></span><span class="a"></span>${v!=null?`<i style="left:${escalaPos(k,v).toFixed(1)}%"></i>`:''}</div>
    <div class="ic-et"><span><span class="badge b-bajo">Bajo</span><small>menos de ${fmt(l[0],k)} ${MAG[k].u}</small></span><span><span class="badge b-medio">Medio</span><small>de ${fmt(l[0],k)} a menos de ${fmt(l[1],k)} ${MAG[k].u}</small></span><span><span class="badge b-alto">Alto</span><small>desde ${fmt(l[1],k)} ${MAG[k].u}</small></span></div>
    <p class="ic-ahora">${v!=null?`Media simulada del Grau i Platja: <b class="mono">${fmt(v,k)} ${MAG[k].u}</b>, nivel ${NIV[nivel(k,v)].toLowerCase()}.`:'Sin valores simulados.'}</p></div>
   <div><ol class="ic-pasos">${e.pasos.map(([nv,val,t,x])=>`<li class="${nv}"><b>${EF_NV[nv]}</b><span>${x}</span><small class="mono">${val}${t?', '+t:''}</small></li>`).join('')}</ol>
    <p class="ic-hacer"><b>Qué hacer:</b> ${e.hacer}.</p><p class="ic-hacer"><b>Más sensibles:</b> ${e.peor.join(', ').toLowerCase()}.</p></div></div>`}).join('')}
  <p class="tv-nota">${EF_AVISO_B} <a class="pq-todos" href="#valores">Guía completa en «Cómo funciona»</a>.</p>
 </div></section>

 <section class="tv-sec"><div class="tv-in tv-dos">
  <div class="tv-head rv"><h2>Sensores que se mueven con la ciudad</h2><p>En lugar de pocas estaciones fijas, muchos sensores pequeños recorren las calles. Cada medición llega con su hora y su ubicación, y el servidor calcula la media de cada zona.</p>
   <p class="tv-priv"><b>Nunca se publica el recorrido de nadie.</b> En el mapa de sensores se muestran medias por zona.</p></div>
  <dl class="tv-port">${PORT.map(([titulo,descripcion])=>`<div><dt>${titulo}</dt><dd>${descripcion}</dd></div>`).join('')}</dl>
 </div></section>

 <section class="tv-sec tv-alt" id="ayuntamientos"><div class="tv-in">
  <div class="tv-head rv"><h2>Ayuntamientos con Respira</h2><p>Cada municipio tiene su propio mapa, sus puntos de reparto y su panel de gestión.</p><p class="tv-datos">${origen}</p></div>
  <div class="tv-aytos">${AYTOS.map(a=>`<article class="tv-ayto rv">
   <div class="tv-ayto-top"><div><h3>${esc(a.n)}</h3><span class="tv-mut">${esc(a.zona)}</span></div>${!aytosVerificados?'<span class="tv-mut">Demostración</span>':a.activo?'<span class="tv-estado"><i></i>Activo</span>':''}</div>
   ${a.id==='gandia'?`<dl class="tv-dl"><div><dt>Sensores en la demo</dt><dd>${SENSORES_DEMO_ACTIVOS}</dd></div><div><dt>Zonas en el mapa</dt><dd>${ZONAS.length}</dd></div><div><dt>Puntos de reparto</dt><dd>${PUNTOS_REPARTO.length}</dd></div><div><dt>${aytosVerificados?'Datos desde':'Inicio del escenario'}</dt><dd>${esc(a.desde)}</dd></div></dl>
   <div class="tv-medias">${Object.keys(MAG).map(k=>{const v=mediaCosta(k),l=nivel(k,v);return `<div><span>${k==='temp'?'Temperatura media':MAG[k].n+' medio'}</span><b>${fmt(v,k)} <small>${MAG[k].u}</small></b>${l==='none'?'':`<em class="tv-nv tv-${l}">${NIV[l]}</em>`}</div>`}).join('')}</div>
   <div class="tv-fila"><a class="btn" href="#mapa">Ver el mapa</a><a class="btn sec" href="#voluntariado">Puntos de reparto</a></div>`:''}</article>`).join('')}
   <article class="tv-ayto tv-nuevo rv" style="--i:1"><h3>¿Tu ayuntamiento?</h3><p class="tv-mut">Respira se pone en marcha con sensores, puntos de reparto, la app para los vecinos y un panel para el personal municipal.</p><a class="btn sec" href="#para-ayuntamientos">Qué incluye</a></article>
  </div>
 </div></section>

 <section class="tv-sec" id="para-ayuntamientos"><div class="tv-in tv-ayu">
  <div class="rv"><span class="tv-eyebrow">Para ayuntamientos</span><h2>Un panel para gestionar todo el servicio</h2><p class="tv-mut">El personal municipal entra con su cuenta y ve solo lo que le corresponde según su rol: administración, técnico o punto de reparto.</p><div class="tv-fila"><a class="btn" href="#acceso">Acceso ayuntamiento</a></div></div>
  <ul class="tv-lista rv" style="--i:1">${[['Flota de sensores','Estado, batería y último dato de cada sensor.'],['Préstamos','Prestar y recoger sensores en los puntos de reparto.'],['Puntos','Reglas de cálculo y revisión de días sospechosos.'],['Regalos y canjes','Catálogo, stock y entrega con código CJ-XXXX.']].map(x=>`<li><b>${x[0]}</b><span class="tv-mut">${x[1]}</span></li>`).join('')}</ul>
 </div></section>

 <footer class="tv-pie"><div class="tv-in"><span>Respira es un servicio de <b>NODIA</b></span><span>Proyecto de Biometría, GTI, UPV. <a href="#privacidad">Privacidad</a></span></div></footer>
</div>`}

/* ---------- comportamiento de la portada ---------- */
/* ---------- tema: fondo blanco o modo oscuro (recordado en este navegador) ---------- */
let TEMA='claro';try{const g=localStorage.getItem('respira-tema');if(g==='claro'||g==='oscuro')TEMA=g}catch(_){}
const IC_LUNA='<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/></svg>';
const IC_SOL='<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/></svg>';
const botonTema=movil=>`<button type="button" class="tv-tema${movil?' movil':''}" ${movil?'data-tv-mobile':''} data-act="tema" aria-pressed="${TEMA==='oscuro'}" aria-label="${TEMA==='oscuro'?'Cambiar a fondo blanco':'Cambiar a modo oscuro'}" title="${TEMA==='oscuro'?'Fondo blanco':'Modo oscuro'}">${TEMA==='oscuro'?IC_SOL:IC_LUNA}</button>`;
function aplicarTema(){document.documentElement.setAttribute('data-tema',TEMA)}
const TV={inicioEscena:null,pausaEn:null,raf:null,target:{x:0,y:0},cur:{x:0,y:0},globo:null,limpiar:null};
function montarInicio(){
 if(TV.limpiar)TV.limpiar();
 const root=document.getElementById('tv-root'),par=document.getElementById('tv-parallax');
 if(!root)return;
 const off=[];const on=(el,ev,fn,o)=>{el.addEventListener(ev,fn,o);off.push(()=>el.removeEventListener(ev,fn,o))};
 const barra=root.querySelector('.tv-nav');
 const actualizarFondo=()=>barra.classList.toggle('con-fondo',window.scrollY>0);
 on(window,'scroll',actualizarFondo,{passive:true});
 actualizarFondo();
 // paralaje con el puntero (se para solo)
 if(!matchMedia('(prefers-reduced-motion: reduce)').matches){
  const tick=()=>{const c=TV.cur,t=TV.target;c.x+=(t.x-c.x)*0.08;c.y+=(t.y-c.y)*0.08;par.style.transform='translate3d('+c.x.toFixed(2)+'px,'+c.y.toFixed(2)+'px,0)';
   TV.raf=(Math.abs(t.x-c.x)>0.05||Math.abs(t.y-c.y)>0.05)?requestAnimationFrame(tick):null};
  on(window,'pointermove',e=>{if(TV.pausaEn!==null)return;const lim=(v,m)=>Math.max(-m,Math.min(m,v));TV.target.x=lim((e.clientX/innerWidth-0.5)*28,14);TV.target.y=lim((e.clientY/innerHeight-0.5)*18,9);if(!TV.raf)TV.raf=requestAnimationFrame(tick)},{passive:true});
 }
 // parque animado (canvas)
 TV.inicioEscena??=performance.now();
 TV.globo=parqueEscena(document.getElementById('tv-parque'),TEMA==='claro');
 // siluetas de los parques: encuadrar cada forma en su tarjeta
 root.parentNode.querySelectorAll('.pq-sil path').forEach(p=>{try{const b=p.getBBox(),m=Math.max(b.width,b.height)*.08;p.parentNode.setAttribute('viewBox',`${(b.x-m).toFixed(1)} ${(b.y-m).toFixed(1)} ${(b.width+2*m).toFixed(1)} ${(b.height+2*m).toFixed(1)}`)}catch(_){}});
 // entrada escalonada al bajar: solo la primera vez que se ve la portada (no al repintar)
 const tvr=root.parentNode;let io=null;
 if(S.iniVisto||!window.IntersectionObserver)tvr.classList.add('quieto');
 else{S.iniVisto=true;io=new IntersectionObserver(es=>es.forEach(x=>{if(x.isIntersecting){x.target.classList.add('in');io.unobserve(x.target)}}),{threshold:.12});tvr.querySelectorAll('.rv').forEach(el=>io.observe(el))}
 TV.limpiar=()=>{off.forEach(f=>f());io&&io.disconnect();if(TV.raf)cancelAnimationFrame(TV.raf);TV.raf=null;TV.globo&&TV.globo.parar();TV.globo=null;TV.limpiar=null;document.body.style.backgroundColor=''};
}

/* Un parque de Gandia a la hora dorada (o de día con el diseño claro).
   Canvas 2D, sin imágenes externas. Para que vaya fluido, el cielo, el
   suelo y cada árbol se dibujan UNA vez en lienzos aparte (sprites) y en
   cada fotograma solo se mueven: los árboles se mecen, el lago ondula y
   refleja el cielo y los árboles, nadan dos patos, pasan pájaros, el
   voluntario pasea con su sensor y un corredor da la vuelta al lago. */
const PARQUE_PAL={
 tarde:{cielo:['#0b1631','#22325a','#5f5677','#d8876a','#f6c27e'],sol:[.8,-.035],solCol:'255,214,150',solDisco:true,nubes:['#f3b88a','#7d6a86'],monte:'#4b4b6b',monte2:'#5f5a78',ciudad:'#3b3753',ventanas:true,
  lejos:['#46584f','#55665a'],hoja:['#2e5a2c','#3b6a32','#4a7a39','#5b8a40','#6d9a46'],brillo:'#e7c46a',sombraHoja:'#16301c',tronco:'#3a2b22',cesped:['#7a8e45','#4f7232','#2d4a22'],hierba:['#557a32','#6f9440','#3d6228','#8aa74c'],
  camino:['#e3c19a','#b99777','#8f765f'],agua:['#f2c48c','#d99d82','#3f8197','#1d5c74'],aguaClara:'110,214,204',orilla:'#c9b38d',flores:['#ffd34d','#ff7aa8','#ffffff','#b48cff','#ff8f4d'],farola:true,persona:'#1d2328',bruma:'246,194,126',polen:'255,226,160'},
 dia:{cielo:['#2f7fc4','#5aa3dc','#93c8ea','#cfe7f2','#eef6f1'],sol:[.82,-.55],solCol:'255,250,228',solDisco:false,nubes:['#ffffff','#c9d9e6'],monte:'#8aa2b6',monte2:'#9fb4c4',ciudad:'#a8bccb',ventanas:false,
  lejos:['#7fa48a','#93b49b'],hoja:['#3e7a37','#4b8a40','#5a9a49','#6ca853','#80b85e'],brillo:'#d9ef9a',sombraHoja:'#244a22',tronco:'#5a4432',cesped:['#9ccc6e','#6fa850','#4f8a3c'],hierba:['#6aa84a','#86bf5c','#4f8f3a','#9fd06f'],
  camino:['#efe5d3','#ddcfb8','#c3b297'],agua:['#cfe9f3','#9fd3ea','#4aa3c8','#2f7ea6'],aguaClara:'120,225,215',orilla:'#e3d3b2',flores:['#ffd34d','#ff6f9c','#ffffff','#9b7bff','#ff8f4d'],farola:false,persona:'#283844',bruma:'238,246,241',polen:'255,255,255'}};
function parqueEscena(cv,dia){
 const P=dia?PARQUE_PAL.dia:PARQUE_PAL.tarde;
 const ctx=cv.getContext('2d');if(!ctx)return {parar(){}};
 const quieto=matchMedia('(prefers-reduced-motion: reduce)').matches;
 let sem=7;const rnd=()=>{sem=(sem*16807)%2147483647;return (sem-1)/2147483646};const entre=(a,b)=>a+rnd()*(b-a);const elige=a=>a[Math.floor(rnd()*a.length)];
 let w=0,h=0,dpr=1,E=null,id=null,vivo=true,visible=true;
 const animar=()=>vivo&&!quieto&&visible&&TV.pausaEn===null;
 const lienzo=(cw,ch)=>{const c=document.createElement('canvas');c.width=Math.max(1,Math.ceil(cw*dpr));c.height=Math.max(1,Math.ceil(ch*dpr));const x=c.getContext('2d');x.setTransform(dpr,0,0,dpr,0,0);return [c,x]};
 const mezcla=(a,b,k)=>{const p=s=>[1,3,5].map(i=>parseInt(s.slice(i,i+2),16));const A=p(a),B=p(b);return 'rgb('+A.map((v,i)=>Math.round(v+(B[i]-v)*k)).join(',')+')'};

 // ---------- árboles (sprites) ----------
 function arbolRedondo(alto,bruma){
  const R=alto*.34,cw=R*2.6,ch=alto*1.05;const [c,x]=lienzo(cw,ch);const bx=cw/2,by=ch-2,cy=by-alto+R*1.05;
  const tr=bruma?mezcla(P.tronco,P.lejos[0],bruma):P.tronco;
  x.strokeStyle=tr;x.lineCap='round';x.lineWidth=Math.max(1.5,alto*.045);x.beginPath();x.moveTo(bx,by);x.quadraticCurveTo(bx-alto*.02,by-alto*.3,bx+alto*.01,cy+R*.2);x.stroke();
  x.lineWidth=Math.max(1,alto*.02);[[-.5,-.4],[.45,-.5],[-.2,-.75]].forEach(([dx,dy])=>{x.beginPath();x.moveTo(bx,cy+R*.5);x.quadraticCurveTo(bx+dx*R*.3,cy+R*.2,bx+dx*R*.8,cy+dy*R*.6);x.stroke()});
  const lx=.62,ly=-.55;const n=Math.round(70+alto*.4);
  for(let i=0;i<n;i++){const a=rnd()*Math.PI*2,d=Math.sqrt(rnd())*.92;const px=Math.cos(a)*d,py=Math.sin(a)*d*.86;const lado=(px*lx+py*ly);
   let col=elige(P.hoja);if(lado>.25)col=mezcla(col,P.brillo,Math.min(.55,(lado-.25)*.8));if(lado<-.3)col=mezcla(col,P.sombraHoja,Math.min(.6,(-lado-.3)));
   if(bruma)col=mezcla('#4a6a4a',P.lejos[1],bruma*.9);
   x.fillStyle=col;x.beginPath();x.arc(bx+px*R,cy+py*R,R*entre(.13,.26),0,7);x.fill()}
  if(!bruma){for(let i=0;i<Math.round(alto*2);i++){const a=rnd()*Math.PI*2,d=Math.sqrt(rnd());const px=Math.cos(a)*d,py=Math.sin(a)*d*.86;if(px*lx+py*ly<.2)continue;x.fillStyle='rgba(255,240,190,'+entre(.15,.4)+')';x.fillRect(bx+px*R*.95,cy+py*R*.95,1.4,1.4)}}
  return {c,cw,ch,ox:bx,oy:by};
 }
 function arbolCipres(alto,bruma){
  const W=alto*.2,cw=W*1.6,ch=alto*1.04;const [c,x]=lienzo(cw,ch);const bx=cw/2,by=ch-2;
  x.fillStyle=P.tronco;x.fillRect(bx-alto*.012,by-alto*.08,alto*.024,alto*.08);
  for(let i=0;i<Math.round(60+alto*.5);i++){const t=rnd();const y=by-alto*.06-t*alto*.92;const half=W*.5*Math.sin(Math.min(1,(1-t))*Math.PI*.62)*(t<.08?t/.08:1);const xx=bx+(rnd()*2-1)*half;
   const base=elige(P.hoja);let col=xx>bx+half*.3?mezcla(base,P.brillo,.3):mezcla(base,P.sombraHoja,.35);if(bruma)col=mezcla(base,P.lejos[1],bruma);
   x.fillStyle=col;x.beginPath();x.ellipse(xx,y,W*.16,W*.24,0,0,7);x.fill()}
  return {c,cw,ch,ox:bx,oy:by};
 }
 function palmera(alto,bruma){
  const cw=alto*1.05,ch=alto*1.06;const [c,x]=lienzo(cw,ch);const bx=cw*.5,by=ch-2;const curva=entre(-.12,.12)*alto;const tx=bx+curva,ty=by-alto*.86;
  x.lineCap='round';
  for(let i=0;i<24;i++){const k=i/24,k2=(i+1)/24;const ax=bx+curva*k*k,ay=by-(by-ty)*k,bx2=bx+curva*k2*k2,by2=by-(by-ty)*k2;x.strokeStyle=i%2?mezcla(P.tronco,'#9a8060',.35):mezcla(P.tronco,'#7a6048',.2);x.lineWidth=alto*(.045-.015*k);x.beginPath();x.moveTo(ax,ay);x.lineTo(bx2,by2);x.stroke()}
  const hojas=11;for(let i=0;i<hojas;i++){const ang=-Math.PI/2+(i/(hojas-1)-.5)*Math.PI*1.55+entre(-.08,.08);const L=alto*entre(.36,.46);
   const ex=tx+Math.cos(ang)*L,ey=ty+Math.sin(ang)*L*.55+L*.38*Math.abs(Math.cos(ang));const mx=tx+Math.cos(ang)*L*.55,my=ty+Math.sin(ang)*L*.5-L*.08;
   const col=mezcla(elige(P.hoja),P.brillo,Math.cos(ang)>0?.3:0);
   for(let s=0;s<=26;s++){const u=s/26;const qx=(1-u)*(1-u)*tx+2*(1-u)*u*mx+u*u*ex,qy=(1-u)*(1-u)*ty+2*(1-u)*u*my+u*u*ey;const dx=2*(1-u)*(mx-tx)+2*u*(ex-mx),dy=2*(1-u)*(my-ty)+2*u*(ey-my),l=Math.hypot(dx,dy)||1;const nx=-dy/l,ny=dx/l;const len=L*.16*Math.sin(u*Math.PI)+1;
    x.strokeStyle=bruma?mezcla(col.startsWith('rgb')?'#4a7a3a':col,P.lejos[1],bruma):col;x.lineWidth=Math.max(.8,alto*.008);x.beginPath();x.moveTo(qx,qy);x.lineTo(qx+nx*len+dx/l*len*.4,qy+ny*len+dy/l*len*.4+len*.3);x.moveTo(qx,qy);x.lineTo(qx-nx*len+dx/l*len*.4,qy-ny*len+dy/l*len*.4+len*.3);x.stroke()}}
  x.fillStyle='#6b4e2e';x.beginPath();x.arc(tx,ty+alto*.02,alto*.03,0,7);x.fill();
  return {c,cw,ch,ox:bx,oy:by};
 }
 const nuevoArbol=(tipo,alto,bruma)=>tipo==='palmera'?palmera(alto,bruma):tipo==='cipres'?arbolCipres(alto,bruma):arbolRedondo(alto,bruma);

 // ---------- preparar la escena (en cada cambio de tamaño) ----------
 function preparar(){
  dpr=Math.min(2,devicePixelRatio||1);w=cv.clientWidth;h=cv.clientHeight;if(!w||!h)return false;cv.width=Math.round(w*dpr);cv.height=Math.round(h*dpr);sem=11;
  const hz=h*.6,ancho=w<700;
  // lago (forma orgánica), a la derecha para dejar libre el texto
  const L={cx:w*(ancho?.75:.7),cy:hz+h*(ancho?.17:.14),rx:w*(ancho?.18:.17),ry:h*(ancho?.06:.062)};L.top=L.cy-L.ry;
  const lagoPath=new Path2D();for(let i=0;i<=48;i++){const a=i/48*Math.PI*2;const r=1+.08*Math.sin(a*3+1)+.05*Math.cos(a*5);const px=L.cx+Math.cos(a)*L.rx*r,py=L.cy+Math.sin(a)*L.ry*r*(Math.sin(a)<0?.85:1);i?lagoPath.lineTo(px,py):lagoPath.moveTo(px,py)}lagoPath.closePath();
  // camino: de abajo a la izquierda hacia el horizonte, rodeando el lago
  const camino=u=>{const x0=w*(ancho?.18:.3),x1=w*(ancho?.34:.5),y0=h+12,y1=hz+h*.012;const x=x0+(x1-x0)*(1-Math.pow(1-u,1.7))-Math.sin(u*Math.PI)*w*.04;return {x,y:y0+(y1-y0)*u,ancho:(1-u)*w*(ancho?.2:.12)+w*.005}};
  // ---- cielo, sol, nubes lejanas, monte (Mondúver), ciudad y bruma ----
  const [cielo,cx]=lienzo(w,hz+4);
  const g=cx.createLinearGradient(0,0,0,hz);[0,.4,.72,.9,1].forEach((k,i)=>g.addColorStop(k,P.cielo[i]));cx.fillStyle=g;cx.fillRect(0,0,w,hz+4);
  const sx=w*P.sol[0],sy=hz+h*P.sol[1];const sg=cx.createRadialGradient(sx,sy,0,sx,sy,Math.max(w,h)*.55);sg.addColorStop(0,`rgba(${P.solCol},.75)`);sg.addColorStop(.12,`rgba(${P.solCol},.35)`);sg.addColorStop(1,`rgba(${P.solCol},0)`);cx.fillStyle=sg;cx.fillRect(0,0,w,hz+4);
  if(P.solDisco){cx.fillStyle='rgba(255,236,190,.95)';cx.beginPath();cx.arc(sx,sy,h*.032,0,7);cx.fill()}
  const monte=(base,amp,col,fase)=>{cx.fillStyle=col;cx.beginPath();cx.moveTo(0,hz);for(let x=0;x<=w;x+=8){const k=x/w;const y=base-amp*(Math.exp(-Math.pow((k-.22-fase)/.16,2))*1+.35*Math.exp(-Math.pow((k-.5)/.25,2))+.25*Math.sin(k*9+fase*20)*.3);cx.lineTo(x,y)}cx.lineTo(w,hz);cx.closePath();cx.fill()};
  monte(hz-h*.01,h*.12,P.monte2,0);monte(hz,h*.07,P.monte,.15);
  cx.fillStyle=P.ciudad;
  for(let x=-10;x<w+10;){
   const bw=entre(10,30),bh=h*entre(.012,.05);
   cx.fillRect(x,hz-bh,bw,bh+3);
   if(P.ventanas){
    // Las ventanas nocturnas no cambian la posición de los demás elementos.
    const semEdificio=sem;
    cx.fillStyle='rgba(255,210,150,.85)';
    for(let k=0;k<3;k++){
     if(rnd()<.4)cx.fillRect(x+entre(2,bw-4),hz-bh+entre(2,bh-4),2,2.5);
    }
    cx.fillStyle=P.ciudad;
    sem=semEdificio;
   }
   x+=bw+entre(0,4);
  }
  const tx=w*.58,th=h*.13;cx.fillRect(tx,hz-th,w*.018,th);cx.beginPath();cx.moveTo(tx-2,hz-th);cx.lineTo(tx+w*.009,hz-th-h*.04);cx.lineTo(tx+w*.018+2,hz-th);cx.fill();
  const bg=cx.createLinearGradient(0,hz-h*.12,0,hz+2);bg.addColorStop(0,`rgba(${P.bruma},0)`);bg.addColorStop(1,`rgba(${P.bruma},.55)`);cx.fillStyle=bg;cx.fillRect(0,hz-h*.12,w,h*.12+4);
  // ---- nubes (sprites que se desplazan) ----
  const nubes=[];for(let i=0;i<(ancho?3:5);i++){const nw=w*entre(.12,.24),nh=nw*.28;const [nc,nx]=lienzo(nw,nh);for(let k=0;k<14;k++){const ex=entre(.15,.85)*nw,ey=nh*entre(.45,.75),rx=nw*entre(.08,.18),ry=nh*entre(.25,.45);const gg=nx.createLinearGradient(0,ey-ry,0,ey+ry);gg.addColorStop(0,P.nubes[0]);gg.addColorStop(1,P.nubes[1]);nx.globalAlpha=.55;nx.fillStyle=gg;nx.beginPath();nx.ellipse(ex,ey,rx,ry,0,0,7);nx.fill()}
   nubes.push({c:nc,w:nw,h:nh,x:entre(-.1,1)*w,y:hz*entre(.12,.62),v:entre(3,7)})}
  // ---- árboles ----
  const fila=[];// lejanos: línea continua de copas
  const [lejosC,lx]=lienzo(w,h*.12+20);const lb=h*.12;for(let x=-20;x<w+20;x+=entre(8,16)){lx.fillStyle=rnd()<.5?P.lejos[0]:P.lejos[1];lx.beginPath();lx.arc(x,lb-entre(0,h*.02),entre(h*.012,h*.026),0,7);lx.fill()}lx.fillRect(-20,lb-4,w+40,24);
  for(let i=0;i<(ancho?10:22);i++){const tipo=rnd()<.18?'palmera':rnd()<.25?'cipres':'redondo';const alto=h*entre(.07,.12);const s=nuevoArbol(tipo,alto,.45);fila.push({s,x:entre(-.02,1.02)*w,y:hz+h*entre(.005,.025),f:rnd()*6,amp:.006,prof:.25})}
  const medios=[];const xs=ancho?[.04,.54,.72,.92,1.02]:[.53,.6,.79,.86,.93,1.0,1.06];
  xs.forEach(fx=>{const tipo=rnd()<.22?'palmera':rnd()<.2?'cipres':'redondo';const alto=h*(fx<.3&&!ancho?entre(.12,.16):entre(.17,.26));const s=nuevoArbol(tipo,alto,0);let y=hz+h*entre(.035,.07);if(Math.abs(fx*w-L.cx)<L.rx*1.05&&y>L.top-4)y=L.top-h*.004;medios.push({s,x:fx*w,y,f:rnd()*6,amp:tipo==='palmera'?.02:.012,prof:.6})});
  medios.sort((a,b)=>a.y-b.y);
  const cerca={s:nuevoArbol('redondo',h*(ancho?.38:.66),0),x:w*(ancho?1.12:.985),y:h*1.0,f:1,amp:.008,prof:1};
  const palmCerca=ancho?null:{s:palmera(h*.62,0),x:w*1.07,y:h*1.02,f:2.5,amp:.018,prof:1};
  // ---- suelo (estático): césped con textura, sombras, camino, orilla, flores ----
  const [suelo,gx]=lienzo(w,h-hz+4);gx.translate(0,-hz);
  const cg=gx.createLinearGradient(0,hz,0,h);cg.addColorStop(0,P.cesped[0]);cg.addColorStop(.35,P.cesped[1]);cg.addColorStop(1,P.cesped[2]);gx.fillStyle=cg;gx.fillRect(0,hz,w,h-hz+4);
  const luzG=gx.createRadialGradient(sx,hz,0,sx,hz,w*.7);luzG.addColorStop(0,`rgba(${P.solCol},.28)`);luzG.addColorStop(1,`rgba(${P.solCol},0)`);gx.fillStyle=luzG;gx.fillRect(0,hz,w,h-hz);
  for(let i=0;i<Math.round(w*h/260);i++){const y=hz+Math.pow(rnd(),1.4)*(h-hz),x=rnd()*w,k=(y-hz)/(h-hz);gx.strokeStyle=elige(P.hierba);gx.globalAlpha=.35+.4*k;gx.lineWidth=.6+k*1.2;const l=1.5+k*7;gx.beginPath();gx.moveTo(x,y);gx.lineTo(x+entre(-1.5,1.5),y-l);gx.stroke()}gx.globalAlpha=1;
  medios.concat([cerca]).forEach(t=>{const sw=t.s.cw*.5,shx=(t.x-sx)*.25;gx.fillStyle='rgba(20,30,15,.28)';gx.beginPath();gx.ellipse(t.x+shx*.3+sw*.15,t.y+2,sw*.9,Math.max(3,sw*.12),0,0,7);gx.fill()});
  // camino de grava
  const cp=new Path2D();const N=50;for(let i=0;i<=N;i++){const c=camino(i/N);i?cp.lineTo(c.x-c.ancho,c.y):cp.moveTo(c.x-c.ancho,c.y)}for(let i=N;i>=0;i--){const c=camino(i/N);cp.lineTo(c.x+c.ancho,c.y)}cp.closePath();
  const pg=gx.createLinearGradient(0,hz,0,h);pg.addColorStop(0,P.camino[0]);pg.addColorStop(.4,P.camino[1]);pg.addColorStop(1,P.camino[2]);gx.fillStyle=pg;gx.fill(cp);
  gx.save();gx.clip(cp);for(let i=0;i<Math.round(w*h/900);i++){const y=hz+rnd()*(h-hz),x=rnd()*w;gx.fillStyle=rnd()<.5?'rgba(255,255,255,.18)':'rgba(60,40,30,.18)';const s=.6+(y-hz)/(h-hz)*2;gx.fillRect(x,y,s,s)}gx.restore();
  gx.strokeStyle='rgba(60,80,40,.35)';gx.lineWidth=2;gx.stroke(cp);
  // orilla del lago
  gx.save();gx.lineWidth=h*.014;gx.strokeStyle=P.orilla;gx.stroke(lagoPath);gx.lineWidth=h*.005;gx.strokeStyle='rgba(60,50,40,.35)';gx.stroke(lagoPath);gx.restore();
  for(let i=0;i<40;i++){const a=rnd()*Math.PI*2;const px=L.cx+Math.cos(a)*L.rx*1.04,py=L.cy+Math.sin(a)*L.ry*1.08;gx.fillStyle=rnd()<.5?'#9c917f':'#bdb3a0';gx.beginPath();gx.ellipse(px,py,entre(1.5,3.5),entre(1,2),0,0,7);gx.fill()}
  // flores en primer plano
  for(let i=0;i<(ancho?140:320);i++){const y=hz+h*.12+Math.pow(rnd(),.7)*(h-hz-h*.12),x=rnd()*w;if(gx.isPointInPath(cp,x*dpr,(y-hz)*dpr))continue;const k=(y-hz)/(h-hz);gx.fillStyle=elige(P.flores);gx.globalAlpha=.85;gx.beginPath();gx.arc(x,y,.8+k*2.4,0,7);gx.fill()}gx.globalAlpha=1;
  // ---- reflejo del lago (estático; se ondula al pintarlo) ----
  const [refl,rx]=lienzo(w,h);rx.save();rx.clip(lagoPath);
  const ag=rx.createLinearGradient(0,L.top,0,L.cy+L.ry);ag.addColorStop(0,P.agua[0]);ag.addColorStop(.3,P.agua[1]);ag.addColorStop(.75,P.agua[2]);ag.addColorStop(1,P.agua[3]);rx.fillStyle=ag;rx.fillRect(0,L.top-4,w,L.ry*2.4);
  // En la composición estrecha los árboles quedan lejos de la orilla.
  if(!ancho){
   rx.globalAlpha=.42;
   rx.filter='blur(1.2px)';
   medios.forEach(t=>{if(Math.abs(t.x-L.cx)>L.rx*1.3)return;rx.save();rx.translate(t.x,2*L.top-t.y+4);rx.scale(1,-1);rx.drawImage(t.s.c,-t.s.ox,-t.s.oy,t.s.cw,t.s.ch);rx.restore()});
   rx.filter='none';
   rx.globalAlpha=1;
  }
  // agua cristalina: borde turquesa poco profundo, fondo de piedras que se transparenta
  rx.lineWidth=h*.035;rx.strokeStyle=`rgba(${P.aguaClara},.42)`;rx.stroke(lagoPath);rx.lineWidth=h*.012;rx.strokeStyle=`rgba(${P.aguaClara},.5)`;rx.stroke(lagoPath);
  for(let i=0;i<70;i++){const a=rnd()*Math.PI*2,d=entre(.78,.96);rx.fillStyle=`rgba(90,120,110,${entre(.12,.25)})`;rx.beginPath();rx.ellipse(L.cx+Math.cos(a)*L.rx*d,L.cy+Math.sin(a)*L.ry*d,entre(1.5,3.5),entre(1,2),0,0,7);rx.fill()}
  rx.restore();
  // juncos en las orillas
  const juncos=[];for(let i=0;i<34;i++){const lado=rnd()<.5?-1:1;const a=(lado<0?Math.PI:0)+entre(-.5,.5);juncos.push({x:L.cx+Math.cos(a)*L.rx*entre(.95,1.06),y:L.cy+Math.sin(a)*L.ry*entre(.9,1.05),l:h*entre(.02,.045),f:rnd()*6})}
  // personas, patos, pájaros, motas
  const motas=Array.from({length:ancho?40:80},()=>({x:rnd()*w,y:rnd()*h,r:entre(.6,1.8),v:entre(.1,.3),f:rnd()*6}));
  E={hz,L,lagoPath,camino,cielo,nubes,lejosC,lb,fila,medios,cerca,palmCerca,suelo,refl,juncos,motas,sx,sy,ancho};
  return true;
 }

 function sprite(t,tt,px){const s=t.s;const sw=quieto?0:Math.sin(tt*.7+t.f)*t.amp+Math.sin(tt*1.9+t.f*2)*t.amp*.3;ctx.save();ctx.translate(t.x+px*t.prof,t.y);ctx.transform(1,0,sw,1,0,0);ctx.drawImage(s.c,-s.ox,-s.oy,s.cw,s.ch);ctx.restore()}
 function persona(x,y,alto,tt,corre,sensor){const p=quieto?0:Math.sin(tt*(corre?9:5));ctx.fillStyle=P.persona;ctx.strokeStyle=P.persona;ctx.lineCap='round';
  ctx.beginPath();ctx.arc(x,y-alto*.9,alto*.11,0,7);ctx.fill();ctx.lineWidth=alto*.2;ctx.beginPath();ctx.moveTo(x,y-alto*.75);ctx.lineTo(x+(corre?alto*.06:0),y-alto*.42);ctx.stroke();
  ctx.lineWidth=alto*.09;ctx.beginPath();ctx.moveTo(x,y-alto*.42);ctx.lineTo(x+p*alto*(corre?.22:.12),y);ctx.moveTo(x,y-alto*.42);ctx.lineTo(x-p*alto*(corre?.22:.12),y);ctx.stroke();
  ctx.lineWidth=alto*.07;ctx.beginPath();ctx.moveTo(x,y-alto*.7);ctx.lineTo(x-p*alto*.14,y-alto*.48);ctx.moveTo(x,y-alto*.7);ctx.lineTo(x+p*alto*.14,y-alto*.48);ctx.stroke();
  if(sensor){const pul=quieto?.6:.5+.5*Math.sin(tt*3);const ex=x+alto*.13,ey=y-alto*.6;const ha=ctx.createRadialGradient(ex,ey,0,ex,ey,alto*.6);ha.addColorStop(0,`rgba(95,196,192,${.6*pul})`);ha.addColorStop(1,'rgba(95,196,192,0)');ctx.fillStyle=ha;ctx.fillRect(ex-alto*.6,ey-alto*.6,alto*1.2,alto*1.2);ctx.fillStyle='#8af0e6';ctx.beginPath();ctx.arc(ex,ey,Math.max(1.6,alto*.05),0,7);ctx.fill()}}

 function dibujar(){
  if(!E&&!preparar()){id=animar()?requestAnimationFrame(dibujar):null;return}
  const tt=((TV.pausaEn??performance.now())-TV.inicioEscena)/1000,px=TV.cur.x*.5,{hz,L}=E;
  ctx.setTransform(dpr,0,0,dpr,0,0);
  ctx.drawImage(E.cielo,0,0,w,hz+4);
  E.nubes.forEach(n=>{let x=n.x+(quieto?0:tt*n.v);x=((x+n.w)%(w+2*n.w))-n.w;ctx.drawImage(n.c,x+px*.1,n.y,n.w,n.h)});
  // pájaros (una bandada cruza de vez en cuando)
  if(!quieto){const ciclo=(tt%34)/34;if(ciclo<.6){const bx=-80+ciclo/.6*(w+160),by=hz*.3+Math.sin(ciclo*6)*20;ctx.strokeStyle='rgba(30,30,40,.7)';ctx.lineWidth=1.3;for(let k=0;k<7;k++){const ox=bx-Math.abs(k-3)*16,oy=by+Math.abs(k-3)*7+(k-3)*2,a=Math.sin(tt*9+k)*4;ctx.beginPath();ctx.moveTo(ox-6,oy-a);ctx.quadraticCurveTo(ox-3,oy-1,ox,oy);ctx.quadraticCurveTo(ox+3,oy-1,ox+6,oy-a);ctx.stroke()}}}
  ctx.drawImage(E.lejosC,px*.15,hz-E.lb,w,E.lejosC.height/dpr);
  E.fila.forEach(t=>sprite(t,tt,px));
  ctx.drawImage(E.suelo,px*.4,hz,w,E.suelo.height/dpr);
  // lago: reflejo ondulado en franjas
  ctx.save();ctx.translate(px*.4,0);ctx.clip(E.lagoPath);
  const top=Math.floor(L.top-2),bot=Math.ceil(L.cy+L.ry*1.1);for(let y=top;y<bot;y+=3){const off=quieto?0:Math.sin(tt*1.6+y*.35)*(1+(y-top)/(bot-top)*1.6);ctx.drawImage(E.refl,0,y*dpr,E.refl.width,3*dpr,off,y,w,3)}
  // brillos del sol y ondas
  for(let i=0;i<26;i++){const k=i/26;const y=L.top+L.ry*.2+k*L.ry*1.6;const x=E.sx+Math.sin(i*2.3)*L.rx*.25*k+ (quieto?0:Math.sin(tt*2+i)*6);const a=quieto?.35:.2+.3*Math.max(0,Math.sin(tt*3+i*1.7));ctx.fillStyle=`rgba(${P.solCol},${a})`;ctx.fillRect(x,y,4+k*10,1.2)}
  for(let i=0;i<9;i++){const fase=quieto?.5:((tt*.15+i/9)%1);const yy=L.top+L.ry*.3+i*(L.ry*1.4/9),ww=L.rx*(.2+.15*Math.abs(Math.sin(i)))*(.6+fase);ctx.strokeStyle=`rgba(255,255,255,${.22*(1-fase)})`;ctx.lineWidth=1;ctx.beginPath();ctx.ellipse(L.cx+Math.sin(i*1.7)*L.rx*.4,yy,ww,1.5,0,0,7);ctx.stroke()}
  // patos
  [[0,1],[.5,-1]].forEach(([desf,dir],k)=>{const u=quieto?.3+desf:((tt/40+desf)%1);const ang=u*Math.PI*2*dir;const x=L.cx+Math.cos(ang)*L.rx*.5,y=L.cy+Math.sin(ang)*L.ry*.45;const s=h*.012*(1+k*.15);const haciaDcha=Math.sin(ang)*dir<0;
   ctx.strokeStyle='rgba(255,255,255,.35)';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(x+(haciaDcha?-s*1.2:s*1.2),y+s*.4);ctx.lineTo(x+(haciaDcha?-s*4:s*4),y-s*.4);ctx.moveTo(x+(haciaDcha?-s*1.2:s*1.2),y+s*.6);ctx.lineTo(x+(haciaDcha?-s*4:s*4),y+s*1.4);ctx.stroke();
   ctx.fillStyle=k?'#efe9dc':'#5b4a3a';ctx.beginPath();ctx.ellipse(x,y,s*1.4,s*.65,0,0,7);ctx.fill();ctx.fillStyle=k?'#efe9dc':'#2f5a3a';ctx.beginPath();ctx.arc(x+(haciaDcha?s*1.1:-s*1.1),y-s*.8,s*.45,0,7);ctx.fill();ctx.fillStyle='#e8a33a';ctx.fillRect(x+(haciaDcha?s*1.45:-s*1.75),y-s*.85,s*.35,s*.2)});
  ctx.restore();
  // juncos
  ctx.strokeStyle=dia?'#4f7a33':'#3c5a2a';ctx.lineWidth=1.2;ctx.beginPath();E.juncos.forEach(j=>{const sw=quieto?0:Math.sin(tt*1.3+j.f)*2;ctx.moveTo(j.x+px*.4,j.y);ctx.quadraticCurveTo(j.x+px*.4+sw*.4,j.y-j.l*.6,j.x+px*.4+sw,j.y-j.l)});ctx.stroke();
  // farolas junto al camino
  [.22,.46,.7].forEach(u=>{const c=E.camino(u);const lx=c.x+c.ancho*1.4+px*.4,esc=1-u*.75,alto=h*.2*esc;
   if(P.farola){const luz=ctx.createRadialGradient(lx,c.y-alto,0,lx,c.y-alto,alto*.45);luz.addColorStop(0,'rgba(255,214,150,.45)');luz.addColorStop(1,'rgba(255,214,150,0)');ctx.fillStyle=luz;ctx.fillRect(lx-alto*.5,c.y-alto*1.5,alto,alto)}
   ctx.fillStyle='#22282a';ctx.fillRect(lx-1.4*esc,c.y-alto,2.8*esc,alto);ctx.beginPath();ctx.arc(lx,c.y-alto,3.6*esc,0,7);ctx.fillStyle=P.farola?'#ffe0aa':'#d8d2c2';ctx.fill()});
  // árboles medios (por profundidad) y personas en el camino
  const vol=(()=>{const c=quieto?.35:((tt/40)%1);const u=c<.5?.06+c*1.4:.06+(1-c)*1.4;const k=Math.min(.78,u);const p=E.camino(k);return {x:p.x+px*.4,y:p.y,a:h*.09*(1-k*.82)}})();
  const paseo=(1-Math.cos(2*Math.PI*(tt/52+.2)))/2;
  const cor=(()=>{const k=.12+(quieto?.7:paseo)*.6;const p=E.camino(k);return {x:p.x+p.ancho*.4+px*.4,y:p.y,a:h*.085*(1-k*.82)}})();
  const capas=E.medios.map(t=>({y:t.y,f:()=>sprite(t,tt,px)})).concat([{y:vol.y,f:()=>persona(vol.x,vol.y,vol.a,tt,false,true)},{y:cor.y,f:()=>persona(cor.x,cor.y,cor.a,tt,false,false)}]);
  capas.sort((a,b)=>a.y-b.y).forEach(c=>c.f());
  // primer plano
  if(E.palmCerca)sprite(E.palmCerca,tt,px);sprite(E.cerca,tt,px);
  E.motas.forEach(m=>{if(!quieto){m.y-=m.v*.35;m.x+=Math.sin(tt*.6+m.f)*.2;if(m.y<-4){m.y=h+4;m.x=Math.random()*w}}ctx.fillStyle=`rgba(${P.polen},${.2+.2*Math.sin(tt*1.5+m.f)})`;ctx.beginPath();ctx.arc(m.x+px*.8,m.y,m.r,0,7);ctx.fill()});
  id=animar()?requestAnimationFrame(dibujar):null;
 }
 // fuera de la pantalla no se dibuja (ahorra batería)
 const vis=window.IntersectionObserver?new IntersectionObserver(es=>{visible=es[0].isIntersecting;if(!id&&animar())id=requestAnimationFrame(dibujar)}):null;vis&&vis.observe(cv);
 E=null;dibujar();
 const ro=window.ResizeObserver?new ResizeObserver(()=>{E=null;if(quieto||TV.pausaEn!==null)dibujar()}):null;ro&&ro.observe(cv);
 return {
  actualizarPausa(){
   if(id)cancelAnimationFrame(id);
   id=animar()?requestAnimationFrame(dibujar):null;
  },
  parar(){vivo=false;id&&cancelAnimationFrame(id);ro&&ro.disconnect();vis&&vis.disconnect()}
 };
}

function vComo(){
 const pasos=[
  [I.walk,'Un portador lleva el sensor','Voluntarios, personal de limpieza y jardines, bicis y motos compartidas y autobuses.'],
  [I.phone,'El móvil o el vehículo lo envía','Recibe cada medición por Bluetooth y la manda con la ubicación y la hora.'],
  [I.server,'El servidor la comprueba','Descarta valores imposibles y calcula la media de cada zona.'],
  [I.mapi,'Mapa por zonas','Se publican medias por zona. Nunca se publica el recorrido de nadie.'],
 ];
 const preguntas=[
  ['¿Por qué una zona dice «Sin datos»?','Los sensores van con sus portadores. Si en la última hora no ha pasado ninguno por esa zona, no hay datos nuevos. Si han pasado más de 24 horas desde la última medición, la zona queda sin datos.'],
  ['¿Se ve por dónde voy cuando llevo un sensor?','No. Tu ubicación se usa para situar cada dato y calcular tus puntos, pero en el mapa público solo aparecen medias por zona. El personal municipal tampoco ve recorridos de personas: los sensores de trabajadores van asignados a un servicio o vehículo.'],
  ['¿Qué significan Bajo, Medio y Alto?','Indican en qué intervalo cae la media de cada medición. <a href="#valores">Consulta los umbrales y las fuentes de cada magnitud.</a>'],
  ['¿Gasta mucha batería o datos?','Muy poco: cada medición ocupa menos de 100 bytes. Puedes enviar solo con wifi desde los ajustes de la app.'],
  ['¿Quién puede ver mis percepciones?','Cualquiera, durante 24 horas, en la capa Comunidad. No llevan tu nombre; solo el punto que eliges, el nivel y la hora.'],
 ];
 return header('como-funciona')+`<main class="contenido-pagina wrap">
  <header class="contenido-cabecera">
   <h1>Sensores que se mueven por la ciudad</h1>
   <p>Así llegan al mapa las mediciones de ozono, temperatura y ruido.</p>
   <nav class="contenido-accesos" aria-label="Accesos de esta página">
    <a class="btn" href="#mapa">${I.mapi} Ver el mapa</a>
    <a href="#valores">Guía de valores</a>
    <a href="#voluntariado">Hazte voluntario</a>
   </nav>
  </header>

  <ol class="contenido-pasos">${pasos.map(([icono,titulo,descripcion])=>`<li>
   <span class="contenido-paso-icono" aria-hidden="true">${icono}</span><h2>${titulo}</h2><p>${descripcion}</p>
  </li>`).join('')}</ol>

  <section class="contenido-seccion" id="valores" aria-labelledby="contenido-valores">
   <div class="contenido-titulo"><h2 id="contenido-valores">Qué significan los valores del mapa</h2>
    <p>Cada zona se colorea según la media de la última hora. El mapa de demostración usa mediciones simuladas.</p></div>
   ${guiaHTML()}
  </section>

  <section class="contenido-seccion" aria-labelledby="contenido-salud">
   <div class="contenido-titulo"><h2 id="contenido-salud">Qué te puede pasar</h2>
    <p>Cómo afectan a la salud el ozono, el calor y el ruido.</p></div>
   ${efectosHTML()}<p class="note">${EF_AVISO_B}</p>
  </section>

  <section class="contenido-seccion" id="voluntariado" aria-labelledby="contenido-voluntarios">
   <div class="contenido-titulo"><h2 id="contenido-voluntarios">Hazte voluntario</h2></div>
   <div class="contenido-voluntariado">
    <ol class="contenido-instrucciones">
     <li><b>Crea una cuenta en la app</b> y apunta tu ID de usuario (U-XXXXX).</li>
     <li><b>Recoge un sensor</b> en un punto de reparto dando tu ID.</li>
     <li><b>Escribe en la app el código</b> que lleva el sensor (RSP-XXXX) y pasea con el móvil.</li>
     <li><b>Devuélvelo</b> en cualquier punto de reparto dentro de los 10 días siguientes a la recogida.</li>
    </ol>
    <aside class="contenido-puntos card" aria-labelledby="contenido-puntos-titulo">
     <h3 id="contenido-puntos-titulo">${I.coin} Cómo se ganan los puntos</h3>
     <p>En el ejemplo de la demo, se suma 1 punto por cada ${REGLAS_DEMO.min} minutos con datos válidos,
      el doble dentro de los parques, hasta ${REGLAS_DEMO.max} al día. No cuentan los ratos con el sensor parado ni a velocidad de vehículo.</p>
     <ul class="contenido-premios">${REGALOS_DEMO.map(g=>`<li><span class="badge ${g.stock?'b-real':'b-none'}">
      ${I.gift} ${esc(g.n)} · ${g.c} pts${g.stock?'':' · agotado'}</span></li>`).join('')}</ul>
    </aside>
   </div>
   <div class="contenido-reparto">
    <h3>Puntos de reparto</h3>
    ${window.RespiraDatos?.repartoVerificado()?'':'<p class="note">Direcciones y horarios de ejemplo para la demostración.</p>'}
    ${PUNTOS_REPARTO.length?`<ul>${PUNTOS_REPARTO.map(([nombre,direccion,horario])=>`<li>
     <b>${esc(nombre)}</b>
     <dl><dt>Dirección</dt><dd>${esc(direccion)}</dd><dt>Horario</dt><dd>${esc(horario)}</dd></dl>
    </li>`).join('')}</ul>`:'<p class="note">No hay puntos de reparto disponibles.</p>'}
   </div>
   <p class="contenido-personal">La descarga estará disponible cuando se publique la app.</p>
   <p class="contenido-personal">Administración da de alta a los trabajadores municipales y les envía un enlace para activar su cuenta en la app. <a href="#acceso">Acceso ayuntamiento</a></p>
  </section>

  <section class="contenido-seccion contenido-faq" aria-labelledby="contenido-preguntas">
   <h2 id="contenido-preguntas">Preguntas frecuentes</h2>
   <div>${preguntas.map(([titulo,respuesta])=>`<details><summary>${titulo}</summary><p>${respuesta}</p></details>`).join('')}</div>
  </section>
 </main>`+footer();
}

function vPriv(){
 const apartados=[
  ['Quién es responsable','El Ayuntamiento, a través de su servicio de medio ambiente, con Nodia como encargada del tratamiento. Contacto del delegado de protección de datos: dpd@ayto.es.'],
  ['Qué datos tratamos','De quien lleva un sensor: cada medición con su ubicación y hora, el código del sensor y su cuenta (nombre de usuario, correo, puntos y canjes). De quien publica percepciones: punto elegido, nivel y hora. La consulta pública del mapa no requiere una cuenta. La cartografía utiliza Google Maps.'],
  ['Para qué','Calcular las medias por zona del mapa público, calcular los puntos de cada voluntario y gestionar el préstamo de sensores y la entrega de regalos.'],
  ['Qué se publica','Las mediciones se publican como medias por zona, sin recorridos ni ubicaciones individuales. En Comunidad aparece el punto elegido, el nivel y la hora de cada percepción. Los sensores de trabajadores públicos se asignan a un servicio o vehículo, no a una persona.'],
  ['Cuánto tiempo','Las ubicaciones individuales se borran a los 30 días; se conservan las medias por zona. Las percepciones se ven 24 h y se borran a los 30 días.'],
  ['Tus derechos','Puedes borrar tu cuenta y tus datos desde la app, en Mi cuenta, o escribir al delegado de protección de datos.'],
 ];
 return header('privacidad')+`<main class="contenido-pagina contenido-privacidad wrap">
  <header class="contenido-cabecera">
   <h1>Política de privacidad</h1>
   <p>Qué datos usa Respira, qué se publica y cómo ejercer tus derechos.</p>
   <p class="contenido-version">Última actualización: <time datetime="2026-11">noviembre de 2026</time>. Texto de ejemplo para el prototipo.</p>
  </header>
  <div class="contenido-politica">
   ${apartados.map(([titulo,texto],i)=>`<section aria-labelledby="contenido-priv-${i}"><h2 id="contenido-priv-${i}">${titulo}</h2><p>${texto}</p></section>`).join('')}
  </div>
 </main>`+footer();
}

/* ---------- acceso (WEB-05) ---------- */
function accesoMunicipal(titulo,descripcion,contenido){
 return header('acceso')+`<main class="contenido-pagina acceso-pagina wrap">
  <header class="contenido-cabecera acceso-cabecera">
   <h1>${titulo}</h1>
   <p>${descripcion}</p>
   <a href="#mapa">Volver al mapa público</a>
  </header>
  <div class="acceso-contenido card">${contenido}</div>
 </main>`+footer();
}

function vAcceso(rec){
 const apiConfigurada=window.RespiraDatos?.conApi() ?? Boolean(window.RESPIRA_CONFIG?.API_URL);
 if(rec){
  if(!apiConfigurada)return accesoMunicipal('Recuperar contraseña','En el modo de demostración no se envían correos.',`
   <a class="btn" href="#acceso">Volver a iniciar sesión</a>`);
  const contenido=S.recOk?`
   <p class="alert ok" role="status">${I.check}<span>Si <b>${esc(S.f.rmail)}</b> está registrado, recibirás un enlace en unos minutos.</span></p>
   <a class="btn" href="#acceso" data-act="recBack">Volver a iniciar sesión</a>`:`
   <form id="f-rec" novalidate>
    <h2>Solicita un enlace</h2>
    <div class="field">
     <label for="r-mail">Correo municipal</label>
     <input class="input" id="r-mail" type="email" data-bind="f.rmail" value="${esc(S.f.rmail||'')}" placeholder="nombre@ayto.es" autocomplete="email" autocapitalize="none" spellcheck="false" required ${inv('rmail')}>
     ${err('rmail')}
    </div>
    <button class="btn" type="submit">Enviar enlace</button>
    <p class="note">Por seguridad, la respuesta es la misma para todos los correos.</p>
    <a class="acceso-enlace" href="#acceso">Volver a iniciar sesión</a>
   </form>`;
  return accesoMunicipal('Recuperar contraseña','Te enviaremos un enlace para crear una nueva.',contenido);
 }
 const demoVisible=!apiConfigurada||window.RESPIRA_CONFIG?.MOSTRAR_CUENTAS_PRUEBA===true;
 const contenido=`<form id="f-login" novalidate>
  <h2>Iniciar sesión</h2>
  ${S.e.login?`<div class="alert bad" role="alert">${I.alert}<span>${S.e.login}</span></div>`:''}
  <div class="field">
   <label for="l-mail">Correo municipal</label>
   <input class="input" id="l-mail" type="email" data-bind="f.mail" value="${esc(S.f.mail||'')}" placeholder="nombre@ayto.es" autocomplete="username" autocapitalize="none" spellcheck="false" required ${inv('mail')}>
   ${err('mail')}
  </div>
  <div class="field">
   <label for="l-pass">Contraseña</label>
   <div class="acceso-clave">
    <input class="input" id="l-pass" type="${S.showPass?'text':'password'}" data-bind="f.pass" value="${esc(S.f.pass||'')}" autocomplete="current-password" required ${inv('pass')}>
    <button type="button" class="acceso-ver" id="l-ver" data-act="showPass" aria-controls="l-pass" aria-label="${S.showPass?'Ocultar':'Mostrar'} contraseña">${S.showPass?'Ocultar':'Mostrar'}</button>
   </div>
   ${err('pass')}
  </div>
  <button class="btn" type="submit">Entrar</button>
  <a class="acceso-enlace" href="#recuperar">¿Has olvidado la contraseña?</a>
 </form>${demoVisible?`<section class="acceso-prueba" aria-labelledby="acceso-prueba-titulo">
  <h3 id="acceso-prueba-titulo">Acceso de prueba</h3>
  <ul>
   ${CUENTAS_PANEL_PRUEBA.map(cuenta=>`<li>
    <div><b>${ROL[cuenta.r]}</b><code>${esc(cuenta.e)}</code><code>${esc(cuenta.p)}</code></div>
    <button class="btn sm ghost" type="button" data-act="usarCuentaPrueba" data-arg="${cuenta.r}" aria-label="Usar cuenta de ${ROL[cuenta.r]}">Usar</button>
   </li>`).join('')}
  </ul>
 </section>`:''}`;
 return accesoMunicipal('Personal municipal','Panel de gestión de sensores, préstamos y puntos.',contenido);
}
/* ---------- panel municipal ---------- */
const pendDev=()=>LOANS.filter(l=>!l.fin&&l.ini!==undefined&&(l.limMs!=null?Date.now()>l.limMs:/^1 nov|^31 oct/.test(l.ini)));
const sinDatos=s=>['activo','prestado'].includes(s.est)&&(s.ult==null||s.ult>1440);

function pshell(cur,inner){
 const usuario=S.user;
 const secciones=[
  ['panel','Flota'],
  ['panel-sensores','Sensores'],
  ['panel-prestamos','Préstamos',pendDev().length],
  ['panel-puntos','Puntos',SOSP.filter(d=>d.estado==='pend').length],
  ['panel-regalos','Regalos y canjes'],
  ['panel-trabajadores','Trabajadores'],
 ].filter(([ruta])=>ACCESS[usuario.r].includes(ruta));
 const enlaces=secciones.map(([ruta,nombre,pendientes])=>{
  const activa=cur===ruta||(ruta==='panel-sensores'&&cur==='panel-editar');
  return `<a href="#${ruta}"${activa?' aria-current="page"':''}>${nombre}${pendientes?`<span class="panel-pendientes" aria-label="${pendientes} pendientes">${pendientes}</span>`:''}</a>`;
 }).join('');
 const rol=ROL[usuario.r]+(usuario.punto?' · '+esc(usuario.punto):'');
 const cuenta=`<div class="panel-cuenta"><b>${esc(usuario.n)}</b><span>${rol}</span>
  <button type="button" class="link" data-act="logout">Cerrar sesión</button></div>`;
 const datos=window.RespiraDatos?.estadoPanel(cur,usuario.r);
 const aviso=datos?.error?`<div class="alert warn" role="status">${I.alert}<div>
  <p>${datos.listo?'No se han podido actualizar los datos. Se muestran los últimos datos recibidos.':'No se han podido cargar los datos de esta sección.'}</p>
  <button type="button" class="btn sm sec" data-act="reintentarPanel">Reintentar</button>
 </div></div>`:'';
 return `<div class="panel-municipal" data-ui>
  <header class="tv tvh panel-cabecera">
   <div class="tv-nav">
    <div class="panel-marca">
     <a class="tv-marca" href="#inicio" aria-label="Respira, inicio"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">${SYM()}</svg>Respira</a>
     <span>Panel municipal</span>
    </div>
    ${botonTema(true)}
    <button type="button" class="tv-burger" data-tv-mobile aria-controls="panel-menu" aria-expanded="false" aria-label="Abrir menú">${TV_BURGER(false)}</button>
    <div class="tv-auth" data-tv-desktop>${botonTema(false)}<a class="btn sm sec" href="#mapa">Ver web pública</a></div>
   </div>
   <div class="tv-panel" id="panel-menu" data-tv-mobile inert>
    <nav class="panel-enlaces" aria-label="Panel municipal">${enlaces}</nav>
    <a class="btn sm sec" href="#mapa">Ver web pública</a>
    ${cuenta}
   </div>
  </header>
  <div class="panel">
   <aside class="pside" data-tv-desktop>
    <nav class="panel-enlaces" aria-label="Panel municipal">${enlaces}</nav>
    ${cuenta}
   </aside>
   <main class="pmain" id="panel-contenido">${aviso}${inner}</main>
  </div>
 </div>`;
}

const panelTitulo=(titulo,descripcion='',accion='')=>`<div class="panel-titulo">
 <div><h1>${titulo}</h1>${descripcion?`<p>${descripcion}</p>`:''}</div>${accion}
</div>`;
const panelDato=valor=>`<span class="panel-dato">${esc(valor)}</span>`;
const panelAccion=(texto,accion,id,nombre='',peligro=false,deshabilitado=false)=>`<button type="button" class="panel-accion${peligro?' panel-accion-peligro':''}" data-act="${accion}" data-arg="${esc(id)}"${nombre?` aria-label="${esc(texto+' '+nombre)}"`:''}${deshabilitado?' disabled':''}>${texto}</button>`;
const panelAcciones=(...acciones)=>`<div class="panel-acciones">${acciones.join('')}</div>`;

function panelTabla(titulo,columnas,filas,vacio){
 const celda=(valor,columna)=>`<td role="cell"><span class="panel-celda-etiqueta" aria-hidden="true">${columna}</span><div>${valor}</div></td>`;
 return `<section class="panel-registros">
  <h2>${titulo}</h2>
  ${filas.length?`<div class="panel-tabla"><table role="table" aria-label="${titulo}">
   <thead role="rowgroup"><tr role="row">${columnas.map(c=>`<th scope="col" role="columnheader">${c}</th>`).join('')}</tr></thead>
   <tbody role="rowgroup">${filas.map(fila=>`<tr role="row">${fila.map((valor,i)=>celda(valor,columnas[i])).join('')}</tr>`).join('')}</tbody>
  </table></div>`:`<p class="panel-vacio">${vacio}</p>`}
 </section>`;
}

const stCell=s=>{
 const [color,nombre]={activo:['var(--lv-bajo)','Activo'],prestado:['var(--primary)','Prestado'],disponible:['var(--ink-3)','Disponible'],mant:['var(--mant)','Mantenimiento']}[s.est]||['var(--ink-3)','Sin estado'];
 return `<span class="st"><i aria-hidden="true" style="background:${color}"></i>${nombre}</span>`;
};
const portador=s=>s.tipo==='vol'?(s.est==='prestado'?`Prestado a <b>${esc(s.user)}</b>`:'<span class="hint">En el punto de reparto</span>'):esc(s.asig);

function vFlota(){
 const consulta=S.fQ.trim().toLowerCase();
 const pendientes=new Set(pendDev().map(l=>l.code));
 const retrasados=SENS.filter(sinDatos);
 const enServicio=SENS.filter(s=>s.est!=='mant').length;
 const activos=SENS.filter(s=>['activo','prestado'].includes(s.est)&&!sinDatos(s)).length;
 const estados={todos:()=>true,sindatos:sinDatos,pend:s=>pendientes.has(s.code),mant:s=>s.est==='mant'};
 const visibles=SENS.filter(s=>(S.fT==='todos'||s.tipo===S.fT)&&estados[S.fE](s)&&
  (!consulta||[s.code,s.asig,s.user].join(' ').toLowerCase().includes(consulta)));
 const filtros=(rotulo,act,valor,opciones)=>`<div class="panel-filtro">
  <span>${rotulo}</span><div class="chips" role="group" aria-label="${rotulo}">
  ${opciones.map(([id,nombre])=>`<button type="button" class="chip" data-act="${act}" data-arg="${id}" aria-pressed="${id===valor}">${nombre}</button>`).join('')}
  </div></div>`;
 const filas=visibles.map(s=>[
  `<b>${panelDato(s.code)}</b>`,TIPOS[s.tipo],
  portador(s)+(pendientes.has(s.code)?' <span class="badge b-alto">Fuera de plazo</span>':''),
  (s.ult==null?'<span class="hint">Nunca</span>':hace(s.ult))+(sinDatos(s)?' <span class="badge b-medio">Sin datos</span>':''),
  stCell(s),
 ]);
 return pshell('panel',`
  ${panelTitulo('Estado de la flota',`${SENS.length} sensores · Datos a ${new Date().toLocaleString('es-ES',{dateStyle:'short',timeStyle:'short'})}`,
    '<a class="btn sec" href="#panel-sensores">Gestionar sensores</a>')}
  <div class="panel-indicadores" aria-label="Resumen de la flota">
   <div><span>Con datos en 24 h</span><strong>${activos}</strong><small>De ${enServicio} en servicio</small></div>
   <button type="button" data-act="fE" data-arg="sindatos" aria-pressed="${S.fE==='sindatos'}">
    <span>Sin datos en 24 h</span><strong class="panel-aviso">${retrasados.length}</strong><small>Filtrar sensores</small>
   </button>
   <button type="button" data-act="fE" data-arg="pend" aria-pressed="${S.fE==='pend'}">
    <span>Fuera de plazo</span><strong class="panel-peligro">${pendientes.size}</strong><small>Ver préstamos vencidos</small>
   </button>
  </div>
  <p class="panel-nota">Un sensor sin datos puede estar inactivo. Si lleva varios días sin enviar mediciones, conviene revisarlo.</p>
  <div class="panel-filtros">
   <div class="field panel-buscar"><label for="f-q">Buscar sensor</label>
    <input class="input" type="search" id="f-q" data-bind="fQ" data-live="1" value="${esc(S.fQ)}" placeholder="Código, línea, servicio o ID"></div>
   ${filtros('Portador','fT',S.fT,[['todos','Todos'],...Object.entries(TIPOS)])}
   ${filtros('Estado','fE',S.fE,[['todos','Todos'],['sindatos','Sin datos en 24 h'],['pend','Fuera de plazo'],['mant','Mantenimiento']])}
  </div>
  ${panelTabla(visibles.length===1?'1 sensor':visibles.length+' sensores',['Código','Portador','Asignación','Último dato','Estado'],filas,'Ningún sensor cumple estos filtros.')}`);
}

function vSensores(){
 const admin=S.user.r==='admin';
 const filas=SENS.map(s=>[
  `<b>${panelDato(s.code)}</b>`,s.mags.map(m=>MAG[m]?.ab).filter(Boolean).join(' · '),TIPOS[s.tipo],
  portador(s)+(s.est==='mant'&&s.motivo?`<p class="hint panel-apunte">${esc(s.motivo)}</p>`:''),
  stCell(s),panelAcciones(panelAccion('Editar','editar',s.code,s.code),
   admin?panelAccion('Dar de baja','baja',s.code,s.code,true,s.est==='prestado'):''),
 ]);
 return pshell('panel-sensores',`
  ${panelTitulo('Sensores','Alta, asignación y mantenimiento'+(admin?' de la flota.':'.'),
   '<button type="button" class="btn" data-act="editar">Nuevo sensor</button>')}
  ${panelTabla('Inventario de sensores',['Código','Magnitudes','Portador','Asignación','Estado','Acciones'],filas,'Todavía no hay sensores registrados.')}
  <p class="panel-nota">${admin?'Para dar de baja un sensor prestado, registra antes su devolución.':'Dar de baja un sensor solo puede hacerlo Administración.'}</p>`);
}

function vEditar(){
 const e=S.edit;
 if(!e){location.hash='panel-sensores';return ''}
 const nuevo=!e.orig;
 const prestado=!nuevo&&byCode(e.orig)?.est==='prestado';
 const indicaciones={
  vol:'Se presta en los puntos de reparto',
  trab:'Asignado a un servicio, nunca a una persona',
  bici:'Va fijo en una bici o moto',
  bus:'Va fijo en un autobús, con corriente',
 };
 const asignacion={
  trab:['Servicio','Ej.: Limpieza · Brigada de parques 2'],
  bus:['Línea y vehículo','Ej.: Línea 2 · Bus 214'],
  bici:['Vehículo o estación','Ej.: Bici compartida · Estación 8'],
 }[e.tipo];
 return pshell('panel-editar',`
  <nav class="crumb" aria-label="Ruta"><a href="#panel-sensores">Sensores</a> › ${nuevo?'Nuevo sensor':esc(e.orig)}</nav>
  ${panelTitulo(nuevo?'Nuevo sensor':'Editar '+esc(e.orig),'Configura las magnitudes, el portador y el estado del dispositivo.')}
  <form id="f-sensor" class="panel-formulario panel-edicion" novalidate>
   <div class="formgrid">
    <div class="field"><label for="e-code-in">Código del sensor</label>
     <input class="input panel-codigo" id="e-code-in" data-bind="edit.code" value="${esc(e.code)}" placeholder="RSP-XXXX" ${nuevo?'':'disabled'}
      aria-describedby="sensor-codigo-ayuda${S.e.code?' e-code':''}"${S.e.code?' aria-invalid="true"':''}>
     <span class="hint" id="sensor-codigo-ayuda">${nuevo?'RSP- y 4 letras o números. Es el que va impreso en la carcasa.':'El código no se puede cambiar.'}</span>
     ${err('code')}
    </div>
    <fieldset class="panel-grupo">
     <legend>Magnitudes que mide</legend>
     <div class="chips">${Object.keys(MAG).map(k=>`<button type="button" class="check" role="checkbox"
      aria-checked="${e.mags.includes(k)}" data-act="emag" data-arg="${k}"${S.e.mags?' aria-invalid="true" aria-describedby="e-mags"':''}>
      <span class="b" aria-hidden="true">${e.mags.includes(k)?I.check:''}</span>${MAG[k].n}</button>`).join('')}</div>
     ${err('mags')}
    </fieldset>
    <fieldset class="panel-grupo full"${prestado?' disabled':''}>
     <legend>Tipo de portador</legend>
     <div class="panel-radios">${Object.entries(TIPOS).map(([k,n])=>`
      <label class="panel-radio"><input type="radio" id="portador-${k}" name="panel-portador" data-bind="edit.tipo" data-rr="1" value="${k}"${e.tipo===k?' checked':''}>
       <span><b>${n}</b><span class="hint">${indicaciones[k]}</span></span></label>`).join('')}</div>
     ${prestado?'<p class="hint panel-apunte">Registra la devolución antes de cambiar el portador.</p>':''}
    </fieldset>
    ${asignacion?`<div class="field full"><label for="ed-asig">${asignacion[0]}</label>
     <input class="input" id="ed-asig" data-bind="edit.asig" value="${esc(e.asig||'')}" placeholder="${asignacion[1]}" ${inv('asig')}>${err('asig')}</div>`:''}
    <fieldset class="panel-grupo full">
     <legend>Estado</legend>
     <div class="chips">${[['ok','En servicio'],['mant','En mantenimiento']].map(([k,n])=>`<button type="button" class="chip"
      data-act="eest" data-arg="${k}" aria-pressed="${e.estado===k}"${prestado&&k==='mant'?' disabled aria-describedby="sensor-devolucion"':''}>${n}</button>`).join('')}</div>
     ${prestado?'<p class="hint panel-apunte" id="sensor-devolucion">Registra la devolución antes de pasar el sensor a mantenimiento.</p>':''}
    </fieldset>
    ${e.estado==='mant'?`<div class="field full"><label for="ed-mot">Motivo del mantenimiento</label>
     <input class="input" id="ed-mot" data-bind="edit.motivo" value="${esc(e.motivo||'')}" placeholder="Ej.: Carcasa rota al devolverlo" ${inv('motivo')}>${err('motivo')}</div>`:''}
   </div>
   <div class="panel-acciones"><button class="btn" type="submit">${nuevo?'Dar de alta':'Guardar cambios'}</button>
    <a class="btn ghost" href="#panel-sensores">Cancelar</a></div>
  </form>`);
}

function vPrestamos(){
 const f=S.f;
 const abiertos=LOANS.filter(l=>!l.fin);
 const pendientes=new Set(pendDev());
 const disponibles=SENS.filter(s=>s.tipo==='vol'&&s.est==='disponible');
 const filas=abiertos.map(l=>[
  `<b>${panelDato(l.code)}</b>`,panelDato(l.user),esc(l.ini),esc(l.lim||'—'),esc(l.punto),
  pendientes.has(l)?'<span class="badge b-alto">Fuera de plazo</span>':'<span class="badge b-real">En curso</span>',
 ]);
 const devoluciones=LOANS.filter(l=>l.fin).map(l=>[
  panelDato(l.code),panelDato(l.user),esc(l.ini),esc(l.lim||l.fin),esc(l.estDev),
 ]);
 return pshell('panel-prestamos',`
  ${panelTitulo('Préstamos de sensores',S.user.punto?'Punto de reparto: '+esc(S.user.punto):'Todos los puntos de reparto')}
  <div class="panel-formularios">
   <form id="f-prestar" class="panel-formulario" novalidate>
    <h2>Prestar un sensor</h2>
    <div class="field"><label for="p-user">ID de usuario de la app</label>
     <input class="input panel-codigo" id="p-user" data-bind="f.puser" value="${esc(f.puser||'')}" placeholder="U-XXXXX" ${inv('puser')}>${err('puser')}</div>
    <div class="field"><label for="p-code">Código del sensor</label>
     <input class="input panel-codigo" id="p-code" data-bind="f.pcode" value="${esc(f.pcode||'')}" placeholder="RSP-XXXX" ${inv('pcode')}>${err('pcode')}</div>
    <div class="field"><label for="p-punto">Punto de recogida</label>
     <select class="input" id="p-punto" data-bind="f.ppunto" aria-describedby="p-plazo${S.e.ppunto?' e-ppunto':''}"${S.e.ppunto?' aria-invalid="true"':''}>
      ${PUNTOS_REPARTO.map(p=>`<option${(f.ppunto||PUNTOS_REPARTO[0][0])===p[0]?' selected':''}>${esc(p[0])}</option>`).join('')}
     </select>${err('ppunto')}
     <span class="hint" id="p-plazo">Plazo de 10 días para devolverlo en cualquier punto de reparto.</span>
    </div>
    <p class="panel-nota"><b>Disponibles ahora</b> ${disponibles.map(s=>panelDato(s.code)).join(', ')||'Ninguno'}</p>
    <button class="btn" type="submit"${PUNTOS_REPARTO.length?'':' disabled'}>Registrar préstamo</button>
    ${PUNTOS_REPARTO.length?'':'<p class="panel-nota">No hay puntos de reparto disponibles.</p>'}
   </form>
   <form id="f-devolver" class="panel-formulario" novalidate>
    <h2>Registrar una devolución</h2>
    <div class="field"><label for="d-code">Código del sensor</label>
     <input class="input panel-codigo" id="d-code" data-bind="f.dcode" value="${esc(f.dcode||'')}" placeholder="RSP-XXXX" ${inv('dcode')}>${err('dcode')}</div>
    <fieldset class="panel-grupo"><legend>¿Cómo llega?</legend>
     <div class="panel-radios panel-radios-col">${[['Correcto','Listo para volver a prestarse'],['Defectuoso','Pasa a mantenimiento']].map(([estado,ayuda])=>`
      <label class="panel-radio"><input type="radio" id="devolucion-${estado.toLowerCase()}" name="panel-devolucion" data-bind="f.dest" value="${estado}"${(f.dest||'Correcto')===estado?' checked':''}>
       <span><b>${estado}</b><span class="hint">${ayuda}</span></span></label>`).join('')}</div>
    </fieldset>
    <button class="btn sec" type="submit">Registrar devolución</button>
   </form>
  </div>
  ${panelTabla('En préstamo ahora ('+abiertos.length+')',['Sensor','Usuario','Desde','Devolver antes de','Punto','Estado'],filas,'No hay sensores prestados.')}
  ${panelTabla('Devoluciones recientes',['Sensor','Usuario','Prestado','Fecha límite','Cómo llegó'],devoluciones,'Todavía no hay devoluciones registradas.')}`);
}

function vPuntos(){
 const reglas=S.rules;
 const ejemplo=Math.min(RULES.max,Math.floor(120/RULES.min)+Math.floor(40/RULES.min)*(RULES.mult-1));
 const filas=SOSP.map(d=>[
  panelDato(d.user),esc(d.d),panelDato(d.min),`<b>${panelDato(d.pts)}</b>`,
  esc(d.mot)+(d.razon?`<p class="hint panel-apunte">${esc(d.razon)}</p>`:''),
  d.estado==='pend'?'<span class="badge b-medio">Pendiente</span>':d.estado==='ok'?'<span class="badge b-bajo">Aprobado</span>':'<span class="badge b-alto">Anulado</span>',
  d.estado==='pend'?panelAcciones(panelAccion('Aprobar','aprobar',d.id,d.user),panelAccion('Anular','anular',d.id,d.user,true)):'<span class="hint">Revisado</span>',
 ]);
 const regla=(id,campo,nombre,ayuda)=>`<div class="field"><label for="r-${id}">${nombre}</label>
  <input class="input" id="r-${id}" data-bind="rules.${id}" value="${esc(reglas[id])}" inputmode="numeric"
   aria-describedby="r-${id}-ayuda${S.e[campo]?' e-'+campo:''}"${S.e[campo]?' aria-invalid="true"':''}>
  <span class="hint" id="r-${id}-ayuda">${ayuda}</span>${err(campo)}</div>`;
 return pshell('panel-puntos',`
  ${panelTitulo('Puntos del voluntariado','El cálculo se hace cada noche con los datos válidos del día.')}
  <div class="panel-formularios panel-formularios-asim">
   <form id="f-rules" class="panel-formulario" novalidate>
    <h2>Reglas de cálculo</h2>
    <div class="formgrid">
     ${regla('min','rmin','Minutos válidos por punto','Entre 1 y 60.')}
     ${regla('mult','rmult','Multiplicador en parques','Entre 1 y 5.')}
     ${regla('max','rmax','Máximo de puntos al día','Entre 10 y 500.')}
    </div>
    <p class="panel-nota">Se descartan los datos con el sensor parado más de 15 minutos o a más de 25 km/h. Los cambios se aplican desde el día siguiente.</p>
    <button class="btn" type="submit">Guardar reglas</button>
   </form>
   <aside class="panel-ejemplo">
    <h2>Ejemplo con las reglas actuales</h2>
    <p>Un voluntario con 120 minutos válidos, 40 de ellos en parques.</p>
    <strong>${ejemplo} <small>puntos</small></strong>
   </aside>
  </div>
  ${panelTabla('Días para revisar',['Usuario','Día','Min. válidos','Puntos','Motivo','Estado','Acciones'],filas,'No hay días para revisar.')}
  <p class="panel-nota">El voluntario ve el resultado en su historial de puntos. Al anular, se le restan los puntos de ese día.</p>`);
}

function vRegalos(){
 const admin=S.user.r==='admin';
 const canje=S.canje;
 const regalo=canje&&(GIFTS.find(x=>x.id===canje.gift)||{n:canje.giftN||'Regalo retirado'});
 const catalogo=GIFTS.map(x=>[
  `<b>${esc(x.n)}</b>`,panelDato(x.c+' pts'),panelDato(x.stock),
  x.stock?'<span class="badge b-bajo">Disponible</span>':'<span class="badge b-none">Agotado</span>',
  panelAcciones(panelAccion('Editar','editRegalo',x.id,x.n),panelAccion('Retirar','retirarRegalo',x.id,x.n,true)),
 ]);
 const canjes=CANJES.map(x=>[
  panelDato(x.code),panelDato(x.user),esc((GIFTS.find(y=>y.id===x.gift)||{n:x.giftN||'Regalo retirado'}).n),
  esc(fecha(x.cad)),
  x.estado==='entregado'?'<span class="badge b-bajo">Entregado</span>':x.cad<TODAY?'<span class="badge b-none">Caducado · puntos devueltos</span>':'<span class="badge b-medio">Pendiente</span>',
 ]);
 return pshell('panel-regalos',`
  ${panelTitulo(admin?'Regalos y canjes':'Entregar regalos',admin?'Catálogo que ven los voluntarios en la app.':'Comprueba el código que enseña el voluntario.',
    admin?'<button type="button" class="btn" data-act="nuevoRegalo">Nuevo regalo</button>':'')}
  <form class="panel-formulario panel-edicion" id="f-canje" novalidate>
   <h2>Entregar un regalo canjeado</h2>
   <div class="field"><label for="c-code">Código de recogida</label><div class="panel-canje">
    <input class="input panel-codigo" id="c-code" data-bind="f.ccode" value="${esc(S.f.ccode||'')}" placeholder="CJ-XXXX" ${inv('ccode')}>
    <button class="btn" type="submit">Comprobar</button></div>${err('ccode')}</div>
   ${canje?`<div class="alert ok panel-resultado-canje" role="status">${I.gift}<span><b>${esc(regalo.n)}</b> para <b>${esc(canje.user)}</b> · válido hasta el ${fecha(canje.cad)}</span>
    <button type="button" class="btn sm" data-act="entregar">Marcar como entregado</button></div>`:''}
   ${window.RespiraDatos?.conApi()?'':'<p class="panel-nota">Códigos de demostración: CJ-8R2T, CJ-2K9L y CJ-5P7Q.</p>'}
  </form>
  ${admin?
    panelTabla('Catálogo',['Regalo','Coste','Existencias','En la app','Acciones'],catalogo,'Todavía no hay regalos en el catálogo.')+
    panelTabla('Canjes',['Código','Usuario','Regalo','Caduca','Estado'],canjes,'Todavía no hay canjes registrados.')
   :''}`);
}

const estTrab=t=>t.estado==='activo'?'<span class="badge b-bajo">Activa</span>':t.caduca<Date.now()?'<span class="badge b-alto">Enlace caducado</span>':'<span class="badge b-medio">Pendiente de activar</span>';

function vTrabajadores(){
 if(!S.user||S.user.r!=='admin')return '';
 const f=S.f;
 const ok=S.altaOk;
 const avisoCorreo=ok?.demo?'Invitación de demostración.':ok?.correoEnviado===true
  ?`Enlace enviado a <b>${esc(ok.email)}</b>.`
  :ok?.correoEnviado===false?'El servidor no pudo enviar el correo. Comparte el enlace por un canal municipal.'
   :'La invitación está preparada. Puedes compartir el enlace por un canal municipal.';
 const filas=TRAB().map(t=>[
  `<b>${esc(t.n)}</b><p class="hint panel-apunte">${esc(t.e)}</p>`,
  panelDato(t.id),panelDato(t.emp),esc(t.serv),esc(t.alta),estTrab(t),
  t.estado==='pendiente'?panelAccion('Reenviar enlace','reenviarActiv',t.id,'a '+t.n):'<span class="hint">—</span>',
 ]);
 return pshell('panel-trabajadores',`
  ${panelTitulo('Trabajadores municipales','Gestiona las cuentas del personal que lleva sensores.')}
  ${ok?`<div class="alert ${ok.correoEnviado===false?'warn':'ok'} panel-alta" role="status">${ok.correoEnviado===false?I.alert:I.check}<div>
   <b>${esc(ok.nombre)} · ${panelDato(ok.id)}</b>
   <p>${avisoCorreo} El enlace caduca en ${ACTIV_H} h y solo se puede usar una vez.</p>
   ${ok.demo?`<a class="btn sm sec" href="${esc(ok.enlace)}">Abrir activación de prueba</a>`:
    ok.correoEnviado!==true&&ok.enlace?`<div class="field"><label for="panel-enlace-activacion">Enlace de activación</label>
     <input class="input" id="panel-enlace-activacion" value="${esc(ok.enlace)}" readonly></div>`:''}
  </div></div>`:''}
  <form class="panel-formulario panel-edicion" id="f-trab" novalidate>
   <h2>Dar de alta un trabajador</h2>
   <div class="formgrid">
    <div class="field"><label for="t-n">Nombre y apellidos</label>
     <input class="input" id="t-n" data-bind="f.tn" value="${esc(f.tn||'')}" autocomplete="off" maxlength="80" required ${inv('tn')}>${err('tn')}</div>
    <div class="field"><label for="t-e">Correo municipal</label>
     <input class="input" id="t-e" type="email" data-bind="f.te" value="${esc(f.te||'')}" placeholder="nombre@ayto.es" autocomplete="off" required ${inv('te')}>${err('te')}</div>
    <div class="field"><label for="t-emp">Número de empleado</label>
     <input class="input" id="t-emp" data-bind="f.temp" value="${esc(f.temp||'')}" placeholder="6 cifras" inputmode="numeric" maxlength="6" required ${inv('temp')}>${err('temp')}</div>
    <div class="field"><label for="t-s">Servicio</label>
     <select class="input" id="t-s" data-bind="f.ts" required ${inv('ts')}><option value="">Elige un servicio…</option>
      ${SERVICIOS.map(x=>`<option${f.ts===x?' selected':''}>${x}</option>`).join('')}</select>${err('ts')}</div>
   </div>
   <p class="panel-nota">El trabajador crea su contraseña con el enlace de activación. Su cuenta sirve para usar la app y carece de permisos de administración. La asignación se hace en <a href="#panel-sensores">Sensores</a>.</p>
   <button type="submit" class="btn">Dar de alta</button>
  </form>
  ${panelTabla('Cuentas de trabajadores',['Trabajador','ID de cuenta','Nº empleado','Servicio','Alta','Estado','Acciones'],filas,'Todavía no hay trabajadores registrados.')}`);
}

function vActivar(){
 const enlace=enlaceAct();
 const recuperacion=enlace?.tipo==='recuperar'||S.activTipo==='recuperar';
 const conocido=Boolean(enlace||S.activOk);
 const titulo=!conocido?'Enlace de acceso':recuperacion?'Crea una contraseña nueva':'Activa tu cuenta de trabajador municipal';
 const descripcion=!conocido?'Activación y recuperación de cuenta de Respira.':recuperacion?'Una vez cambiada, podrás volver a iniciar sesión.':'Usa el enlace que te ha enviado tu ayuntamiento para crear tu contraseña.';
 let contenido;

 if(S.activOk){
  const enPanel=['admin','tecnico','reparto'].includes(S.activRol);
  contenido=`<p class="alert ok" role="status">${I.check}<span><b>${recuperacion?'Contraseña cambiada.':'Cuenta activada.'}</b>
   Ya puedes entrar ${enPanel?'en el panel':'en la app Respira'} con <b>${esc(S.activOk)}</b> y la contraseña que acabas de crear.</span></p>
   ${enPanel?'<a class="btn" href="#acceso">Iniciar sesión</a>':'<a class="btn sec" href="#mapa">Ver el mapa</a>'}
   ${recuperacion?'':'<p class="note">Tu cuenta de trabajador municipal sirve para llevar el sensor que te asigne tu servicio. La gestión de sensores se hace desde el panel municipal.</p>'}`;
 }else if(enlace===undefined&&window.RespiraDatos?.conApi()){
  contenido='<p class="muted" role="status">Comprobando el enlace…</p>';
 }else if(enlace===false){
  contenido=`<p class="alert warn" role="status">${I.alert}<span>No se ha podido comprobar el enlace. Inténtalo de nuevo.</span></p>
   <button type="button" class="btn" data-act="reintentarEnlace">Reintentar</button>`;
 }else if(!enlace||enlace.caduca<Date.now()){
  const mensaje=enlace?'<b>El enlace ha caducado.</b>':'<b>Este enlace no es válido.</b> Puede que ya se haya usado o que administración haya enviado uno nuevo.';
  const siguiente=recuperacion?'<a class="btn" href="#recuperar">Solicitar otro enlace</a>':'<p class="note">Pide a administración que te reenvíe el enlace de activación. Si querías cambiar tu contraseña, <a href="#recuperar">solicita otro enlace de recuperación</a>.</p>';
  contenido=`<p class="alert bad" role="alert">${I.alert}<span>${mensaje}</span></p>${siguiente}`;
 }else{
  const norma=enlace.rol==='vecino'?'Mínimo 8 caracteres.':'Mínimo 10 caracteres, con letras y números, sin incluir tu nombre de usuario.';
  const horas=enlace.horas??ACTIV_H;
  contenido=`<form id="f-activar" novalidate>
   <h2>Hola, ${esc(enlace.n.split(' ')[0])}</h2>
   <p class="muted">Crea la contraseña de tu cuenta <b class="mono">${esc(enlace.id)}</b> (${esc(enlace.e)}).</p>
   <div class="field">
    <label for="ac-p1">Contraseña nueva</label>
    <input class="input" id="ac-p1" type="password" data-bind="f.ap1" autocomplete="new-password" required
     aria-describedby="ac-norma${S.e.ap1?' e-ap1':''}" ${S.e.ap1?'aria-invalid="true"':''}>
    <span class="hint" id="ac-norma">${norma}</span>
    ${err('ap1')}
   </div>
   <div class="field">
    <label for="ac-p2">Repite la contraseña</label>
    <input class="input" id="ac-p2" type="password" data-bind="f.ap2" autocomplete="new-password" required ${inv('ap2')}>
    ${err('ap2')}
   </div>
   <button class="btn" type="submit">${recuperacion?'Cambiar contraseña':'Activar cuenta'}</button>
   <p class="note">El enlace se puede usar una vez y caduca ${horas===1?'una hora':horas+' horas'} después de su envío.</p>
  </form>`;
 }
 return accesoMunicipal(titulo,descripcion,contenido);
}

/* ---------- modales ---------- */
function modal(){const m=S.modal;if(!m)return '';let b='';
 if(m.k==='baja'){b=`<h2>¿Dar de baja ${m.id}?</h2><p class="muted" style="margin:0">Dejará de aparecer en la flota y no se podrá prestar. Sus mediciones anteriores se conservan.</p><form id="f-baja" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="b-mot">Motivo</label><select class="input" id="b-mot" data-bind="f.bmot" ${inv('bmot')}><option value="">Elige un motivo…</option>${['Perdido o no devuelto','Avería sin reparación','Sustituido por otro sensor','Otro'].map(o=>`<option ${S.f.bmot===o?'selected':''}>${o}</option>`).join('')}</select>${err('bmot')}</div><div class="field"><label for="b-conf">Para confirmar, escribe <b class="mono">${m.id}</b></label><input class="input mono" id="b-conf" data-bind="f.bconf" value="${esc(S.f.bconf||'')}" autocomplete="off" ${inv('bconf')}>${err('bconf')}</div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn danger" type="submit">Dar de baja</button></div></form>`}
 if(m.k==='anular'){b=`<h2>Anular los puntos del ${SOSP.find(d=>d.id===m.id).d}</h2><form id="f-anular" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="a-mot">Motivo (lo verá el voluntario)</label><textarea class="input" id="a-mot" data-bind="f.amot" maxlength="200" ${inv('amot')}>${esc(S.f.amot||'')}</textarea>${err('amot')}</div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn danger" type="submit">Anular puntos</button></div></form>`}
 if(m.k==='regalo'){b=`<h2>${m.id?'Editar regalo':'Nuevo regalo'}</h2><form id="f-regalo" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="g-n">Nombre</label><input class="input" id="g-n" data-bind="f.gn" value="${esc(S.f.gn||'')}" ${inv('gn')}>${err('gn')}</div><div class="formgrid"><div class="field"><label for="g-c">Coste en puntos</label><input class="input mono" id="g-c" data-bind="f.gc" value="${esc(S.f.gc||'')}" inputmode="numeric" ${inv('gc')}>${err('gc')}</div><div class="field"><label for="g-s">Existencias</label><input class="input mono" id="g-s" data-bind="f.gs" value="${esc(S.f.gs||'')}" inputmode="numeric" ${inv('gs')}>${err('gs')}</div></div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn" type="submit">Guardar</button></div></form>`}
 if(m.k==='guia'){b=`<div class="gv-modal-top"><div><span class="lbl">Guía</span><h2>Qué significan los valores</h2></div><button type="button" class="btn ghost sm" data-act="closeModal" aria-label="Cerrar">${I.x}</button></div>${guiaHTML()}`}
 if(m.k==='confirm'){b=`<h2>${esc(m.t)}</h2><p class="muted" style="margin:0">${m.p}</p><div class="acts"><button class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn ${m.danger?'danger':''}" data-act="confirmOk">${m.ok}</button></div>`}
 return `<div class="modal" data-act="backdrop"><div class="box${m.k==='guia'?' gv-box':''}" role="dialog" aria-modal="true" aria-labelledby="dialogo-titulo" tabindex="-1">${b.replace('<h2>','<h2 id="dialogo-titulo">')}</div></div>`}

/* ---------- render ---------- */
const $r=document.getElementById('root');
let origenDialogo;
function localizarFoco(elemento){
 if(!elemento||!$r.contains(elemento))return null;
 if(elemento.id)return '#'+CSS.escape(elemento.id);
 const {act,arg}=elemento.dataset;
 if(!act)return null;
 return `[data-act="${CSS.escape(act)}"]`+(arg===undefined?':not([data-arg])':`[data-arg="${CSS.escape(arg)}"]`);
}
function enfocarSeccion(elemento){
 if(!elemento)return;
 elemento.setAttribute('tabindex','-1');
 elemento.focus({preventScroll:true});
}
function sincronizarDialogo(selectorFoco,habiaDialogo,activar){
 const dialogo=$r.querySelector('.modal [role="dialog"]');
 for(const elemento of $r.children){
  if(!elemento.classList.contains('modal'))elemento.inert=Boolean(dialogo);
 }
 if(dialogo){
  if(!habiaDialogo)origenDialogo=selectorFoco;
  const anterior=habiaDialogo&&selectorFoco&&dialogo.querySelector(selectorFoco);
  (anterior||dialogo.querySelector('[data-act="closeModal"]')||dialogo).focus({preventScroll:true});
 }else if(habiaDialogo){
  const origen=origenDialogo&&[...$r.querySelectorAll(origenDialogo)].find(el=>el.getClientRects().length&&!el.disabled);
  origenDialogo=null;
  if(origen)origen.focus({preventScroll:true});
  else if(activar)enfocarSeccion($r.querySelector('main h1'));
 }
}

// Actualiza los controles y los resultados sin desmontar el mapa de Google.
function renderMapaEnSitio(html){
 const plantilla=document.createElement('template');
 plantilla.innerHTML=html;
 const vista=plantilla.content;
 for(const selector of ['.tvh','.mapa-encabezado','.mapa-filtros','.side','.mapa-pie']){
  $r.querySelector(selector).replaceWith(vista.querySelector(selector));
 }
 for(const [selector,padre] of [['.mapctl',$r.querySelector('.mapwrap')],['.modal',$r]]){
  const anterior=padre.querySelector(selector);
  const nuevo=vista.querySelector(selector);
  if(nuevo&&anterior)anterior.replaceWith(nuevo);
  else if(nuevo)padre.append(nuevo);
  else anterior?.remove();
 }
}

function render(){const r=route();
 if(PAN.includes(r)&&!S.user){location.replace('#acceso');S.e={};S.f.notice=true;return}
 if(PAN.includes(r)&&!ACCESS[S.user.r].includes(r)){location.replace('#'+HOME[S.user.r]);toast('Tu rol no tiene acceso a esa sección');return}
 const V={inicio:vInicio,mapa:()=>{S.layer='zonas';S.avSel=null;return vMapa()},comunidad:()=>{S.layer='comunidad';S.sel=null;return vMapa()},historial:vHist,comparar:vCmp,'como-funciona':vComo,privacidad:vPriv,acceso:()=>vAcceso(false),recuperar:()=>vAcceso(true),panel:vFlota,'panel-sensores':vSensores,'panel-editar':vEditar,'panel-prestamos':vPrestamos,'panel-puntos':vPuntos,'panel-regalos':vRegalos,'panel-trabajadores':vTrabajadores,activar:vActivar};
 const ae=document.activeElement;const keep=ae&&ae.id&&ae.dataset&&ae.dataset.live?[ae.id,ae.selectionStart]:null;
 const mapaAbierto=r==='mapa'||r==='comunidad';
 const listaAnterior=$r.querySelector('.mapview .side');
 const scrollLista=listaAnterior?.scrollTop||0;
 const habiaDialogo=Boolean($r.querySelector('.modal'));
 const selectorFoco=r==='inicio'?null:localizarFoco(ae);
 if(r!=='inicio'&&TV.limpiar)TV.limpiar();
 const datosPanel=PAN.includes(r)?window.RespiraDatos?.estadoPanel(r,S.user.r):null;
 const html=datosPanel&&!datosPanel.listo?pshell(r,`<h1>Panel municipal</h1>${datosPanel.error?'':'<p role="status">Cargando datos…</p>'}`):V[r]()+modal();
 if(mapaAbierto&&listaAnterior)renderMapaEnSitio(html);
 else $r.innerHTML=html;
 if(mapaAbierto){
  GMAP().montar();
  $r.querySelector('.mapview .side').scrollTop=scrollLista;
 }
 if (!S.modal&&!habiaDialogo&&selectorFoco&&!S.focusErr&&!document.activeElement?.closest('#root')){
  const control=[...$r.querySelectorAll(selectorFoco)].find(el=>el.getClientRects().length&&!el.disabled);
  control?.focus({preventScroll:true});
 }
 if(S.modal||habiaDialogo)sincronizarDialogo(selectorFoco,habiaDialogo,r!=='inicio');
 if(S.modal&&S.modal.k==='guia'&&S.modal.foco){const k=S.modal.foco;S.modal.foco=null;requestAnimationFrame(()=>{const el=document.querySelector('.gv-box .gv[data-k="'+k+'"]');if(!el)return;el.scrollIntoView({block:'nearest'});el.classList.add('gv-foco');setTimeout(()=>el.classList.remove('gv-foco'),2200)})}
 if(r==='inicio')montarInicio();
 if(keep){const el=document.getElementById(keep[0]);if(el){el.focus();try{el.setSelectionRange(keep[1],keep[1])}catch(_){}}}
 if(S.focusErr){S.focusErr=false;const el=$r.querySelector('[aria-invalid="true"]');el&&el.focus()}
 if(r==='acceso'&&S.f.notice){S.f.notice=false;toast('Inicia sesión para entrar en el panel')}
}

/* ---------- acciones ---------- */
// Panel con servidor: si hay API, la acción la hace datos-api.js (y devuelve true)
const SRV=(k,d)=>{const D=window.RespiraDatos;if(!D||!D.conApi())return false;D.accion(k,d);return true};
const enlaceAct=()=>window.RespiraDatos&&RespiraDatos.conApi()?RespiraDatos.enlace(S.tok):STAFF.find(x=>x.r==='trabajador'&&x.token&&x.token===S.tok);
const codigoCanje=()=>String(S.f.ccode||'').trim().toUpperCase();
function limpiarCanje(){
 S.canje=null;
 $r.querySelector('.panel-resultado-canje')?.remove();
}
const vEmail=v=>/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(v);
const A={
 pausaParque:(_,boton)=>{
  const ahora=performance.now();
  if(TV.pausaEn===null){
   TV.pausaEn=ahora;
   if(TV.raf)cancelAnimationFrame(TV.raf);
   TV.raf=null;
  }else{
   TV.inicioEscena+=ahora-TV.pausaEn;
   TV.pausaEn=null;
  }
  TV.globo?.actualizarPausa?.();
  const pausa=TV.pausaEn!==null;
  boton.setAttribute('aria-label',etiquetaParque(pausa));
  boton.title=etiquetaParque(pausa);
  boton.innerHTML=iconoParque(pausa);
 },
 tema:(_,boton)=>{
  const cambiar=()=>{
   TEMA=TEMA==='oscuro'?'claro':'oscuro';
   try{localStorage.setItem('respira-tema',TEMA)}catch(_){}
   aplicarTema();
   render();
   $r.querySelector(boton.matches('[data-tv-mobile]')?'.tv-tema.movil':'.tv-tema:not(.movil)')?.focus({preventScroll:true});
  };
  transicionVista(cambiar);
 },
 toast:a=>toast(a),
 mag:a=>{S.mag=a;render()},
 sel:seleccionarZona,
 zoom:()=>GMAP().reset(),
 tilt:()=>GMAP().perspectiva(),
 avf:a=>{S.avf=+a;S.avSel=null;render()},
 avt:a=>{S.avt=a;S.avf=0;S.avSel=null;render()},
 buscarZona:()=>document.getElementById('q')?.focus(),
 verPercepcion:id=>{
  S.avSel=id;
  render();
  if(matchMedia('(max-width: 780px)').matches)document.getElementById('aviso-'+id)?.focus();
 },
 tabIni:a=>{S.tabIni=a;document.querySelectorAll('[data-act=tabIni]').forEach(b=>{const seleccionada=b.dataset.arg===a;b.setAttribute('aria-selected',seleccionada);b.tabIndex=seleccionada?0:-1});document.querySelectorAll('.ic-panel').forEach(p=>p.hidden=p.id!=='icp-'+a)},
 verZona:a=>{S.q='';S.sel=a;nav('mapa')},
 toHist:a=>{S.hist={id:a,mag:S.mag==='gen'?'o3':S.mag,per:'24h',desde:'',hasta:''};S.sel=null;nav('historial')},
 toCmp:a=>{
  const desdeHistorial=route()==='historial';
  const mag=desdeHistorial?S.hist.mag:S.mag;
  S.cmp.a=a;
  S.cmp.mag=mag==='gen'?'o3':mag;
  if(desdeHistorial&&PERIODOS_SERIE.some(([per])=>per===S.hist.per))S.cmp.per=S.hist.per;
  if(S.cmp.b===a)S.cmp.b=ZONAS.find(z=>z.id!==a&&est(z)==='ok').id;
  S.sel=null;
  nav('comparar');
 },
 hmag:a=>{S.hist.mag=a;render()}, hper:a=>{S.hist.per=a;S.e={};render()},
 swap:()=>{const c=S.cmp;[c.a,c.b]=[c.b,c.a];render()},
 usarCuentaPrueba:rol=>{
  const cuenta=CUENTAS_PANEL_PRUEBA.find(c=>c.r===rol);
  if(!cuenta)return;
  S.f.mail=cuenta.e;
  S.f.pass=cuenta.p;
  S.e={};
  render();
  $r.querySelector('#f-login button[type="submit"]')?.focus();
 },
 showPass:()=>{S.showPass=!S.showPass;render()}, recBack:()=>{S.recOk=false},
 logout:()=>{window.RespiraDatos&&RespiraDatos.cerrarSesion();S.user=null;S.f={};S.altaOk=null;S.edit=null;S.canje=null;toast('Has cerrado sesión');nav('acceso')},
 reintentarPanel:()=>window.RespiraDatos?.reintentarPanel(),
 reintentarEnlace:()=>window.RespiraDatos?.reintentarEnlace(S.tok),
 reenviarActiv:id=>{
  if(S.user?.r!=='admin'){toast('Solo administración puede hacerlo');return}
  if(SRV('reenviar',{id}))return;
  const t=TRAB().find(x=>x.id===id&&x.estado==='pendiente');
  if(!t)return;
  t.token=nuevoToken();
  t.caduca=Date.now()+ACTIV_H*36e5;
  S.altaOk={id:t.id,nombre:t.n,email:t.e,enlace:'#activar/'+t.token,demo:true};
  render();toast('Enlace de prueba renovado. El anterior ya no sirve.');
 },
 fT:a=>{S.fT=a;render()}, fE:a=>{S.fE=S.fE===a&&a!=='todos'?'todos':a;if(route()!=='panel')nav('panel');else render()},
 editar:a=>{const s=a&&byCode(a);S.edit=s?{orig:s.code,code:s.code,mags:[...s.mags],tipo:s.tipo,asig:s.asig||'',estado:s.est==='mant'?'mant':'ok',motivo:s.motivo||''}:{orig:null,code:'',mags:[],tipo:'vol',asig:'',estado:'ok',motivo:''};S.e={};nav('panel-editar')},
 emag:a=>{const m=S.edit.mags;const i=m.indexOf(a);i>=0?m.splice(i,1):m.push(a);delete S.e.mags;render()},
 etipo:a=>{const s=S.edit.orig&&byCode(S.edit.orig);if(s&&s.est==='prestado'){toast('Está prestado: registra la devolución antes de cambiar el tipo');return}S.edit.tipo=a;delete S.e.asig;render()},
 eest:a=>{S.edit.estado=a;delete S.e.motivo;render()},
 baja:a=>{if(S.user.r!=='admin')return;S.f.bmot='';S.f.bconf='';S.e={};S.modal={k:'baja',id:a};render()},
 dest:a=>{S.f.dest=a;render()},
 aprobar:a=>{if(SRV('revisar',{id:a,decision:'aprobar'}))return;SOSP.find(d=>d.id===a).estado='ok';render();toast('Día aprobado: los puntos se mantienen')},
 anular:a=>{S.f.amot='';S.e={};S.modal={k:'anular',id:a};render()},
 nuevoRegalo:()=>{S.f.gn='';S.f.gc='';S.f.gs='';S.e={};S.modal={k:'regalo',id:null};render()},
 editRegalo:a=>{const g=GIFTS.find(x=>x.id===a);S.f.gn=g.n;S.f.gc=String(g.c);S.f.gs=String(g.stock);S.e={};S.modal={k:'regalo',id:a};render()},
 retirarRegalo:a=>{const g=GIFTS.find(x=>x.id===a);S.modal={k:'confirm',t:`¿Retirar «${g.n}»?`,p:'Dejará de verse en la app. Los canjes pendientes de este regalo se podrán seguir entregando.',ok:'Retirar',danger:true,fn:()=>{if(SRV('retirar',{id:a}))return;GIFTS=GIFTS.filter(x=>x.id!==a);toast('Regalo retirado del catálogo')}};render()},
 entregar:(_,boton)=>{const c=S.canje;if(!c||c.code!==codigoCanje())return;if(SRV('entregar',{codigo:c.code})){boton.disabled=true;return}const g=GIFTS.find(x=>x.id===c.gift);c.estado='entregado';if(g)g.stock=Math.max(0,g.stock-1);S.canje=null;S.f.ccode='';render();toast(g&&g.stock===0?`Entregado. «${g.n}» queda agotado en la app.`:'Regalo entregado')},
 closeModal:()=>{S.modal=null;S.e={};render()},
 guia:()=>{S.modal={k:'guia'};render()},
 aValores:k=>{S.modal={k:'guia',foco:k};render()},
 backdrop:(a,el,ev)=>{if(ev.target===el)A.closeModal()},
 confirmOk:()=>{const f=S.modal.fn;S.modal=null;f();render()}
};
function menuMovil(boton,abierto){
 const panel=document.getElementById(boton.getAttribute('aria-controls'));
 panel.inert=!abierto;
 boton.setAttribute('aria-expanded',abierto);
 boton.setAttribute('aria-label',abierto?'Cerrar menú':'Abrir menú');
 boton.innerHTML=TV_BURGER(abierto);
}
const menuAbierto=()=>$r.querySelector('.tv-burger[aria-expanded="true"]');
matchMedia('(min-width: 768px)').addEventListener('change',e=>{
 const boton=menuAbierto();
 if(e.matches&&boton)menuMovil(boton,false);
});
document.addEventListener('click',e=>{
 const boton=e.target.closest('.tv-burger');
 const abierto=menuAbierto();
 if(boton){menuMovil(boton,boton!==abierto);return}
 if(abierto&&(!e.target.closest('.tv-panel')||e.target.closest('a[href]')))menuMovil(abierto,false);
 const t=e.target.closest('[data-act]');if(!t||t.disabled)return;if(t.dataset.act==='backdrop'&&e.target!==t)return;const f=A[t.dataset.act];if(!f)return;if(!e.target.closest('a[href]'))e.preventDefault();f(t.dataset.arg,t,e);
});
document.addEventListener('keydown',e=>{
 const dialogo=S.modal&&$r.querySelector('.modal [role="dialog"]');
 if(dialogo&&e.key==='Tab'){
  const controles=[...dialogo.querySelectorAll('a[href],button,input,select,textarea,summary,[tabindex]:not([tabindex="-1"])')]
   .filter(el=>!el.disabled&&el.tabIndex>=0&&el.getClientRects().length);
  const indice=controles.indexOf(document.activeElement);
  if(indice<0||e.shiftKey&&indice===0||!e.shiftKey&&indice===controles.length-1){
   e.preventDefault();
   (controles[e.shiftKey?controles.length-1:0]||dialogo).focus();
  }
  return;
 }
 const pestaña=e.target.closest('[data-act="tabIni"]');
 if(pestaña&&['ArrowLeft','ArrowRight','Home','End'].includes(e.key)){
  const pestañas=[...pestaña.parentElement.querySelectorAll('[role="tab"]')];
  const actual=pestañas.indexOf(pestaña);
  const siguiente=e.key==='Home'?0:e.key==='End'?pestañas.length-1:
   (actual+(e.key==='ArrowLeft'?-1:1)+pestañas.length)%pestañas.length;
  e.preventDefault();
  A.tabIni(pestañas[siguiente].dataset.arg);
  pestañas[siguiente].focus();
  return;
 }
 if(e.key!=='Escape')return;
 const boton=menuAbierto();
 if(boton){menuMovil(boton,false);boton.focus()}
 else if(S.modal)A.closeModal();
 else if(S.sel)A.sel('');
});
function setPath(b,v){const p=b.split('.');let o=S;for(let i=0;i<p.length-1;i++)o=o[p[i]];o[p[p.length-1]]=v}
const ERRKEY={'f.mail':'mail','f.pass':'pass','f.rmail':'rmail','f.puser':'puser','f.pcode':'pcode','f.dcode':'dcode','f.ccode':'ccode','f.bconf':'bconf','f.amot':'amot','f.gn':'gn','f.gc':'gc','f.gs':'gs','edit.code':'code','edit.asig':'asig','edit.motivo':'motivo','rules.min':'rmin','rules.mult':'rmult','rules.max':'rmax','hist.desde':'desde','hist.hasta':'hasta'};
$r.addEventListener('input',e=>{const el=e.target,b=el.dataset.bind;if(!b||el.tagName==='SELECT')return;setPath(b,el.value);
 if(b==='f.ccode'&&S.canje&&S.canje.code!==codigoCanje())limpiarCanje();
 if(el.dataset.live){if(b==='q'&&S.sel&&!zonaCoincide(byZ(S.sel)))S.sel=null;render();return}
 const key=ERRKEY[b];if(key&&S.e[key]){delete S.e[key];el.removeAttribute('aria-invalid');const x=document.getElementById('e-'+key);x&&x.remove()}});
$r.addEventListener('change',e=>{const el=e.target;const b=el.dataset.bind;if(!b)return;setPath(b,el.value);if(el.dataset.rr)render();});
$r.addEventListener('submit',e=>{e.preventDefault();const id=e.target.id;const er={};const ok=()=>{S.e=er;if(Object.keys(er).length){S.focusErr=true;render();return false}S.e={};return true};const up=v=>(v||'').trim().toUpperCase();
 if(id==='f-login'){const m=(S.f.mail||'').trim().toLowerCase(),p=S.f.pass||'';
  if(!m)er.mail='Escribe tu correo.';else if(!vEmail(m))er.mail='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';
  if(!p)er.pass='Escribe tu contraseña.';
  if(!ok())return;
  if(SRV('login',{email:m,clave:p}))return;
  const u=STAFF.find(x=>x.e===m&&x.p!=null&&x.p===p);
  if(!u){S.f.pass='';S.e={login:'Correo o contraseña incorrectos.'};render();return}
  if(u.r==='trabajador'){S.f.pass='';S.e={login:'Tu cuenta es de trabajador municipal. Entra en la app Respira con este correo.'};render();return}
  S.user=u;S.f={};S.e={};toast(`Hola, ${u.n.split(' ')[0]}`);nav(HOME[u.r]);return}
 if(id==='f-trab'){
  if(!S.user||S.user.r!=='admin'){toast('Solo una cuenta de administración puede dar de alta trabajadores');return}
  const n=(S.f.tn||'').trim().replace(/\s+/g,' '),m=(S.f.te||'').trim().toLowerCase(),emp=(S.f.temp||'').trim(),sv=S.f.ts||'';
  if(!n)er.tn='Escribe el nombre y los apellidos.';else if(n.split(' ').length<2)er.tn='Escribe al menos un nombre y un apellido.';
  if(!m)er.te='Escribe el correo municipal.';else if(!vEmail(m))er.te='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';else if(!m.endsWith('@ayto.es'))er.te='Tiene que ser un correo municipal (termina en @ayto.es).';else if(STAFF.some(x=>x.e.toLowerCase()===m))er.te='Ya hay una cuenta con este correo.';
  if(!emp)er.temp='Escribe el número de empleado.';else if(!/^\d{6}$/.test(emp))er.temp='Son 6 cifras, por ejemplo 104233.';else if(TRAB().some(x=>x.emp===emp))er.temp='Este número de empleado ya tiene una cuenta.';
  if(!sv)er.ts='Elige el servicio.';
  if(!ok())return;
  if(SRV('trabajador',{nombre:n,email:m,num_empleado:emp,servicio:sv}))return;
  const num=Math.max(0,...TRAB().map(x=>+x.id.slice(2)))+1,cid='T-'+String(num).padStart(4,'0');
  const t={n,e:m,p:null,r:'trabajador',id:cid,emp,serv:sv,estado:'pendiente',alta:'4 nov',por:S.user.e,token:nuevoToken(),caduca:Date.now()+ACTIV_H*36e5};
  STAFF.push(t);
  S.altaOk={id:cid,nombre:n,email:m,enlace:'#activar/'+t.token,demo:true};
  S.f.tn='';S.f.te='';S.f.temp='';S.f.ts='';render();toast(`Trabajador ${cid} dado de alta`);return}
 if(id==='f-activar'){const t=enlaceAct();
  if(!t||t.caduca<Date.now()){render();return}
  const p1=S.f.ap1||'',p2=S.f.ap2||'';
  const vec=t.rol==='vecino';if(!p1)er.ap1='Escribe una contraseña.';else if(vec&&p1.length<8)er.ap1=`Tiene que tener al menos 8 caracteres (has escrito ${p1.length}).`;else if(vec){}else if(p1.length<10)er.ap1=`Tiene que tener al menos 10 caracteres (has escrito ${p1.length}).`;else if(!/[a-z]/i.test(p1)||!/\d/.test(p1))er.ap1='Tiene que llevar letras y números.';else if(p1.toLowerCase().includes(t.e.split('@')[0]))er.ap1='No puede contener tu nombre de usuario.';
  if(!er.ap1&&p2!==p1)er.ap2='Las dos contraseñas no coinciden.';
  if(!ok()){S.f.ap1='';S.f.ap2='';render();return}
  if(SRV('activar',{token:S.tok,clave:p1}))return;
  t.p=p1;t.estado='activo';t.token=null;t.caduca=null;S.activOk=t.e;S.f.ap1='';S.f.ap2='';render();return}
 if(id==='f-rec'){const m=(S.f.rmail||'').trim();if(!m)er.rmail='Escribe tu correo.';else if(!vEmail(m))er.rmail='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';if(!ok())return;if(SRV('recuperar',{email:m}))return;S.recOk=true;render();return}
 if(id==='f-rango'){
  const h=S.hist,{desde,hasta}=h;
  const fechaUTC=d=>Date.parse(d+'T00:00:00Z');
  const hoyUTC=new Date().toISOString().slice(0,10);
  if(!desde)er.desde='Elige la fecha de inicio.';
  else if(desde<'2026-06-01')er.desde='No hay datos antes del 01/06/2026.';
  else if(desde>hoyUTC)er.desde='No puede ser una fecha futura.';
  if(!hasta)er.hasta='Elige la fecha de fin.';
  else if(hasta>hoyUTC)er.hasta='No puede ser una fecha futura.';
  else if(desde&&hasta<desde)er.hasta='La fecha de fin debe ser igual o posterior a la de inicio.';
  else if(desde&&fechaUTC(hasta)-fechaUTC(desde)>=90*864e5)er.hasta='El rango máximo es de 90 días.';
  if(!Object.keys(er).length&&!modeloSerie(byZ(h.id),h.mag,'custom',{desde,hasta}))er.hasta='Elige un rango de fechas válido.';
  if(!ok())return;
  h.rango={desde,hasta};
  render();
  toast(`Mostrando del ${desde.split('-').reverse().join('/')} al ${hasta.split('-').reverse().join('/')}`);
  return
 }
 if(id==='f-sensor'){const s=S.edit;const c=up(s.code);
  if(!s.orig){if(!c)er.code='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.code='Formato no válido: RSP- y 4 letras o números, por ejemplo RSP-7Z2K.';else if(byCode(c))er.code=`Ya existe un sensor con el código ${c}.`}
  if(!s.mags.length)er.mags='Marca al menos una magnitud.';
  if(s.tipo!=='vol'&&(s.asig||'').trim().length<3)er.asig='Indica a qué servicio, línea o vehículo va asignado (mínimo 3 caracteres).';
  if(s.estado==='mant'&&byCode(s.orig)?.est==='prestado')er.motivo='Registra la devolución antes de pasar el sensor a mantenimiento.';
  else if(s.estado==='mant'&&(s.motivo||'').trim().length<5)er.motivo='Explica el motivo del mantenimiento (mínimo 5 caracteres).';
  if(!ok())return;
  if(SRV('sensor',{orig:s.orig,codigo:c,tipo:s.tipo,mags:s.mags,asig:s.asig,mant:s.estado==='mant',motivo:s.motivo}))return;
  const asig=s.tipo==='vol'?null:s.asig.trim();
  if(s.orig){const o=byCode(s.orig);o.mags=s.mags;o.tipo=s.tipo;o.asig=asig;if(s.estado==='mant'){o.est='mant';o.motivo=s.motivo.trim()}else if(o.est==='mant'){o.est=s.tipo==='vol'?'disponible':'activo';o.motivo=null}else if(o.est!=='prestado')o.est=s.tipo==='vol'?'disponible':'activo';toast(`${o.code} guardado`)}
  else{SENS.push({code:c,tipo:s.tipo,asig,mags:s.mags,est:s.estado==='mant'?'mant':s.tipo==='vol'?'disponible':'activo',motivo:s.estado==='mant'?s.motivo.trim():null,ult:null,bat:s.tipo==='bus'?null:100});toast(`${c} dado de alta${s.tipo==='vol'?': ya se puede prestar':''}`)}
  S.edit=null;nav('panel-sensores');return}
 if(id==='f-baja'){const c=S.modal.id;if(!S.f.bmot)er.bmot='Elige un motivo.';if(up(S.f.bconf)!==c)er.bconf=`Escribe exactamente ${c} para confirmar.`;if(!ok())return;if(SRV('baja',{codigo:c,motivo:S.f.bmot}))return;SENS=SENS.filter(x=>x.code!==c);S.modal=null;render();toast(`${c} dado de baja. Sus mediciones se conservan.`);return}
 if(id==='f-prestar'){const u=up(S.f.puser),c=up(S.f.pcode);
  if(!u)er.puser='Escribe el ID que aparece en la app del voluntario.';else if(!/^U-\d{5}$/.test(u))er.puser='Formato no válido: U- y 5 cifras, por ejemplo U-48213.';else if(!APPUSERS.some(x=>x.id===u))er.puser=`No existe ningún usuario ${u}. Pídele que lo compruebe en la app, en Mi cuenta.`;else if(LOANS.some(l=>l.user===u&&!l.fin))er.puser=`${u} ya tiene un sensor prestado. Tiene que devolverlo antes de llevarse otro.`;
  const s=byCode(c);if(!c)er.pcode='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.pcode='Formato no válido: RSP- y 4 letras o números.';else if(!s)er.pcode=`No existe ningún sensor ${c}.`;else if(s.tipo!=='vol')er.pcode=`${c} va asignado a ${TIPOS[s.tipo].toLowerCase()}: no se presta.`;else if(s.est==='prestado')er.pcode=`${c} ya está prestado a ${s.user}.`;else if(s.est==='mant')er.pcode=`${c} está en mantenimiento.`;
  if(!ok())return;if(SRV('prestar',{usuario:u,codigo:c,punto:S.f.ppunto||PUNTOS_REPARTO[0][0]}))return;s.est='prestado';s.user=u;LOANS.unshift({code:c,user:u,ini:'2 nov 10:42',fin:null,lim:'12 nov 10:42',punto:S.f.ppunto||PUNTOS_REPARTO[0][0]});S.f.puser='';S.f.pcode='';render();toast(`${c} prestado a ${u}. Ya puede escribir el código en su app.`);return}
 if(id==='f-devolver'){const c=up(S.f.dcode);const s=byCode(c);const l=LOANS.find(x=>x.code===c&&!x.fin);
  if(!c)er.dcode='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.dcode='Formato no válido: RSP- y 4 letras o números.';else if(!s)er.dcode=`No existe ningún sensor ${c}.`;else if(!l)er.dcode=`${c} no figura como prestado.`;
  if(!ok())return;if(SRV('devolver',{codigo:c,estado:S.f.dest||'Correcto'}))return;const st=S.f.dest||'Correcto';l.fin='2 nov 10:42';l.estDev=st;s.user=null;if(st==='Defectuoso'){s.est='mant';s.motivo='No se ha entregado en buenas condiciones'}else{s.est='disponible'}
  S.f.dcode='';S.f.dest='Correcto';render();toast(st==='Defectuoso'?`${c} devuelto y en mantenimiento`:`${c} devuelto`);return}
 if(id==='f-rules'){const r=S.rules;const chk=(k,v,a,b,msg)=>{const n=String(v).trim();if(!/^\d+$/.test(n))er[k]='Escribe un número entero.';else if(+n<a||+n>b)er[k]=msg};
  chk('rmin',r.min,1,60,'Debe estar entre 1 y 60 minutos.');chk('rmult',r.mult,1,5,'Debe estar entre 1 y 5.');chk('rmax',r.max,10,500,'Debe estar entre 10 y 500 puntos.');
  if(!ok())return;if(SRV('reglas',{min:r.min,mult:r.mult,max:r.max}))return;RULES={min:+r.min,mult:+r.mult,max:+r.max};render();toast('Reglas guardadas: se aplican desde mañana');return}
 if(id==='f-anular'){const t=(S.f.amot||'').trim();if(t.length<10)er.amot='Explica el motivo con al menos 10 caracteres: el voluntario lo verá.';if(!ok())return;if(SRV('revisar',{id:S.modal.id,decision:'anular',razon:t}))return;const d=SOSP.find(x=>x.id===S.modal.id);d.estado='anulado';d.razon=t;S.modal=null;render();toast(`Puntos anulados: se restan ${d.pts} a ${d.user}`);return}
 if(id==='f-regalo'){const n=(S.f.gn||'').trim(),c=String(S.f.gc||'').trim(),st=String(S.f.gs||'').trim();
  if(n.length<3)er.gn='Escribe un nombre de al menos 3 caracteres.';else if(GIFTS.some(g=>g.n.toLowerCase()===n.toLowerCase()&&g.id!==S.modal.id))er.gn='Ya hay un regalo con ese nombre.';
  if(!/^\d+$/.test(c)||+c<1||+c>5000)er.gc='Entre 1 y 5000 puntos.';if(!/^\d+$/.test(st)||+st>999)er.gs='Entre 0 y 999 unidades.';
  if(!ok())return;if(SRV('regalo',{id:S.modal.id,nombre:n,coste:c,stock:st}))return;if(S.modal.id){Object.assign(GIFTS.find(g=>g.id===S.modal.id),{n,c:+c,stock:+st})}else GIFTS.push({id:'g'+Date.now(),n,c:+c,stock:+st});S.modal=null;render();toast('Catálogo actualizado');return}
 if(id==='f-canje'){const c=codigoCanje();limpiarCanje();if(c&&/^CJ-[A-Z0-9]{4}$/.test(c)&&SRV('comprobarCanje',{codigo:c}))return;const x=CANJES.find(y=>y.code===c);
  if(!c)er.ccode='Escribe el código que enseña el voluntario.';else if(!/^CJ-[A-Z0-9]{4}$/.test(c))er.ccode='Formato no válido: CJ- y 4 letras o números.';else if(!x)er.ccode=`No existe ningún canje ${c}.`;else if(x.estado==='entregado')er.ccode=`El canje ${c} ya se entregó.`;else if(x.cad<TODAY)er.ccode=`El canje ${c} caducó el ${fecha(x.cad)}: los puntos ya se devolvieron al usuario.`;
  if(!ok())return;S.canje=x;render();return}
});
/* Lo que necesita js/mapa-google.js */
window.Respira={S,ZONAS,AVISOS,MAG,PERC,nivel,est,fmt,hace,esc,avOk,render,byZ,seleccionarZona,zonaCoincide,sens:()=>SENS,nav,toast,HOME,estado:{get SENS(){return SENS},set SENS(v){SENS=v},get LOANS(){return LOANS},set LOANS(v){LOANS=v},get RULES(){return RULES},set RULES(v){RULES=v},get SOSP(){return SOSP},set SOSP(v){SOSP=v},get GIFTS(){return GIFTS},set GIFTS(v){GIFTS=v},get CANJES(){return CANJES},set CANJES(v){CANJES=v},get STAFF(){return STAFF},set STAFF(v){STAFF=v},get APPUSERS(){return APPUSERS},set APPUSERS(v){APPUSERS=v},AYTOS,PUNTOS_REPARTO}};
window.Respira.zonaMapa=zonaMapa;
aplicarTema();
render();
desplazarASeccion();
})();
