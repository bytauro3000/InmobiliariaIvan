package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.service.impl.NotificacionAdminEmailService.NotificacionPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test unitario (sin contexto Spring) del template del correo de notificación de pagos.
 * Env: la fecha/hora bajo el logo y el bloque de voucher en la parte inferior.
 */
class NotificacionAdminEmailServiceTest {

    private static final LocalDateTime PAGO_HOY      = LocalDateTime.of(2026, 9, 24, 15, 43, 10);
    private static final LocalDateTime OPER_VOUCHER  = LocalDateTime.of(2026, 9, 23, 10, 5, 0);

    private NotificacionPago pago(String medioPago, LocalDateTime fechaPago, LocalDateTime fechaOperacion) {
        return new NotificacionPago("PAGO DE LETRA", "PAGO_LETRA", 123,
                "Pago de letra N\u00B0 3 Mz. C Lt. 18 del Programa: PROGRAMA DE VIVIENDA PALMAS DE MALLORCA ROMA ALTA",
                "FELIX BENJAMIN SILVA MEDINA", new BigDecimal("196.00"), Moneda.USD,
                medioPago, fechaPago, fechaOperacion);
    }

    // ── resolverFechaHora ─────────────────────────────────────────────────────

    @Test
    void fechaHora_efectivo_usaFechaDePago() {
        assertEquals(PAGO_HOY, NotificacionAdminEmailService.resolverFechaHora("EFECTIVO", PAGO_HOY, OPER_VOUCHER));
    }

    @Test
    void fechaHora_bancario_usaFechaDeOperacion() {
        assertEquals(OPER_VOUCHER, NotificacionAdminEmailService.resolverFechaHora("YAPE", PAGO_HOY, OPER_VOUCHER));
    }

    @Test
    void fechaHora_bancario_sinOperacion_haceFallbackAPago() {
        assertEquals(PAGO_HOY, NotificacionAdminEmailService.resolverFechaHora("DEPOSITO", PAGO_HOY, null));
    }

    @Test
    void fechaHora_nulo_haceFallbackAPago() {
        assertEquals(PAGO_HOY, NotificacionAdminEmailService.resolverFechaHora(null, PAGO_HOY, null));
    }

    // ── esBancario ────────────────────────────────────────────────────────────

    @Test
    void esBancario_todoMenosEfectivo() {
        assertTrue(NotificacionAdminEmailService.esBancario("YAPE"));
        assertTrue(NotificacionAdminEmailService.esBancario("PLIN"));
        assertTrue(NotificacionAdminEmailService.esBancario("TRANSFERENCIA"));
        assertTrue(NotificacionAdminEmailService.esBancario("DEPOSITO"));
        assertTrue(NotificacionAdminEmailService.esBancario("TARJETA"));
        assertFalse(NotificacionAdminEmailService.esBancario("EFECTIVO"));
        assertFalse(NotificacionAdminEmailService.esBancario("-"));
        assertFalse(NotificacionAdminEmailService.esBancario(null));
        assertFalse(NotificacionAdminEmailService.esBancario(""));
    }

    // ── construirBloqueVoucher ────────────────────────────────────────────────

    @Test
    void voucher_efectivo_noSeMuestraAunqueHayaUrls() {
        String bloque = NotificacionAdminEmailService.construirBloqueVoucher("EFECTIVO",
                List.of("https://res.cloudinary.com/x/v1/vouchers/uno.jpg"));
        assertEquals("", bloque);
    }

    @Test
    void voucher_bancario_sinImagenes_noSeMuestra() {
        String bloque = NotificacionAdminEmailService.construirBloqueVoucher("YAPE", List.of());
        assertEquals("", bloque);
    }

    @Test
    void voucher_bancario_conImagenes_seMuestra() {
        String bloque = NotificacionAdminEmailService.construirBloqueVoucher("YAPE",
                List.of("https://res.cloudinary.com/x/v1/vouchers/uno.jpg",
                        "https://res.cloudinary.com/x/v1/vouchers/dos.jpg"));
        assertTrue(bloque.contains("<img src=\"https://res.cloudinary.com/x/v1/vouchers/uno.jpg\""));
        assertTrue(bloque.contains("<img src=\"https://res.cloudinary.com/x/v1/vouchers/dos.jpg\""));
        assertTrue(bloque.contains("Voucher"));
    }

