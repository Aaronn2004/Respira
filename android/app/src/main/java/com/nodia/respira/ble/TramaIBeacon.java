package com.nodia.respira.ble;

import java.util.Arrays;

// ==============================================================
// TramaIBeacon: interpreta los bytes de un anuncio BLE como iBeacon.
//
// En vez de suponer posiciones fijas, busca la cabecera iBeacon
// 4C 00 02 15 (fabricante Apple + tipo iBeacon + longitud 21).
// Así funciona tanto si el anuncio lleva el campo "flags" delante como si no.
//
//   ... 4C 00 | 02 15 | UUID (16) | major (2) | minor (2) | txPower (1)
//
// Convención de nuestro proyecto:
//   major = (tipoMedicion << 8) + contador
//   minor = valor (entero)
// ==============================================================
public class TramaIBeacon {

    private static final byte[] CABECERA_IBEACON = { 0x4C, 0x00, 0x02, 0x15 };
    private static final int LONGITUD_TRAS_CABECERA = 16 + 2 + 2 + 1;

    private final byte[] losBytes;
    private byte[] uuid;
    private byte[] major;
    private byte[] minor;
    private byte txPower;
    private boolean esIBeacon = false;

    // --------------------------------------------------------------
    // bytes: [N] --> TramaIBeacon()
    // --------------------------------------------------------------
    public TramaIBeacon(byte[] bytes) {
        this.losBytes = bytes;

        int posicionCabecera = buscarCabecera(bytes);
        if (posicionCabecera < 0) {
            return;
        }

        int inicioUUID = posicionCabecera + CABECERA_IBEACON.length;
        this.uuid = Arrays.copyOfRange(bytes, inicioUUID, inicioUUID + 16);
        this.major = Arrays.copyOfRange(bytes, inicioUUID + 16, inicioUUID + 18);
        this.minor = Arrays.copyOfRange(bytes, inicioUUID + 18, inicioUUID + 20);
        this.txPower = bytes[inicioUUID + 20];
        this.esIBeacon = true;
    }

    // --------------------------------------------------------------
    // bytes: [N] --> buscarCabecera() --> Z   (-1 si no está)
    // --------------------------------------------------------------
    private static int buscarCabecera(byte[] bytes) {
        if (bytes == null) {
            return -1;
        }
        int ultimaPosicionPosible = bytes.length - CABECERA_IBEACON.length - LONGITUD_TRAS_CABECERA;
        for (int i = 0; i <= ultimaPosicionPosible; i++) {
            if (bytes[i] == CABECERA_IBEACON[0] && bytes[i + 1] == CABECERA_IBEACON[1]
                    && bytes[i + 2] == CABECERA_IBEACON[2] && bytes[i + 3] == CABECERA_IBEACON[3]) {
                return i;
            }
        }
        return -1;
    }

    // --------------------------------------------------------------
    // esIBeacon() --> B
    // --------------------------------------------------------------
    public boolean esIBeacon() {
        return esIBeacon;
    }

    // --------------------------------------------------------------
    // getUUIDComoTexto() --> Text
    // --------------------------------------------------------------
    public String getUUIDComoTexto() {
        return Utilidades.bytesToString(uuid);
    }

    // --------------------------------------------------------------
    // getMajorEntero() --> N
    // --------------------------------------------------------------
    public int getMajorEntero() {
        return Utilidades.bytesToIntOK(major);
    }

    // --------------------------------------------------------------
    // getTipoMedicion() --> N    (byte alto del major)
    // --------------------------------------------------------------
    public int getTipoMedicion() {
        return getMajorEntero() >> 8;
    }

    // --------------------------------------------------------------
    // getContador() --> N        (byte bajo del major)
    // --------------------------------------------------------------
    public int getContador() {
        return getMajorEntero() & 0xFF;
    }

    // --------------------------------------------------------------
    // getValorEntero() --> N     (minor)
    // --------------------------------------------------------------
    public int getValorEntero() {
        return Utilidades.bytesToIntOK(minor);
    }

    public byte[] getLosBytes() { return losBytes; }
    public byte[] getUUID() { return uuid; }
    public byte[] getMajor() { return major; }
    public byte[] getMinor() { return minor; }
    public byte getTxPower() { return txPower; }
}
