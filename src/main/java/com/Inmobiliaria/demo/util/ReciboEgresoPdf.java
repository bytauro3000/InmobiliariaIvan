package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.service.LogoCacheService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
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

/**
 * Plantilla del RECIBO DE EGRESOS (serie EG01). Documento interno de salida de
 * dinero (pago de comisión a vendedor). Reutilizable a futuro para egresos
 * genéricos (parceleros/proveedores).
 */
public class ReciboEgresoPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_MEDIO = new DeviceGray(0.45f);
    private static final DeviceRgb AZUL_MARINO = new DeviceRgb(0, 32, 96);

    private static String empresa() {
        return EmpresaContext.empresaService.obtenerActiva().getNombreLegal();
    }

    private static String direccion() {
        return EmpresaPdfUtil.direccionCompleta();
    }

    private static String telefono() {
        return "Cel.: " + EmpresaContext.empresaService.obtenerActiva().getCelular();
    }

    private static String ruc() {
        return "R.U.C.: " + EmpresaContext.empresaService.obtenerActiva().getRuc();
    }

    private static String logoUrl() {
        return EmpresaContext.empresaService.obtenerActiva().getLogoSmallUrl();
    }

    public static byte[] generar(ReciboEgreso egreso) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf, PageSize.A5.rotate())) {

            doc.setMargins(16, 18, 10, 52);

            PdfFont courier = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            String serieCorrelativo = egreso.getSerie() + "-" + egreso.getNumero();
            String monedaSimbolo = "PEN".equals(egreso.getMoneda()) ? "S/" : "$";
            String monedaNombre = "PEN".equals(egreso.getMoneda()) ? "SOLES" : "DOLAR AMERICANO";
            String fechaStr = egreso.getFechaEmision() != null ? egreso.getFechaEmision().format(FMT) : "-";
            String montoStr = DF.format(egreso.getMonto() != null ? egreso.getMonto() : BigDecimal.ZERO);

            ImageData logoData = LogoCacheService.logoImageData();
            Image logoImg = (logoData != null
                    ? new Image(logoData)
                    : new Image(ImageDataFactory.create(new URL(logoUrl()))))
                    .setWidth(50).setHeight(50)
                    .setAutoScale(true)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ═══════════════ ENCABEZADO ═══════════════
            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{0.17f, 1}))
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
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(4).setTextAlignment(TextAlignment.CENTER);
            celdaEmpresa.add(new Paragraph(empresa())
                    .setFont(courierBold).setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph(direccion())
                    .setFont(courier).setFontSize(7.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph(telefono() + "          " + ruc())
                    .setFont(courier).setFontSize(7.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            celdaEmpresa.add(new Paragraph("RECIBO DE EGRESO")
                    .setFont(courierBold).setFontSize(14)
                    .setFontColor(AZUL_MARINO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph("N° " + serieCorrelativo)
                    .setFont(courierBold).setFontSize(9f).setFontColor(AZUL_MARINO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(1));
            encabezado.addCell(celdaEmpresa);

            doc.add(encabezado);

            // ═══════════════ DATOS DEL BENEFICIARIO ═══════════════
            Table tablaCliente = new Table(UnitValue.createPercentArray(new float[]{0.22f, 1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(4);

            tablaCliente.addCell(labelCell("Pagado a", courierBold));
            tablaCliente.addCell(valueCell(egreso.getBeneficiario() != null ? egreso.getBeneficiario().toUpperCase() : "-", courier));
            tablaCliente.addCell(labelCell("Concepto", courierBold));
            tablaCliente.addCell(valueCell(egreso.getConcepto() != null ? egreso.getConcepto().toUpperCase() : "-", courier));
            tablaCliente.addCell(labelCell("Contrato N°", courierBold));
            tablaCliente.addCell(valueCell(egreso.getIdContrato() != null ? String.valueOf(egreso.getIdContrato()) : "-", courier));
            tablaCliente.addCell(labelCell("Fecha de Emisión", courierBold));
            tablaCliente.addCell(valueCell(fechaStr, courier));
            tablaCliente.addCell(labelCell("Tipo de Moneda", courierBold));
            tablaCliente.addCell(valueCell(monedaNombre, courier));

            doc.add(tablaCliente);

            doc.add(new Paragraph()
                    .setBorderBottom(new SolidBorder(GRIS_MEDIO, 0.5f))
                    .setMarginTop(3).setMarginBottom(3));

            // ═══════════════ MONTO ═══════════════
            Table tablaMonto = new Table(UnitValue.createPercentArray(new float[]{1, 0.55f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaSon = new Cell()
                    .setBorder(Border.NO_BORDER).setPadding(1)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaSon.add(new Paragraph("SON: " + NumeroALetras.convertir(egreso.getMonto(), "PEN".equals(egreso.getMoneda()) ? com.Inmobiliaria.demo.enums.Moneda.PEN : com.Inmobiliaria.demo.enums.Moneda.USD))
                    .setFont(courier).setFontSize(7f).setItalic());
            tablaMonto.addCell(celdaSon);

            Table tTotal = new Table(UnitValue.createPercentArray(new float[]{1, 0.7f}))
                    .setWidth(UnitValue.createPercentValue(100));
            tTotal.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 2f))
                    .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                    .setPadding(2).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("Importe Total:").setFont(courierBold).setFontSize(10f)));
            tTotal.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 2f))
                    .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                    .setPadding(2).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(monedaSimbolo + " " + montoStr).setFont(courierBold).setFontSize(10f)));
            tablaMonto.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(tTotal));

            doc.add(tablaMonto);

            doc.add(new Paragraph()
                    .setBorderBottom(new SolidBorder(GRIS_MEDIO, 0.5f))
                    .setMarginTop(5).setMarginBottom(3));

            doc.add(new Paragraph("DOCUMENTO INTERNO SIN VALOR TRIBUTARIO")
                    .setFont(courier).setFontSize(8f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0));

            // ── LÍNEA GRIS IZQUIERDA ──
            com.itextpdf.kernel.pdf.canvas.PdfCanvas lienzo = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(pdf.getFirstPage());
            lienzo.setStrokeColor(new DeviceGray(0.55f))
                  .setLineWidth(1.2f)
                  .moveTo(0, 210f)
                  .lineTo(28, 210f)
                  .stroke();
            lienzo.release();

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar recibo de egreso PDF", e);
        }
    }

    private static Cell labelCell(String texto, PdfFont bold) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0.5f)
                .add(new Paragraph(texto).setFont(bold).setFontSize(7.5f)
                        .setTextAlignment(TextAlignment.LEFT));
    }

    private static Cell valueCell(String texto, PdfFont normal) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(1.5f)
                .add(new Paragraph(": " + (texto != null ? texto : "-")).setFont(normal).setFontSize(7.5f));
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ReciboEgresoPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}