package com.nodia.respira.ble;

// # Conversión de bytes de anuncios BLE a texto, hexadecimal y enteros sin signo.

public class Utilidades {

    // --------------------------------------------------------------
    // + bytes: [Z]
    //       -->
    //     bytesToString() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------
    // + bytes: [Z]
    //       -->
    //     bytesToHexString() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // --------------------------------------------------------------
    // + bytes: [Z]
    //       -->
    //     bytesToIntOK() --x
    //       <--
    //   Z
    // --------------------------------------------------------------
    public static int bytesToIntOK(byte[] bytes) {
        if (bytes == null || bytes.length < 2) return 0;
        return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
    }
}
