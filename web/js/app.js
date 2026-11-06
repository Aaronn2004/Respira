/* =============================================================
   Respira Web (portada + web pública + panel municipal). JS vanilla.
   Generado desde el prototipo web v35 que se ve en Claude (diseño de la versión B).
   - Fondo blanco con botón de modo oscuro (data-tema en <html>, localStorage «respira-tema»).
   - El mapa usa Google Maps JavaScript API si hay clave en js/config.js (js/mapa-google.js).
     Sin clave, usa el mapa SVG de demostración.
   - Datos: de DEMOSTRACIÓN (constantes de abajo). Al conectar la API real,
     se sustituyen por fetch() al servidor.
   ============================================================= */
(function(){
"use strict";
const GMAP=()=>window.RespiraMapa&&window.RespiraMapa.activo()?window.RespiraMapa:null;
/* ---------- marca: Respira (producto) y Nodia (empresa) ---------- */
const RW=`<g transform="translate(55 1.9) scale(0.457)"><g transform="translate(-6.60 74.40) scale(0.10000)"><path d="M66 0V-254V-525H185L186 -346H205Q213 -413 232.5 -455.0Q252 -497 287.0 -517.0Q322 -537 374 -537Q383 -537 393.5 -536.5Q404 -536 418 -533L412 -380Q397 -387 379.0 -390.0Q361 -393 347 -393Q308 -393 280.0 -375.0Q252 -357 234.5 -323.5Q217 -290 211 -242V0Z M734.0 14Q669.0 14 618.5 -4.0Q568.0 -22 533.0 -56.0Q498.0 -90 479.5 -140.0Q461.0 -190 461.0 -253Q461.0 -315 478.5 -367.5Q496.0 -420 530.0 -458.5Q564.0 -497 613.0 -518.0Q662.0 -539 724.0 -539Q784.0 -539 831.0 -519.5Q878.0 -500 909.5 -461.0Q941.0 -422 955.5 -365.5Q970.0 -309 966.0 -236L554.0 -232V-311L883.0 -314L832.0 -274Q839.0 -327 825.0 -361.0Q811.0 -395 784.5 -411.0Q758.0 -427 726.0 -427Q688.0 -427 659.0 -407.0Q630.0 -387 614.0 -348.5Q598.0 -310 598.0 -256Q598.0 -171 635.5 -131.0Q673.0 -91 734.0 -91Q762.0 -91 781.5 -98.5Q801.0 -106 813.5 -118.0Q826.0 -130 833.5 -145.0Q841.0 -160 846.0 -175L971.0 -148Q962.0 -111 944.5 -81.5Q927.0 -52 898.0 -30.5Q869.0 -9 828.5 2.5Q788.0 14 734.0 14Z M1271.0 14Q1218.0 14 1175.0 4.0Q1132.0 -6 1101.0 -25.0Q1070.0 -44 1051.0 -70.5Q1032.0 -97 1026.0 -130L1136.0 -175Q1140.0 -153 1158.0 -134.0Q1176.0 -115 1206.5 -103.5Q1237.0 -92 1280.0 -92Q1326.0 -92 1351.0 -105.5Q1376.0 -119 1376.0 -144Q1376.0 -163 1362.0 -174.0Q1348.0 -185 1321.0 -193.0Q1294.0 -201 1256.0 -208Q1219.0 -217 1180.5 -227.0Q1142.0 -237 1109.5 -255.0Q1077.0 -273 1056.5 -302.5Q1036.0 -332 1036.0 -378Q1036.0 -426 1062.0 -462.0Q1088.0 -498 1138.0 -518.5Q1188.0 -539 1260.0 -539Q1325.0 -539 1374.0 -521.5Q1423.0 -504 1454.5 -471.5Q1486.0 -439 1497.0 -394L1380.0 -355Q1376.0 -379 1360.5 -397.0Q1345.0 -415 1319.5 -424.0Q1294.0 -433 1260.0 -433Q1217.0 -433 1193.5 -419.0Q1170.0 -405 1170.0 -382Q1170.0 -363 1185.5 -351.0Q1201.0 -339 1229.0 -331.5Q1257.0 -324 1295.0 -316Q1335.0 -308 1373.0 -297.5Q1411.0 -287 1441.5 -270.0Q1472.0 -253 1490.0 -225.0Q1508.0 -197 1508.0 -153Q1508.0 -101 1480.0 -63.5Q1452.0 -26 1399.0 -6.0Q1346.0 14 1271.0 14Z M1595.0 168V-258V-525H1714.0L1715.0 -379L1733.0 -378Q1742.0 -431 1764.5 -467.0Q1787.0 -503 1823.0 -521.0Q1859.0 -539 1906.0 -539Q1976.0 -539 2026.0 -504.5Q2076.0 -470 2102.5 -407.5Q2129.0 -345 2129.0 -260Q2129.0 -181 2105.0 -119.5Q2081.0 -58 2033.5 -22.0Q1986.0 14 1915.0 14Q1865.0 14 1830.5 -5.0Q1796.0 -24 1773.5 -58.5Q1751.0 -93 1736.0 -141H1717.0Q1723.0 -114 1728.0 -86.0Q1733.0 -58 1736.0 -31.5Q1739.0 -5 1739.0 20V168ZM1865.0 -101Q1901.0 -101 1926.0 -121.0Q1951.0 -141 1964.5 -177.0Q1978.0 -213 1978.0 -259Q1978.0 -309 1964.0 -344.5Q1950.0 -380 1924.0 -400.0Q1898.0 -420 1862.0 -420Q1830.0 -420 1807.0 -406.0Q1784.0 -392 1768.5 -369.0Q1753.0 -346 1746.0 -319.5Q1739.0 -293 1739.0 -269V-250Q1739.0 -231 1744.0 -210.5Q1749.0 -190 1760.0 -170.5Q1771.0 -151 1786.0 -135.5Q1801.0 -120 1821.0 -110.5Q1841.0 -101 1865.0 -101Z M2223.0 0V-525H2367.0V0ZM2296.0 -604Q2254.0 -604 2231.5 -621.5Q2209.0 -639 2209.0 -673Q2209.0 -708 2231.5 -726.0Q2254.0 -744 2296.0 -744Q2339.0 -744 2361.5 -726.0Q2384.0 -708 2384.0 -673Q2384.0 -640 2361.5 -622.0Q2339.0 -604 2296.0 -604Z M2490.0 0V-254V-525H2609.0L2610.0 -346H2629.0Q2637.0 -413 2656.5 -455.0Q2676.0 -497 2711.0 -517.0Q2746.0 -537 2798.0 -537Q2807.0 -537 2817.5 -536.5Q2828.0 -536 2842.0 -533L2836.0 -380Q2821.0 -387 2803.0 -390.0Q2785.0 -393 2771.0 -393Q2732.0 -393 2704.0 -375.0Q2676.0 -357 2658.5 -323.5Q2641.0 -290 2635.0 -242V0Z M3037.0 14Q2993.0 14 2959.0 -3.5Q2925.0 -21 2906.0 -54.0Q2887.0 -87 2887.0 -136Q2887.0 -179 2903.5 -208.0Q2920.0 -237 2952.0 -255.0Q2984.0 -273 3031.0 -285.0Q3078.0 -297 3138.0 -306Q3170.0 -311 3190.5 -315.5Q3211.0 -320 3221.0 -330.5Q3231.0 -341 3231.0 -361Q3231.0 -389 3211.0 -409.0Q3191.0 -429 3148.0 -429Q3119.0 -429 3094.5 -419.0Q3070.0 -409 3052.5 -389.0Q3035.0 -369 3027.0 -338L2900.0 -377Q2912.0 -418 2934.5 -448.0Q2957.0 -478 2989.0 -498.5Q3021.0 -519 3062.0 -529.0Q3103.0 -539 3151.0 -539Q3228.0 -539 3276.5 -514.5Q3325.0 -490 3349.0 -438.5Q3373.0 -387 3373.0 -306V-216Q3373.0 -181 3374.5 -145.0Q3376.0 -109 3378.5 -72.5Q3381.0 -36 3384.0 0H3257.0Q3253.0 -25 3249.5 -57.5Q3246.0 -90 3244.0 -123H3226.0Q3212.0 -85 3186.0 -54.0Q3160.0 -23 3122.5 -4.5Q3085.0 14 3037.0 14ZM3098.0 -90Q3117.0 -90 3136.5 -97.0Q3156.0 -104 3174.5 -116.5Q3193.0 -129 3208.5 -148.0Q3224.0 -167 3234.0 -191L3232.0 -275L3255.0 -270Q3237.0 -257 3213.0 -249.5Q3189.0 -242 3163.5 -238.0Q3138.0 -234 3113.0 -229.0Q3088.0 -224 3068.5 -216.0Q3049.0 -208 3037.5 -194.5Q3026.0 -181 3026.0 -157Q3026.0 -126 3046.0 -108.0Q3066.0 -90 3098.0 -90Z"/></g></g>`;
const SYM=(p='var(--primary)',a='var(--accent)')=>`<path d="M20.35 3.32A21 21 0 1 0 44.68 27.65" fill="none" stroke="${a}" stroke-width="4.4" stroke-linecap="round"/><path d="M26.26 11.20A13 13 0 1 0 36.80 21.74" fill="none" stroke="${p}" stroke-width="4.4" stroke-linecap="round"/><circle cx="24" cy="24" r="6" fill="${p}"/>`;
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
const TODAY='2026-11-02';
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
  peor:['Niños','Mayores','Asma o EPOC','Deportistas'],hacer:'Nada de deporte fuera de 12 a 20 h',
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
 o3:{bajo:'Aire sin problemas para nadie.',medio:'Quien hace deporte intenso puede notarlo.',alto:'Supera el umbral de información de la UE: evita el esfuerzo al aire libre.',fuente:'Referencia europea de 120 y 180 µg/m³ (≈ 0,060 y 0,090 ppm)'},
 temp:{bajo:'Temperatura agradable.',medio:'Calor: busca sombra y bebe agua.',alto:'Riesgo para mayores, niños y enfermos crónicos.',fuente:'Valores de ejemplo del prototipo, pendientes de fuente oficial'},
 ruido:{bajo:'Ambiente tranquilo.',medio:'Ruido de tráfico habitual: molesta y cansa.',alto:'Si es habitual, daña la salud (sueño, tensión, corazón).',fuente:'Valores de ejemplo del prototipo, pendientes de fuente oficial'}};
const guiaRango=(k,i)=>{const l=LIM[k];return i===0?'&lt; '+fmt(l[0],k):i===1?fmt(l[0],k)+' – '+fmt(l[1],k):'≥ '+fmt(l[1],k)};
const guiaHTML=()=>`<div class="gv-grid">${Object.keys(MAG).map(k=>`<article class="gv" data-k="${k}">
 <header class="gv-top"><span class="efb-ico efb-${k}">${sv(EF_ICO[k],22,1.9)}</span><div><h3>${MAG[k].n}</h3><span class="gv-u">${k==='o3'?MAG[k].ab+' · ':''}${MAG[k].u}</span></div></header>
 <div class="gv-barra" aria-hidden="true"><i class="lv-b"></i><i class="lv-m"></i><i class="lv-a"></i></div>
 <ul class="gv-niv">${['bajo','medio','alto'].map((n,i)=>`<li><span class="badge b-${n}">${NIV[n]}</span><b class="gv-r">${guiaRango(k,i)}</b><span class="gv-t">${GUIA_TXT[k][n]}</span></li>`).join('')}</ul>
 <span class="gv-fuente">${GUIA_TXT[k].fuente}</span></article>`).join('')}</div>
 <div class="gv-estado"><b class="ef-t">Estado del dato</b><div class="gv-est">${[
  ['real','Reciente','Medido en la última hora.'],
  ['ant','Antiguo','Medido hace entre 1 y 24 horas. Se muestra con su hora.'],
  ['sin','Sin datos','Nadie con sensor ha pasado en 24 horas. Trama gris: no significa «todo bien».']].map(([c,t,d])=>`<div><i class="gv-sw gv-${c}"></i><span><b>${t}</b>${d}</span></div>`).join('')}</div>
 <p class="gv-nota">Cada zona muestra la media de la última hora. Parques con contorno; barrios en tono suave. Los sensores de bajo coste no sustituyen a las estaciones ni a los avisos oficiales.</p></div>`;
const EF_AVISO_B='Orientativo: no sustituye al médico. El mapa da medias de la última hora. Si hay síntomas graves, llama al 112.';
const efectosHTML=efectosB;

const NIV={bajo:'Bajo',medio:'Medio',alto:'Alto'};
const PERC={ruido:{n:'Ruido',lv:['Bajo','Medio','Alto']},temp:{n:'Temperatura',lv:['Agradable','Calor','Mucho calor']},aire:{n:'Aire',lv:['Limpio','Regular','Malo']}};
const avOk=a=>(!S.avf||a.niv===S.avf)&&(S.avt==='todos'||a.tipo===S.avt);
const nivel=(m,v)=>v==null?'none':v<LIM[m][0]?'bajo':v<LIM[m][1]?'medio':'alto';
const fmt=(v,m)=>v==null?'—':m==='o3'?(+v).toFixed(3):m==='temp'?(+v).toFixed(1):String(Math.round(v));
const hace=m=>m==null?'sin datos':m<60?`hace ${m} min`:m<1440?`hace ${Math.round(m/60)} h`:`hace ${Math.round(m/1440)} d`;
const esc=s=>String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
/* Zonas: parques y barrios. Valor = media de lo medido por los portadores en la última hora. */
const ZONAS=[
 {id:'ribera',tipo:'parque',n:'Parque de la Ribera',path:'M80 110 q70-50 170-25 q40 30 15 80 q-60 40-160 15 q-45-25-25-70z',x:200,y:122,hace:6,nm:84,o3:0.041,temp:26.8,ruido:49},
 {id:'molino',tipo:'parque',n:'Jardín del Molino',path:'M570 70 q90-30 160 10 q20 60-40 85 q-90 15-125-25 q-15-40 5-70z',x:650,y:100,hace:14,nm:31,o3:0.072,temp:24.1,ruido:57},
 {id:'central',tipo:'parque',n:'Parque Central',path:'M370 290 q80-30 170-5 q40 35 0 70 q-80 25-170 0 q-40-30 0-65z',x:455,y:318,hace:3,nm:152,o3:0.097,temp:27.9,ruido:67},
 {id:'estacion',tipo:'parque',n:'Jardines de la Estación',path:'M110 405 q70-25 140 5 q15 45-45 55 q-80 5-100-25z',x:205,y:428,hace:190,nm:0,o3:0.055,temp:22.0,ruido:61},
 {id:'oeste',tipo:'parque',n:'Parque del Oeste',path:'M650 400 q80-20 140 15 q10 50-60 60 q-70-5-85-40z',x:722,y:430,hace:null,nm:0,o3:null,temp:null,ruido:null},
 {id:'huerto',tipo:'parque',n:'Huerto Urbano Norte',path:'M340 40 q40-20 110 0 q20 40-30 60 q-60 10-80-20z',x:395,y:58,hace:38,nm:12,o3:0.048,temp:25.6,ruido:52},
 {id:'norte',tipo:'barrio',n:'Barrio Norte',rect:[0,0,900,200],lx:286,ly:172,hace:4,nm:210,o3:0.058,temp:25.2,ruido:59},
 {id:'centro',tipo:'barrio',n:'Centro',rect:[0,262,900,122],lx:600,ly:300,hace:2,nm:388,o3:0.066,temp:26.1,ruido:63},
 {id:'sur',tipo:'barrio',n:'Barrio Sur',rect:[0,392,900,168],lx:360,ly:515,hace:11,nm:96,o3:0.049,temp:24.0,ruido:55}
];
const est=z=>z.hace==null||z.hace>1440?'none':z.hace>60?'old':'ok';
const byZ=id=>ZONAS.find(z=>z.id===id);
const AVISOS=[{id:1,tipo:'ruido',x:150,y:255,niv:3,min:20,dir:'Av. del Río, 40'},{id:2,tipo:'temp',x:560,y:205,niv:2,min:120,dir:'C/ del Molino, 18'},{id:3,tipo:'aire',x:330,y:380,niv:1,min:300,dir:'C/ Mayor, 61'},{id:4,tipo:'ruido',x:540,y:340,niv:3,min:12,dir:'Pl. Mayor, 4',n:5},{id:5,tipo:'temp',x:810,y:180,niv:3,min:540,dir:'Av. Norte, 7'},{id:6,tipo:'aire',x:260,y:90,niv:2,min:900,dir:'C/ Alta, 30'},{id:7,tipo:'ruido',x:470,y:470,niv:1,min:60,dir:'C/ Poniente, 12'}];
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
const byCode=c=>SENS.find(s=>s.code===c);
let LOANS=[{code:'RSP-4F7K',user:'U-48213',ini:'2 nov 09:12',fin:null,punto:'Biblioteca Central'},{code:'RSP-2B9Q',user:'U-11902',ini:'1 nov 10:05',fin:null,punto:'Centro Cívico Norte'},{code:'RSP-7H3M',user:'U-48213',ini:'31 oct 17:40',fin:'1 nov 09:02',punto:'Biblioteca Central',estDev:'Correcto'}];
let RULES={min:5,mult:2,max:100};
let SOSP=[{id:'d1',user:'U-30577',d:'1 nov',min:520,pts:100,mot:'Llega al máximo diario',estado:'pend'},{id:'d2',user:'U-11902',d:'1 nov',min:180,pts:58,mot:'Sensor 3 h sin moverse en el mismo punto (descontado)',estado:'pend'},{id:'d3',user:'U-48213',d:'31 oct',min:310,pts:91,mot:'Llega al 90 % del máximo',estado:'ok'}];
let GIFTS=[{id:'g1',n:'Bolsa de tela',c:100,stock:0},{id:'g2',n:'Gorra Respira',c:150,stock:12},{id:'g3',n:'Cantimplora',c:250,stock:8},{id:'g4',n:'Mochila del ayuntamiento',c:600,stock:3}];
let CANJES=[{code:'CJ-8R2T',user:'U-48213',gift:'g2',cad:'2026-11-06',estado:'pendiente'},{code:'CJ-2K9L',user:'U-11902',gift:'g3',cad:'2026-10-30',estado:'pendiente'},{code:'CJ-5P7Q',user:'U-30577',gift:'g2',cad:'2026-11-04',estado:'entregado'}];
let STAFF=[{n:'Ana García',e:'admin@ayto.es',p:'Admin2026!',r:'admin'},{n:'Luis Pérez',e:'tecnico@ayto.es',p:'Tecnico2026!',r:'tecnico'},{n:'Marta Ruiz',e:'reparto@ayto.es',p:'Reparto2026!',r:'reparto',punto:'Biblioteca Central'}];
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
const S={view:{x:0,y:0,w:900,h:560},tilt:false,layer:'zonas',mag:'o3',sel:null,q:'',menu:false,hist:{id:'central',mag:'o3',per:'24h',desde:'',hasta:''},cmp:{a:'ribera',b:'central',mag:'o3',per:'24h'},avf:0,avt:'todos',
 user:null,tries:0,lock:false,f:{},e:{},modal:null,edit:null,fT:'todos',fE:'todos',fQ:'',recOk:false,canje:null,rules:{...RULES}};
const PUB=['inicio','mapa','comunidad','historial','comparar','como-funciona','privacidad'];
const PAN=['panel','panel-sensores','panel-editar','panel-prestamos','panel-puntos','panel-regalos','panel-trabajadores'];
const ACCESS={admin:PAN,tecnico:['panel','panel-sensores','panel-editar','panel-prestamos'],reparto:['panel-prestamos','panel-regalos'],trabajador:[]};
const HOME={admin:'panel',tecnico:'panel',reparto:'panel-prestamos'};
function route(){const h=location.hash.slice(1);if(h.startsWith('activar/')){S.tok=h.slice(8);return 'activar'}return [...PUB,...PAN,'acceso','recuperar'].includes(h)?h:'inicio'}
function nav(r){S.e={};S.modal=null;S.menu=false;if(location.hash.slice(1)===r)render();else location.hash=r;window.scrollTo(0,0)}
addEventListener('hashchange',()=>{S.e={};S.modal=null;S.menu=false;if(!location.hash.startsWith('#activar/'))S.activOk=null;if(!location.hash.startsWith('#panel-trabajadores'))S.altaOk=null;render()});
let tT;function toast(m){const t=document.getElementById('toast');t.innerHTML=`<div class="t">${m}</div>`;clearTimeout(tT);tT=setTimeout(()=>t.innerHTML='',3800)}
const err=k=>S.e[k]?`<span class="err" id="e-${k}">${I.err}${S.e[k]}</span>`:'';
const inv=k=>S.e[k]?'aria-invalid="true" aria-describedby="e-'+k+'"':'';
const badge=(m,v)=>{const l=nivel(m,v);return l==='none'?'<span class="badge b-none">Sin datos</span>':`<span class="badge b-${l}">${NIV[l]}</span>`};
const estTag=z=>est(z)==='none'?'<span class="badge b-none">Sin datos</span>':est(z)==='old'?'<span class="badge b-none">Antiguo</span>':'';
const fecha=d=>d.split('-').reverse().slice(0,2).join('/');

/* ---------- mapa ---------- */
function mapBase(){
 /* Imitación del estilo de Google Maps (en la web real es Maps JavaScript API) */
 const H=[-120,40,132,290,388,470,600,700],V=[-120,90,200,325,450,580,700,810,1000];
 let blocks='',blds='';
 for(let i=0;i<V.length-1;i++)for(let j=0;j<H.length-1;j++){const x=V[i]+8,y=H[j]+8,w=V[i+1]-V[i]-16,h=H[j+1]-H[j]-16;if(w<8||h<8)continue;
  blocks+=`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="3"/>`;
  for(let a=x+6;a<x+w-20;a+=30)for(let b=y+6;b<y+h-16;b+=24){const q=(a*7+b*13)%11;if(q<3)continue;blds+=`<rect x="${a}" y="${b}" width="${14+q}" height="${10+(q%4)*3}" rx="1.5"/>`}}
 const road=(d,w)=>`<path d="${d}" stroke="var(--map-casing)" stroke-width="${w+2}"/><path d="${d}" stroke="var(--map-road)" stroke-width="${w}"/>`;
 const art=(d,w)=>`<path d="${d}" stroke="var(--map-art-c)" stroke-width="${w+3}"/><path d="${d}" stroke="var(--map-art)" stroke-width="${w}"/>`;
 const lbl=(x,y,t,r=0,sz=11)=>`<text x="${x}" y="${y}"${r?` transform="rotate(${r} ${x} ${y})"`:''} font-size="${sz}">${t}</text>`;
 return `<defs><pattern id="hatch" width="9" height="9" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="9" height="9" fill="var(--surface-2)"/><line x1="0" y1="0" x2="0" y2="9" stroke="var(--lv-none)" stroke-width="3"/></pattern>
 <filter id="sombra" x="-20%" y="-30%" width="140%" height="170%"><feDropShadow dx="0" dy="1.5" stdDeviation="1.8" flood-color="#000" flood-opacity=".28"/></filter></defs>
 <rect x="-200" y="-200" width="1300" height="960" fill="var(--map-bg)"/>
 <g fill="var(--map-block)">${blocks}</g><g fill="var(--map-bld)">${blds}</g>
 ${ZONAS.filter(z=>z.tipo==='parque').map(z=>`<path d="${z.path}" fill="var(--map-park)"/>`).join('')}
 <path d="M-210 215 C 150 180, 260 260, 430 232 S 700 175, 1110 205 L1110 252 C 720 225, 600 280, 430 276 S 150 228, -210 262 Z" fill="var(--map-water)"/>
 <g fill="none" stroke-linecap="round">
  ${H.filter(y=>y!==388&&y!==132).map(y=>road(`M-200 ${y} L1100 ${y-6}`,7)).join('')}${V.filter(x=>x!==325).map(x=>road(`M${x} -200 L${x+10} 760`,7)).join('')}
  ${road('M260 238 L560 228',8)}${road('M700 130 L690 210',5)}
  ${art('M-200 392 L1100 382',11)}${art('M326 -200 L316 760',11)}${art('M-200 134 L1100 124',9)}
 </g>
 <g font-family="Roboto, Figtree, Arial, sans-serif" fill="var(--map-label)" font-weight="500" stroke="var(--map-bg)" stroke-width="3.5" paint-order="stroke" stroke-linejoin="round">
  ${lbl(120,380,'Carrer del Poniente')}${lbl(338,540,'Carrer Major',-90)}${lbl(680,246,'Riu Serpis',-4,12)}${lbl(640,118,'Av. del Nord')}${lbl(20,284,'C/ del Riu',0,10)}${lbl(600,322,'C/ del Molí',0,10)}
 </g>`}
/* Etiqueta «píldora» como la de la web real (js/mapa-google.js) */
function pildora(x,y,nombre,valor,col,parque,sel,id,al,antiguo){
 const vw=Math.max(34,valor.length*7+14),w=vw+nombre.length*(parque?7:6.6)+(antiguo?30:20),h=parque?32:28;
 return `<g class="pin${sel?' sel':''}" transform="translate(${(x-w/2).toFixed(1)} ${(y-h/2).toFixed(1)})" data-act="sel" data-arg="${id}" role="button" tabindex="0" aria-label="${esc(al)}">
 <rect class="ring" x="-3" y="-3" width="${w+6}" height="${h+6}" rx="${h/2+3}"/><rect width="${w}" height="${h}" rx="${h/2}" fill="var(--surface)" ${parque?'filter="url(#sombra)"':'fill-opacity=".88" stroke="var(--line)"'}/>
 <rect x="4" y="4" width="${vw}" height="${h-8}" rx="${(h-8)/2}" fill="${col==='none'?'var(--surface-2)':col}" ${col==='none'?'stroke="var(--lv-none)" stroke-dasharray="3 2"':''}/>
 <text x="${4+vw/2}" y="${h/2+4}" class="pvg" style="${col==='none'?'fill:var(--ink-3)':''}">${esc(valor)}</text>
 <text x="${vw+11}" y="${h/2+4.5}" class="plg"${parque?'':' style="font-weight:500"'}>${esc(nombre)}</text>${antiguo?`<text x="${w-16}" y="${h/2+4.5}" font-size="11">🕒</text>`:''}</g>`}
function barrioSVG(z){const v=z[S.mag],e=est(z),l=e==='none'||v==null?'none':nivel(S.mag,v);const [x,y,w,h]=z.rect;const col=l==='none'?'var(--lv-none)':'var(--lv-'+l+')';
 return `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${l==='none'?'url(#hatch)':col}" opacity="${l==='none'?.3:.12}" data-act="sel" data-arg="${z.id}"/>
 <rect x="${x+1}" y="${y+1}" width="${w-2}" height="${h-2}" fill="none" stroke="${col}" stroke-opacity=".5" stroke-width="1.5" stroke-dasharray="9 6" pointer-events="none"/>
 ${pildora(z.lx+40,z.ly,z.n,l==='none'?'–':fmt(v,S.mag),l==='none'?'none':col,false,S.sel===z.id,z.id,`${z.n}: ${v==null?'sin datos':fmt(v,S.mag)+' '+MAG[S.mag].u}`,e==='old')}`}
function parqueSVG(z){const v=z[S.mag],e=est(z),l=e==='none'||v==null?'none':nivel(S.mag,v);
 const outline=l==='none'?`<path d="${z.path}" fill="url(#hatch)" fill-opacity=".8" stroke="var(--lv-none)" stroke-width="2.5" stroke-dasharray="7 5"/>`:`<path class="zone-p f-${l}" d="${z.path}" fill-opacity="${e==='old'?.25:.42}" stroke="${e==='old'?'var(--ink-2)':'var(--lv-'+l+')'}" stroke-width="3"${e==='old'?' stroke-dasharray="7 5"':''}/>`;
 return `<g data-act="sel" data-arg="${z.id}" style="cursor:pointer">${outline}</g>${pildora(z.x,z.y,z.n,l==='none'?'–':fmt(v,S.mag),l==='none'?'none':'var(--lv-'+l+')',true,S.sel===z.id,z.id,`${z.n}: ${l==='none'?'sin datos':fmt(v,S.mag)+' '+MAG[S.mag].u}${e==='old'?', dato antiguo':''}`,e==='old')}`}
function avisoSVG(a){const c=['','var(--lv-bajo)','var(--lv-medio)','var(--lv-alto)'][a.niv];if(!avOk(a))return '';
 if(a.n)return `<g transform="translate(${a.x} ${a.y})"><circle r="25" fill="var(--ink)"/><text class="pv" style="fill:var(--surface)" font-size="15">${a.n}</text><rect class="plbg" x="-32" y="29" width="64" height="18" rx="9"/><text class="pl" y="42">${a.n} avisos</text></g>`;
 return `<g transform="translate(${a.x} ${a.y})"><rect x="-19" y="-19" width="38" height="38" rx="11" fill="var(--surface)" stroke="${c}" stroke-width="2.5"/>${[7,12,17].map((h,i)=>`<rect x="${-9+i*7}" y="${10-h}" width="5" height="${h}" rx="1.5" fill="${i<a.niv?c:'var(--surface-3)'}"/>`).join('')}<rect class="plbg" x="-30" y="25" width="60" height="18" rx="9"/><text class="pl" y="38">${hace(a.min).replace('hace ','')}</text></g>`}
const barsIco=(n,s=18)=>{const c=['','var(--lv-bajo)','var(--lv-medio)','var(--lv-alto)'][n];return `<span style="display:inline-flex;align-items:flex-end;gap:2px;height:${s}px">${[7,12,18].map((h,i)=>`<i style="width:4px;border-radius:2px;height:${h*s/18}px;background:${i<n?c:'var(--surface-3)'}"></i>`).join('')}</span>`};

/* ---------- gráfica ---------- */
function serie(id,mag,per,shift=0){const n=per==='24h'?48:per==='7d'?56:60;let seed=[...id+mag+per].reduce((a,c)=>a+c.charCodeAt(0),7+shift);const r=()=>{seed=(seed*9301+49297)%233280;return seed/233280};
 const z=byZ(id);const base=z[mag]??(mag==='o3'?0.05:mag==='temp'?22:58);const o=[];
 for(let i=0;i<n;i++){const t=i/n;const d=per==='24h'?Math.sin((t-.3)*Math.PI*2):Math.sin(t*Math.PI*2*(per==='7d'?7:30));let v=mag==='o3'?base*(0.75+0.35*d+0.12*(r()-.5)):mag==='temp'?base+3*d+(r()-.5):base+6*d+3*(r()-.5);
  if(per==='24h'&&i>=4&&i<=12&&z.tipo==='parque')v=null;if(per!=='24h'&&r()<.06)v=null;o.push(v)}if(z[mag]!=null)o[n-1]=z[mag];return o}
function chart(series,mag,per,w=860,h=330){
 const all=series.flatMap(x=>x.d).filter(v=>v!=null);const l=LIM[mag];let mn=Math.min(...all,l[0]*.6),mx=Math.max(...all,l[1]*1.08);if(mag==='temp'){mn=Math.min(...all)-2;mx=Math.max(...all,l[0])+2}
 const n=series[0].d.length,x0=46,y0=h-28,X=i=>x0+i*(w-x0-10)/(n-1),Y=v=>y0-(v-mn)/(mx-mn)*(y0-12);
 const band=(a,b,c)=>{const ya=Y(Math.min(Math.max(a,mn),mx)),yb=Y(Math.min(Math.max(b,mn),mx));return yb<ya?`<rect x="${x0}" y="${yb}" width="${w-x0-10}" height="${ya-yb}" fill="${c}" opacity=".5"/>`:''};
 let s=`<svg viewBox="0 0 ${w} ${h}" width="100%" role="img" aria-label="Gráfica de ${MAG[mag].n}">`+band(mn,l[0],'var(--lv-bajo-soft)')+band(l[0],l[1],'var(--lv-medio-soft)')+band(l[1],mx,'var(--lv-alto-soft)');
 l.forEach(t=>{if(t>mn&&t<mx)s+=`<line x1="${x0}" x2="${w-10}" y1="${Y(t)}" y2="${Y(t)}" stroke="var(--ink-3)" stroke-dasharray="4 4"/><text x="${x0-6}" y="${Y(t)+4}" font-size="11" text-anchor="end" fill="var(--ink-3)" font-family="JetBrains Mono,monospace">${fmt(t,mag)}</text>`});
 const d0=series[0].d;let st=null;d0.forEach((v,i)=>{if(v==null&&st==null)st=i;if((v!=null||i===n-1)&&st!=null){const e=v==null?i:i-1;s+=`<rect x="${X(Math.max(st-.5,0))}" y="12" width="${X(Math.min(e+.5,n-1))-X(Math.max(st-.5,0))}" height="${y0-12}" fill="var(--surface-2)"/>`;if(e-st>3)s+=`<text x="${(X(st)+X(e))/2}" y="${y0/2}" font-size="11" text-anchor="middle" fill="var(--ink-3)">sin datos</text>`;st=null}});
 series.forEach((se,k)=>{let seg=[],segs=[];se.d.forEach((v,i)=>{if(v==null){if(seg.length)segs.push(seg);seg=[]}else seg.push([X(i),Y(v)])});if(seg.length)segs.push(seg);
  segs.forEach(p=>{const d=p.map((q,i)=>(i?'L':'M')+q[0].toFixed(1)+' '+q[1].toFixed(1)).join(' ');if(k===0)s+=`<path d="${d} L${p[p.length-1][0]} ${y0} L${p[0][0]} ${y0}Z" fill="var(--primary)" opacity=".1"/>`;s+=`<path d="${d}" fill="none" stroke="${k?'var(--sim)':'var(--primary)'}" stroke-width="2.5" ${k?'stroke-dasharray="7 5"':''} stroke-linejoin="round"/>`});
  const lv=se.d[n-1];if(lv!=null)s+=`<circle cx="${X(n-1)}" cy="${Y(lv)}" r="5" fill="${k?'var(--sim)':'var(--primary)'}" stroke="var(--surface)" stroke-width="2"/>`});
 s+=`<line x1="${x0}" x2="${w-10}" y1="${y0}" y2="${y0}" stroke="var(--line)"/>`;
 const labs=per==='24h'?['00:00','06:00','12:00','18:00','ahora']:per==='7d'?['lun','mié','vie','dom','hoy']:['3 oct','10 oct','17 oct','24 oct','hoy'];
 labs.forEach((t,i)=>s+=`<text x="${x0+i*(w-x0-10)/4}" y="${h-8}" font-size="11.5" text-anchor="${i===0?'start':i===4?'end':'middle'}" fill="var(--ink-3)">${t}</text>`);
 return s+'</svg>'}
const statsOf=d=>{const v=d.filter(x=>x!=null);return v.length?[Math.min(...v),v.reduce((a,b)=>a+b,0)/v.length,Math.max(...v)]:[null,null,null]};

/* ---------- web pública ---------- */
function header(cur){
 /* Misma barra que la portada: marca, píldora de enlaces, botones y menú móvil */
 const com=S.layer==='comunidad';
 const activo=h=>h==='comunidad'?cur==='mapa'&&com:h==='mapa'?(cur==='mapa'&&!com)||['historial','comparar'].includes(cur):cur===h;
 const links=TV_LINKS.map(([h,n])=>`<a class="tv-link" href="#${h}"${activo(h)?' aria-current="page"':''}>${n}</a>`).join('');
 return `<header class="tv tvh"><nav class="tv-nav" aria-label="Principal">
  <a class="tv-marca" href="#inicio" aria-label="Respira, inicio"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">${SYM()}</svg>Respira</a>
  <div class="tv-pill" data-tv-desktop>${links}</div>
  <button type="button" class="tv-burger" data-tv-mobile data-act="${S.menu?'menuClose':'menu'}" aria-controls="tvh-menu" aria-expanded="${!!S.menu}" aria-label="${S.menu?'Cerrar menú':'Abrir menú'}">${TV_BURGER(!!S.menu)}</button>
  ${botonTema(true)}<div class="tv-auth" data-tv-desktop>${botonTema(false)}<a class="btn sm sec" href="#acceso">Acceso ayuntamiento</a></div>
 </nav>${S.menu?`<div id="tvh-menu" class="tv-panel" data-tv-mobile>${links}<div class="tv-panel-pie"><a class="btn sm sec" href="#acceso">Ayuntamiento</a></div></div>`:''}</header>`}
const footer=()=>`<footer class="wrap"><div class="inner"><span style="display:inline-flex;gap:8px;align-items:center;flex-wrap:wrap">${LOGO(20)}<span>· un servicio de</span>${NODIA(15,'var(--ink)')}<b style="letter-spacing:.08em;color:var(--ink)">NODIA</b></span><span>Proyecto de Biometría · GTI · UPV · <a href="#privacidad">Privacidad</a></span></div></footer>`;
const zonaOpts=(sel,ex)=>['parque','barrio'].map(t=>`<optgroup label="${t==='parque'?'Parques':'Barrios'}">${ZONAS.filter(z=>z.tipo===t&&est(z)!=='none').map(z=>`<option value="${z.id}" ${z.id===sel?'selected':''}>${esc(z.n)}</option>`).join('')}</optgroup>`).join('');

function vMapa(){const zon=S.layer==='zonas';
 const q=S.q.trim();const bad=q&&/[^\p{L}\p{N}\s.,º\-\/]/u.test(q);const nq=q.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu,'');
 const list=ZONAS.filter(z=>q.length<2||bad||z.n.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu,'').includes(nq));
 const row=z=>{const v=z[S.mag];const l=nivel(S.mag,v);return `<button class="srow" data-act="sel" data-arg="${z.id}" aria-current="${S.sel===z.id}"><span class="dotl" style="background:${est(z)==='none'||v==null?'transparent;border:2px dashed var(--lv-none)':'var(--lv-'+l+')'}"></span><span style="flex:1;min-width:0"><b style="font-weight:600">${esc(z.n)}</b><br><span class="hint">${z.nm} mediciones · ${hace(z.hace)}</span></span><span class="mono" style="font-weight:700">${est(z)==='none'?'':fmt(v,S.mag)}</span>${estTag(z)}</button>`};
 const side=zon?`<div class="field"><label for="q" class="lbl">Buscar</label><div style="position:relative"><span style="position:absolute;left:12px;top:13px;color:var(--ink-3)">${I.search}</span><input class="input" id="q" data-bind="q" data-live="1" value="${esc(S.q)}" placeholder="Parque o barrio" style="padding-left:38px" maxlength="40" ${bad?'aria-invalid="true" aria-describedby="e-q"':''} autocomplete="off"></div>${bad?`<span class="err" id="e-q">${I.err}Hay caracteres no válidos. Usa letras, números y espacios.</span>`:q.length===1?'<span class="hint">Escribe al menos 2 caracteres.</span>':''}</div>
 <div class="seg" role="tablist" aria-label="Capa"><button role="tab" aria-selected="true" data-act="layer" data-arg="zonas">Sensores</button><button role="tab" aria-selected="false" data-act="layer" data-arg="comunidad">Comunidad</button></div>
 <div class="field"><span class="lbl">Magnitud</span><div class="chips">${Object.keys(MAG).map(k=>`<button class="chip" aria-pressed="${S.mag===k}" data-act="mag" data-arg="${k}">${MAG[k].n}</button>`).join('')}</div></div>
 ${bad?'':!list.length?`<p class="note">No hay zonas para «${esc(q)}».</p>`:['parque','barrio'].map(t=>{const L=list.filter(z=>z.tipo===t);return L.length?`<div class="field"><span class="lbl">${t==='parque'?'Parques':'Barrios'}${q.length>1?' · '+L.length:''}</span><div class="card slist">${L.map(row).join('')}</div></div>`:''}).join('')}
 <p class="note" style="margin-top:auto">Media de la última hora con lo que miden voluntarios, personal de limpieza y mantenimiento, bicis y autobuses. <a href="#como-funciona">¿De dónde salen los datos?</a></p>`
 :`<div class="seg" role="tablist" aria-label="Capa"><button role="tab" aria-selected="false" data-act="layer" data-arg="zonas">Sensores</button><button role="tab" aria-selected="true" data-act="layer" data-arg="comunidad">Comunidad</button></div>
 <p class="note">Lo que notan los vecinos sobre el ruido, el calor y el aire. Son <b>percepciones</b>, no mediciones. Se muestran las de las últimas 24 h.</p>
 <div class="field"><span class="lbl">Tipo</span><div class="chips">${[['todos','Todos'],['ruido','Ruido'],['temp','Temperatura'],['aire','Aire']].map(([k,t])=>`<button class="chip" aria-pressed="${S.avt===k}" data-act="avt" data-arg="${k}">${t}</button>`).join('')}</div></div>
 <div class="field"><span class="lbl">Nivel</span><div class="chips">${[[0,'Todos'],[3,S.avt==='todos'?'Alto':PERC[S.avt].lv[2]],[2,S.avt==='todos'?'Medio':PERC[S.avt].lv[1]],[1,S.avt==='todos'?'Bajo':PERC[S.avt].lv[0]]].map(([n,t])=>`<button class="chip" aria-pressed="${S.avf===n}" data-act="avf" data-arg="${n}">${n?barsIco(n,14):''}${t}</button>`).join('')}</div></div>
 <div class="field"><span class="lbl">Percepciones recientes</span><div class="card slist">${AVISOS.filter(avOk).length?'':'<div class="srow" style="cursor:default"><span class="hint">No hay percepciones con estos filtros.</span></div>'}${AVISOS.filter(avOk).map(a=>`<div class="srow" style="cursor:default">${barsIco(a.niv)}<span style="flex:1"><b style="font-weight:600">${PERC[a.tipo].n}: ${PERC[a.tipo].lv[a.niv-1]}${a.n?' · '+a.n+' avisos':''}</b><br><span class="hint">${esc(a.dir)}</span></span><span class="hint">${hace(a.min)}</span></div>`).join('')}</div></div>
 <div class="card com-info"><b>De dónde salen estos datos</b>
  <ul>
   <li><span>${I.users}</span><span><b>Los publican vecinos</b> desde la app Respira, con una cuenta gratuita.</span></li>
   <li><span>${I.pin}</span><span>Cada uno marca <b>qué nota</b> (ruido, calor o aire), <b>dónde</b> y <b>cuánto</b>: bajo, medio o alto.</span></li>
   <li><span>${I.info}</span><span>Se ven <b>24 horas</b> y después desaparecen. Máximo 3 por hora y cuenta.</span></li>
   <li><span>${I.shield}</span><span><b>Sin nombres:</b> solo el punto, el nivel y la hora. Quien la publica puede editarla o borrarla.</span></li>
  </ul>
  <p class="hint" style="margin:0">Son <b>percepciones</b>, no mediciones: no las mide ningún sensor y no cuentan en las medias de las zonas. Para datos medidos, usa la pestaña Sensores.</p></div>`;
 const z=S.sel&&byZ(S.sel);const e=z&&est(z);
 const pop=z&&zon?`<div class="popup" role="dialog" aria-label="${esc(z.n)}"><div style="display:flex;gap:10px"><div style="flex:1"><span class="hint">${z.tipo==='parque'?'Parque':'Barrio'} · ${z.nm} mediciones en la última hora</span><h2 style="font-size:20px">${esc(z.n)}</h2></div><button class="ib" data-act="sel" data-arg="" aria-label="Cerrar">${I.x}</button></div>
 ${e==='none'?`<div class="alert warn">${I.info}<span>Nadie con sensor ha pasado por aquí en las últimas 24 h. No significa que el aire esté limpio.</span></div>`:`<div>${Object.keys(MAG).map(k=>`<div class="vrow"><span style="flex:1">${MAG[k].n}</span><span class="v">${fmt(z[k],k)}</span><span class="hint" style="width:28px">${z[k]==null?'':MAG[k].u}</span>${e==='old'?'<span class="badge b-none">Antiguo</span>':badge(k,z[k])}${`<button type="button" class="gv-q" data-act="aValores" data-arg="${k}" aria-label="Qué significa el valor de ${MAG[k].n.toLowerCase()}" title="Qué significa">?</button>`}</div>`).join('')}</div><span class="hint">Última medición: ${hace(z.hace)}${e==='old'?' · <b style="color:var(--lv-medio)">dato antiguo</b>':''}. Media de la zona; no se muestran recorridos.</span>`}
 <div style="display:flex;gap:8px"><button class="btn sm" style="flex:1" data-act="toHist" data-arg="${z.id}" ${e==='none'?'disabled':''}>Ver historial</button><button class="btn sm sec" data-act="toCmp" data-arg="${z.id}" ${e==='none'?'disabled':''}>Comparar</button></div></div>`:'';
 const legend=zon?`<div class="legend"><b>${MAG[S.mag].ab} (${MAG[S.mag].u}) · última hora</b><span><i style="background:var(--lv-bajo)"></i>Bajo</span><span><i style="background:var(--lv-medio)"></i>Medio</span><span><i style="background:var(--lv-alto)"></i>Alto</span><span><i style="background:repeating-linear-gradient(45deg,var(--lv-none) 0 2px,transparent 2px 5px);border:1px dashed var(--lv-none)"></i>Sin datos (más de 24 h)</span><span><i style="background:var(--ink-2)"></i>Antiguo (1–24 h)</span>${'<button type="button" class="gv-abrir" data-act="guia">'+sv('<circle cx="12" cy="12" r="9"/><path d="M9.6 9.3a2.5 2.5 0 0 1 4.8.9c0 1.7-2.4 2.2-2.4 3.6M12 17v.01"/>',15,2)+'Qué significan</button>'}<span class="hint">Parques con contorno · barrios en tono suave</span></div>`:`<div class="legend"><b>${S.avt==='todos'?'Percepciones · 24 h':PERC[S.avt].n+' percibido'}</b>${[1,2,3].map(k=>`<span>${barsIco(k,14)} ${S.avt==='todos'?['Bajo','Medio','Alto'][k-1]:PERC[S.avt].lv[k-1]}</span>`).join('')}<span class="hint">No son mediciones</span></div>`;
 return header('mapa')+`<main class="mapview"><aside class="side" aria-label="Filtros y lista">${side}</aside><div class="mapwrap${S.tilt&&!GMAP()?' tilt':''}">${GMAP()?GMAP().slotHTML()+pop+GMAP().controlesHTML()+legend:`<svg class="map" viewBox="${vb()}" preserveAspectRatio="xMidYMid slice">${mapBase()}${zon?ZONAS.filter(z=>z.tipo==='barrio').map(barrioSVG).join('')+ZONAS.filter(z=>z.tipo==='parque').map(parqueSVG).join(''):AVISOS.map(avisoSVG).join('')}</svg>${pop}<div class="mapctl"><button aria-label="Vista inicial" data-act="zoom" data-arg="reset">⟲</button><button class="t3" aria-label="Vista en perspectiva" aria-pressed="${S.tilt}" data-act="tilt">3D</button></div><div class="gtipo" role="group" aria-label="Tipo de mapa"><button aria-pressed="true">Mapa</button><button aria-pressed="false" data-act="toastMsg" data-arg="La vista Satélite la da Google Maps en la web real">Satélite</button></div><button class="gfull" aria-label="Pantalla completa" data-act="toastMsg" data-arg="Pantalla completa: control de Google Maps en la web real">⛶</button><div class="gzoom"><button aria-label="Acercar" data-act="zoom" data-arg="in">+</button><button aria-label="Alejar" data-act="zoom" data-arg="out">−</button></div><span class="glogo" aria-hidden="true">Google</span><span class="gcred" aria-hidden="true">Datos del mapa ©2026 Google · Imitación para el prototipo</span>${legend}`}</div></main>`}

