package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.EmailService;
import com.Inmobiliaria.demo.util.BoletaElectronicaPdf;
import com.Inmobiliaria.demo.util.ComprobantePagoLetraPdf;
import com.Inmobiliaria.demo.util.NumeroALetras;
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
            String nombreArchivo;
            byte[] pdf;

            Comprobante comp = pago.getComprobante();
            if (comp != null && comp.getTipoComprobante() == TipoComprobante.BOLETA
                    && comp.getHashCdr() != null && !comp.getHashCdr().isBlank()) {
                pdf = generarBoletaPdf(comp, pago.getLetra());
                nombreArchivo = "boleta-electronica-" + comp.getNumeroCompleto() + ".pdf";
            } else {
                pdf = ComprobantePagoLetraPdf.generar(pago, "SECRETARIA");
                nombreArchivo = "comprobante-pago-" + pago.getIdPago() + ".pdf";
            }

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Pago - Inmobiliaria Constructora IVAN E.I.R.L.");
            helper.setText(construirCuerpo(pago), true);
            helper.addAttachment(nombreArchivo, new ByteArrayResource(pdf), "application/pdf");

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
            PagoLetras primero = pagos.get(0);
            Comprobante comp = primero.getComprobante();
            boolean esBoleta = comp != null && comp.getTipoComprobante() == TipoComprobante.BOLETA
                    && comp.getHashCdr() != null && !comp.getHashCdr().isBlank();

            byte[] pdf;
            String nombreArchivo;

            if (esBoleta) {
                pdf = generarBoletaPdf(comp, primero.getLetra());
                nombreArchivo = "boleta-electronica-" + comp.getNumeroCompleto() + ".pdf";
            } else {
                pdf = pagos.size() == 1
                        ? ComprobantePagoLetraPdf.generar(primero, "SECRETARIA")
                        : ComprobantePagoLetraPdf.generarMultiple(pagos, "SECRETARIA");
                String nroComp = comp != null ? comp.getNumeroCompleto() : String.valueOf(primero.getIdPago());
                nombreArchivo = pagos.size() == 1
                        ? "comprobante-pago-" + primero.getIdPago() + ".pdf"
                        : "comprobante-" + nroComp + ".pdf";
            }

            String cuerpo = construirCuerpoMultiple(pagos);

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

    private byte[] generarBoletaPdf(Comprobante comp, LetraCambio letra) {
        var contrato = letra.getContrato();

        String numeroLetra = letra.getNumeroLetra();
        if (numeroLetra != null && numeroLetra.contains("/")) {
            numeroLetra = numeroLetra.substring(0, numeroLetra.indexOf("/"));
        }

        String nombrePrograma = "";
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            var lote = contrato.getLotes().iterator().next().getLote();
            if (lote != null && lote.getPrograma() != null) {
                nombrePrograma = lote.getPrograma().getNombrePrograma();
            }
        }
        String descripcion = "LETRA " + numeroLetra
            + " POR LA COMPRA DE UN LOTE DE TERRENO RUSTICO PROGRAMA DE VIV. "
            + (nombrePrograma != null ? nombrePrograma.toUpperCase() : "");

        String clienteNombre = "";
        String clienteDoc = "";
        String direccionCliente = "";
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            var c = contrato.getClientes().iterator().next().getCliente();
            clienteNombre = (c.getNombre() + " " + c.getApellidos()).trim().toUpperCase();
            clienteDoc = c.getNumDoc() != null ? c.getNumDoc() : "";
            direccionCliente = c.getDireccion() != null ? c.getDireccion().toUpperCase() : "-";
        }

        String moneda = contrato.getMoneda() != null ? contrato.getMoneda().name() : "USD";
        String montoStr = String.format("%.2f", comp.getMonto());

        return BoletaElectronicaPdf.generarBoletaSimple(
            comp.getSerie(),
            comp.getNumero().toString(),
            comp.getFechaEmision().toString(),
            moneda,
            montoStr,
            clienteNombre,
            clienteDoc,
            direccionCliente,
            descripcion,
            NumeroALetras.convertir(comp.getMonto(), contrato.getMoneda()),
            comp.getMonto(),
            comp.getHashCdr()
        );
    }

    private String construirCuerpo(PagoLetras pago) {
        String cliente = "-";
        if (pago.getLetra().getContrato().getClientes() != null
                && !pago.getLetra().getContrato().getClientes().isEmpty()) {
            var c = pago.getLetra().getContrato().getClientes().iterator().next().getCliente();
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