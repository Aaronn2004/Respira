<?php
// ==============================================================
// Copia este archivo como config.php EN EL SERVIDOR (Plesk) y rellénalo.
// config.php está en .gitignore: la contraseña NUNCA se sube a git.
// Los datos salen de Plesk › Bases de datos › respira › Usuarios.
// ==============================================================
return [
    'host'    => 'localhost',
    'bd'      => 'respira',
    'usuario' => 'pon_aqui_el_usuario',
    'clave'   => 'pon_aqui_la_contraseña',
    'prefijo' => '',
    // Opcional. Dirección de la web: se usa en los enlaces de los correos
    // (recuperar contraseña) y de activación de trabajadores. Si se deja
    // vacía, se deduce sola (…/respira/api → …/respira/web/).
    'url_web' => '',
    // Mediciones simuladas para que todas las zonas tengan datos (salvo las
    // que enseñan «Sin datos» y «Antiguo»). Se marcan como 'simulado' y los
    // datos reales mandan sobre ellas. false = solo datos reales.
    'simulacion' => true,
    // Opcional. Remitente de los correos (Plesk tiene que poder enviar desde él).
    // 'correo_remitente' => 'no-responder@tu-dominio',
];