function vHist(){const h=S.hist,z=byZ(h.id);const per=h.per==='custom'?'30d':h.per;const d=serie(h.id,h.mag,per);const st=statsOf(d);
 return header('historial')+`<main class="page wrap"><div class="inner"><nav class="crumb" aria-label="Ruta"><a href="#mapa">Mapa</a> › Historial</nav>
 <div class="pagehead"><div><span class="hint">${z.tipo==='parque'?'Parque':'Barrio'}</span><h1>${esc(z.n)}</h1></div><button class="btn sec" data-act="toCmp" data-arg="${z.id}">${I.swap} Comparar con otra zona</button></div>
 <div class="toolbar"><div class="field" style="width:280px;max-width:100%"><label for="h-id">Zona</label><select class="input" id="h-id" data-bind="hist.id" data-rr="1">${zonaOpts(h.id)}</select></div>
 <div class="field"><span class="flabel">Magnitud</span><div class="seg" role="tablist">${Object.keys(MAG).map(k=>`<button role="tab" aria-selected="${h.mag===k}" data-act="hmag" data-arg="${k}">${MAG[k].n}</button>`).join('')}</div></div>
 <div class="field"><span class="flabel">Periodo</span><div class="chips">${[['24h','24 h'],['7d','7 días'],['30d','30 días'],['custom','Entre dos fechas']].map(p=>`<button class="chip" aria-pressed="${h.per===p[0]}" data-act="hper" data-arg="${p[0]}">${p[1]}</button>`).join('')}</div></div></div>
 ${h.per==='custom'?`<form class="card" id="f-rango" style="padding:16px;display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap" novalidate><div class="field" style="width:200px"><label for="h-desde">Desde</label><input class="input mono" type="date" id="h-desde" data-bind="hist.desde" value="${h.desde}" ${inv('desde')}>${err('desde')}</div><div class="field" style="width:200px"><label for="h-hasta">Hasta</label><input class="input mono" type="date" id="h-hasta" data-bind="hist.hasta" value="${h.hasta}" ${inv('hasta')}>${err('hasta')}</div><button class="btn" type="submit" style="margin-top:27px">Aplicar</button><span class="hint" style="flex-basis:100%">Máximo 90 días. Los datos empiezan el 01/06/2026.</span></form>`:''}
 <div class="grid2"><section class="card" style="padding:16px 14px 8px"><div style="display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;padding:0 4px 8px"><b>${MAG[h.mag].n} (${MAG[h.mag].u}) · media de la zona</b><span class="hint">Fondo verde, ámbar y rojo: niveles Bajo, Medio y Alto</span></div>${chart([{d}],h.mag,per)}<p class="hint" style="margin:6px 4px">Las franjas grises son ratos en los que no pasó ningún sensor por la zona. No se rellenan ni se interpolan.</p></section>
 <aside style="display:flex;flex-direction:column;gap:14px"><div class="stats"><div><span>Mínimo</span><b>${fmt(st[0],h.mag)}</b></div><div><span>Media</span><b>${fmt(st[1],h.mag)}</b></div><div><span>Máximo</span><b>${fmt(st[2],h.mag)}</b></div></div><p class="note">Cada punto de la gráfica es la media de lo medido en la zona en ${per==='24h'?'30 minutos':per==='7d'?'3 horas':'12 horas'}. Los datos son orientativos y no sustituyen a las estaciones oficiales.</p></aside></div>
 </div></main>`+footer()}

