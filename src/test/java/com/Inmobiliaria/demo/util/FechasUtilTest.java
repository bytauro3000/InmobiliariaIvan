package com.Inmobiliaria.demo.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test unitario de las reglas de fecha+hora usadas por los pagos (columnas DATETIME).
 */
class FechasUtilTest {

    private static final LocalDate AYER = LocalDate.of(2026, 9, 23);
    private static final LocalTime HORA_VOUCHER = LocalTime.of(14, 35, 22);

    @Test
    void horaExacta_seConserva_aunqueElDiaSeaOtro() {
        LocalDateTime resultado = FechasUtil.aFechaHora(AYER, HORA_VOUCHER);
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35, 22), resultado,
                "la hora leída del voucher debe quedar exacta aunque el pago no sea de hoy");
    }

    @Test
    void sinHora_diaPasado_esMedianoche() {
        LocalDateTime resultado = FechasUtil.aFechaHora(AYER, (LocalTime) null);
        assertEquals(AYER.atStartOfDay(), resultado);
    }

    @Test
    void sinHora_diaHoy_usaAhora() {
        LocalDateTime resultado = FechasUtil.aFechaHora(LocalDate.now(), (LocalTime) null);
        assertEquals(LocalDate.now(), resultado.toLocalDate());
        assertNotNull(resultado.toLocalTime());
    }

    @Test
    void edicion_conHoraNueva_laAplica() {
        LocalDateTime original = LocalDateTime.of(2026, 9, 20, 9, 0, 0);
        LocalDateTime resultado = FechasUtil.aFechaHora(AYER, HORA_VOUCHER, original);
        assertEquals(LocalDateTime.of(2026, 9, 23, 14, 35, 22), resultado);
    }

    @Test
    void edicion_sinHora_mismoDia_conservaOriginal() {
        LocalDateTime original = LocalDateTime.of(2026, 9, 20, 9, 30, 15);
        LocalDateTime resultado = FechasUtil.aFechaHora(LocalDate.of(2026, 9, 20), null, original);
        assertEquals(original, resultado);
    }
}
