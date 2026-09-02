package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.entity.Voucher;
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
import com.itextpdf.kernel.pdf.PdfViewerPreferences;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Plantilla del RECIBO DE EGRESOS (serie EG01). Documento interno de salida de
 * dinero (pago de comisión a vendedor). Reutilizable a futuro para egresos
 * genéricos (parceleros/proveedores). Incluye reverso con vouchers adjuntos.
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

    public static byte[] generar(ReciboEgreso egreso, List<Voucher> vouchers) {
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
            String fechaOperacionStr = egreso.getFechaOperacion() != null
                    ? egreso.getFechaOperacion().format(FMT) : null;

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
            tablaCliente.addCell(conceptoCell(egreso.getConcepto(), courier));
            tablaCliente.addCell(labelCell("Contrato N°", courierBold));
            tablaCliente.addCell(valueCell(egreso.getIdContrato() != null ? String.valueOf(egreso.getIdContrato()) : "-", courier));
            tablaCliente.addCell(labelCell("Fecha de Emisión", courierBold));
            tablaCliente.addCell(valueCell(fechaStr, courier));
            tablaCliente.addCell(labelCell("Tipo de Moneda", courierBold));
            tablaCliente.addCell(valueCell(monedaNombre, courier));

            if (egreso.getMedioPago() != null && !egreso.getMedioPago().isBlank()) {
                tablaCliente.addCell(labelCell("Medio de Pago", courierBold));
                tablaCliente.addCell(valueCell(egreso.getMedioPago(), courier));
            }
            if (egreso.getNumeroOperacion() != null && !egreso.getNumeroOperacion().isBlank()) {
                tablaCliente.addCell(labelCell("N° Operación", courierBold));
                tablaCliente.addCell(valueCell(egreso.getNumeroOperacion(), courier));
            }
            if (fechaOperacionStr != null) {
                tablaCliente.addCell(labelCell("Fecha Operación", courierBold));
                tablaCliente.addCell(valueCell(fechaOperacionStr, courier));
            }

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

            agregarPaginaReverso(doc, pdf, vouchers, courierBold);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar recibo de egreso PDF", e);
        }
    }

    /** Celdas de concepto multi-línea (detalle por lote del egreso). */
    private static Cell conceptoCell(String concepto, PdfFont normal) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(1.5f);
        if (concepto != null) {
            String[] lineas = concepto.split("\\r?\\n");
            for (String linea : lineas) {
                cell.add(new Paragraph(": " + linea).setFont(normal).setFontSize(7.5f)
                        .setMarginBottom(0.5f));
            }
        } else {
            cell.add(new Paragraph(": -").setFont(normal).setFontSize(7.5f));
        }
        return cell;
    }

    private static void agregarPaginaReverso(Document doc, PdfDocument pdf,
                                             List<Voucher> vouchers, PdfFont courierBold) {
        if (vouchers == null || vouchers.isEmpty()) return;

        pdf.getCatalog().setViewerPreferences(
            new PdfViewerPreferences()
                .setDuplex(PdfViewerPreferences.PdfViewerPreferencesConstants.DUPLEX_FLIP_LONG_EDGE)
        );

        doc.setTopMargin(8f);
        doc.setBottomMargin(8f);
        doc.setLeftMargin(10f);
        doc.setRightMargin(10f);

        final int    COLS    = 3;
        final float  IMG_W   = 172f;
        final float  IMG_H   = 280f;
        final float  CELL_H  = 290f;

        int total = vouchers.size();
        int idx   = 0;

        while (idx < total) {
            List<Voucher> grupo = vouchers.subList(idx, Math.min(idx + COLS, total));
            int n = grupo.size();

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            doc.add(new Paragraph("VOUCHERS DE PAGO ADJUNTOS")
                    .setFont(courierBold).setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4).setMarginTop(0));

            Table lineaTitulo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);
            lineaTitulo.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(0).setHeight(1));
            doc.add(lineaTitulo);

            float[] colWidths = new float[COLS];
            Arrays.fill(colWidths, 1f);
            Table grid = new Table(UnitValue.createPercentArray(colWidths))
                    .setWidth(UnitValue.createPercentValue(100));

            int vaciosIzq = (COLS - n) / 2;
            for (int i = 0; i < vaciosIzq; i++) {
                grid.addCell(new Cell().setBorder(Border.NO_BORDER).setHeight(CELL_H));
            }

            for (Voucher v : grupo) {
                Cell cell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(4)
                        .setHeight(CELL_H)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                try {
                    Image img = new Image(ImageDataFactory.create(new URL(v.getUrl())))
                            .setWidth(IMG_W)
                            .setHeight(IMG_H)
                            .setAutoScale(false)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                    cell.add(img);
                } catch (Exception e) {
                    cell.add(new Paragraph("[ Imagen no\ndisponible ]")
                            .setFont(courierBold).setFontSize(8f)
                            .setTextAlignment(TextAlignment.CENTER));
                }
                grid.addCell(cell);
            }

            int vaciosDer = COLS - n - vaciosIzq;
            for (int i = 0; i < vaciosDer; i++) {
                grid.addCell(new Cell().setBorder(Border.NO_BORDER).setHeight(CELL_H));
            }

            doc.add(grid);
            idx += COLS;
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