package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.service.EmailService;
import com.Inmobiliaria.demo.util.ComprobantePagoLetraPdf;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void enviarComprobante(PagoLetras pago, String destinatario) {
        try {
            byte[] pdf = ComprobantePagoLetraPdf.generar(pago, "SECRETARIA");

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Pago - Inmobiliaria Constructora IVAN E.I.R.L.");
            helper.setText(construirCuerpo(pago), true);
            helper.addAttachment(
                "comprobante-pago-" + pago.getIdPago() + ".pdf",
                new ByteArrayResource(pdf),
                "application/pdf"
            );

            mailSender.send(mensaje);
            log.info("Comprobante enviado a {} para el pago ID {}", destinatario, pago.getIdPago());

        } catch (Exception e) {
            log.error("Error al enviar comprobante al correo {} para pago ID {}: {}",
                    destinatario, pago.getIdPago(), e.getMessage());
        }
    }

    @Override
    public void enviarComprobanteATodos(List<PagoLetras> pagos, List<String> destinatarios) {
        if (pagos == null || pagos.isEmpty() || destinatarios == null || destinatarios.isEmpty()) return;

        try {
            byte[] pdf = pagos.size() == 1
                    ? ComprobantePagoLetraPdf.generar(pagos.get(0), "SECRETARIA")
                    : ComprobantePagoLetraPdf.generarMultiple(pagos, "SECRETARIA");

            PagoLetras primero = pagos.get(0);
            String cuerpo = construirCuerpoMultiple(pagos);

            // ── CORRECCIÓN: leer número desde la relación Comprobante ─────────
            String numeroComprobante = (primero.getComprobante() != null)
                    ? primero.getComprobante().getNumeroCompleto()
                    : String.valueOf(primero.getIdPago());

            String nombreArchivo = pagos.size() == 1
                    ? "comprobante-pago-" + primero.getIdPago() + ".pdf"
                    : "comprobante-" + numeroComprobante + ".pdf";

            for (String destinatario : destinatarios) {
                try {
                    MimeMessage mensaje = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                    helper.setTo(destinatario);
                    helper.setSubject("Comprobante de Pago - Inmobiliaria Constructora IVAN E.I.R.L.");
                    helper.setText(cuerpo, true);
                    helper.addAttachment(nombreArchivo, new ByteArrayResource(pdf), "application/pdf");
                    mailSender.send(mensaje);
                    log.info("Comprobante enviado a {} (pagos: {})", destinatario,
                            pagos.stream().map(p -> String.valueOf(p.getIdPago())).collect(Collectors.joining(",")));
                } catch (Exception e) {
                    log.error("Error enviando a {}: {}", destinatario, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error generando PDF para envío múltiple: {}", e.getMessage());
        }
    }

    private String construirCuerpoMultiple(List<PagoLetras> pagos) {
        PagoLetras primero = pagos.get(0);
        var contrato = primero.getLetra().getContrato();
        Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
        String simbolo = moneda == Moneda.PEN ? "S/" : "$";

        String clientes = "-";
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            clientes = contrato.getClientes().stream()
                    .map(cc -> cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos())
                    .collect(Collectors.joining(", "));
        }

        String letras;
        if (pagos.size() == 1) {
            String num = primero.getLetra().getNumeroLetra() != null
                    ? primero.getLetra().getNumeroLetra().replace("/", " de ") : "-";
            letras = "la letra <strong>" + num + "</strong>";
        } else {
            List<String> nums = pagos.stream()
                    .map(p -> p.getLetra().getNumeroLetra() != null
                            ? p.getLetra().getNumeroLetra().split("/")[0].trim() : "?")
                    .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)))
                    .collect(Collectors.toList());
            String ultima = nums.get(nums.size() - 1);
            String anteriores = String.join(", ", nums.subList(0, nums.size() - 1));
            letras = "las letras <strong>N° " + anteriores + " y " + ultima + "</strong>";
        }

        BigDecimal total = pagos.stream()
                .map(PagoLetras::getImportePagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
            + "<p>Estimado(a) <strong>" + clientes + "</strong>,</p>"
            + "<p>Adjunto encontrará el comprobante de pago correspondiente a "
            + letras + " registrado el <strong>" + primero.getFechaPago() + "</strong>.</p>"
            + "<p>Importe total pagado: <strong>" + simbolo + " " + total + "</strong></p>"
            + "<p>También puede escanear el código QR del comprobante para visualizarlo en línea.</p>"
            + "<br>"
            + "<p>Atentamente,</p>"
            + "<p><strong>Inmobiliaria Constructora \"IVAN\" E.I.R.L.</strong><br>"
            + "Av. Alfredo Mendiola N° 3623, 3er. Piso Of. 301<br>"
            + "Telf.: (01) 413-8679</p>"
            + "</body></html>";
    }

    private String construirCuerpo(PagoLetras pago) {
        String cliente = "-";
        if (pago.getLetra().getContrato().getClientes() != null
                && !pago.getLetra().getContrato().getClientes().isEmpty()) {
            var c = pago.getLetra().getContrato().getClientes().get(0).getCliente();
            cliente = c.getNombre() + " " + c.getApellidos();
        }

        String numeroLetra = pago.getLetra().getNumeroLetra() != null
                ? pago.getLetra().getNumeroLetra().replace("/", " de ") : "-";

        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
            + "<p>Estimado(a) <strong>" + cliente + "</strong>,</p>"
            + "<p>Adjunto encontrará el comprobante de pago correspondiente a su letra <strong>"
            + numeroLetra + "</strong> registrado el <strong>"
            + pago.getFechaPago() + "</strong>.</p>"
            + "<p>Importe pagado: <strong>$ " + pago.getImportePagado() + "</strong></p>"
            + "<p>También puede escanear el código QR del comprobante para visualizarlo en línea.</p>"
            + "<br>"
            + "<p>Atentamente,</p>"
            + "<p><strong>Inmobiliaria Constructora \"IVAN\" E.I.R.L.</strong><br>"
            + "Av. Alfredo Mendiola N° 3623, 3er. Piso Of. 301<br>"
            + "Telf.: (01) 413-8679</p>"
            + "</body></html>";
    }
}