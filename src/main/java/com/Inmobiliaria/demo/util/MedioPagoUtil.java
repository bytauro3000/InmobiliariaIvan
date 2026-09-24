package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.enums.MedioPago;

/**
 * Medios de pago que exigen voucher (N° de operación y fecha de operación).
 * Lista única compartida por letras, mora, inicial e inscripción.
 */
public final class MedioPagoUtil {

    private MedioPagoUtil() {
    }

    public static boolean esBancario(MedioPago medio) {
        return medio == MedioPago.DEPOSITO
            || medio == MedioPago.TRANSFERENCIA
            || medio == MedioPago.YAPE
            || medio == MedioPago.PLIN
            || medio == MedioPago.OTROS;
    }
}
