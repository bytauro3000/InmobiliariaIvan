package com.Inmobiliaria.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Utilidad para convertir fechas de negocio (solo día, type="date" del frontend)
 * a fecha+hora para persistir en columnas DATETIME.
 *
 * Regla:
 *  - fecha null          → fecha y hora actuales (registro en curso)
 *  - fecha = hoy         → fecha y hora actuales (pago registrado hoy conserva la hora real)
 *  - fecha pasada/futura → 00:00:00 (fecha elegida manualmente, ej. soporte editando)
 *  - hora explícita      → se conserva TAL CUAL (hora leída del voucher por OCR,
 *                          válida aunque el día sea otro: "ayer 14:35:22" queda exacta)
 */
public final class FechasUtil {

    private FechasUtil() {
    }

    public static LocalDateTime aFechaHora(LocalDate fecha) {
        if (fecha == null) {
            return LocalDateTime.now();
        }
        return fecha.equals(LocalDate.now()) ? LocalDateTime.now() : fecha.atStartOfDay();
    }

    /** Con hora exacta del voucher: si la hay, manda ella (sin importar si es hoy o ayer). */
    public static LocalDateTime aFechaHora(LocalDate fecha, LocalTime hora) {
        if (hora != null) {
            return LocalDateTime.of(fecha != null ? fecha : LocalDate.now(), hora);
        }
        return aFechaHora(fecha);
    }

    /**
     * Variante para EDICIÓN de pagos existentes:
     *  - fecha null            → se conserva la fecha original
     *  - mismo día que original→ se conserva la hora original (evita perder la hora real)
     *  - día distinto (hoy)    → fecha y hora actuales
     *  - día distinto (pasado) → 00:00:00
     *  - hora explícita nueva  → se aplica (fecha elegida + hora del voucher re-subido)
     */
    public static LocalDateTime aFechaHora(LocalDate fecha, LocalDateTime original) {
        if (fecha == null) {
            return original != null ? original : LocalDateTime.now();
        }
        if (original != null && fecha.equals(original.toLocalDate())) {
            return original;
        }
        return fecha.equals(LocalDate.now()) ? LocalDateTime.now() : fecha.atStartOfDay();
    }

    /** Edición con hora explícita del voucher: sin hora, conserva el original como antes. */
    public static LocalDateTime aFechaHora(LocalDate fecha, LocalTime hora, LocalDateTime original) {
        if (hora != null) {
            return aFechaHora(fecha, hora);
        }
        return aFechaHora(fecha, original);
    }
}