function vCmp(){const c=S.cmp;const A=byZ(c.a),B=byZ(c.b);const same=c.a===c.b;
 let body='';if(same)body=`<div class="alert bad" role="alert">${I.alert}<span><b>Has elegido la misma zona dos veces.</b> Cambia la zona B para poder comparar.</span></div>`;
 else{const da=serie(c.a,c.mag,c.per),db=serie(c.b,c.mag,c.per,3);const sa=statsOf(da),sb=statsOf(db);const gaps=d=>d.filter(v=>v==null).length*(c.per==='24h'?.5:c.per==='7d'?3:12);
  body=`<div class="grid2"><section class="card" style="padding:16px 14px 8px"><div style="display:flex;gap:18px;flex-wrap:wrap;padding:0 4px 8px;font-size:14px"><span><b style="color:var(--primary)">━━</b> A · ${esc(A.n)}</span><span><b style="color:var(--sim)">┅┅</b> B · ${esc(B.n)}</span></div>${chart([{d:da},{d:db}],c.mag,c.per)}<p class="hint" style="margin:6px 4px">Las dos series se distinguen por el trazo (continuo y discontinuo), no solo por el color.</p></section>
  <aside style="display:flex;flex-direction:column;gap:14px"><div class="tw"><table><thead><tr><th scope="col"></th><th scope="col">A</th><th scope="col">B</th></tr></thead><tbody>${[['Mínimo',sa[0],sb[0]],['Media',sa[1],sb[1]],['Máximo',sa[2],sb[2]]].map(r=>`<tr><td>${r[0]}</td><td class="mono">${fmt(r[1],c.mag)}</td><td class="mono">${fmt(r[2],c.mag)}</td></tr>`).join('')}<tr><td>Horas sin datos</td><td class="mono">${gaps(da)} h</td><td class="mono">${gaps(db)} h</td></tr></tbody></table></div></aside></div>`}
 return header('comparar')+`<main class="page wrap"><div class="inner"><nav class="crumb"><a href="#mapa">Mapa</a> › <a href="#historial">Historial</a> › Comparar</nav><div class="pagehead"><h1>Comparar zonas</h1></div>
 <div class="toolbar"><div class="field" style="width:260px;max-width:100%"><label for="c-a">Zona A (línea continua)</label><select class="input" id="c-a" data-bind="cmp.a" data-rr="1">${zonaOpts(c.a)}</select></div><div class="field" style="width:260px;max-width:100%"><label for="c-b">Zona B (línea discontinua)</label><select class="input" id="c-b" data-bind="cmp.b" data-rr="1" ${same?'aria-invalid="true"':''}>${zonaOpts(c.b)}</select></div>
 <div class="field" style="width:170px"><label for="c-m">Magnitud</label><select class="input" id="c-m" data-bind="cmp.mag" data-rr="1">${Object.keys(MAG).map(k=>`<option value="${k}" ${c.mag===k?'selected':''}>${MAG[k].n}</option>`).join('')}</select></div><div class="field" style="width:150px"><label for="c-p">Periodo</label><select class="input" id="c-p" data-bind="cmp.per" data-rr="1">${[['24h','24 h'],['7d','7 días'],['30d','30 días']].map(p=>`<option value="${p[0]}" ${c.per===p[0]?'selected':''}>${p[1]}</option>`).join('')}</select></div>
 <button class="btn ghost" data-act="swap" style="margin-bottom:1px">${I.swap} Intercambiar</button></div>${body}</div></main>`+footer()}

