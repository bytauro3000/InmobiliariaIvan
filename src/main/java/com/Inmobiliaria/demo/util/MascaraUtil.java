package com.Inmobiliaria.demo.util;

/**
 * Enmascarado de datos sensibles para el PORTAL DEL CLIENTE.
 * El portal es solo lectura y nunca debe exponer DNI ni teléfonos completos.
 */
public final class MascaraUtil {

    private MascaraUtil() {
    }

    /** "12345678" → "****5678"; null/blank → null; cortos → todo enmascarado menos el último. */
    public static String enmascararDocumento(String numDoc) {
        if (numDoc == null || numDoc.isBlank()) {
            return null;
        }
        String limpio = numDoc.trim();
        if (limpio.length() <= 4) {
            return "*".repeat(limpio.length());
        }
        return "*".repeat(limpio.length() - 4) + limpio.substring(limpio.length() - 4);
    }

    /** "987654321" → "9**-***-321"; null/blank → null. */
    public static String enmascararCelular(String celular) {
        if (celular == null || celular.isBlank()) {
            return null;
        }
        String limpio = celular.trim().replaceAll("\\s+", "");
        if (limpio.length() <= 3) {
            return "*".repeat(limpio.length());
        }
        int inicioUltimo = limpio.length() - 3;
        int inicioMedio = Math.max(1, inicioUltimo - 3);
        return limpio.charAt(0)
                + "*".repeat(inicioMedio - 1) + "-"
                + "*".repeat(inicioUltimo - inicioMedio) + "-"
                + limpio.substring(inicioUltimo);
    }
}
