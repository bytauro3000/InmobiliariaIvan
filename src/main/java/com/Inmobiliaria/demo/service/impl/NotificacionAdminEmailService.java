package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.enums.Moneda;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionAdminEmailService {

    private final JavaMailSender mailSender;

    @Value("${notificacion.admin.email}")
    private String adminEmail;

    public void notificarPagoLetra(String detalleLetra, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago) {
        enviar("PAGO DE LETRA", detalleLetra, clienteNombre, importe, moneda, medioPago);
    }

    public void notificarPagoMora(String detalleMora, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago) {
        enviar("PAGO DE MORA", detalleMora, clienteNombre, importe, moneda, medioPago);
    }

    public void notificarPagoInicial(String detalle, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago) {
        enviar("PAGO INICIAL / CUOTA", detalle, clienteNombre, importe, moneda, medioPago);
    }

    public void notificarPagoServicio(String detalle, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago) {
        enviar("PAGO DE SERVICIO", detalle, clienteNombre, importe, moneda, medioPago);
    }

    private void enviar(String tipoPago, String detalle, String clienteNombre, BigDecimal importe, Moneda moneda, String medioPago) {
        try {
            String simbolo = moneda == Moneda.PEN ? "S/." : "$";
            String asunto = "\uD83D\uDD14 Nuevo pago registrado - " + tipoPago;

            String medioLabel = medioPago != null ? medioPago : "-";

            String cuerpo = """
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 16px;">
                        <img src="https://res.cloudinary.com/dlgqaifrk/image/upload/w_120,h_120,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png" alt="Logo" style="max-width: 80px; height: auto;">
                    </div>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr><td style="padding: 8px 0; color: #666;">Importe</td><td style="padding: 8px 0; font-weight: bold; color: #2e7d32; font-size: 16px;">%s %s</td></tr>
                        <tr><td style="padding: 8px 0; color: #666;">Detalle</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #666;">Medio de pago</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #666;">Cliente</td><td style="padding: 8px 0; font-weight: bold;">%s</td></tr>
                    </table>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
                    <p style="color: #999; font-size: 12px;">Sistema de Gesti\u00f3n Inmobiliaria IVAN</p>
                </div>
                </body>
                </html>
                """.formatted(simbolo, String.format("%.2f", importe), detalle, medioLabel, clienteNombre);

            String[] destinatarios = adminEmail.split("[,;]");
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setTo(destinatarios);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);
            mailSender.send(mensaje);

            log.info("Notificacion enviada a {} para {} - {}", adminEmail, tipoPago, detalle);

        } catch (Exception e) {
            log.error("Error al enviar notificacion admin para {}: {}", tipoPago, e.getMessage());
        }
    }
}