/* ---------- portada (v10): la Tierra a pantalla completa ----------
   Paleta «atmósfera»: noche orbital + el azul de la capa de aire que se
   ve en el borde del planeta. Esquinas rectas en toda la portada. */
const AYTOS=[{id:'gandia',n:'Gandia',zona:'La Safor · Comunitat Valenciana',desde:'junio 2026',activo:true}];
const TV_LINKS=[['mapa','Mapa'],['comunidad','Comunidad'],['como-funciona','Cómo funciona'],['privacidad','Privacidad']];
function mediaCiudad(m){const v=ZONAS.filter(z=>est(z)!=='none'&&z[m]!=null).map(z=>z[m]);return v.length?v.reduce((a,b)=>a+b,0)/v.length:null}
const TV_BURGER=open=>`<svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true"><path d="${open?'M18 6 6 18M6 6l12 12':'M3 6h18M3 12h18M3 18h18'}" style="fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round"/></svg>`;
function vInicio(){
 const activos=SENS.filter(x=>x.est==='activo'||x.est==='prestado').length;
 const parques=ZONAS.filter(z=>z.tipo==='parque').length,barrios=ZONAS.filter(z=>z.tipo==='barrio').length;
 const MED=[['o3','Ozono','Se forma con el sol y el tráfico. Irrita ojos y vías respiratorias, sobre todo las tardes de verano.'],['temp','Temperatura','Las calles sin sombra acumulan calor. Medirla por zonas muestra dónde faltan árboles y refugios climáticos.'],['ruido','Ruido','El tráfico y el ocio nocturno afectan al descanso y a la salud. Se mide en decibelios.']];
 const PORT=[[I.broom,'Personal municipal','Las brigadas de limpieza y de jardines lo llevan en su ruta de trabajo.'],[I.bike,'Bicis y motos compartidas','El vehículo envía datos mientras circula.'],[I.bus,'Autobuses urbanos','Cubren las avenidas principales todo el día.'],[I.walk,'Vecinos voluntarios','Recogen un sensor en un punto de reparto, pasean y ganan puntos.']];
 const WEB=[['mapa','Mapa por zonas','Ozono, temperatura y ruido de cada parque y barrio en la última hora.'],['comunidad','Comunidad','Lo que notan los vecinos: ruido, calor o aire cargado.'],['historial','Historial','Cómo ha cambiado una zona en 24 horas, 7 días o un mes.'],['comparar','Comparar','Dos zonas en la misma gráfica.'],['como-funciona','Voluntariado','Cómo llevar un sensor y qué regalos puedes conseguir.'],['privacidad','Privacidad','Qué se guarda y por qué nunca se publica el recorrido de nadie.']];
 const links=TV_LINKS.map(([h,n])=>`<a class="tv-link" href="#${h}">${n}</a>`).join('');
 return `<div class="tv">
 <section class="tv-hero" id="tv-root" aria-label="Respira">
  <div class="tv-parallax" id="tv-parallax">
   <canvas class="tv-media" id="tv-parque" data-tv-anim aria-hidden="true"></canvas>
  </div>
  <div class="tv-scrim" data-tv-anim></div>
  <div class="tv-col">
   <nav class="tv-nav" data-tv-anim aria-label="Principal">
    <a class="tv-marca" href="#inicio" aria-label="Respira, inicio"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">${SYM()}</svg>Respira</a>
    <div class="tv-pill" data-tv-desktop>${links}</div>
    <button type="button" class="tv-burger" id="tv-burger" data-tv-mobile aria-controls="tv-mobile-menu" aria-expanded="false" aria-label="Abrir menú">${TV_BURGER(false)}</button>
    ${botonTema(true)}<div class="tv-auth" data-tv-desktop>${botonTema(false)}<a class="btn sm sec" href="#acceso">Acceso ayuntamiento</a></div>
   </nav>
   <div id="tv-mobile-menu" class="tv-panel" data-tv-mobile hidden>${links}<div class="tv-panel-pie"><a class="btn sm sec" href="#acceso">Ayuntamiento</a></div></div>
   <main class="tv-main">
    <h1 class="tv-h1" data-tv-anim>El aire de tu ciudad, medido calle a calle</h1>
    <p class="tv-p" data-tv-anim>Ozono, temperatura y ruido de cada parque y barrio de Gandia, medidos por sensores que viajan con quienes recorren la ciudad.</p>
    <div class="tv-ctas" data-tv-anim><a class="btn tv-cta" href="#mapa">${I.mapi} Ver el mapa de Gandia</a><a class="btn sec tv-cta tv-blur" href="#como-funciona">Hazte voluntario</a></div>
   </main>
  </div>
 </section>

 <section class="tv-sec" id="ayuntamientos"><div class="tv-in">
  <div class="tv-head"><span class="tv-eyebrow">Dónde está Respira</span><h2>Ayuntamientos con Respira</h2><p>Cada municipio tiene su propio mapa, sus puntos de reparto y su panel de gestión.</p></div>
  <div class="tv-aytos">${AYTOS.map(a=>`<article class="tv-ayto">
   <div class="tv-ayto-top"><div><h3>${esc(a.n)}</h3><span class="tv-mut">${esc(a.zona)}</span></div><span class="tv-estado"><i></i>Activo</span></div>
   <dl class="tv-dl"><div><dt>Sensores en la calle</dt><dd>${activos}</dd></div><div><dt>Zonas medidas</dt><dd>${parques+barrios}</dd></div><div><dt>Puntos de reparto</dt><dd>${PUNTOS_REPARTO.length}</dd></div><div><dt>Datos desde</dt><dd>${a.desde}</dd></div></dl>
   <div class="tv-medias">${Object.keys(MAG).map(k=>{const v=mediaCiudad(k),l=nivel(k,v);return `<div><span>${k==='temp'?'Temperatura media':MAG[k].n+' medio'}</span><b>${fmt(v,k)} <small>${MAG[k].u}</small></b>${l==='none'?'':`<em class="tv-nv tv-${l}">${NIV[l]}</em>`}</div>`}).join('')}</div>
   <div class="tv-fila"><a class="btn" href="#mapa">Abrir el mapa</a><a class="btn sec" href="#como-funciona">Puntos de reparto</a></div></article>`).join('')}
   <article class="tv-ayto tv-nuevo"><h3>¿Tu ayuntamiento?</h3><p class="tv-mut">Respira se pone en marcha con sensores, puntos de reparto, la app para los vecinos y un panel para el personal municipal.</p><a class="btn sec" href="#para-ayuntamientos">Qué incluye</a></article>
  </div>
 </div></section>

 <section class="tv-sec tv-alt"><div class="tv-in">
  <div class="tv-head"><span class="tv-eyebrow">Qué medimos</span><h2>Tres magnitudes que se notan en la calle</h2></div>
  <div class="tv-tres">${MED.map(([k,n,d])=>`<article class="tv-mag"><h3>${n} <span>${MAG[k].u}</span></h3><p class="tv-mut">${d}</p><div class="tv-escala"><span class="tv-bajo">Bajo &lt; ${fmt(LIM[k][0],k)}</span><span class="tv-medio">Medio</span><span class="tv-alto">Alto ≥ ${fmt(LIM[k][1],k)}</span></div></article>`).join('')}</div>
  <p class="tv-nota">Umbrales orientativos. El mapa muestra la media de la última hora en cada zona; «Sin datos» significa que no ha pasado ningún sensor, no que el aire esté limpio.</p>
  <div class="tv-head ef-head" id="efectos"><span class="tv-eyebrow">Efectos en la salud</span><h2>Qué te puede pasar</h2><p>Cuanto más sube, más daño. Así afecta cada nivel.</p></div>
  ${efectosHTML()}
  <p class="tv-nota">${EF_AVISO_B}</p>
 </div></section>

 <section class="tv-sec"><div class="tv-in tv-dos">
  <div class="tv-head"><span class="tv-eyebrow">De dónde salen los datos</span><h2>Sensores que se mueven con la ciudad</h2><p>En lugar de pocas estaciones fijas, muchos sensores pequeños recorren las calles. Cada medición llega con su hora y su ubicación, y el servidor calcula la media de cada zona.</p>
   <p class="tv-priv"><b>Nunca se publica el recorrido de nadie.</b> En el mapa solo hay medias por parque y barrio.</p></div>
  <div class="tv-port">${PORT.map(p=>`<div class="tv-p4"><span class="tv-ico">${p[0]}</span><div><h3>${p[1]}</h3><p class="tv-mut">${p[2]}</p></div></div>`).join('')}</div>
 </div></section>

 <section class="tv-sec tv-alt"><div class="tv-in">
  <div class="tv-head"><span class="tv-eyebrow">En esta web</span><h2>Qué puedes hacer aquí</h2></div>
  <div class="tv-web">${WEB.map(w=>`<a class="tv-w" href="#${w[0]}"><b>${w[1]}</b><span class="tv-mut">${w[2]}</span><i aria-hidden="true">→</i></a>`).join('')}</div>
 </div></section>

 <section class="tv-sec" id="para-ayuntamientos"><div class="tv-in tv-ayu">
  <div><span class="tv-eyebrow">Para ayuntamientos</span><h2>Un panel para gestionar todo el servicio</h2><p class="tv-mut">El personal municipal entra con su cuenta y ve solo lo que le corresponde según su rol: administración, técnico o punto de reparto.</p><div class="tv-fila"><a class="btn" href="#acceso">Acceso ayuntamiento</a></div></div>
  <ul class="tv-lista">${[['Flota de sensores','Estado, batería y último dato de cada sensor.'],['Préstamos','Prestar y recoger sensores en los puntos de reparto.'],['Puntos','Reglas de cálculo y revisión de días sospechosos.'],['Regalos y canjes','Catálogo, stock y entrega con código CJ-XXXX.']].map(x=>`<li><b>${x[0]}</b><span class="tv-mut">${x[1]}</span></li>`).join('')}</ul>
 </div></section>

 <footer class="tv-pie"><div class="tv-in"><span>Respira · un servicio de <b>NODIA</b></span><span>Proyecto de Biometría · GTI · UPV · <a href="#privacidad">Privacidad</a></span></div></footer>
</div>`}

/* ---------- comportamiento de la portada ---------- */
/* ---------- tema: fondo blanco o modo oscuro (recordado en este navegador) ---------- */
let TEMA='claro';try{const g=localStorage.getItem('respira-tema');if(g==='claro'||g==='oscuro')TEMA=g}catch(_){}
const IC_LUNA='<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/></svg>';
const IC_SOL='<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/></svg>';
const botonTema=movil=>`<button type="button" class="tv-tema${movil?' movil':''}" ${movil?'data-tv-mobile':''} data-act="tema" aria-pressed="${TEMA==='oscuro'}" aria-label="${TEMA==='oscuro'?'Cambiar a fondo blanco':'Cambiar a modo oscuro'}" title="${TEMA==='oscuro'?'Fondo blanco':'Modo oscuro'}">${TEMA==='oscuro'?IC_SOL:IC_LUNA}</button>`;
function aplicarTema(){document.documentElement.setAttribute('data-tema',TEMA)}
const TV={raf:null,target:{x:0,y:0},cur:{x:0,y:0},globo:null,limpiar:null};
function montarInicio(){
 if(TV.limpiar)TV.limpiar();
 const root=document.getElementById('tv-root'),par=document.getElementById('tv-parallax'),
   burger=document.getElementById('tv-burger'),panel=document.getElementById('tv-mobile-menu');
 if(!root)return;
 const off=[];const on=(el,ev,fn,o)=>{el.addEventListener(ev,fn,o);off.push(()=>el.removeEventListener(ev,fn,o))};
 // menú móvil
 const setMenu=open=>{panel.hidden=!open;burger.setAttribute('aria-expanded',open);burger.setAttribute('aria-label',open?'Cerrar menú':'Abrir menú');burger.innerHTML=TV_BURGER(open)};
 on(burger,'click',()=>setMenu(panel.hidden));
 panel.querySelectorAll('a').forEach(a=>on(a,'click',()=>setMenu(false)));
 on(window,'keydown',e=>{if(e.key==='Escape'&&!panel.hidden){setMenu(false);burger.focus()}});
 const mq=matchMedia('(min-width: 768px)');const alCambiar=()=>{if(mq.matches)setMenu(false)};on(mq,'change',alCambiar);
 let ro=null;if(window.ResizeObserver){ro=new ResizeObserver(es=>{for(const e of es)if(e.contentRect.width>=768)setMenu(false)});ro.observe(root)}
 // barra fija: al bajar, gana fondo para que se lea sobre el contenido
 const navFija=root.querySelector('.tv-nav');const alScroll=()=>navFija.classList.toggle('con-fondo',window.scrollY>8);
 on(window,'scroll',alScroll,{passive:true});alScroll();
 // paralaje con el puntero (se para solo)
 if(!matchMedia('(prefers-reduced-motion: reduce)').matches){
  const tick=()=>{const c=TV.cur,t=TV.target;c.x+=(t.x-c.x)*0.08;c.y+=(t.y-c.y)*0.08;par.style.transform='translate3d('+c.x.toFixed(2)+'px,'+c.y.toFixed(2)+'px,0)';
   TV.raf=(Math.abs(t.x-c.x)>0.05||Math.abs(t.y-c.y)>0.05)?requestAnimationFrame(tick):null};
  on(window,'pointermove',e=>{TV.target.x=(e.clientX/innerWidth-0.5)*28;TV.target.y=(e.clientY/innerHeight-0.5)*18;if(!TV.raf)TV.raf=requestAnimationFrame(tick)},{passive:true});
 }
 // parque animado (canvas)
 const claro=luminoso(getComputedStyle(document.body).backgroundColor);
 TV.globo=parqueEscena(document.getElementById('tv-parque'),claro);
 TV.limpiar=()=>{off.forEach(f=>f());ro&&ro.disconnect();if(TV.raf)cancelAnimationFrame(TV.raf);TV.raf=null;TV.globo&&TV.globo.parar();TV.globo=null;TV.limpiar=null;document.body.style.backgroundColor=''};
}

/* Un parque de Gandia a la hora dorada (o de día con el diseño claro).
   Canvas 2D, sin imágenes externas. Para que vaya fluido, el cielo, el
   suelo y cada árbol se dibujan UNA vez en lienzos aparte (sprites) y en
   cada fotograma solo se mueven: los árboles se mecen, el lago ondula y
   refleja el cielo y los árboles, nadan dos patos, pasan pájaros, el
   voluntario pasea con su sensor y un corredor da la vuelta al lago. */
