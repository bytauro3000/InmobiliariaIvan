package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.Moneda;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ComprobantePagoLetraPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_OSCURO = new DeviceGray(0.15f);
    private static final DeviceGray GRIS_MEDIO  = new DeviceGray(0.45f);

    private static final String EMPRESA   = "INMOBILIARIA CONSTRUCTORA \"IVAN\" E.I.R.L.";
    private static final String DIRECCION = "Av. Alfredo Mendiola N 3623  3er. Piso Of. 301 - Urb. Panamericana Norte - Los Olivos - Lima";
    private static final String TELEFONO  = "Cel.: +51 987-891-788";
    private static final String RUC       = "R.U.C.: 20537853108";
    private static final String BASE_URL  = "https://inmobiliariaivan.onrender.com/api/pagos";

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODO AUXILIAR: construye "NOMBRE APELLIDO (DNI: 12345678)" por cliente
    // Soporta 1 o 2 clientes separados con " / "
    // ─────────────────────────────────────────────────────────────────────────
    private static String construirTextoClientes(Contrato contrato) {
        if (contrato.getClientes() == null || contrato.getClientes().isEmpty()) return "-";
        return contrato.getClientes().stream()
                .map(cc -> {
                    var c = cc.getCliente();
                    String nombre = c.getNombre() + " " + c.getApellidos();
                    String dni    = (c.getNumDoc() != null && !c.getNumDoc().isBlank())
                                    ? c.getNumDoc() : "--------";
                    return nombre.toUpperCase() + " (DNI: " + dni + ")";
                })
                .collect(Collectors.joining("/"));
    }

    private static Cell construirCeldaClientes(Contrato contrato,
                                               PdfFont normal, PdfFont bold,
                                               float size, float padding) {
        Cell celda = new Cell()
                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setBorderBottom(Border.NO_BORDER)
                .setPadding(padding);

        if (contrato.getClientes() == null || contrato.getClientes().isEmpty()) {
            celda.add(lineaDato("Recibi de:", "-", normal, bold, size));
            return celda;
        }

        java.util.Collection<com.Inmobiliaria.demo.entity.ContratoCliente> clientes = contrato.getClientes();
        boolean esPrimero = true;
        for (com.Inmobiliaria.demo.entity.ContratoCliente cc : clientes) {
            com.Inmobiliaria.demo.entity.Cliente c = cc.getCliente();
            String nombre = c.getNombre() + " " + c.getApellidos();
            String dni    = (c.getNumDoc() != null && !c.getNumDoc().isBlank())
                            ? c.getNumDoc() : "--------";
            String linea  = nombre.toUpperCase() + " (DNI: " + dni + ")";

            Paragraph p;
            if (esPrimero) {
                p = new Paragraph()
                        .add(new Text("Recibi de:").setFont(bold).setFontSize(size))
                        .add(new Text(linea).setFont(normal).setFontSize(size))
                        .setMarginBottom(0);
                esPrimero = false;
            } else {
                float indentPts = size * 0.6f * 10f;
                p = new Paragraph()
                        .add(new Text(linea).setFont(normal).setFontSize(size))
                        .setMarginLeft(indentPts)
                        .setMarginTop(0).setMarginBottom(0);
            }
            p.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
                    com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
            celda.add(p);
        }
        return celda;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PÁGINA REVERSO: vouchers adjuntos
    // Siempre 3 por página en fila, mismo tamaño fijo.
    // Si hay menos de 3 en la última página, se centran con celdas vacías.
    // ─────────────────────────────────────────────────────────────────────────
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

        // Tamaño fijo siempre igual — el mismo que se ve bien con 3 vouchers
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

            doc.add(new Paragraph("COMPROBANTES DE PAGO ADJUNTOS")
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

            // Siempre 3 columnas — las vacías centran los vouchers cuando hay menos de 3
            float[] colWidths = new float[COLS];
            Arrays.fill(colWidths, 1f);
            Table grid = new Table(UnitValue.createPercentArray(colWidths))
                    .setWidth(UnitValue.createPercentValue(100));

            // Celdas vacías a la izquierda para centrar cuando hay 1 o 2 vouchers
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
                            .setFont(courierBold).setFontSize(7f)
                            .setFontColor(GRIS_MEDIO)
                            .setTextAlignment(TextAlignment.CENTER));
                }
                grid.addCell(cell);
            }

            // Celdas vacías a la derecha
            int vaciosDer = COLS - n - vaciosIzq;
            for (int i = 0; i < vaciosDer; i++) {
                grid.addCell(new Cell().setBorder(Border.NO_BORDER).setHeight(CELL_H));
            }

            doc.add(grid);

            idx += COLS;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Elimina páginas en blanco al final del PDF (iText a veces genera una extra)
    // ─────────────────────────────────────────────────────────────────────────
    private static byte[] eliminarPaginasEnBlanco(byte[] pdfBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(pdfBytes)),
                    new PdfWriter(out));
            int n = pdfDoc.getNumberOfPages();
            for (int i = n; i >= 1; i--) {
                PdfPage page = pdfDoc.getPage(i);
                boolean esBlanco = true;
                for (int j = 0; j < page.getContentStreamCount(); j++) {
                    byte[] content = page.getContentStream(j).getBytes();
                    if (content != null && content.length > 10) {
                        esBlanco = false;
                        break;
                    }
                }
                if (esBlanco) {
                    pdfDoc.removePage(i);
                } else {
                    break; // Solo eliminar las del final
                }
            }
            pdfDoc.close();
            return out.toByteArray();
        } catch (Exception e) {
            return pdfBytes; // Si falla, devolver el original
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPROBANTE INDIVIDUAL
    // ─────────────────────────────────────────────────────────────────────────
    public static byte[] generar(PagoLetras pago, String rolUsuario) {
        return generar(pago, rolUsuario, Collections.emptyList());
    }

    public static byte[] generar(PagoLetras pago, String rolUsuario, List<Voucher> vouchers) {
        float fs1   = 10f;
        float cH1   = (4f * fs1 * 1.6f) + (5f * 2f) + (3f * 3f);
        float tot1  = 115f + 5f + 30f + cH1 + 72f;
        float marg1 = Math.min(30f, Math.max(8f, (420f - tot1) / 2f));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            PageSize a5h = PageSize.A5.rotate();
            Document doc = new Document(pdf, a5h);
            doc.setMargins(marg1, 18, marg1, 52);

            LetraCambio letra    = pago.getLetra();
            Contrato    contrato = letra.getContrato();

            TipoComprobante tipoCompPago = (pago.getComprobante() != null)
                    ? pago.getComprobante().getTipoComprobante()
                    : null;

            String tituloPrincipal = (tipoCompPago == TipoComprobante.BOLETA)
                    ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";

            String numComp = (pago.getComprobante() != null
                    && pago.getComprobante().getNumeroCompleto() != null)
                    ? pago.getComprobante().getNumeroCompleto() : "----------";

            String clientes = construirTextoClientes(contrato);

            String usuarioRegistro = "-";
            if (contrato.getUsuario() != null) {
                usuarioRegistro = contrato.getUsuario().getNombres()
                        + " " + contrato.getUsuario().getApellidos();
            }

            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().iterator().next();
                loteInfo = "Mz. " + cl.getLote().getManzana()
                        + " Lt. " + cl.getLote().getNumeroLote()
                        + " - " + cl.getLote().getPrograma().getNombrePrograma();
            }

            String importeTexto = letra.getImporteLetras() != null ? letra.getImporteLetras() : "-";
            String fechaPagoStr = pago.getFechaPago() != null ? pago.getFechaPago().format(FMT) : "-";
            String fechaVencStr = letra.getFechaVencimiento() != null ? letra.getFechaVencimiento().format(FMT) : "-";
            String medioPago    = pago.getMedioPago() != null ? pago.getMedioPago().name() : "-";
            String numOp        = (pago.getNumeroOperacion() != null && !pago.getNumeroOperacion().isBlank())
                                  ? "   N\u00b0 Op: " + pago.getNumeroOperacion() : "";

            Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
            String simboloMoneda  = (monedaContrato == Moneda.PEN) ? "S/" : "$";

            String numLetraFormateado = letra.getNumeroLetra() != null
                    ? letra.getNumeroLetra().replace("/", " de ") : "-";
            String concepto = "Pago de la Letra N\u00b0 " + numLetraFormateado
                    + "  -  Contrato N\u00b0 " + contrato.getIdContrato() + "  -  " + loteInfo;

            // QR
            String urlQr = BASE_URL + "/" + pago.getIdPago() + "/comprobante-pdf";
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(52).setHeight(52)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            String logoUrl = "https://res.cloudinary.com/dlgqaifrk/image/upload/e_grayscale,w_200,h_200,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png";
            Image logoImg = new Image(ImageDataFactory.create(new URL(logoUrl)))
                    .setWidth(70).setHeight(70)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ── ENCABEZADO ──
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
            celdaEmpresa.add(new Paragraph(EMPRESA)
                    .setFont(courierBold).setFontSize(11f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(DIRECCION)
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(TELEFONO + "          " + RUC)
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            celdaEmpresa.add(new Paragraph(tituloPrincipal)
                    .setFont(courierBold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph("N\u00b0 " + numComp)
                    .setFont(courierBold).setFontSize(9f)
                    .setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(2));
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea tu\ncomprobante")
                    .setFont(arial).setFontSize(7f)
                    .setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2).setMarginBottom(0));
            encabezado.addCell(celdaQr);
            doc.add(encabezado);

            // ── FILA: Recibi de + Caja monto ──
            float fuenteCliente = clientes.length() > 120 ? 7.5f
                                : clientes.length() > 100 ? 8.5f
                                : clientes.length() > 80  ? 9f : 10f;
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);
            filaRecibo.addCell(construirCeldaClientes(contrato, courier, courierBold, fuenteCliente, 8f));
            filaRecibo.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simboloMoneda + " " + DF.format(pago.getImportePagado()))
                            .setFont(courierBold).setFontSize(15)
                            .setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // ── CUERPO ──
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setPaddingTop(5).setPaddingBottom(5);
            celdaCuerpo.add(lineaDato("La cantidad de: ", importeTexto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Por concepto de: ", concepto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Medio de pago: ", medioPago + numOp, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Fecha venc. letra: ", fechaVencStr, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // ── PIE ──
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaNombre = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaNombre.add(new Paragraph(usuarioRegistro)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4).setMarginBottom(1));
            celdaNombre.add(new Paragraph(rolUsuario != null ? rolUsuario.toUpperCase() : "SECRETARIA")
                    .setFont(courier).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaNombre);

            String[] pf = fechaPagoStr.split("/");
            String dia  = pf.length > 0 ? pf[0] : "--";
            String mes  = pf.length > 1 ? pf[1] : "--";
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

            Cell celdaFirma = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(10).setMarginBottom(2));
            Table lineaFirma = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(85))
                    .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(3);
            lineaFirma.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(0).setHeight(1));
            celdaFirma.add(lineaFirma);
            celdaFirma.add(new Paragraph("GERENTE GENERAL")
                    .setFont(courierBold).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaFirma);

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

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante PDF: " + e.getMessage(), e);
        }

        return eliminarPaginasEnBlanco(out.toByteArray());
    }


    // ─────────────────────────────────────────────────────────────────────────
    // COMPROBANTE MÚLTIPLE
    // ─────────────────────────────────────────────────────────────────────────
    public static byte[] generarMultiple(List<PagoLetras> pagos, String rolUsuario) {
        return generarMultiple(pagos, rolUsuario, Collections.emptyList());
    }

    public static byte[] generarMultiple(List<PagoLetras> pagos, String rolUsuario, List<Voucher> vouchers) {
        if (pagos == null || pagos.isEmpty()) throw new RuntimeException("Lista de pagos vacía");
        if (pagos.size() == 1) return generar(pagos.get(0), rolUsuario, vouchers);

        PagoLetras  primero   = pagos.get(0);
        LetraCambio letraRef  = primero.getLetra();
        Contrato    contrato  = letraRef.getContrato();

        java.math.BigDecimal totalImporte = pagos.stream()
                .map(PagoLetras::getImportePagado)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        List<String> numerosLetra = pagos.stream()
                .map(p -> p.getLetra().getNumeroLetra() != null
                        ? p.getLetra().getNumeroLetra().split("/")[0].trim() : "?")
                .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)))
                .collect(Collectors.toList());

        String totalLetras = letraRef.getNumeroLetra() != null
                ? letraRef.getNumeroLetra().split("/")[1].trim() : "?";

        String letrasStr;
        if (numerosLetra.size() == 1) {
            letrasStr = "N\u00b0 " + numerosLetra.get(0);
        } else {
            String ultimas    = numerosLetra.get(numerosLetra.size() - 1);
            String anteriores = String.join(", ", numerosLetra.subList(0, numerosLetra.size() - 1));
            letrasStr = "N\u00b0 " + anteriores + " y " + ultimas;
        }

        Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
        String importeTexto   = NumeroALetras.convertir(totalImporte, monedaContrato);
        String simboloMoneda  = (monedaContrato == Moneda.PEN) ? "S/" : "$";

        String loteInfo = "-";
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            ContratoLote cl = contrato.getLotes().iterator().next();
            loteInfo = "Mz. " + cl.getLote().getManzana()
                    + " Lt. " + cl.getLote().getNumeroLote()
                    + " - " + cl.getLote().getPrograma().getNombrePrograma();
        }
        String concepto = "Pago de las Letras " + letrasStr + " de " + totalLetras
                + " Letras  -  Contrato N\u00b0 " + contrato.getIdContrato() + "  -  " + loteInfo;

        String clientes = construirTextoClientes(contrato);

        String usuarioRegistro = "-";
        if (contrato.getUsuario() != null) {
            usuarioRegistro = contrato.getUsuario().getNombres()
                    + " " + contrato.getUsuario().getApellidos();
        }

        TipoComprobante tipoCompMultiple = (primero.getComprobante() != null)
                ? primero.getComprobante().getTipoComprobante()
                : null;

        String tituloPrincipal = (tipoCompMultiple == TipoComprobante.BOLETA)
                ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";
        String numComp = (primero.getComprobante() != null
                && primero.getComprobante().getNumeroCompleto() != null)
                ? primero.getComprobante().getNumeroCompleto() : "----------";

        String fechaPagoStr = primero.getFechaPago() != null ? primero.getFechaPago().format(FMT) : "-";
        String medioPago    = primero.getMedioPago() != null ? primero.getMedioPago().name() : "-";
        String numOp        = (primero.getNumeroOperacion() != null && !primero.getNumeroOperacion().isBlank())
                              ? "   N\u00b0 Op: " + primero.getNumeroOperacion() : "";

        List<String> fechasVenc = pagos.stream()
                .sorted(java.util.Comparator.comparingInt(p -> {
                    try { return Integer.parseInt(p.getLetra().getNumeroLetra().split("/")[0].trim()); }
                    catch (Exception ex) { return 0; }
                }))
                .map(p -> p.getLetra().getFechaVencimiento() != null
                        ? p.getLetra().getFechaVencimiento().format(FMT) : "-")
                .collect(Collectors.toList());
        String fechasVencStr;
        if (fechasVenc.size() == 1) {
            fechasVencStr = fechasVenc.get(0);
        } else {
            String todas = String.join(", ", fechasVenc.subList(0, fechasVenc.size() - 1));
            fechasVencStr = todas + " y " + fechasVenc.get(fechasVenc.size() - 1);
        }

        int numLetras   = pagos.size();
        float padCuerpo    = numLetras >= 10 ? 3f : numLetras >= 5 ? 4f : 6f;
        float fsCuerpo     = 9.5f;
        float marginTopRec = numLetras >= 10 ? 2f : numLetras >= 5 ? 3f : 4f;
        float padRecibo    = numLetras >= 10 ? 4f : numLetras >= 5 ? 5f : 7f;

        int charsL1   = 56;
        int charsCont = 79;
        int charsFechas = fechasVencStr.length();
        int lineasExtra = 0;
        if (charsFechas > charsL1) {
            lineasExtra = (int)Math.ceil((float)(charsFechas - charsL1) / charsCont);
        }
        float lineH    = fsCuerpo * 1.6f;
        float cuerpoH  = (4f * lineH) + (padCuerpo * 2f) + (3f * 3f) + (lineasExtra * lineH);
        float totalH   = 115f + marginTopRec + 28f + cuerpoH + 72f;
        float libre    = 420f - totalH;
        float margenSim = Math.min(30f, Math.max(8f, libre / 2f));
        float margenTop = margenSim;
        float margenV   = margenSim;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc   = new Document(pdf, PageSize.A5.rotate());
            doc.setMargins(margenTop, 18, margenV, 52);

            String urlQr = BASE_URL + "/comprobante-multiple/" + numComp;
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(52).setHeight(52)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            String logoUrl = "https://res.cloudinary.com/dlgqaifrk/image/upload/e_grayscale,w_200,h_200,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png";
            Image logoImg  = new Image(ImageDataFactory.create(new URL(logoUrl)))
                    .setWidth(70).setHeight(70).setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ── ENCABEZADO ──
            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{0.18f, 1, 0.22f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaLogo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(Border.NO_BORDER).setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER);
            celdaLogo.add(logoImg);
            encabezado.addCell(celdaLogo);

            Cell celdaEmpresa = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                    .setPadding(5).setTextAlignment(TextAlignment.CENTER);
            celdaEmpresa.add(new Paragraph(EMPRESA)
                    .setFont(courierBold).setFontSize(11f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(DIRECCION)
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(TELEFONO + "          " + RUC)
                    .setFont(courier).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            celdaEmpresa.add(new Paragraph(tituloPrincipal)
                    .setFont(courierBold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEmpresa.add(new Paragraph("N\u00b0 " + numComp)
                    .setFont(courierBold).setFontSize(9f)
                    .setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(2));
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER).setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea tu\ncomprobante")
                    .setFont(arial).setFontSize(7f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(2).setMarginBottom(0));
            encabezado.addCell(celdaQr);
            doc.add(encabezado);

            // ── FILA: Recibi de + Monto total ──
            float fuenteCliente = clientes.length() > 120 ? 7.5f
                                : clientes.length() > 100 ? 8.5f
                                : clientes.length() > 80  ? 9f : 10f;
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(marginTopRec);
            filaRecibo.addCell(construirCeldaClientes(contrato, courier, courierBold, fuenteCliente, padRecibo));
            filaRecibo.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simboloMoneda + " " + DF.format(totalImporte))
                            .setFont(courierBold).setFontSize(15).setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // ── CUERPO ──
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8).setPaddingTop(padCuerpo).setPaddingBottom(padCuerpo);
            celdaCuerpo.add(lineaDato("La cantidad de: ", importeTexto, courier, courierBold, fsCuerpo));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Por concepto de: ", concepto, courier, courierBold, fsCuerpo));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Medio de pago: ", medioPago + numOp, courier, courierBold, fsCuerpo));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Fechas de vencimiento: ", fechasVencStr, courier, courierBold, fsCuerpo));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // ── PIE ──
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaNombre = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaNombre.add(new Paragraph(usuarioRegistro)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4).setMarginBottom(1));
            celdaNombre.add(new Paragraph(rolUsuario != null ? rolUsuario.toUpperCase() : "SECRETARIA")
                    .setFont(courier).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaNombre);

            String[] pf = fechaPagoStr.split("/");
            String dia  = pf.length > 0 ? pf[0] : "--";
            String mes  = pf.length > 1 ? pf[1] : "--";
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

            Cell celdaFirma = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(10).setMarginBottom(2));
            Table lineaFirma = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(85))
                    .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(3);
            lineaFirma.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(0).setHeight(1));
            celdaFirma.add(lineaFirma);
            celdaFirma.add(new Paragraph("GERENTE GENERAL")
                    .setFont(courierBold).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaFirma);

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

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante múltiple: " + e.getMessage(), e);
        }
        return eliminarPaginasEnBlanco(out.toByteArray());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────
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

    private static Cell celdaFechaBox(String valor, PdfFont bold) {
        return new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(3).setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(valor).setFont(bold).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ComprobantePagoLetraPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}