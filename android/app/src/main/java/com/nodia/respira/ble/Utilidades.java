package com.nodia.respira.ble;

public class Utilidades {

    // --------------------------------------------------------------
    // bytes: [N] --> bytesToString() --> Text
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
    // bytes: [N] --> bytesToHexString() --> Text
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
    // bytes: [N]_2 --> bytesToIntOK() --> N   (big endian, sin signo)
    // --------------------------------------------------------------
    public static int bytesToIntOK(byte[] bytes) {
        if (bytes == null || bytes.length < 2) return 0;
        return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
    }
}