function luminoso(c){const m=String(c).match(/\d+(\.\d+)?/g);if(!m)return true;const [r,g,b]=m.map(Number);return (0.299*r+0.587*g+0.114*b)>140}
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
 let w=0,h=0,dpr=1,E=null,id=null,vivo=true;const t0=performance.now();
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
  const L={cx:w*(ancho?.6:.7),cy:hz+h*(ancho?.17:.14),rx:w*(ancho?.34:.17),ry:h*(ancho?.06:.062)};L.top=L.cy-L.ry;
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
  cx.fillStyle=P.ciudad;for(let x=-10;x<w+10;){const bw=entre(10,30),bh=h*entre(.012,.05);cx.fillRect(x,hz-bh,bw,bh+3);if(P.ventanas)for(let k=0;k<3;k++)if(rnd()<.4){cx.fillStyle='rgba(255,210,150,.85)';cx.fillRect(x+entre(2,bw-4),hz-bh+entre(2,bh-4),2,2.5);cx.fillStyle=P.ciudad}x+=bw+entre(0,4)}
  const tx=w*.58,th=h*.13;cx.fillRect(tx,hz-th,w*.018,th);cx.beginPath();cx.moveTo(tx-2,hz-th);cx.lineTo(tx+w*.009,hz-th-h*.04);cx.lineTo(tx+w*.018+2,hz-th);cx.fill();
  const bg=cx.createLinearGradient(0,hz-h*.12,0,hz+2);bg.addColorStop(0,`rgba(${P.bruma},0)`);bg.addColorStop(1,`rgba(${P.bruma},.55)`);cx.fillStyle=bg;cx.fillRect(0,hz-h*.12,w,h*.12+4);
  // ---- nubes (sprites que se desplazan) ----
  const nubes=[];for(let i=0;i<(ancho?3:5);i++){const nw=w*entre(.12,.24),nh=nw*.28;const [nc,nx]=lienzo(nw,nh);for(let k=0;k<14;k++){const ex=entre(.15,.85)*nw,ey=nh*entre(.45,.75),rx=nw*entre(.08,.18),ry=nh*entre(.25,.45);const gg=nx.createLinearGradient(0,ey-ry,0,ey+ry);gg.addColorStop(0,P.nubes[0]);gg.addColorStop(1,P.nubes[1]);nx.globalAlpha=.55;nx.fillStyle=gg;nx.beginPath();nx.ellipse(ex,ey,rx,ry,0,0,7);nx.fill()}
   nubes.push({c:nc,w:nw,h:nh,x:entre(-.1,1)*w,y:hz*entre(.12,.62),v:entre(3,7)})}
  // ---- árboles ----
  const fila=[];// lejanos: línea continua de copas
  const [lejosC,lx]=lienzo(w,h*.12+20);const lb=h*.12;for(let x=-20;x<w+20;x+=entre(8,16)){lx.fillStyle=rnd()<.5?P.lejos[0]:P.lejos[1];lx.beginPath();lx.arc(x,lb-entre(0,h*.02),entre(h*.012,h*.026),0,7);lx.fill()}lx.fillRect(-20,lb-4,w+40,24);
  for(let i=0;i<(ancho?10:22);i++){const tipo=rnd()<.18?'palmera':rnd()<.25?'cipres':'redondo';const alto=h*entre(.07,.12);const s=nuevoArbol(tipo,alto,.45);fila.push({s,x:entre(-.02,1.02)*w,y:hz+h*entre(.005,.025),f:rnd()*6,amp:.006,prof:.25})}
  const medios=[];const xs=ancho?[.08,.28,.5,.72,.92,1.02]:[.53,.6,.79,.86,.93,1.0,1.06];
  xs.forEach(fx=>{const tipo=rnd()<.22?'palmera':rnd()<.2?'cipres':'redondo';const alto=h*(fx<.3&&!ancho?entre(.12,.16):entre(.17,.26));const s=nuevoArbol(tipo,alto,0);let y=hz+h*entre(.035,.07);if(Math.abs(fx*w-L.cx)<L.rx*1.05&&y>L.top-4)y=L.top-h*.004;medios.push({s,x:fx*w,y,f:rnd()*6,amp:tipo==='palmera'?.02:.012,prof:.6})});
  medios.sort((a,b)=>a.y-b.y);
  const cerca={s:nuevoArbol('redondo',h*(ancho?.5:.66),0),x:w*(ancho?1.02:.985),y:h*1.0,f:1,amp:.008,prof:1};
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
  rx.globalAlpha=.42;try{rx.filter='blur(1.2px)'}catch(_){}
  medios.forEach(t=>{if(Math.abs(t.x-L.cx)>L.rx*1.3)return;rx.save();rx.translate(t.x,2*L.top-t.y+4);rx.scale(1,-1);rx.drawImage(t.s.c,-t.s.ox,-t.s.oy,t.s.cw,t.s.ch);rx.restore()});
  rx.filter='none';rx.globalAlpha=1;
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
  if(!E){if(!preparar()){id=requestAnimationFrame(dibujar);return}}
  const tt=(performance.now()-t0)/1000,px=(TV.cur?TV.cur.x:0)*.5,{hz,L}=E;
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
  const cor=(()=>{const c=quieto?.7:((tt/26+.4)%1);const k=.12+c*.6;const p=E.camino(k);return {x:p.x+p.ancho*.4+px*.4,y:p.y,a:h*.085*(1-k*.82)}})();
  const capas=E.medios.map(t=>({y:t.y,f:()=>sprite(t,tt,px)})).concat([{y:vol.y,f:()=>persona(vol.x,vol.y,vol.a,tt,false,true)},{y:cor.y,f:()=>persona(cor.x,cor.y,cor.a,tt,true,false)}]);
  capas.sort((a,b)=>a.y-b.y).forEach(c=>c.f());
  // primer plano
  if(E.palmCerca)sprite(E.palmCerca,tt,px);sprite(E.cerca,tt,px);
  E.motas.forEach(m=>{if(!quieto){m.y-=m.v*.35;m.x+=Math.sin(tt*.6+m.f)*.2;if(m.y<-4){m.y=h+4;m.x=Math.random()*w}}ctx.fillStyle=`rgba(${P.polen},${.2+.2*Math.sin(tt*1.5+m.f)})`;ctx.beginPath();ctx.arc(m.x+px*.8,m.y,m.r,0,7);ctx.fill()});
  if(vivo&&!quieto)id=requestAnimationFrame(dibujar);
 }
 E=null;dibujar();
 const ro=window.ResizeObserver?new ResizeObserver(()=>{E=null;if(quieto)dibujar()}):null;ro&&ro.observe(cv);
 return {parar(){vivo=false;id&&cancelAnimationFrame(id);ro&&ro.disconnect()}};
}

function vComo(){return header('como-funciona')+`<main class="page wrap"><div class="inner"><div class="pagehead"><div><span class="lbl">Cómo funciona</span><h1>Sensores que se mueven por la ciudad</h1></div></div>
 <div class="steps">${[[I.walk,'Un portador lleva el sensor','Voluntarios, personal de limpieza y jardines, bicis compartidas y autobuses.'],[I.phone,'El móvil o el vehículo lo envía','Recibe cada medición por Bluetooth y la manda con la ubicación y la hora.'],[I.server,'El servidor la comprueba','Descarta lo imposible y calcula la media de cada zona.'],[I.mapi,'Mapa por zonas','Medias por parque y barrio. Nunca se publica el recorrido de nadie.']].map((s,i)=>`<div class="step"><span class="n">PASO ${i+1}</span><span class="stepico">${s[0]}</span><h3>${s[1]}</h3><p class="muted" style="margin:0;font-size:14px">${s[2]}</p></div>`).join('')}</div>
 <section class="gv-sec" id="valores" style="display:flex;flex-direction:column;gap:14px"><div><span class="lbl">Guía de valores</span><h2 style="font-size:26px">Qué significan los valores del mapa</h2><p class="muted" style="margin:6px 0 0">Cada zona se colorea según la media de la última hora.</p></div>${guiaHTML()}</section>
 <section class="ef-sec" style="display:flex;flex-direction:column;gap:14px"><div><span class="lbl">Efectos en la salud</span><h2 style="font-size:26px">Qué te puede pasar</h2><p class="muted" style="margin:6px 0 0">Cuanto más sube, más daño. Así afecta cada nivel.</p></div>
 ${efectosHTML()}<p class="note">${EF_AVISO_B}</p></section>
 <section style="display:flex;flex-direction:column;gap:14px"><h2 style="font-size:26px">Hazte voluntario</h2>
 <div style="display:flex;flex-direction:column;gap:14px"><ol style="margin:0;padding-left:22px;display:flex;flex-direction:column;gap:8px;color:var(--ink-2)"><li><b style="color:var(--ink)">Crea una cuenta en la app</b> y apunta tu ID de usuario (U-XXXXX).</li><li><b style="color:var(--ink)">Recoge un sensor</b> en un punto de reparto dando tu ID.</li><li><b style="color:var(--ink)">Escribe en la app el código</b> que lleva el sensor (RSP-XXXX) y pasea con el móvil.</li><li><b style="color:var(--ink)">Devuélvelo</b> en cualquier punto de reparto antes de 24 h.</li></ol>
 <div class="tw"><table><thead><tr><th scope="col">Punto de reparto</th><th scope="col">Dirección</th><th scope="col">Horario</th></tr></thead><tbody>${PUNTOS_REPARTO.map(p=>`<tr><td><b>${p[0]}</b></td><td>${p[1]}</td><td>${p[2]}</td></tr>`).join('')}</tbody></table></div>
 <div class="card" style="padding:16px;display:flex;flex-direction:column;gap:10px"><b style="display:flex;gap:8px;align-items:center">${I.coin} Cómo se ganan los puntos</b><p class="muted" style="margin:0">Cada noche sumamos ${RULES.min===5?'1 punto cada 5 minutos':'1 punto cada '+RULES.min+' minutos'} con datos válidos, el doble dentro de los parques, hasta ${RULES.max} al día. No cuentan los ratos con el sensor parado ni a velocidad de vehículo.</p>
 <div style="display:flex;gap:8px;flex-wrap:wrap">${GIFTS.map(g=>`<span class="badge ${g.stock?'b-real':'b-none'}" style="font-size:13px;padding:6px 12px">${I.gift} ${esc(g.n)} · ${g.c} pts${g.stock?'':' · agotado'}</span>`).join('')}</div></div></div></section>
 <section><h2 style="font-size:24px;margin-bottom:12px">Preguntas frecuentes</h2><div class="card">
 <details open><summary>¿Por qué una zona dice «Sin datos»?</summary><p>Los sensores van con sus portadores. Si en la última hora no ha pasado ninguno por esa zona, no hay datos nuevos. No significa que el aire esté limpio.</p></details>
 <details><summary>¿Se ve por dónde voy cuando llevo un sensor?</summary><p>No. Tu ubicación se usa para situar cada dato y calcular tus puntos, pero en el mapa público solo aparecen medias por zona. El personal municipal tampoco ve recorridos de personas: los sensores de trabajadores van asignados a un servicio o vehículo.</p></details>
 <details><summary>¿Qué significan Bajo, Medio y Alto?</summary><p>Para el ozono usamos como referencia orientativa 0,060 y 0,090 ppm (aprox. 120 y 180 µg/m³, valores europeos). Están pendientes de confirmar con la fuente oficial.</p></details>
 <details><summary>¿Gasta mucha batería o datos?</summary><p>Muy poco: cada medición ocupa menos de 100 bytes. Puedes enviar solo con wifi desde los ajustes de la app.</p></details>
 <details><summary>¿Quién puede ver mis percepciones?</summary><p>Cualquiera, durante 24 horas, en la capa Comunidad. No llevan tu nombre; solo el punto que eliges, el nivel y la hora.</p></details></div></section></div></main>`+footer()}
function vPriv(){return header('privacidad')+`<main class="page wrap"><div class="inner"><div class="prose"><span class="lbl">Privacidad</span><h1>Política de privacidad</h1><span class="hint">Última actualización: noviembre de 2026 (texto de ejemplo para el prototipo)</span>
 ${[['Quién es responsable','El Ayuntamiento, a través de su servicio de medio ambiente, con Nodia como encargada del tratamiento. Contacto del delegado de protección de datos: dpd@ayto.es.'],['Qué datos tratamos','De quien lleva un sensor: cada medición con su ubicación y hora, el código del sensor y su cuenta (nombre de usuario, correo, puntos y canjes). De quien publica percepciones: punto elegido, nivel y hora. La web pública no recoge ningún dato.'],['Para qué','Calcular las medias por zona del mapa público, calcular los puntos de cada voluntario y gestionar el préstamo de sensores y la entrega de regalos.'],['Qué se publica','Solo medias por zona. Nunca recorridos ni ubicaciones individuales. Los sensores de trabajadores públicos se asignan a un servicio o vehículo, no a una persona.'],['Cuánto tiempo','Las ubicaciones individuales se borran a los 30 días; se conservan las medias por zona. Las percepciones se ven 24 h y se borran a los 30 días.'],['Tus derechos','Puedes borrar tu cuenta y tus datos desde la app, en Mi cuenta, o escribir al delegado de protección de datos.']].map(s=>`<h2>${s[0]}</h2><p>${s[1]}</p>`).join('')}</div></div></main>`+footer()}

/* ---------- acceso (WEB-05) ---------- */
function vAcceso(rec){
 const art=`<div class="art"><a class="logo" href="#inicio" aria-label="Respira, inicio">${LOGO(36,'#FFFFFF','#FFFFFF','#5FC4C0')}</a><div style="display:flex;flex-direction:column;gap:14px"><h2>Panel de gestión de sensores, préstamos y puntos</h2><span style="display:inline-flex;gap:8px;align-items:center;color:#B9CFC4">Un servicio de ${NODIA(22,'#FFFFFF')}<b style="letter-spacing:.08em;color:#fff">NODIA</b></span></div><a href="#mapa" style="color:#9EE3C9">← Volver al mapa público</a></div>`;
 if(rec)return `<div class="login">${art}<div class="formside">${S.recOk?`<div style="width:min(400px,100%);display:flex;flex-direction:column;gap:14px"><div class="alert ok">${I.check}<span>Si <b>${esc(S.f.rmail)}</b> está registrado, recibirás un enlace en unos minutos.</span></div><a class="btn" href="#acceso" data-act="recBack">Volver a iniciar sesión</a></div>`:`<form id="f-rec" novalidate><h1 style="font-size:30px">Recuperar contraseña</h1><p class="muted" style="margin:0">Te enviaremos un enlace para crear una nueva.</p><div class="field"><label for="r-mail">Correo municipal</label><input class="input" id="r-mail" type="email" data-bind="f.rmail" value="${esc(S.f.rmail||'')}" placeholder="nombre@ayto.es" autocomplete="email" ${inv('rmail')}>${err('rmail')}</div><button class="btn" type="submit">Enviar enlace</button><p class="note">Por seguridad, no te diremos si el correo existe o no.</p><a href="#acceso">← Volver a iniciar sesión</a></form>`}</div></div>`;
 return `<div class="login">${art}<div class="formside"><form id="f-login" novalidate><div><span class="lbl">Acceso restringido</span><h1 style="font-size:30px">Personal municipal</h1></div>
 ${S.lock?`<div class="alert bad" role="alert">${I.alert}<span><b>Acceso bloqueado 15 minutos</b> por 5 intentos fallidos. <button type="button" class="link" data-act="unlock">Desbloquear (solo demo)</button></span></div>`:S.e.login?`<div class="alert bad" role="alert">${I.alert}<span>${S.e.login}</span></div>`:''}
 <div class="field"><label for="l-mail">Correo</label><input class="input" id="l-mail" type="email" data-bind="f.mail" value="${esc(S.f.mail||'')}" placeholder="nombre@ayto.es" autocomplete="username" ${inv('mail')} ${S.lock?'disabled':''}>${err('mail')}</div>
 <div class="field"><label for="l-pass">Contraseña</label><div style="position:relative"><input class="input" id="l-pass" type="${S.showPass?'text':'password'}" data-bind="f.pass" value="${esc(S.f.pass||'')}" autocomplete="current-password" style="padding-right:84px" ${inv('pass')} ${S.lock?'disabled':''}><button type="button" class="link" style="position:absolute;right:12px;top:13px" data-act="showPass">${S.showPass?'Ocultar':'Mostrar'}</button></div>${err('pass')}</div>
 <button class="btn" type="submit" ${S.lock?'disabled':''}>Entrar</button><a href="#recuperar" style="align-self:center">¿Has olvidado la contraseña?</a>
 <div class="demo-creds"><b>Cuentas de prueba del prototipo</b><br>Administración: <code>admin@ayto.es</code> · <code>Admin2026!</code><br>Técnico: <code>tecnico@ayto.es</code> · <code>Tecnico2026!</code><br>Punto de reparto: <code>reparto@ayto.es</code> · <code>Reparto2026!</code><br>Trabajador (no entra al panel): <code>psanz@ayto.es</code> · <code>Trabajo2026!</code></div></form></div></div>`}

/* ---------- panel ---------- */
const pendDev=()=>LOANS.filter(l=>!l.fin&&l.ini!==undefined&&/^1 nov|^31 oct/.test(l.ini));
const sinDatos=s=>['activo','prestado'].includes(s.est)&&(s.ult==null||s.ult>1440);
const batBaja=s=>s.bat!=null&&s.bat<25&&s.est!=='baja';
function pshell(cur,inner){const u=S.user;const L=[['panel','Flota',I.grid],['panel-sensores','Sensores',I.pin],['panel-prestamos','Préstamos',I.box,pendDev().length],['panel-puntos','Puntos',I.coin,SOSP.filter(d=>d.estado==='pend').length],['panel-regalos','Regalos y canjes',I.gift],['panel-trabajadores','Trabajadores',I.users,TRAB().filter(t=>t.estado==='pendiente').length]].filter(l=>ACCESS[u.r].includes(l[0]));
 return `<div class="panel"><nav class="pside" aria-label="Panel"><a class="logo" href="#${HOME[u.r]}" aria-label="Respira, panel" style="flex-direction:column;align-items:flex-start;gap:4px">${LOGO(26,'#FFFFFF','#FFFFFF','#5FC4C0')}<span style="font-family:var(--f-body);font-size:12px;font-weight:600;color:var(--side-mut);letter-spacing:.06em;text-transform:uppercase">Panel municipal</span></a>${L.map(([h,n,ic,c])=>`<a href="#${h}" ${cur===h||(h==='panel-sensores'&&cur==='panel-editar')?'aria-current="page"':''}>${ic}${n}${c?`<span class="cnt">${c}</span>`:''}</a>`).join('')}<div class="who"><b>${esc(u.n)}</b><span>${ROL[u.r]}${u.punto?' · '+u.punto:''}</span><a href="#mapa" style="padding:0;color:#9EE3C9">Ver web pública</a><button data-act="logout">Cerrar sesión</button></div></nav><main class="pmain">${inner}</main></div>`}