    // ── construirCuerpo (template completo) ───────────────────────────────────

    @Test
    void cuerpo_muestraFechaYHoraDebajoDelLogo() {
        String html = NotificacionAdminEmailService.construirCuerpo(
                pago("EFECTIVO", PAGO_HOY, null),
                "https://res.cloudinary.com/x/logo.png", "INMOBILIARIA CONSTRUCTORA MERRUIC", List.of());

        int posLogo   = html.indexOf("logo.png");
        int posFecha  = html.indexOf(">24/09/2026<");
        int posHora   = html.indexOf(">15:43<");
        int posTabla  = html.indexOf(">Importe<");

        assertTrue(posLogo > 0, "debe tener logo");
        assertTrue(posFecha > posLogo, "la fecha debe ir debajo del logo");
        assertTrue(posHora > posFecha, "la hora debe ir al costado de la fecha");
        assertTrue(posTabla > posHora, "la tabla de datos debe ir después de fecha/hora");
    }

    @Test
    void cuerpo_conVoucher_muestraImagenEnParteInferior() {
        String html = NotificacionAdminEmailService.construirCuerpo(
                pago("YAPE", PAGO_HOY, OPER_VOUCHER),
                "https://res.cloudinary.com/x/logo.png", "INMOBILIARIA CONSTRUCTORA MERRUIC",
                List.of("https://res.cloudinary.com/x/v1/vouchers/pago.jpg"));

        int posTabla  = html.indexOf(">Cliente<");
        int posVoucher = html.indexOf("<img src=\"https://res.cloudinary.com/x/v1/vouchers/pago.jpg\"");
        int posPie    = html.indexOf("Sistema de Gesti");

        assertTrue(posVoucher > posTabla, "el voucher debe ir debajo de la tabla");
        assertTrue(posPie > posVoucher, "el voucher debe ir antes del pie de página");
        // fecha mostrada = fecha de operación (medio bancario)
        assertTrue(html.contains(">23/09/2026<"), "bancario muestra fecha de operación");
    }

    @Test
    void cuerpo_efectivo_noBloqueVoucher_yConservaContenidoOriginal() {
        String html = NotificacionAdminEmailService.construirCuerpo(
                pago("EFECTIVO", PAGO_HOY, null),
                "https://res.cloudinary.com/x/logo.png", "INMOBILIARIA CONSTRUCTORA MERRUIC",
                List.of("https://res.cloudinary.com/x/v1/vouchers/no-deberia-verse.jpg"));

        assertFalse(html.contains("no-deberia-verse.jpg"), "EFECTIVO no muestra voucher");
        assertTrue(html.contains("$ 196.00"), "importe con símbolo");
        assertTrue(html.contains("Pago de letra N\u00B0 3 Mz. C Lt. 18"), "detalle");
        assertTrue(html.contains("EFECTIVO"), "medio de pago");
        assertTrue(html.contains("FELIX BENJAMIN SILVA MEDINA"), "cliente");
        assertTrue(html.contains("Sistema de Gesti\u00f3n INMOBILIARIA CONSTRUCTORA MERRUIC"), "pie");
    }

    @Test
    void cuerpo_sinPlaceholderSinReservar_noLanzaErrorDeFormato() {
        // .formatted lanzaría MissingFormatArgumentException si el template y los
        // argumentos no coinciden; este test lo detectaría como fallo.
        String html = NotificacionAdminEmailService.construirCuerpo(
                pago("TRANSFERENCIA", PAGO_HOY, null),
                "logo.png", "EMPRESA", List.of());
        assertFalse(html.contains("%s"), "no deben quedar placeholders sin resolver");
        assertFalse(html.contains("null"), "no deben aparecer literales 'null'");
    }
}
