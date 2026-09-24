package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.EmpresaService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionAdminEmailService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final EmpresaService empresaService;
    private final VoucherRepository voucherRepository;

    @Value("${notificacion.admin.email}")
    private String adminEmail;

    public void notificarPagoLetra(String detalleLetra, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago,
                                   LocalDateTime fechaPago, LocalDateTime fechaOperacion, Integer idPago) {
        enviar(new NotificacionPago("PAGO DE LETRA", "PAGO_LETRA", idPago,
                detalleLetra, clienteNombre, importe, moneda, medioPago, fechaPago, fechaOperacion));
    }

    public void notificarPagoMora(String detalleMora, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago,
                                  LocalDateTime fechaPago, Integer idPagoMora) {
        enviar(new NotificacionPago("PAGO DE MORA", "PAGO_MORA", idPagoMora,
                detalleMora, clienteNombre, importe, moneda, medioPago, fechaPago, null));
    }

    public void notificarPagoInicial(String detalle, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago,
                                     LocalDateTime fechaPago, Integer idPagoInicial) {
        enviar(new NotificacionPago("PAGO INICIAL / CUOTA", "PAGO_INICIAL", idPagoInicial,
                detalle, clienteNombre, importe, moneda, medioPago, fechaPago, null));
    }

    public void notificarPagoServicio(String detalle, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago,
                                      LocalDateTime fechaPago, Integer idPagoInscripcion) {
        enviar(new NotificacionPago("PAGO DE SERVICIO", "PAGO_INSCRIPCION", idPagoInscripcion,
                detalle, clienteNombre, importe, moneda, medioPago, fechaPago, null));
    }

    private void enviar(NotificacionPago p) {
        try {
            String asunto = "\uD83D\uDD14 Nuevo pago registrado - " + p.tipo();

            String logoUrl = empresaService.obtenerActiva().getLogoSmallUrl();
            String nombreLegal = empresaService.obtenerActiva().getNombreLegal();

            String cuerpo = construirCuerpo(p, logoUrl, nombreLegal,
                    obtenerUrlsVoucher(p.tipoOrigenVoucher(), p.idReferencia()));

            String[] destinatarios = adminEmail.split("[,;]");
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setTo(destinatarios);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);
            mailSender.send(mensaje);

            log.info("Notificacion enviada a {} para {} - {}", adminEmail, p.tipo(), p.detalle());

        } catch (Exception e) {
            log.error("Error al enviar notificacion admin para {}: {}", p.tipo(), e.getMessage());
        }
    }

    /**
     * Construye el HTML del correo. Método puro (sin dependencias de Spring) para que
     * pueda ser verificado por test unitario: si el template fallara, el correo no
     * llegaría nunca y solo veríamos un error en los logs.
     */
    static String construirCuerpo(NotificacionPago p, String logoUrl, String nombreLegal, List<String> urlsVoucher) {
        String simbolo = p.moneda() == Moneda.PEN ? "S/." : "$";
        String medioLabel = p.medioPago() != null ? p.medioPago() : "-";

        LocalDateTime fechaHora = resolverFechaHora(p.medioPago(), p.fechaPago(), p.fechaOperacion());
        String fechaTxt = fechaHora.format(FMT_FECHA);
        String horaTxt  = fechaHora.format(FMT_HORA);

        String bloqueVoucher = construirBloqueVoucher(p.medioPago(), urlsVoucher);

        return """
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
            <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                <div style="text-align: center; margin-bottom: 6px;">
                    <img src="%s" alt="Logo" style="max-width: 80px; height: auto;">
                </div>
                <div style="text-align: center; margin-bottom: 16px; color: #555; font-size: 13px;">
                    <span style="font-weight: bold;">%s</span>&nbsp;&nbsp;&nbsp;<span style="font-weight: bold;">%s</span>
                </div>
                <table style="width: 100%%; border-collapse: collapse;">
                    <tr><td style="padding: 8px 0; color: #666;">Importe</td><td style="padding: 8px 0; font-weight: bold; color: #2e7d32; font-size: 16px;">%s %s</td></tr>
                    <tr><td style="padding: 8px 0; color: #666;">Detalle</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 8px 0; color: #666;">Medio de pago</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 8px 0; color: #666;">Cliente</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                </table>
                %s
                <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
                <p style="color: #999; font-size: 12px;">Sistema de Gesti\u00f3n %s</p>
            </div>
            </body>
            </html>
            """.formatted(logoUrl, fechaTxt, horaTxt, simbolo, String.format(Locale.US, "%.2f", p.importe()),
                p.detalle(), medioLabel, p.clienteNombre(), bloqueVoucher, nombreLegal);
    }

    /**
     * Medio de pago bancario = todo lo que no sea EFECTIVO (coincide con la validación
     * del frontend, que exige voucher para cualquier medio distinto de EFECTIVO).
     */
    static boolean esBancario(String medioPago) {
        return medioPago != null && !medioPago.isBlank() && !"-".equals(medioPago) && !"EFECTIVO".equals(medioPago);
    }

    /**
     * Fecha y hora mostradas bajo el logo:
     *  - medio bancario → fecha de operación del voucher (fallback: fecha de pago)
     *  - EFECTIVO / sin dato → fecha de pago
     */
    static LocalDateTime resolverFechaHora(String medioPago, LocalDateTime fechaPago, LocalDateTime fechaOperacion) {
        if (esBancario(medioPago) && fechaOperacion != null) {
            return fechaOperacion;
        }
        if (fechaPago != null) {
            return fechaPago;
        }
        if (fechaOperacion != null) {
            return fechaOperacion;
        }
        return LocalDateTime.now();
    }

    /** Bloque con las imágenes del voucher; vacío si es EFECTIVO o no hay vouchers. */
    static String construirBloqueVoucher(String medioPago, List<String> urls) {
        if (!esBancario(medioPago) || urls == null || urls.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-top: 16px; text-align: center;\">");
        sb.append("<div style=\"font-size: 12px; color: #888; margin-bottom: 8px;\">Voucher</div>");
        for (String url : urls) {
            sb.append(String.format(
                "<img src=\"%s\" alt=\"Voucher\" style=\"display: block; width: 100%%; max-width: 440px; margin: 0 auto 8px; border-radius: 8px; border: 1px solid #eee;\">",
                url));
        }
        sb.append("</div>");
        return sb.toString();
    }

    /** Consulta defensiva: un fallo aquí nunca debe impedir el envío del correo. */
    private List<String> obtenerUrlsVoucher(String tipoOrigen, Integer idReferencia) {
        if (idReferencia == null) {
            return List.of();
        }
        try {
            return voucherRepository.findByTipoOrigenAndReferenciaId(tipoOrigen, idReferencia)
                    .stream()
                    .map(Voucher::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("No se pudieron obtener vouchers {}-{} para el correo: {}", tipoOrigen, idReferencia, e.getMessage());
            return List.of();
        }
    }

    record NotificacionPago(String tipo, String tipoOrigenVoucher, Integer idReferencia,
                            String detalle, String clienteNombre, BigDecimal importe, Moneda moneda,
                            String medioPago, LocalDateTime fechaPago, LocalDateTime fechaOperacion) {
    }
}