const stCell=s=>{const m={activo:['var(--lv-bajo)','Activo'],prestado:['var(--primary)','Prestado'],disponible:['var(--ink-3)','Disponible'],mant:['var(--mant)','Mantenimiento']}[s.est];return `<span class="st"><i style="background:${m[0]}"></i>${m[1]}</span>`};
const portador=s=>s.tipo==='vol'?(s.est==='prestado'?`Prestado a <b class="mono">${s.user}</b>`:'<span class="hint">En el punto de reparto</span>'):esc(s.asig);
const batCell=s=>s.bat==null?'<span class="hint">Corriente</span>':`<span class="mono" style="${s.bat<25?'color:var(--lv-alto);font-weight:700':''}">${s.bat} %</span>`;
function vFlota(){const q=S.fQ.trim().toLowerCase();
 let rows=SENS.filter(s=>(S.fT==='todos'||s.tipo===S.fT)&&(S.fE==='todos'||(S.fE==='sindatos'&&sinDatos(s))||(S.fE==='bateria'&&batBaja(s))||(S.fE==='pend'&&pendDev().some(l=>l.code===s.code))||(S.fE==='mant'&&s.est==='mant'))&&(!q||(s.code+' '+(s.asig||'')+' '+(s.user||'')).toLowerCase().includes(q)));
 const act=SENS.filter(s=>['activo','prestado'].includes(s.est)&&s.ult!=null&&s.ult<=1440).length;
 return pshell('panel',`<div class="pagehead"><div><h1>Estado de la flota</h1><span class="hint">Datos a 02/11/2026 10:42 · ${SENS.length} sensores</span></div>${S.user.r==='admin'||S.user.r==='tecnico'?'<a class="btn sec" href="#panel-sensores">Gestionar sensores</a>':''}</div>
 <div class="kpis"><div class="kpi"><span class="t">Con datos hoy</span><b>${act}</b><span>de ${SENS.filter(s=>s.est!=='mant').length} en servicio</span></div><button class="kpi" data-act="fE" data-arg="sindatos"><span class="t">Sin datos en 24 h</span><b style="color:var(--lv-medio)">${SENS.filter(sinDatos).length}</b><span>Ver ›</span></button><button class="kpi" data-act="fE" data-arg="bateria"><span class="t">Batería baja (&lt;25 %)</span><b style="color:var(--lv-alto)">${SENS.filter(batBaja).length}</b><span>Ver ›</span></button><button class="kpi" data-act="fE" data-arg="pend"><span class="t">Pendientes de devolución</span><b style="color:var(--lv-alto)">${pendDev().length}</b><span>Más de 24 h fuera ›</span></button></div>
 <p class="note">«Sin datos» no equivale a avería: puede que el sensor no haya salido ese día o que su autobús esté en cocheras. Si un sensor activo lleva varios días sin datos, conviene revisarlo.</p>
 <div class="toolbar"><div class="field" style="width:240px;max-width:100%"><label for="f-q" class="lbl">Buscar</label><input class="input" id="f-q" data-bind="fQ" data-live="1" value="${esc(S.fQ)}" placeholder="Código, línea, servicio o ID"></div>
 <div class="field"><span class="lbl">Portador</span><div class="chips">${[['todos','Todos'],...Object.entries(TIPOS)].map(([k,n])=>`<button class="chip" aria-pressed="${S.fT===k}" data-act="fT" data-arg="${k}">${n}</button>`).join('')}</div></div>
 <div class="field"><span class="lbl">Estado</span><div class="chips">${[['todos','Todos'],['sindatos','Sin datos 24 h'],['bateria','Batería baja'],['pend','Pendiente de devolución'],['mant','Mantenimiento']].map(([k,n])=>`<button class="chip" aria-pressed="${S.fE===k}" data-act="fE" data-arg="${k}">${n}</button>`).join('')}</div></div></div>
 <div class="tw"><table><thead><tr><th scope="col">Código</th><th scope="col">Portador</th><th scope="col">Asignación</th><th scope="col">Último dato</th><th scope="col">Batería</th><th scope="col">Estado</th></tr></thead><tbody>${rows.length?rows.map(s=>`<tr><td class="mono"><b>${s.code}</b></td><td>${TIPOS[s.tipo]}</td><td>${portador(s)}${pendDev().some(l=>l.code===s.code)?' <span class="badge b-alto">Pendiente de devolución</span>':''}</td><td>${s.ult==null?'<span class="hint">Nunca</span>':hace(s.ult)}${sinDatos(s)?' <span class="badge b-medio">Sin datos</span>':''}</td><td>${batCell(s)}</td><td>${stCell(s)}</td></tr>`).join(''):'<tr><td colspan="6" class="hint" style="text-align:center;padding:24px">Ningún sensor cumple estos filtros.</td></tr>'}</tbody></table></div>`)}

function vSensores(){const admin=S.user.r==='admin';
 return pshell('panel-sensores',`<div class="pagehead"><div><h1>Sensores</h1><span class="hint">Alta, asignación, mantenimiento${admin?' y baja':''}</span></div><button class="btn" data-act="editar">+ Nuevo sensor</button></div>
 <div class="tw"><table><thead><tr><th scope="col">Código</th><th scope="col">Mide</th><th scope="col">Portador</th><th scope="col">Asignación</th><th scope="col">Estado</th><th scope="col"><span class="hint" style="text-transform:none;letter-spacing:0">Acciones</span></th></tr></thead><tbody>${SENS.map(s=>`<tr><td class="mono"><b>${s.code}</b></td><td>${s.mags.map(m=>MAG[m].ab).join(' · ')}</td><td>${TIPOS[s.tipo]}</td><td>${portador(s)}${s.est==='mant'?`<br><span class="hint">${esc(s.motivo||'')}</span>`:''}</td><td>${stCell(s)}</td><td style="white-space:nowrap"><button class="link" data-act="editar" data-arg="${s.code}">Editar</button>${admin?` · <button class="link" style="color:var(--lv-alto)" data-act="baja" data-arg="${s.code}" ${s.est==='prestado'?'disabled title="Está prestado: primero registra la devolución"':''}>Dar de baja</button>`:''}</td></tr>`).join('')}</tbody></table></div>
 ${admin?'':'<p class="note">Dar de baja un sensor solo puede hacerlo Administración.</p>'}`)}
function vEditar(){const e=S.edit;if(!e){location.hash='panel-sensores';return ''}const nuevo=!e.orig;
 return pshell('panel-editar',`<nav class="crumb"><a href="#panel-sensores">Sensores</a> › ${nuevo?'Nuevo sensor':e.orig}</nav><h1>${nuevo?'Nuevo sensor':'Editar '+e.orig}</h1>
 <form class="card" id="f-sensor" style="padding:22px;display:flex;flex-direction:column;gap:18px;max-width:760px" novalidate><div class="formgrid">
 <div class="field"><label for="e-code-in">Código del sensor</label><input class="input mono" id="e-code-in" data-bind="edit.code" value="${esc(e.code)}" placeholder="RSP-XXXX" ${nuevo?'':'disabled'} style="text-transform:uppercase" ${inv('code')}><span class="hint">${nuevo?'RSP- y 4 letras o números. Es el que va impreso en la carcasa.':'El código no se puede cambiar.'}</span>${err('code')}</div>
 <div class="field"><span class="flabel">Magnitudes que mide</span><div class="chips" ${S.e.mags?'aria-describedby="e-mags"':''}>${Object.keys(MAG).map(k=>`<button type="button" class="check" role="checkbox" aria-checked="${e.mags.includes(k)}" data-act="emag" data-arg="${k}"><span class="b">${e.mags.includes(k)?I.check:''}</span>${MAG[k].n}</button>`).join('')}</div>${err('mags')}</div>
 <div class="field full"><span class="flabel">Tipo de portador</span><div role="radiogroup" style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px" ${e.orig&&byCode(e.orig).est==='prestado'?'aria-disabled="true"':''}>${Object.entries(TIPOS).map(([k,n])=>`<button type="button" class="radio" role="radio" aria-checked="${e.tipo===k}" data-act="etipo" data-arg="${k}"><span class="o"></span><span><b>${n}</b><br><span class="hint">${k==='vol'?'Se presta en los puntos de reparto':k==='trab'?'Asignado a un servicio, nunca a una persona':k==='bici'?'Va fijo en una bici o moto':'Va fijo en un autobús, con corriente'}</span></span></button>`).join('')}</div></div>
 ${e.tipo!=='vol'?`<div class="field full"><label for="ed-asig">${e.tipo==='trab'?'Servicio':e.tipo==='bus'?'Línea y vehículo':'Vehículo o estación'}</label><input class="input" id="ed-asig" data-bind="edit.asig" value="${esc(e.asig||'')}" placeholder="${e.tipo==='trab'?'Ej.: Limpieza · Brigada de parques 2':e.tipo==='bus'?'Ej.: Línea 2 · Bus 214':'Ej.: Bici compartida · Estación 8'}" ${inv('asig')}>${err('asig')}</div>`:''}
 <div class="field full"><span class="flabel">Estado</span><div class="chips">${[['ok','En servicio'],['mant','En mantenimiento']].map(([k,n])=>`<button type="button" class="chip" aria-pressed="${e.estado===k}" data-act="eest" data-arg="${k}">${n}</button>`).join('')}</div></div>
 ${e.estado==='mant'?`<div class="field full"><label for="ed-mot">Motivo del mantenimiento</label><input class="input" id="ed-mot" data-bind="edit.motivo" value="${esc(e.motivo||'')}" placeholder="Ej.: Carcasa rota al devolverlo" ${inv('motivo')}>${err('motivo')}</div>`:''}
 </div><div style="display:flex;gap:10px;flex-wrap:wrap"><button class="btn" type="submit">${nuevo?'Dar de alta':'Guardar cambios'}</button><a class="btn ghost" href="#panel-sensores">Cancelar</a></div></form>`)}

function vPrestamos(){const f=S.f;const abiertos=LOANS.filter(l=>!l.fin);const cerrados=LOANS.filter(l=>l.fin);
 return pshell('panel-prestamos',`<div class="pagehead"><div><h1>Préstamos de sensores</h1><span class="hint">${S.user.punto?'Punto de reparto: '+S.user.punto:'Todos los puntos de reparto'}</span></div></div>
 <div class="formgrid">
 <form class="card" id="f-prestar" style="padding:20px;display:flex;flex-direction:column;gap:14px" novalidate><h2 style="font-size:20px">Prestar un sensor</h2>
 <div class="field"><label for="p-user">ID de usuario de la app</label><input class="input mono" id="p-user" data-bind="f.puser" value="${esc(f.puser||'')}" placeholder="U-XXXXX" style="text-transform:uppercase" ${inv('puser')}>${err('puser')}</div>
 <div class="field"><label for="p-code">Código del sensor</label><input class="input mono" id="p-code" data-bind="f.pcode" value="${esc(f.pcode||'')}" placeholder="RSP-XXXX" style="text-transform:uppercase" ${inv('pcode')}>${err('pcode')}</div>
 <button class="btn" type="submit">Registrar préstamo</button><span class="hint">Disponibles ahora: ${SENS.filter(s=>s.tipo==='vol'&&s.est==='disponible').map(s=>`<span class="mono">${s.code}</span>`).join(', ')||'ninguno'}</span></form>
 <form class="card" id="f-devolver" style="padding:20px;display:flex;flex-direction:column;gap:14px" novalidate><h2 style="font-size:20px">Registrar una devolución</h2>
 <div class="field"><label for="d-code">Código del sensor</label><input class="input mono" id="d-code" data-bind="f.dcode" value="${esc(f.dcode||'')}" placeholder="RSP-XXXX" style="text-transform:uppercase" ${inv('dcode')}>${err('dcode')}</div>
 <div class="field"><span class="flabel">¿Cómo llega?</span><div role="radiogroup" style="display:flex;flex-direction:column;gap:6px">${[['Correcto','Listo para volver a prestarse'],['Batería baja','Se queda cargando'],['Dañado','Pasa a mantenimiento']].map(o=>`<button type="button" class="radio" role="radio" aria-checked="${(f.dest||'Correcto')===o[0]}" data-act="dest" data-arg="${o[0]}"><span class="o"></span><span><b>${o[0]}</b> <span class="hint">· ${o[1]}</span></span></button>`).join('')}</div></div>
 <button class="btn sec" type="submit">Registrar devolución</button></form></div>
 <h2 style="font-size:20px">En préstamo ahora (${abiertos.length})</h2>
 <div class="tw"><table><thead><tr><th scope="col">Sensor</th><th scope="col">Usuario</th><th scope="col">Desde</th><th scope="col">Punto</th><th scope="col">Estado</th></tr></thead><tbody>${abiertos.length?abiertos.map(l=>`<tr><td class="mono"><b>${l.code}</b></td><td class="mono">${l.user}</td><td>${l.ini}</td><td>${l.punto}</td><td>${pendDev().includes(l)?'<span class="badge b-alto">Pendiente de devolución · más de 24 h</span>':'<span class="badge b-real">En curso</span>'}</td></tr>`).join(''):'<tr><td colspan="5" class="hint" style="text-align:center;padding:20px">No hay sensores prestados.</td></tr>'}</tbody></table></div>
 <h2 style="font-size:20px">Devoluciones recientes</h2>
 <div class="tw"><table><thead><tr><th scope="col">Sensor</th><th scope="col">Usuario</th><th scope="col">Prestado</th><th scope="col">Devuelto</th><th scope="col">Cómo llegó</th></tr></thead><tbody>${cerrados.map(l=>`<tr><td class="mono">${l.code}</td><td class="mono">${l.user}</td><td>${l.ini}</td><td>${l.fin}</td><td>${l.estDev}</td></tr>`).join('')}</tbody></table></div>`)}

function vPuntos(){const r=S.rules;
 return pshell('panel-puntos',`<div class="pagehead"><div><h1>Puntos del voluntariado</h1><span class="hint">El cálculo se hace cada noche con los datos válidos del día</span></div></div>
 <div class="grid2"><form class="card" id="f-rules" style="padding:20px;display:flex;flex-direction:column;gap:14px" novalidate><h2 style="font-size:20px">Reglas de cálculo</h2>
 <div class="formgrid"><div class="field"><label for="r-min">Minutos válidos por punto</label><input class="input mono" id="r-min" data-bind="rules.min" value="${esc(r.min)}" inputmode="numeric" ${inv('rmin')}><span class="hint">Entre 1 y 60.</span>${err('rmin')}</div>
 <div class="field"><label for="r-mult">Multiplicador en parques</label><input class="input mono" id="r-mult" data-bind="rules.mult" value="${esc(r.mult)}" inputmode="numeric" ${inv('rmult')}><span class="hint">Entre 1 y 5.</span>${err('rmult')}</div>
 <div class="field"><label for="r-max">Máximo de puntos al día</label><input class="input mono" id="r-max" data-bind="rules.max" value="${esc(r.max)}" inputmode="numeric" ${inv('rmax')}><span class="hint">Entre 10 y 500.</span>${err('rmax')}</div></div>
 <p class="note">Siempre se descartan los datos con el sensor parado más de 15 minutos o a velocidad de vehículo (más de 25 km/h). Los cambios se aplican desde el día siguiente.</p>
 <button class="btn" type="submit" style="align-self:flex-start">Guardar reglas</button></form>
 <aside class="card" style="padding:20px;display:flex;flex-direction:column;gap:8px;align-self:start"><b>Ejemplo con las reglas actuales</b><span class="muted">Un voluntario con 120 minutos válidos, 40 de ellos en parques:</span><b class="mono" style="font-size:26px">${Math.min(RULES.max,Math.floor(120/RULES.min)+Math.floor(40/RULES.min)*(RULES.mult-1))} puntos</b></aside></div>
 <h2 style="font-size:20px">Días para revisar</h2>
 <div class="tw"><table><thead><tr><th scope="col">Usuario</th><th scope="col">Día</th><th scope="col">Min. válidos</th><th scope="col">Puntos</th><th scope="col">Motivo</th><th scope="col">Estado</th><th scope="col"><span class="hint" style="text-transform:none;letter-spacing:0">Acciones</span></th></tr></thead><tbody>${SOSP.map(d=>`<tr><td class="mono">${d.user}</td><td>${d.d}</td><td class="mono">${d.min}</td><td class="mono"><b>${d.pts}</b></td><td>${esc(d.mot)}${d.razon?`<br><span class="hint">${esc(d.razon)}</span>`:''}</td><td>${d.estado==='pend'?'<span class="badge b-medio">Pendiente</span>':d.estado==='ok'?'<span class="badge b-bajo">Aprobado</span>':'<span class="badge b-alto">Anulado</span>'}</td><td style="white-space:nowrap">${d.estado==='pend'?`<button class="link" data-act="aprobar" data-arg="${d.id}">Aprobar</button> · <button class="link" style="color:var(--lv-alto)" data-act="anular" data-arg="${d.id}">Anular</button>`:'<span class="hint">Revisado</span>'}</td></tr>`).join('')}</tbody></table></div>
 <p class="note">El voluntario ve el resultado en su historial de puntos. Al anular, se le restan los puntos de ese día.</p>`)}

function vRegalos(){const admin=S.user.r==='admin';const c=S.canje;const g=c&&GIFTS.find(x=>x.id===c.gift);
 const entregar=`<form class="card" id="f-canje" style="padding:20px;display:flex;flex-direction:column;gap:14px" novalidate><h2 style="font-size:20px">Entregar un regalo canjeado</h2>
 <div class="field"><label for="c-code">Código de recogida</label><div style="display:flex;gap:8px;flex-wrap:wrap"><input class="input mono" id="c-code" data-bind="f.ccode" value="${esc(S.f.ccode||'')}" placeholder="CJ-XXXX" style="text-transform:uppercase;flex:1;min-width:160px" ${inv('ccode')}><button class="btn" type="submit">Comprobar</button></div>${err('ccode')}</div>
 ${c?`<div class="alert ok" style="align-items:center">${I.gift}<span style="flex:1"><b>${esc(g.n)}</b> para <b class="mono">${c.user}</b> · válido hasta el ${fecha(c.cad)}</span><button type="button" class="btn sm" data-act="entregar">Marcar como entregado</button></div>`:''}
 <span class="hint">Prueba con CJ-8R2T (válido), CJ-2K9L (caducado) o CJ-5P7Q (ya entregado).</span></form>`;
 return pshell('panel-regalos',`<div class="pagehead"><div><h1>${admin?'Regalos y canjes':'Entregar regalos'}</h1><span class="hint">${admin?'Catálogo que ven los voluntarios en la app':'Comprueba el código que enseña el voluntario'}</span></div>${admin?'<button class="btn" data-act="nuevoRegalo">+ Nuevo regalo</button>':''}</div>
 ${entregar}
 ${admin?`<h2 style="font-size:20px">Catálogo</h2><div class="tw"><table><thead><tr><th scope="col">Regalo</th><th scope="col">Coste</th><th scope="col">Existencias</th><th scope="col">En la app</th><th scope="col"><span class="hint" style="text-transform:none;letter-spacing:0">Acciones</span></th></tr></thead><tbody>${GIFTS.map(x=>`<tr><td><b>${esc(x.n)}</b></td><td class="mono">${x.c} pts</td><td class="mono">${x.stock}</td><td>${x.stock?'<span class="badge b-bajo">Disponible</span>':'<span class="badge b-none">Agotado</span>'}</td><td style="white-space:nowrap"><button class="link" data-act="editRegalo" data-arg="${x.id}">Editar</button> · <button class="link" style="color:var(--lv-alto)" data-act="retirarRegalo" data-arg="${x.id}">Retirar</button></td></tr>`).join('')}</tbody></table></div>
 <h2 style="font-size:20px">Canjes</h2><div class="tw"><table><thead><tr><th scope="col">Código</th><th scope="col">Usuario</th><th scope="col">Regalo</th><th scope="col">Caduca</th><th scope="col">Estado</th></tr></thead><tbody>${CANJES.map(x=>`<tr><td class="mono">${x.code}</td><td class="mono">${x.user}</td><td>${esc((GIFTS.find(y=>y.id===x.gift)||{n:'Regalo retirado'}).n)}</td><td>${fecha(x.cad)}</td><td>${x.estado==='entregado'?'<span class="badge b-bajo">Entregado</span>':x.cad<TODAY?'<span class="badge b-none">Caducado · puntos devueltos</span>':'<span class="badge b-medio">Pendiente</span>'}</td></tr>`).join('')}</tbody></table></div>`:''}`)}

