package com.Inmobiliaria.demo.util;

public class NumeroALetras {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "};
    private static final String[] DECENAS = {"", "DIEZ ", "VEINTE ", "TREINTA ", "CUARENTA ", "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "};
    private static final String[] ESPECIALES = {"DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ", "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE "};
    private static final String[] CENTENAS = {"", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINCIENTOS ", "SEISCIENTOS ", "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String convertir(double numero) {
        long entero = (long) numero;
        int centavos = (int) (Math.round((numero - entero) * 100));
        
        String letras = convertirRecursivo(entero).trim();
        return "(" + letras + " CON " + String.format("%02d", centavos) + "/100 DÓLARES AMERICANOS)";
    }

    private static String convertirRecursivo(long n) {
        if (n == 0) return "CERO ";
        if (n == 100) return "CIEN ";
        if (n < 10) return UNIDADES[(int) n];
        if (n < 20) return ESPECIALES[(int) (n - 10)];
        if (n < 30) return (n == 20) ? "VEINTE " : "VEINTI" + convertirRecursivo(n % 10);
        if (n < 100) return DECENAS[(int) (n / 10)] + ((n % 10 != 0) ? "Y " + convertirRecursivo(n % 10) : "");
        if (n < 1000) return CENTENAS[(int) (n / 100)] + convertirRecursivo(n % 100);
        if (n < 1000000) {
            String miles = (n / 1000 == 1) ? "MIL " : convertirRecursivo(n / 1000) + "MIL ";
            return miles + convertirRecursivo(n % 1000);
        }
        if (n < 1000000000L) {
            String millones = (n / 1000000 == 1) ? "UN MILLÓN " : convertirRecursivo(n / 1000000) + "MILLONES ";
            return millones + convertirRecursivo(n % 1000000);
        }
        return "NÚMERO DEMASIADO GRANDE";
    }
}