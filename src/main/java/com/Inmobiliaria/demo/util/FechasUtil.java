package com.Inmobiliaria.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Utilidad para convertir fechas de negocio (solo día, type="date" del frontend)
 * a fecha+hora para persistir en columnas DATETIME.
 *
 * Regla:
 *  - fecha null          → fecha y hora actuales (registro en curso)
 *  - fecha = hoy         → fecha y hora actuales (pago registrado hoy conserva la hora real)
 *  - fecha pasada/futura → 00:00:00 (fecha elegida manualmente, ej. soporte editando)
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

    /**
     * Variante para EDICIÓN de pagos existentes:
     *  - fecha null            → se conserva la fecha original
     *  - mismo día que original→ se conserva la hora original (evita perder la hora real)
     *  - día distinto (hoy)    → fecha y hora actuales
     *  - día distinto (pasado) → 00:00:00
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
}
