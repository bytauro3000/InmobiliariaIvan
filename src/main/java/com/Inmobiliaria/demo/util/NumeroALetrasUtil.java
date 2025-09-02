package com.Inmobiliaria.demo.util;

import java.math.BigDecimal;

public class NumeroALetrasUtil {
    private static final String[] UNIDADES = {
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"
    };

    private static final String[] DIEZ_A_QUINCE = {
        "diez", "once", "doce", "trece", "catorce", "quince"
    };

    private static final String[] DECENAS = {
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    private static final String[] CENTENAS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos",
        "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    public static String convertir(BigDecimal importe) {
        int parteEntera = importe.intValue();
        int parteDecimal = importe.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        String letras = convertirNumero(parteEntera).toUpperCase();
        String centavos = String.format("%02d", parteDecimal);

        return letras + " CON " + centavos + "/100 DÓLARES AMERICANOS";
    }

    private static String convertirNumero(int numero) {
        if (numero == 0) return "cero";
        if (numero == 100) return "cien";

        StringBuilder resultado = new StringBuilder();

        if (numero >= 1_000_000) {
            int millones = numero / 1_000_000;
            resultado.append(millones == 1 ? "un millón" : convertirNumero(millones) + " millones");
            numero %= 1_000_000;
            if (numero > 0) resultado.append(" ");
        }

        if (numero >= 1000) {
            int miles = numero / 1000;
            if (miles == 1) {
                resultado.append("mil");
            } else {
                resultado.append(convertirNumero(miles)).append(" mil");
            }
            numero %= 1000;
            if (numero > 0) resultado.append(" ");
        }

        if (numero >= 100) {
            int c = numero / 100;
            resultado.append(CENTENAS[c]);
            numero %= 100;
            if (numero > 0) resultado.append(" ");
        }

        if (numero >= 10 && numero <= 15) {
            resultado.append(DIEZ_A_QUINCE[numero - 10]);
        } else if (numero < 10) {
            resultado.append(UNIDADES[numero]);
        } else {
            int d = numero / 10;
            int u = numero % 10;
            if (d == 2 && u > 0) {
                resultado.append("veinti").append(UNIDADES[u]);
            } else {
                resultado.append(DECENAS[d]);
                if (u > 0) resultado.append(" y ").append(UNIDADES[u]);
            }
        }

        return resultado.toString().trim();
    }
}