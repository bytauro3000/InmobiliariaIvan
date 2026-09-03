package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.service.LogoCacheService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfViewerPreferences;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
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
 * dinero (pago de comisión a vendedor). Estructura similar al RECIBO DE INGRESO:
 * - Cabecera con logo/empresa y "RECIBO DE EGRESO N° EG01-x"
 * - Fila "Pagado a: <vendedor> (DNI: ...)" + caja con el monto
 * - Cuerpo: "La cantidad de:", "Por concepto de:", "Medio de pago:"
 * - Pie con 3 cuadros: vendedor + DNI + firma | fecha de pago | usuario que
 *   realizó el pago + firma. Reverso con vouchers adjuntos.
 */
public class ReciboEgresoPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_OSCURO = new DeviceGray(0.15f);
    private static final DeviceGray GRIS_MEDIO = new DeviceGray(0.45f);

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document doc = new Document(pdf, PageSize.A5.rotate())) {

            float fs1 = 10f;
            float cH1 = (4f * fs1 * 1.6f) + (5f * 2f) + (3f * 3f);
            float tot1 = 115f + 5f + 30f + cH1 + 72f;
            float marg1 = Math.min(30f, Math.max(8f, (420f - tot1) / 2f));
            doc.setMargins(marg1, 18, marg1, 52);

            PdfFont courier = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            String serieCorrelativo = egreso.getSerie() + "-" + egreso.getNumero();
            String monedaSimbolo = "PEN".equals(egreso.getMoneda()) ? "S/" : "$";
            String montoStr = DF.format(egreso.getMonto() != null ? egreso.getMonto() : BigDecimal.ZERO);
            String fechaEmisionStr = egreso.getFechaEmision() != null ? egreso.getFechaEmision().format(FMT) : "-";
            String fechaOperacionStr = egreso.getFechaOperacion() != null
                    ? egreso.getFechaOperacion().format(FMT) : null;

            String beneficiario = egreso.getBeneficiario() != null ? egreso.getBeneficiario().toUpperCase() : "-";
            String dni = egreso.getDniBeneficiario() != null && !egreso.getDniBeneficiario().isBlank()
                    ? egreso.getDniBeneficiario() : "--------";

            String importeLetras = NumeroALetras.convertir(
                    egreso.getMonto() != null ? egreso.getMonto() : BigDecimal.ZERO,
                    "PEN".equals(egreso.getMoneda()) ? Moneda.PEN : Moneda.USD);

            String medioPago = egreso.getMedioPago() != null ? egreso.getMedioPago() : "-";
            String numOp = (egreso.getNumeroOperacion() != null && !egreso.getNumeroOperacion().isBlank())
                    ? "   N\u00b0 Op: " + egreso.getNumeroOperacion() : "";
            String fechaOp = (fechaOperacionStr != null) ? "   Fecha Op: " + fechaOperacionStr : "";

            String usuarioRegistro = egreso.getUsuarioRegistro() != null
                    ? egreso.getUsuarioRegistro() : "SECRETARIA";

            // ── ENCABEZADO ──
            ImageData logoData = LogoCacheService.logoImageData();
            Image logoImg = (logoData != null
                    ? new Image(logoData)
                    : new Image(ImageDataFactory.create(new URL(logoUrl()))))
                    .setWidth(70).setHeight(70)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{0.18f, 1, 0.22f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaLogo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);
            celdaLogo.add(logoImg);
            encabezado.addCell(celdaLogo);

            Cell celdaEmpresa = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPadding(5).setTextAlignment(TextAlignment.CENTER);
            celdaEmpresa.add(new Paragraph(empresa())
                    .setFont(courierBold).setFontSize(11f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(direccion())
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(telefono() + "          " + ruc())
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            celdaEmpresa.add(new Paragraph("RECIBO DE EGRESO")
                    .setFont(courierBold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph("N\u00b0 " + serieCorrelativo)
                    .setFont(courierBold).setFontSize(9f).setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(2));
            encabezado.addCell(celdaEmpresa);

            // Celda derecha vacía (sin QR) pero con borde para el recuadro completo
            Cell celdaDerecha = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaDerecha.add(new Paragraph("DOCUMENTO\nINTERNO")
                    .setFont(courier).setFontSize(7f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER));
            encabezado.addCell(celdaDerecha);

            doc.add(encabezado);

            // ── FILA: Pagado a + Caja monto ──
            float fuenteCliente = beneficiario.length() > 100 ? 8.5f
                    : beneficiario.length() > 80 ? 9f : 10f;

            Table filaPagado = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);

            Cell celdaPagado = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setPadding(8);
            Paragraph pPagado = new Paragraph()
                    .add(new Text("Pagado a: ").setFont(courierBold).setFontSize(fuenteCliente))
                    .add(new Text(beneficiario + " (DNI: " + dni + ")").setFont(courier).setFontSize(fuenteCliente))
                    .setMarginBottom(0);
            pPagado.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
                    com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
            celdaPagado.add(pPagado);
            filaPagado.addCell(celdaPagado);

            filaPagado.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(monedaSimbolo + " " + montoStr)
                            .setFont(courierBold).setFontSize(15)
                            .setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaPagado);

            // ── CUERPO ──
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8).setPaddingTop(5).setPaddingBottom(5);

            // "La cantidad de" + concepto (multilínea) en un solo párrafo
            Paragraph pCantidad = new Paragraph()
                    .add(new Text("La cantidad de: ").setFont(courierBold).setFontSize(10f))
                    .add(new Text(importeLetras).setFont(courier).setFontSize(10f))
                    .setMarginBottom(1);
            pCantidad.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
                    com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
            celdaCuerpo.add(pCantidad);
            celdaCuerpo.add(separadorLinea());

            String concepto = egreso.getConcepto() != null ? egreso.getConcepto() : "-";
            Paragraph pConcepto = new Paragraph()
                    .add(new Text("Por concepto de: ").setFont(courierBold).setFontSize(10f));
            String[] lineas = concepto.split("\\r?\\n");
            for (int i = 0; i < lineas.length; i++) {
                pConcepto.add(new Text(lineas[i]).setFont(courier).setFontSize(9f));
                if (i < lineas.length - 1) {
                    pConcepto.add(new Text("\n                   ").setFont(courier).setFontSize(9f));
                }
            }
            pConcepto.setMarginBottom(1);
            pConcepto.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
                    com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
            celdaCuerpo.add(pConcepto);
            celdaCuerpo.add(separadorLinea());

            celdaCuerpo.add(lineaDato("Medio de pago: ", medioPago + numOp + fechaOp, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Fecha de Emisión: ", fechaEmisionStr, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // ── PIE (3 cuadros) ──
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Cuadro izquierdo: vendedor + DNI + línea firma
            Cell celdaVendedor = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaVendedor.add(new Paragraph(beneficiario)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4).setMarginBottom(1));
            celdaVendedor.add(new Paragraph("DNI: " + dni)
                    .setFont(courier).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaVendedor.add(lineaFirma());
            celdaVendedor.add(new Paragraph("VENDEDOR / FIRMA")
                    .setFont(courierBold).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaVendedor);

            // Cuadro central: fecha de pago (día/mes/año en cajas)
            String[] pf = fechaEmisionStr.split("/");
            String dia = pf.length > 0 ? pf[0] : "--";
            String mes = pf.length > 1 ? pf[1] : "--";
            String anio = pf.length > 2 ? pf[2] : "----";

            Cell celdaFecha = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(8).setPaddingRight(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaFecha.add(new Paragraph("Fecha de Pago")
                    .setFont(courierBold).setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            Table lineaSep = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginLeft(-8).setMarginRight(-8).setMarginBottom(4);
            lineaSep.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(0).setHeight(1));
            celdaFecha.add(lineaSep);
            celdaFecha.add(new Paragraph("DIA    MES    A\u00d1O")
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            Table tablaFecha = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));
            tablaFecha.addCell(celdaFechaBox(dia, courierBold));
            tablaFecha.addCell(celdaFechaBox(mes, courierBold));
            tablaFecha.addCell(celdaFechaBox(anio, courierBold));
            celdaFecha.add(tablaFecha);
            pie.addCell(celdaFecha);

            // Cuadro derecho: usuario que realizó el pago + línea firma
            Cell celdaUsuario = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaUsuario.add(new Paragraph(" ").setFont(courier).setFontSize(10).setMarginBottom(2));
            celdaUsuario.add(lineaFirma());
            celdaUsuario.add(new Paragraph(usuarioRegistro)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4).setMarginBottom(1));
            celdaUsuario.add(new Paragraph("REALIZÓ EL PAGO")
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaUsuario);

            doc.add(pie);

            // ── LÍNEA GRIS IZQUIERDA ──
            PdfPage page = pdf.getFirstPage();
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.setStrokeColor(new DeviceGray(0.55f))
                  .setLineWidth(1.2f)
                  .moveTo(0, 210f)
                  .lineTo(28, 210f)
                  .stroke();
            canvas.release();

            // ── REVERSO: vouchers adjuntos ──
            agregarPaginaReverso(doc, pdf, vouchers, courierBold);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar recibo de egreso PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
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

    private static Paragraph lineaDato(String label, String valor,
                                        PdfFont normal, PdfFont bold, float size) {
        Paragraph p = new Paragraph()
                .add(new Text(label).setFont(bold).setFontSize(size))
                .add(new Text(valor).setFont(normal).setFontSize(size))
                .setMarginBottom(1);
        p.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
                com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
        return p;
    }

    private static Table separadorLinea() {
        Table linea = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(1).setMarginBottom(1);
        linea.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceGray(0.75f), 0.5f))
                .setPadding(0).setHeight(1));
        return linea;
    }

    private static Table lineaFirma() {
        Table linea = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(85))
                .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(3);
        linea.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(0).setHeight(1));
        return linea;
    }

    private static Cell celdaFechaBox(String valor, PdfFont bold) {
        return new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(3).setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(valor).setFont(bold).setFontSize(9f));
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ReciboEgresoPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}