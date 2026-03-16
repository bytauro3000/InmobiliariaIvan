package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ComprobantePagoLetraPdf {

    private static final DeviceRgb AZUL_HEADER = new DeviceRgb(21, 67, 120);
    private static final DeviceRgb AZUL_CLARO  = new DeviceRgb(235, 242, 252);
    private static final DateTimeFormatter FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF   = new DecimalFormat("#,##0.00");

    public static byte[] generar(PagoLetras pago) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont normal = cargarFuente("fonts/ARIAL.TTF");
            PdfFont bold   = cargarFuente("fonts/ARIALBD.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document    doc = new Document(pdf);
            doc.setMargins(40, 50, 40, 50);

            LetraCambio letra    = pago.getLetra();
            Contrato    contrato = letra.getContrato();

            // ── ENCABEZADO ────────────────────────────────────────────────
            Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));

            header.addCell(new Cell()
                    .setBackgroundColor(AZUL_HEADER)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("COMPROBANTE DE PAGO DE LETRA")
                            .setFont(bold).setFontSize(16)
                            .setFontColor(ColorConstants.WHITE)
                            .setMarginBottom(2))
                    .add(new Paragraph("Inmobiliaria Florida")
                            .setFont(normal).setFontSize(10)
                            .setFontColor(ColorConstants.WHITE)));
            doc.add(header);
            doc.add(new Paragraph(" ").setFontSize(4));

            // ── DATOS DEL COMPROBANTE ─────────────────────────────────────
            String numComp = (pago.getTipoComprobante() != null && pago.getNumeroComprobante() != null)
                    ? pago.getTipoComprobante().name() + " N° " + pago.getNumeroComprobante()
                    : "Sin comprobante";

            Table infoTop = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            infoTop.addCell(celdaDato("N° Comprobante:", numComp, normal, bold));
            infoTop.addCell(celdaDato("Fecha de pago:",
                    pago.getFechaPago() != null ? pago.getFechaPago().format(FMT) : "-",
                    normal, bold));
            infoTop.addCell(celdaDato("N° Contrato:",
                    String.valueOf(contrato.getIdContrato()), normal, bold));
            infoTop.addCell(celdaDato("N° Letra:",
                    letra.getNumeroLetra(), normal, bold));
            doc.add(infoTop);

            // ── DATOS DEL CLIENTE ─────────────────────────────────────────
            doc.add(titulSeccion("DATOS DEL CLIENTE", bold));

            String clientes = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                clientes = contrato.getClientes().stream()
                        .map(cc -> cc.getCliente().getNombre()
                                + " " + cc.getCliente().getApellidos())
                        .collect(Collectors.joining(" / "));
            }

            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().get(0);
                loteInfo = cl.getLote().getPrograma().getNombrePrograma()
                        + "  —  Mz. " + cl.getLote().getManzana()
                        + "  Lt. " + cl.getLote().getNumeroLote();
            }

            Table tablaCliente = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);
            tablaCliente.addCell(celdaDato("Cliente(s):", clientes, normal, bold));
            tablaCliente.addCell(celdaDato("Lote:", loteInfo, normal, bold));
            doc.add(tablaCliente);

            // ── DETALLE DEL PAGO ──────────────────────────────────────────
            doc.add(titulSeccion("DETALLE DEL PAGO", bold));

            Table detalle = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            detalle.addCell(celdaDato("Importe pagado:",
                    "$ " + DF.format(pago.getImportePagado()), normal, bold));
            detalle.addCell(celdaDato("Medio de pago:",
                    pago.getMedioPago() != null ? pago.getMedioPago().name() : "-",
                    normal, bold));
            detalle.addCell(celdaDato("N° Operación:",
                    pago.getNumeroOperacion() != null ? pago.getNumeroOperacion() : "-",
                    normal, bold));
            detalle.addCell(celdaDato("Fecha de operación:",
                    pago.getFechaOperacion() != null ? pago.getFechaOperacion().format(FMT) : "-",
                    normal, bold));
            detalle.addCell(celdaDato("Importe en letras:",
                    letra.getImporteLetras() != null ? letra.getImporteLetras() : "-",
                    normal, bold));
            detalle.addCell(celdaDato("Fecha vencimiento letra:",
                    letra.getFechaVencimiento() != null ? letra.getFechaVencimiento().format(FMT) : "-",
                    normal, bold));

            // Observaciones en fila completa si existe
            if (pago.getObservaciones() != null && !pago.getObservaciones().isBlank()) {
                detalle.addCell(new Cell(1, 2)
                        .setBackgroundColor(AZUL_CLARO)
                        .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                        .setPadding(6)
                        .add(new Paragraph()
                                .add(new Text("Observaciones:  ").setFont(bold).setFontSize(9))
                                .add(new Text(pago.getObservaciones()).setFont(normal).setFontSize(9))));
            }
            doc.add(detalle);

            // ── TOTAL DESTACADO ───────────────────────────────────────────
            doc.add(new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(16)
                    .addCell(new Cell()
                            .setBackgroundColor(AZUL_HEADER)
                            .setBorder(Border.NO_BORDER)
                            .setPadding(10)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .add(new Paragraph()
                                    .add(new Text("TOTAL PAGADO:   ")
                                            .setFont(normal).setFontSize(12)
                                            .setFontColor(ColorConstants.WHITE))
                                    .add(new Text("$ " + DF.format(pago.getImportePagado()))
                                            .setFont(bold).setFontSize(14)
                                            .setFontColor(ColorConstants.WHITE)))));

            // ── PIE DE PÁGINA ─────────────────────────────────────────────
            doc.add(new Paragraph(
                    "Este documento es un comprobante interno de pago de letra de cambio.")
                    .setFont(normal).setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ComprobantePagoLetraPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }

    private static Cell celdaDato(String label, String valor, PdfFont normal, PdfFont bold) {
        return new Cell()
                .setBackgroundColor(AZUL_CLARO)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(6)
                .add(new Paragraph()
                        .add(new Text(label + "  ").setFont(bold).setFontSize(9))
                        .add(new Text(valor).setFont(normal).setFontSize(9)));
    }

    private static Paragraph titulSeccion(String texto, PdfFont bold) {
        return new Paragraph(texto)
                .setFont(bold).setFontSize(10)
                .setFontColor(AZUL_HEADER)
                .setMarginTop(6).setMarginBottom(4)
                .setBorderBottom(new SolidBorder(AZUL_HEADER, 0.8f));
    }
}