package com.Inmobiliaria.demo.util;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NotaCreditoElectronicaPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_MEDIO  = new DeviceGray(0.45f);
    private static final DeviceRgb  AZUL_MARINO = new DeviceRgb(0, 32, 96);

    private static final String EMPRESA   = "INMOBILIARIA CONSTRUCTORA \"IVAN\" E.I.R.L.";
    private static final String DIRECCION = "Av. Alfredo Mendiola N 3623  3er. Piso Of. 301 - Urb. Panamericana Norte - Los Olivos - Lima";
    private static final String TELEFONO  = "Cel.: +51 987-891-788";
    private static final String RUC       = "R.U.C.: 20537853108";

    private static final String LOGO_URL = "https://res.cloudinary.com/dlgqaifrk/image/upload/w_200,h_200,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png";

    public static byte[] generar(
            String serie, String correlativo, String fechaEmision,
            String tipoMoneda, String montoStr,
            String clienteNombre, String clienteDoc,
            String direccionCliente, String detalleDescripcion,
            String montoLetras, BigDecimal mtoOperInafectas,
            String hashCdr,
            String codigoMotivo, String motivoNotaCredito,
            String origSerie, String origCorrelativo, String origFechaEmision) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf, PageSize.A5.rotate())) {

            doc.setMargins(16, 18, 10, 52);

            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            String serieCorrelativo = serie + "-" + correlativo;
            String monedaSimbolo    = "PEN".equals(tipoMoneda) ? "S/" : "$";
            String monedaNombre     = "PEN".equals(tipoMoneda) ? "SOLES" : "DOLAR AMERICANO";
            String origSerieCorrelativo = origSerie + "-" + origCorrelativo;

            String fechaStr = fechaEmision;
            if (fechaEmision != null && fechaEmision.length() >= 10) {
                try {
                    fechaStr = LocalDate.parse(fechaEmision.substring(0, 10)).format(FMT);
                } catch (Exception ignored) {}
            }

            String origFechaStr = origFechaEmision;
            if (origFechaEmision != null && origFechaEmision.length() >= 10) {
                try {
                    origFechaStr = LocalDate.parse(origFechaEmision.substring(0, 10)).format(FMT);
                } catch (Exception ignored) {}
            }

            // QR: RUC|07|serie|correlativo|0.00|total|fecha|tipoDocCliente|numDoc|hash
            String qrData = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                "20537853108", "07", serie, correlativo, "0.00",
                montoStr, fechaEmision != null ? fechaEmision.substring(0, 10) : LocalDate.now().toString(),
                "1", clienteDoc, hashCdr != null ? hashCdr : "");

            BarcodeQRCode qrCode = new BarcodeQRCode(qrData);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(50).setHeight(50)
                    .setAutoScale(true)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            Image logoImg = new Image(ImageDataFactory.create(new URL(LOGO_URL)))
                    .setWidth(50).setHeight(50)
                    .setAutoScale(true)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ═══════════════ ENCABEZADO ═══════════════
            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{0.17f, 1, 0.21f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaLogo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(6).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);
            celdaLogo.add(logoImg);
            encabezado.addCell(celdaLogo);

            Cell celdaEmpresa = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                    .setPadding(4).setTextAlignment(TextAlignment.CENTER);
            celdaEmpresa.add(new Paragraph(EMPRESA)
                    .setFont(courierBold).setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph(DIRECCION)
                    .setFont(courier).setFontSize(7.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph(TELEFONO + "          " + RUC)
                    .setFont(courier).setFontSize(7.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            celdaEmpresa.add(new Paragraph("NOTA DE CRÉDITO ELECTRÓNICA")
                    .setFont(courierBold).setFontSize(14)
                    .setFontColor(AZUL_MARINO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph("N° " + serieCorrelativo)
                    .setFont(courierBold).setFontSize(9f).setFontColor(AZUL_MARINO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(1));
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setPadding(5).setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Código SUNAT")
                    .setFont(arial).setFontSize(6f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER));
            encabezado.addCell(celdaQr);

            doc.add(encabezado);

            // ═══════════════ COMPROBANTE AFECTADO ═══════════════
            Table tablaAfectado = new Table(UnitValue.createPercentArray(new float[]{0.20f, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(4);

            tablaAfectado.addCell(labelCell("Comp. Afectado", courierBold));
            tablaAfectado.addCell(valueCell(origSerieCorrelativo + "  (" + origFechaStr + ")", courier));

            tablaAfectado.addCell(labelCell("Motivo", courierBold));
            String motivoCompleto = codigoMotivo + " - " + motivoNotaCredito;
            tablaAfectado.addCell(valueCell(motivoCompleto, courier));

            doc.add(tablaAfectado);

            // ═══════════════ DATOS DEL CLIENTE ═══════════════
            Table tablaCliente = new Table(UnitValue.createPercentArray(new float[]{0.20f, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(3);

            tablaCliente.addCell(labelCell("Señor(es)", courierBold));
            tablaCliente.addCell(valueCell(clienteNombre.toUpperCase(), courier));
            tablaCliente.addCell(labelCell("DNI", courierBold));
            tablaCliente.addCell(valueCell(clienteDoc, courier));
            tablaCliente.addCell(labelCell("Dirección", courierBold));
            tablaCliente.addCell(valueCell(direccionCliente.toUpperCase(), courier));
            tablaCliente.addCell(labelCell("Fecha de Emisión", courierBold));
            tablaCliente.addCell(valueCell(fechaStr != null ? fechaStr : "-", courier));
            tablaCliente.addCell(labelCell("Tipo de Moneda", courierBold));
            tablaCliente.addCell(valueCell(monedaNombre, courier));

            doc.add(tablaCliente);

            doc.add(new Paragraph()
                    .setBorderBottom(new SolidBorder(GRIS_MEDIO, 0.5f))
                    .setMarginTop(3).setMarginBottom(3));

            // ═══════════════ DETALLE ═══════════════
            Table tablaDetalle = new Table(UnitValue.createPercentArray(new float[]{0.6f, 0.08f, 0.15f, 0.17f}))
                    .setWidth(UnitValue.createPercentValue(100));
            String[] headersDetalle = {"DESCRIPCIÓN", "CANT.", "VALOR UNIT.", "IMPORTE"};
            for (String h : headersDetalle) {
                tablaDetalle.addHeaderCell(new Cell()
                        .setBackgroundColor(AZUL_MARINO).setPadding(3)
                        .add(new Paragraph(h).setFont(courierBold).setFontSize(9f)
                                .setFontColor(ColorConstants.WHITE)
                                .setTextAlignment(h.equals("DESCRIPCIÓN") ? TextAlignment.LEFT : TextAlignment.RIGHT)));
            }

            BigDecimal montoDecimal;
            try { montoDecimal = new BigDecimal(montoStr.replace(",", "")); }
            catch (Exception e) { montoDecimal = BigDecimal.ZERO; }

            tablaDetalle.addCell(new Cell().setPadding(3)
                    .add(new Paragraph(detalleDescripcion).setFont(courier).setFontSize(9f)));
            tablaDetalle.addCell(new Cell().setPadding(3).setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("1").setFont(courier).setFontSize(9f)));
            tablaDetalle.addCell(new Cell().setPadding(3).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(monedaSimbolo + " " + montoStr).setFont(courier).setFontSize(9f)));
            tablaDetalle.addCell(new Cell().setPadding(3).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(monedaSimbolo + " " + montoStr).setFont(courier).setFontSize(9f)));

            doc.add(tablaDetalle);

            // ═══════════════ FOOTER ═══════════════
            Table tablaFooter = new Table(UnitValue.createPercentArray(new float[]{1, 0.42f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(2);

            Cell celdaVacia = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setHeight(8f);

            tablaFooter.addCell(celdaVacia);
            tablaFooter.addCell(filaTotalCompacta2("Op. Gravada:", monedaSimbolo + " 0.00", courier));
            Cell celdaSinImp = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaSinImp.add(new Paragraph("(*) Sin impuestos.")
                    .setFont(arial).setFontSize(6.5f).setFontColor(GRIS_MEDIO));
            tablaFooter.addCell(celdaSinImp);
            tablaFooter.addCell(filaTotalCompacta2("Op. Exonerada:", monedaSimbolo + " 0.00", courier));
            Cell celdaIncluye = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaIncluye.add(new Paragraph("(**) Incluye impuestos, de ser Op. Gravada.")
                    .setFont(arial).setFontSize(6.5f).setFontColor(GRIS_MEDIO));
            tablaFooter.addCell(celdaIncluye);
            tablaFooter.addCell(filaTotalCompacta2("Op. Inafecta:",
                monedaSimbolo + " " + DF.format(mtoOperInafectas != null ? mtoOperInafectas : montoDecimal), courier));
            tablaFooter.addCell(celdaVacia);
            tablaFooter.addCell(filaTotalCompacta2("IGV:", monedaSimbolo + " 0.00", courier));

            Cell celdaSon = new Cell()
                    .setBorder(Border.NO_BORDER).setPadding(1)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaSon.add(new Paragraph("SON: " + (montoLetras != null ? montoLetras : "-"))
                    .setFont(courier).setFontSize(8f).setItalic());
            if (hashCdr != null && !hashCdr.isBlank()) {
                celdaSon.add(new Paragraph("RESUMEN: " + hashCdr)
                        .setFont(courier).setFontSize(6f).setFontColor(GRIS_MEDIO));
            }
            tablaFooter.addCell(celdaSon);
            tablaFooter.addCell(filaTotalImporte("Importe Total:", monedaSimbolo + " " + montoStr, courierBold));

            doc.add(tablaFooter);

            doc.add(new Paragraph()
                    .setBorderBottom(new SolidBorder(GRIS_MEDIO, 0.5f))
                    .setMarginTop(5).setMarginBottom(3));

            // ═══════════════ LEYENDA SUNAT ═══════════════
            doc.add(new Paragraph(
                    "Esta es una representación impresa de la Nota de Crédito Electrónica, generada en el Sistema de la SUNAT. " +
                    "El Emisor Electrónico puede verificarla utilizando su clave SOL, el Adquirente o Usuario puede consultar " +
                    "su validez en SUNAT Virtual: www.sunat.gob.pe, en Opciones sin Clave SOL / Consulta de Validez del CPE.")
                    .setFont(arial).setFontSize(7.5f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0));

            // ── LÍNEA GRIS IZQUIERDA ──
            PdfPage primeraPagina = pdf.getFirstPage();
            PdfCanvas lienzo = new PdfCanvas(primeraPagina);
            lienzo.setStrokeColor(new DeviceGray(0.55f))
                  .setLineWidth(1.2f)
                  .moveTo(0, 210f)
                  .lineTo(28, 210f)
                  .stroke();
            lienzo.release();

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar nota de crédito electrónica PDF", e);
        }
    }

    private static Cell labelCell(String texto, PdfFont bold) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0.5f)
                .add(new Paragraph(texto).setFont(bold).setFontSize(8.5f)
                        .setTextAlignment(TextAlignment.LEFT));
    }

    private static Cell valueCell(String texto, PdfFont normal) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(1.5f)
                .add(new Paragraph(": " + (texto != null ? texto : "-")).setFont(normal).setFontSize(8.5f));
    }

    private static Cell filaTotalCompacta2(String label, String value, PdfFont normal) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.55f}))
                .setWidth(UnitValue.createPercentValue(100));
        t.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1f)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(label).setFont(normal).setFontSize(8f)));
        t.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1f)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(value).setFont(normal).setFontSize(8f)));
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(t);
    }

    private static Cell filaTotalImporte(String label, String value, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.55f}))
                .setWidth(UnitValue.createPercentValue(100));
        t.addCell(new Cell()
                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 2f))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(2).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(label).setFont(bold).setFontSize(10f)));
        t.addCell(new Cell()
                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 2f))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(2).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(value).setFont(bold).setFontSize(10f)));
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(t);
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                NotaCreditoElectronicaPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}