/* ---------- trabajadores municipales (alta desde administración) ---------- */
const estTrab=t=>t.estado==='activo'?'<span class="badge b-bajo">Activa</span>':t.caduca<Date.now()?'<span class="badge b-alto">Enlace caducado</span>':'<span class="badge b-medio">Pendiente de activar</span>';
function vTrabajadores(){
 if(!S.user||S.user.r!=='admin')return '';   // doble barrera: la ruta ya lo impide
 const f=S.f,ok=S.altaOk&&STAFF.find(x=>x.id===S.altaOk);
 return pshell('panel-trabajadores',`<div class="pagehead"><div><h1>Trabajadores municipales</h1><span class="hint">Cuentas del personal que lleva sensores. No dan acceso a este panel.</span></div></div>
 ${ok?`<div class="alert ok" role="status">${I.check}<span style="flex:1"><b>${esc(ok.n)}</b> dado de alta como trabajador municipal (<b class="mono">${ok.id}</b>). Le hemos enviado a <b>${esc(ok.e)}</b> un enlace para activar la cuenta y crear su contraseña; caduca en ${ACTIV_H} h y solo sirve una vez.</span><a class="btn sm sec" href="#activar/${ok.token}" title="Solo en el prototipo: abre el enlace que recibiría por correo">Abrir correo de prueba</a></div>`:''}
 <form class="card trab-form" id="f-trab" novalidate>
  <h2 style="font-size:20px">Dar de alta un trabajador</h2>
  <div class="formgrid">
   <div class="field"><label for="t-n">Nombre y apellidos</label><input class="input" id="t-n" data-bind="f.tn" value="${esc(f.tn||'')}" autocomplete="off" maxlength="80" ${inv('tn')}>${err('tn')}</div>
   <div class="field"><label for="t-e">Correo municipal</label><input class="input" id="t-e" type="email" data-bind="f.te" value="${esc(f.te||'')}" placeholder="nombre@ayto.es" autocomplete="off" ${inv('te')}>${err('te')}</div>
   <div class="field"><label for="t-emp">Nº de empleado</label><input class="input mono" id="t-emp" data-bind="f.temp" value="${esc(f.temp||'')}" placeholder="6 cifras" inputmode="numeric" maxlength="6" ${inv('temp')}>${err('temp')}</div>
   <div class="field"><label for="t-s">Servicio</label><select class="input" id="t-s" data-bind="f.ts" ${inv('ts')}><option value="">Elige un servicio…</option>${SERVICIOS.map(x=>`<option ${f.ts===x?'selected':''}>${x}</option>`).join('')}</select>${err('ts')}</div>
  </div>
  <p class="note" style="margin:0">La cuenta se crea con el perfil <b>Trabajador municipal</b>: puede llevar un sensor asignado, pero <b>no tiene acceso al panel</b> ni a ninguna función de administración. La contraseña la crea el propio trabajador con el enlace de activación; administración no la ve nunca. El sensor se asigna aparte, en <a href="#panel-sensores">Sensores</a>.</p>
  <div><button class="btn" type="submit">${I.users} Dar de alta</button></div>
 </form>
 <div class="tw"><table><thead><tr><th scope="col">Trabajador</th><th scope="col">ID de cuenta</th><th scope="col">Nº empleado</th><th scope="col">Servicio</th><th scope="col">Alta</th><th scope="col">Estado</th><th scope="col"><span class="hint" style="text-transform:none;letter-spacing:0">Acciones</span></th></tr></thead><tbody>
 ${TRAB().map(t=>`<tr><td><b>${esc(t.n)}</b><br><span class="hint">${esc(t.e)}</span></td><td class="mono">${t.id}</td><td class="mono">${t.emp}</td><td>${esc(t.serv)}</td><td>${t.alta}</td><td>${estTrab(t)}</td><td style="white-space:nowrap">${t.estado==='pendiente'?`<button class="link" data-act="reenviarActiv" data-arg="${t.id}">Reenviar enlace</button>`:'<span class="hint">—</span>'}</td></tr>`).join('')}
 </tbody></table></div>`)}

function vActivar(){
 const t=STAFF.find(x=>x.r==='trabajador'&&x.token&&x.token===S.tok);
 const art=`<div class="art"><a class="logo" href="#inicio" aria-label="Respira, inicio">${LOGO(36,'#FFFFFF','#FFFFFF','#5FC4C0')}</a><div style="display:flex;flex-direction:column;gap:14px"><h2>Activa tu cuenta de trabajador municipal</h2><span style="display:inline-flex;gap:8px;align-items:center;color:#B9CFC4">Un servicio de ${NODIA(22,'#FFFFFF')}<b style="letter-spacing:.08em;color:#fff">NODIA</b></span></div><a href="#mapa" style="color:#9EE3C9">← Ir al mapa público</a></div>`;
 let b;
 if(S.activOk)b=`<div style="width:min(420px,100%);display:flex;flex-direction:column;gap:14px"><div class="alert ok">${I.check}<span><b>Cuenta activada.</b> Ya puedes entrar en la app Respira con <b>${esc(S.activOk)}</b> y la contraseña que acabas de crear.</span></div><p class="note" style="margin:0">Tu cuenta es de <b>trabajador municipal</b>: sirve para llevar el sensor que te asigne tu servicio. No da acceso al panel de gestión.</p></div>`;
 else if(!t)b=`<div style="width:min(420px,100%);display:flex;flex-direction:column;gap:14px"><div class="alert bad" role="alert">${I.alert}<span><b>Este enlace no es válido.</b> Puede que ya se haya usado o que administración haya enviado uno nuevo. Pide que te lo reenvíen.</span></div></div>`;
 else if(t.caduca<Date.now())b=`<div style="width:min(420px,100%);display:flex;flex-direction:column;gap:14px"><div class="alert bad" role="alert">${I.alert}<span><b>El enlace ha caducado</b> (dura ${ACTIV_H} h). Pide a administración que te envíe uno nuevo.</span></div></div>`;
 else b=`<form id="f-activar" novalidate><div><span class="lbl">Trabajador municipal</span><h1 style="font-size:30px">Hola, ${esc(t.n.split(' ')[0])}</h1></div><p class="muted" style="margin:0">Crea la contraseña de tu cuenta <b class="mono">${t.id}</b> (${esc(t.e)}).</p>
  <div class="field"><label for="ac-p1">Contraseña nueva</label><input class="input" id="ac-p1" type="password" data-bind="f.ap1" value="" autocomplete="new-password" ${inv('ap1')}>${err('ap1')}<span class="hint">Mínimo 10 caracteres, con letras y números.</span></div>
  <div class="field"><label for="ac-p2">Repite la contraseña</label><input class="input" id="ac-p2" type="password" data-bind="f.ap2" value="" autocomplete="new-password" ${inv('ap2')}>${err('ap2')}</div>
  <button class="btn" type="submit">Activar cuenta</button></form>`;
 return `<div class="login">${art}<div class="formside">${b}</div></div>`}

/* ---------- modales ---------- */
function modal(){const m=S.modal;if(!m)return '';let b='';
 if(m.k==='baja'){b=`<h2>¿Dar de baja ${m.id}?</h2><p class="muted" style="margin:0">Dejará de aparecer en la flota y no se podrá prestar. Sus mediciones anteriores se conservan.</p><form id="f-baja" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="b-mot">Motivo</label><select class="input" id="b-mot" data-bind="f.bmot" ${inv('bmot')}><option value="">Elige un motivo…</option>${['Perdido o no devuelto','Avería sin reparación','Sustituido por otro sensor','Otro'].map(o=>`<option ${S.f.bmot===o?'selected':''}>${o}</option>`).join('')}</select>${err('bmot')}</div><div class="field"><label for="b-conf">Para confirmar, escribe <b class="mono">${m.id}</b></label><input class="input mono" id="b-conf" data-bind="f.bconf" value="${esc(S.f.bconf||'')}" autocomplete="off" ${inv('bconf')}>${err('bconf')}</div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn danger" type="submit">Dar de baja</button></div></form>`}
 if(m.k==='anular'){b=`<h2>Anular los puntos del ${SOSP.find(d=>d.id===m.id).d}</h2><form id="f-anular" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="a-mot">Motivo (lo verá el voluntario)</label><textarea class="input" id="a-mot" data-bind="f.amot" maxlength="200" ${inv('amot')}>${esc(S.f.amot||'')}</textarea>${err('amot')}</div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn danger" type="submit">Anular puntos</button></div></form>`}
 if(m.k==='regalo'){b=`<h2>${m.id?'Editar regalo':'Nuevo regalo'}</h2><form id="f-regalo" style="display:flex;flex-direction:column;gap:12px" novalidate><div class="field"><label for="g-n">Nombre</label><input class="input" id="g-n" data-bind="f.gn" value="${esc(S.f.gn||'')}" ${inv('gn')}>${err('gn')}</div><div class="formgrid"><div class="field"><label for="g-c">Coste en puntos</label><input class="input mono" id="g-c" data-bind="f.gc" value="${esc(S.f.gc||'')}" inputmode="numeric" ${inv('gc')}>${err('gc')}</div><div class="field"><label for="g-s">Existencias</label><input class="input mono" id="g-s" data-bind="f.gs" value="${esc(S.f.gs||'')}" inputmode="numeric" ${inv('gs')}>${err('gs')}</div></div><div class="acts"><button type="button" class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn" type="submit">Guardar</button></div></form>`}
 if(m.k==='guia'){b=`<div class="gv-modal-top"><div><span class="lbl">Guía</span><h2>Qué significan los valores</h2></div><button type="button" class="btn ghost sm" data-act="closeModal" aria-label="Cerrar">${I.x}</button></div>${guiaHTML()}`}
 if(m.k==='confirm'){b=`<h2>${m.t}</h2><p class="muted" style="margin:0">${m.p}</p><div class="acts"><button class="btn ghost" data-act="closeModal">Cancelar</button><button class="btn ${m.danger?'danger':''}" data-act="confirmOk">${m.ok}</button></div>`}
 return `<div class="modal" data-act="backdrop"><div class="box${m.k==='guia'?' gv-box':''}" role="dialog" aria-modal="true"${m.k==='guia'?' aria-label="Qué significan los valores"':''}>${b}</div></div>`}

/* ---------- render ---------- */
const $r=document.getElementById('root');
function render(){const r=route();
 if(PAN.includes(r)&&!S.user){location.replace('#acceso');S.e={};S.f.notice=true;return}
 if(PAN.includes(r)&&!ACCESS[S.user.r].includes(r)){location.replace('#'+HOME[S.user.r]);toast('Tu rol no tiene acceso a esa sección');return}
 const V={inicio:vInicio,mapa:()=>{S.layer='zonas';return vMapa()},comunidad:()=>{S.layer='comunidad';return vMapa()},historial:vHist,comparar:vCmp,'como-funciona':vComo,privacidad:vPriv,acceso:()=>vAcceso(false),recuperar:()=>vAcceso(true),panel:vFlota,'panel-sensores':vSensores,'panel-editar':vEditar,'panel-prestamos':vPrestamos,'panel-puntos':vPuntos,'panel-regalos':vRegalos,'panel-trabajadores':vTrabajadores,activar:vActivar};
 const ae=document.activeElement;const keep=ae&&ae.id&&ae.dataset&&ae.dataset.live?[ae.id,ae.selectionStart]:null;
 if(r!=='inicio'&&TV.limpiar)TV.limpiar();
 $r.innerHTML=V[r]()+modal();
 if(GMAP()&&(r==='mapa'||r==='comunidad'))GMAP().montar();
 if(S.modal&&S.modal.k==='guia'&&S.modal.foco){const k=S.modal.foco;S.modal.foco=null;requestAnimationFrame(()=>{const el=document.querySelector('.gv-box .gv[data-k="'+k+'"]');if(!el)return;el.scrollIntoView({block:'nearest'});el.classList.add('gv-foco');setTimeout(()=>el.classList.remove('gv-foco'),2200)})}
 if(r==='inicio')montarInicio();
 if(keep){const el=document.getElementById(keep[0]);if(el){el.focus();try{el.setSelectionRange(keep[1],keep[1])}catch(_){}}}
 if(S.focusErr){S.focusErr=false;const el=$r.querySelector('[aria-invalid="true"]');el&&el.focus()}
 if(r==='acceso'&&S.f.notice){S.f.notice=false;toast('Inicia sesión para entrar en el panel')}
}

/* ---------- vista del mapa: zoom, arrastre y perspectiva ---------- */
const vb=()=>{const v=S.view;return `${v.x.toFixed(1)} ${v.y.toFixed(1)} ${v.w.toFixed(1)} ${v.h.toFixed(1)}`};
function clampV(){const v=S.view;v.x=Math.max(-120,Math.min(1020-v.w,v.x));v.y=Math.max(-90,Math.min(650-v.h,v.y))}
function zoomAt(f,cx,cy){const v=S.view;const nw=Math.min(900,Math.max(220,v.w*f)),k=nw/v.w;v.x=cx-(cx-v.x)*k;v.y=cy-(cy-v.y)*k;v.w=nw;v.h=v.h*k;if(nw>=900){v.x=0;v.y=0;v.w=900;v.h=560}clampV()}
function applyVB(){const m=document.querySelector('.mapwrap svg.map');m&&m.setAttribute('viewBox',vb())}
function svgPt(m,cx,cy){const p=m.createSVGPoint();p.x=cx;p.y=cy;return p.matrixTransform(m.getScreenCTM().inverse())}
let drag=null,justDragged=false;
document.addEventListener('wheel',e=>{const m=e.target.closest&&e.target.closest('.mapwrap svg.map');if(!m)return;e.preventDefault();const p=svgPt(m,e.clientX,e.clientY);zoomAt(e.deltaY>0?1.15:0.87,p.x,p.y);applyVB()},{passive:false});
document.addEventListener('pointerdown',e=>{const m=e.target.closest&&e.target.closest('.mapwrap svg.map');if(!m||e.button!==0)return;const r=m.getBoundingClientRect();drag={x:e.clientX,y:e.clientY,vx:S.view.x,vy:S.view.y,k:Math.max(r.width/S.view.w,r.height/S.view.h),moved:false,m}});
document.addEventListener('pointermove',e=>{if(!drag)return;const dx=e.clientX-drag.x,dy=e.clientY-drag.y;if(!drag.moved&&Math.hypot(dx,dy)<5)return;if(!drag.moved){drag.moved=true;drag.m.closest('.mapwrap').classList.add('drag')}
 S.view.x=drag.vx-dx/drag.k;S.view.y=drag.vy-dy/drag.k*(S.tilt?1.4:1);clampV();applyVB()});
document.addEventListener('pointerup',()=>{if(!drag)return;if(drag.moved){justDragged=true;const w=drag.m.closest('.mapwrap');w&&w.classList.remove('drag');setTimeout(()=>justDragged=false,50)}drag=null});
document.addEventListener('click',e=>{if(justDragged){justDragged=false;e.stopPropagation();e.preventDefault()}},true);

