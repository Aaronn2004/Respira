/* Copia este archivo como js/config.js y pon tu clave.
   js/config.js está en .gitignore: la clave NUNCA se sube al repositorio.
   En Google Cloud, restringe la clave a «Maps JavaScript API» y a tus
   dominios (p. ej. http://127.0.0.1:5500/* para Live Server). */
window.RESPIRA_CONFIG = {
  MAPS_API_KEY: 'pon_aqui_tu_clave_de_maps_javascript_api',
  // Opcional: ID de un mapa VECTORIAL (Google Cloud > Map Management).
  // Activa el botón 3D (inclinar y girar). Vacío = sin botón 3D.
  MAP_ID: '',
  // API de Respira (servidor/api/index.php). Con ella, la web enseña datos REALES;
  // vacía o sin responder, enseña los de demostración (y lo avisa).
  //   Plesk:            'https://TU_DOMINIO/respira/api/index.php'
  //   En tu PC (php -S): 'http://localhost:8080/index.php'
  API_URL: 'https://TU_DOMINIO/respira/api/index.php',
};