/* ---------- acciones ---------- */
const vEmail=v=>/^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(v);
const A={
 tema:()=>{TEMA=TEMA==='oscuro'?'claro':'oscuro';try{localStorage.setItem('respira-tema',TEMA)}catch(_){}aplicarTema();render()},
 toast:a=>toast(a), menu:()=>{S.menu=true;render()}, menuClose:()=>{S.menu=false;render()},
 layer:a=>{S.layer=a;S.sel=null;if(a==='comunidad'&&route()!=='comunidad')location.hash='comunidad';else if(a==='zonas'&&route()!=='mapa')location.hash='mapa';else render()},
 mag:a=>{S.mag=a;render()}, sel:(a,el)=>{S.sel=a||null;if(a&&el&&el.classList.contains('srow')&&S.view.w<900){const z=byZ(a);const cx=z.x||z.lx,cy=z.y||z.ly;S.view.x=cx-S.view.w/2;S.view.y=cy-S.view.h/2;clampV()}render()},
 toastMsg:a=>toast(a),
 zoom:a=>{if(GMAP()){GMAP().zoom(a);return}const v=S.view;if(a==='reset'){S.view={x:0,y:0,w:900,h:560};S.tilt=false}else{const z=S.sel&&byZ(S.sel);const cx=z?(z.x||z.lx):v.x+v.w/2,cy=z?(z.y||z.ly):v.y+v.h/2;zoomAt(a==='in'?0.7:1/0.7,cx,cy)}render()},
 tilt:()=>{if(GMAP()){GMAP().perspectiva();return}S.tilt=!S.tilt;render();toast(S.tilt?'Vista en perspectiva: puedes seguir arrastrando y tocando las zonas':'Vista en planta')}, avf:a=>{S.avf=+a;render()}, avt:a=>{S.avt=a;S.avf=0;render()},
 toHist:a=>{S.hist={id:a,mag:S.mag,per:'24h',desde:'',hasta:''};S.sel=null;nav('historial')},
 toCmp:a=>{S.cmp.a=a;if(S.cmp.b===a)S.cmp.b=ZONAS.find(z=>z.id!==a&&est(z)==='ok').id;S.sel=null;nav('comparar')},
 hmag:a=>{S.hist.mag=a;render()}, hper:a=>{S.hist.per=a;S.e={};render()},
 swap:()=>{const c=S.cmp;[c.a,c.b]=[c.b,c.a];render()},
 showPass:()=>{S.showPass=!S.showPass;render()}, unlock:()=>{S.lock=false;S.tries=0;S.e={};render()}, recBack:()=>{S.recOk=false},
 logout:()=>{S.user=null;S.f={};S.altaOk=null;toast('Has cerrado sesión');nav('acceso')},
 reenviarActiv:id=>{if(!S.user||S.user.r!=='admin'){toast('Solo administración puede hacerlo');return}const t=STAFF.find(x=>x.id===id&&x.r==='trabajador'&&x.estado==='pendiente');if(!t)return;t.token=nuevoToken();t.caduca=Date.now()+ACTIV_H*36e5;S.altaOk=t.id;render();toast(`Enlace nuevo enviado a ${t.e}. El anterior ya no sirve.`)},
 fT:a=>{S.fT=a;render()}, fE:a=>{S.fE=S.fE===a&&a!=='todos'?'todos':a;if(route()!=='panel')nav('panel');else render()},
 editar:a=>{const s=a&&byCode(a);S.edit=s?{orig:s.code,code:s.code,mags:[...s.mags],tipo:s.tipo,asig:s.asig||'',estado:s.est==='mant'?'mant':'ok',motivo:s.motivo||''}:{orig:null,code:'',mags:[],tipo:'vol',asig:'',estado:'ok',motivo:''};S.e={};nav('panel-editar')},
 emag:a=>{const m=S.edit.mags;const i=m.indexOf(a);i>=0?m.splice(i,1):m.push(a);delete S.e.mags;render()},
 etipo:a=>{const s=S.edit.orig&&byCode(S.edit.orig);if(s&&s.est==='prestado'){toast('Está prestado: registra la devolución antes de cambiar el tipo');return}S.edit.tipo=a;delete S.e.asig;render()},
 eest:a=>{S.edit.estado=a;delete S.e.motivo;render()},
 baja:a=>{if(S.user.r!=='admin')return;S.f.bmot='';S.f.bconf='';S.e={};S.modal={k:'baja',id:a};render()},
 dest:a=>{S.f.dest=a;render()},
 aprobar:a=>{SOSP.find(d=>d.id===a).estado='ok';render();toast('Día aprobado: los puntos se mantienen')},
 anular:a=>{S.f.amot='';S.e={};S.modal={k:'anular',id:a};render()},
 nuevoRegalo:()=>{S.f.gn='';S.f.gc='';S.f.gs='';S.e={};S.modal={k:'regalo',id:null};render()},
 editRegalo:a=>{const g=GIFTS.find(x=>x.id===a);S.f.gn=g.n;S.f.gc=String(g.c);S.f.gs=String(g.stock);S.e={};S.modal={k:'regalo',id:a};render()},
 retirarRegalo:a=>{const g=GIFTS.find(x=>x.id===a);S.modal={k:'confirm',t:`¿Retirar «${g.n}»?`,p:'Dejará de verse en la app. Los canjes pendientes de este regalo se podrán seguir entregando.',ok:'Retirar',danger:true,fn:()=>{GIFTS=GIFTS.filter(x=>x.id!==a);toast('Regalo retirado del catálogo')}};render()},
 entregar:()=>{const c=S.canje;const g=GIFTS.find(x=>x.id===c.gift);c.estado='entregado';if(g)g.stock=Math.max(0,g.stock-1);S.canje=null;S.f.ccode='';render();toast(g&&g.stock===0?`Entregado. «${g.n}» queda agotado en la app.`:'Regalo entregado')},
 closeModal:()=>{S.modal=null;S.e={};render()},
 guia:()=>{S.modal={k:'guia'};render()},
 aValores:k=>{S.modal={k:'guia',foco:k};render()},
 backdrop:(a,el,ev)=>{if(ev.target===el)A.closeModal()}, menuBackdrop:(a,el,ev)=>{if(ev.target===el)A.menuClose()},
 confirmOk:()=>{const f=S.modal.fn;S.modal=null;f();render()}
};
document.addEventListener('click',e=>{const t=e.target.closest('[data-act]');if(!t||t.disabled)return;if((t.dataset.act==='backdrop'||t.dataset.act==='menuBackdrop')&&e.target!==t)return;const f=A[t.dataset.act];if(!f)return;if(!e.target.closest('a[href]'))e.preventDefault();f(t.dataset.arg,t,e)});
document.addEventListener('keydown',e=>{if((e.key==='Enter'||e.key===' ')&&e.target.matches('g[role=button]')){e.preventDefault();e.target.dispatchEvent(new MouseEvent('click',{bubbles:true}))}if(e.key==='Escape'){if(S.modal)A.closeModal();else if(S.menu)A.menuClose();else if(S.sel)A.sel('')}});
function setPath(b,v){const p=b.split('.');let o=S;for(let i=0;i<p.length-1;i++)o=o[p[i]];o[p[p.length-1]]=v}
const ERRKEY={'f.mail':'mail','f.pass':'pass','f.rmail':'rmail','f.puser':'puser','f.pcode':'pcode','f.dcode':'dcode','f.ccode':'ccode','f.bconf':'bconf','f.amot':'amot','f.gn':'gn','f.gc':'gc','f.gs':'gs','edit.code':'code','edit.asig':'asig','edit.motivo':'motivo','rules.min':'rmin','rules.mult':'rmult','rules.max':'rmax','hist.desde':'desde','hist.hasta':'hasta'};
$r.addEventListener('input',e=>{const el=e.target,b=el.dataset.bind;if(!b||el.tagName==='SELECT')return;setPath(b,el.value);
 if(el.dataset.live){render();return}
 const key=ERRKEY[b];if(key&&S.e[key]){delete S.e[key];el.removeAttribute('aria-invalid');const x=document.getElementById('e-'+key);x&&x.remove()}});
$r.addEventListener('change',e=>{const el=e.target;const b=el.dataset.bind;if(!b)return;setPath(b,el.value);if(el.dataset.rr)render();});
$r.addEventListener('submit',e=>{e.preventDefault();const id=e.target.id;const er={};const ok=()=>{S.e=er;if(Object.keys(er).length){S.focusErr=true;render();return false}S.e={};return true};const up=v=>(v||'').trim().toUpperCase();
 if(id==='f-login'){const m=(S.f.mail||'').trim().toLowerCase(),p=S.f.pass||'';
  if(!m)er.mail='Escribe tu correo.';else if(!vEmail(m))er.mail='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';else if(!m.endsWith('@ayto.es'))er.mail='Usa tu correo municipal (termina en @ayto.es).';
  if(!p)er.pass='Escribe tu contraseña.';else if(p.length<8)er.pass=`La contraseña tiene al menos 8 caracteres (has escrito ${p.length}).`;
  if(!ok())return;
  const u=STAFF.find(x=>x.e===m&&x.p!=null&&x.p===p);
  if(!u){S.tries++;S.f.pass='';if(S.tries>=5){S.lock=true}else S.e={login:`<b>Correo o contraseña incorrectos.</b> Te quedan ${5-S.tries} intento${5-S.tries===1?'':'s'} antes del bloqueo temporal.`};render();return}
  if(u.r==='trabajador'){S.tries=0;S.f.pass='';S.e={login:'<b>Tu cuenta es de trabajador municipal.</b> No tiene acceso al panel de gestión: entra en la app Respira con este correo.'};render();return}
  S.user=u;S.tries=0;S.f={};S.e={};toast(`Hola, ${u.n.split(' ')[0]}`);nav(HOME[u.r]);return}
 if(id==='f-trab'){
  if(!S.user||S.user.r!=='admin'){toast('Solo una cuenta de administración puede dar de alta trabajadores');return}
  const n=(S.f.tn||'').trim().replace(/\s+/g,' '),m=(S.f.te||'').trim().toLowerCase(),emp=(S.f.temp||'').trim(),sv=S.f.ts||'';
  if(!n)er.tn='Escribe el nombre y los apellidos.';else if(n.split(' ').length<2)er.tn='Escribe al menos un nombre y un apellido.';
  if(!m)er.te='Escribe el correo municipal.';else if(!vEmail(m))er.te='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';else if(!m.endsWith('@ayto.es'))er.te='Tiene que ser un correo municipal (termina en @ayto.es).';else if(STAFF.some(x=>x.e.toLowerCase()===m))er.te='Ya hay una cuenta con este correo.';
  if(!emp)er.temp='Escribe el número de empleado.';else if(!/^\d{6}$/.test(emp))er.temp='Son 6 cifras, por ejemplo 104233.';else if(TRAB().some(x=>x.emp===emp))er.temp='Este número de empleado ya tiene una cuenta.';
  if(!sv)er.ts='Elige el servicio.';
  if(!ok())return;
  const num=Math.max(0,...TRAB().map(x=>+x.id.slice(2)))+1,cid='T-'+String(num).padStart(4,'0');
  const t={n,e:m,p:null,r:'trabajador',id:cid,emp,serv:sv,estado:'pendiente',alta:'4 nov',por:S.user.e,token:nuevoToken(),caduca:Date.now()+ACTIV_H*36e5};
  STAFF.push(t);S.altaOk=cid;S.f.tn='';S.f.te='';S.f.temp='';S.f.ts='';render();toast(`Trabajador ${cid} dado de alta`);return}
 if(id==='f-activar'){const t=STAFF.find(x=>x.r==='trabajador'&&x.token&&x.token===S.tok);
  if(!t||t.caduca<Date.now()){render();return}
  const p1=S.f.ap1||'',p2=S.f.ap2||'';
  if(!p1)er.ap1='Escribe una contraseña.';else if(p1.length<10)er.ap1=`Tiene que tener al menos 10 caracteres (has escrito ${p1.length}).`;else if(!/[a-z]/i.test(p1)||!/\d/.test(p1))er.ap1='Tiene que llevar letras y números.';else if(p1.toLowerCase().includes(t.e.split('@')[0]))er.ap1='No puede contener tu nombre de usuario.';
  if(!er.ap1&&p2!==p1)er.ap2='Las dos contraseñas no coinciden.';
  if(!ok()){S.f.ap1='';S.f.ap2='';render();return}
  t.p=p1;t.estado='activo';t.token=null;t.caduca=null;S.activOk=t.e;S.f.ap1='';S.f.ap2='';render();return}
 if(id==='f-rec'){const m=(S.f.rmail||'').trim();if(!m)er.rmail='Escribe tu correo.';else if(!vEmail(m))er.rmail='El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';if(!ok())return;S.recOk=true;render();return}
 if(id==='f-rango'){const h=S.hist;if(!h.desde)er.desde='Elige la fecha de inicio.';else if(h.desde<'2026-06-01')er.desde='No hay datos antes del 01/06/2026.';else if(h.desde>TODAY)er.desde='No puede ser una fecha futura.';
  if(!h.hasta)er.hasta='Elige la fecha de fin.';else if(h.hasta>TODAY)er.hasta='No puede ser una fecha futura.';else if(h.desde&&h.hasta<h.desde)er.hasta='La fecha de fin debe ser posterior a la de inicio.';
  else if(h.desde&&(new Date(h.hasta)-new Date(h.desde))/864e5>90)er.hasta='El rango máximo es de 90 días.';if(!ok())return;render();toast(`Mostrando del ${h.desde.split('-').reverse().join('/')} al ${h.hasta.split('-').reverse().join('/')}`);return}
 if(id==='f-sensor'){const s=S.edit;const c=up(s.code);
  if(!s.orig){if(!c)er.code='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.code='Formato no válido: RSP- y 4 letras o números, por ejemplo RSP-7Z2K.';else if(byCode(c))er.code=`Ya existe un sensor con el código ${c}.`}
  if(!s.mags.length)er.mags='Marca al menos una magnitud.';
  if(s.tipo!=='vol'&&(s.asig||'').trim().length<3)er.asig='Indica a qué servicio, línea o vehículo va asignado (mínimo 3 caracteres).';
  if(s.estado==='mant'&&(s.motivo||'').trim().length<5)er.motivo='Explica el motivo del mantenimiento (mínimo 5 caracteres).';
  if(!ok())return;
  const asig=s.tipo==='vol'?null:s.asig.trim();
  if(s.orig){const o=byCode(s.orig);o.mags=s.mags;o.tipo=s.tipo;o.asig=asig;if(s.estado==='mant'){o.est='mant';o.motivo=s.motivo.trim()}else if(o.est==='mant'){o.est=s.tipo==='vol'?'disponible':'activo';o.motivo=null}else if(o.est!=='prestado')o.est=s.tipo==='vol'?'disponible':'activo';toast(`${o.code} guardado`)}
  else{SENS.push({code:c,tipo:s.tipo,asig,mags:s.mags,est:s.estado==='mant'?'mant':s.tipo==='vol'?'disponible':'activo',motivo:s.estado==='mant'?s.motivo.trim():null,ult:null,bat:s.tipo==='bus'?null:100});toast(`${c} dado de alta${s.tipo==='vol'?': ya se puede prestar':''}`)}
  S.edit=null;nav('panel-sensores');return}
 if(id==='f-baja'){const c=S.modal.id;if(!S.f.bmot)er.bmot='Elige un motivo.';if(up(S.f.bconf)!==c)er.bconf=`Escribe exactamente ${c} para confirmar.`;if(!ok())return;SENS=SENS.filter(x=>x.code!==c);S.modal=null;render();toast(`${c} dado de baja. Sus mediciones se conservan.`);return}
 if(id==='f-prestar'){const u=up(S.f.puser),c=up(S.f.pcode);
  if(!u)er.puser='Escribe el ID que aparece en la app del voluntario.';else if(!/^U-\d{5}$/.test(u))er.puser='Formato no válido: U- y 5 cifras, por ejemplo U-48213.';else if(!APPUSERS.some(x=>x.id===u))er.puser=`No existe ningún usuario ${u}. Pídele que lo compruebe en la app, en Mi cuenta.`;else if(LOANS.some(l=>l.user===u&&!l.fin))er.puser=`${u} ya tiene un sensor prestado. Tiene que devolverlo antes de llevarse otro.`;
  const s=byCode(c);if(!c)er.pcode='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.pcode='Formato no válido: RSP- y 4 letras o números.';else if(!s)er.pcode=`No existe ningún sensor ${c}.`;else if(s.tipo!=='vol')er.pcode=`${c} va asignado a ${TIPOS[s.tipo].toLowerCase()}: no se presta.`;else if(s.est==='prestado')er.pcode=`${c} ya está prestado a ${s.user}.`;else if(s.est==='mant')er.pcode=`${c} está en mantenimiento.`;
  if(!ok())return;s.est='prestado';s.user=u;LOANS.unshift({code:c,user:u,ini:'2 nov 10:42',fin:null,punto:S.user.punto||'Biblioteca Central'});S.f.puser='';S.f.pcode='';render();toast(`${c} prestado a ${u}. Ya puede escribir el código en su app.`);return}
 if(id==='f-devolver'){const c=up(S.f.dcode);const s=byCode(c);const l=LOANS.find(x=>x.code===c&&!x.fin);
  if(!c)er.dcode='Escribe el código del sensor.';else if(!/^RSP-[A-Z0-9]{4}$/.test(c))er.dcode='Formato no válido: RSP- y 4 letras o números.';else if(!s)er.dcode=`No existe ningún sensor ${c}.`;else if(!l)er.dcode=`${c} no figura como prestado.`;
  if(!ok())return;const st=S.f.dest||'Correcto';l.fin='2 nov 10:42';l.estDev=st;s.user=null;if(st==='Dañado'){s.est='mant';s.motivo='Dañado al devolverlo'}else{s.est='disponible';if(st==='Batería baja')s.bat=12}
  S.f.dcode='';S.f.dest='Correcto';render();toast(st==='Dañado'?`${c} devuelto y en mantenimiento`:`${c} devuelto${st==='Batería baja'?': ponlo a cargar':''}`);return}
 if(id==='f-rules'){const r=S.rules;const chk=(k,v,a,b,msg)=>{const n=String(v).trim();if(!/^\d+$/.test(n))er[k]='Escribe un número entero.';else if(+n<a||+n>b)er[k]=msg};
  chk('rmin',r.min,1,60,'Debe estar entre 1 y 60 minutos.');chk('rmult',r.mult,1,5,'Debe estar entre 1 y 5.');chk('rmax',r.max,10,500,'Debe estar entre 10 y 500 puntos.');
  if(!ok())return;RULES={min:+r.min,mult:+r.mult,max:+r.max};render();toast('Reglas guardadas: se aplican desde mañana');return}
 if(id==='f-anular'){const t=(S.f.amot||'').trim();if(t.length<10)er.amot='Explica el motivo con al menos 10 caracteres: el voluntario lo verá.';if(!ok())return;const d=SOSP.find(x=>x.id===S.modal.id);d.estado='anulado';d.razon=t;S.modal=null;render();toast(`Puntos anulados: se restan ${d.pts} a ${d.user}`);return}
 if(id==='f-regalo'){const n=(S.f.gn||'').trim(),c=String(S.f.gc||'').trim(),st=String(S.f.gs||'').trim();
  if(n.length<3)er.gn='Escribe un nombre de al menos 3 caracteres.';else if(GIFTS.some(g=>g.n.toLowerCase()===n.toLowerCase()&&g.id!==S.modal.id))er.gn='Ya hay un regalo con ese nombre.';
  if(!/^\d+$/.test(c)||+c<1||+c>5000)er.gc='Entre 1 y 5000 puntos.';if(!/^\d+$/.test(st)||+st>999)er.gs='Entre 0 y 999 unidades.';
  if(!ok())return;if(S.modal.id){Object.assign(GIFTS.find(g=>g.id===S.modal.id),{n,c:+c,stock:+st})}else GIFTS.push({id:'g'+Date.now(),n,c:+c,stock:+st});S.modal=null;render();toast('Catálogo actualizado');return}
 if(id==='f-canje'){const c=up(S.f.ccode);S.canje=null;const x=CANJES.find(y=>y.code===c);
  if(!c)er.ccode='Escribe el código que enseña el voluntario.';else if(!/^CJ-[A-Z0-9]{4}$/.test(c))er.ccode='Formato no válido: CJ- y 4 letras o números.';else if(!x)er.ccode=`No existe ningún canje ${c}.`;else if(x.estado==='entregado')er.ccode=`El canje ${c} ya se entregó.`;else if(x.cad<TODAY)er.ccode=`El canje ${c} caducó el ${fecha(x.cad)}: los puntos ya se devolvieron al usuario.`;
  if(!ok())return;S.canje=x;render();return}
});
/* Lo que necesita js/mapa-google.js */
window.Respira={S,ZONAS,AVISOS,MAG,PERC,nivel,est,fmt,hace,esc,avOk,render,byZ};
aplicarTema();
render();
})();
