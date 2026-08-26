package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.LogoCacheService;
import com.itextpdf.barcodes.BarcodeQRCode;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.Inmobiliaria.demo.config.EmpresaContext;

/**
 * Genera el comprobante PDF de pago inicial de un contrato.
 * Diseño idéntico a ComprobanteInscripcionPdf, adaptado al contexto de inicial.
 */
public class ComprobantePagoInicialPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00",
            new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_OSCURO = new DeviceGray(0.15f);
    private static final DeviceGray GRIS_MEDIO  = new DeviceGray(0.45f);

    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
    private static String direccion() { return EmpresaPdfUtil.direccionCompleta(); }
    private static String telefono() { return "Cel.: " + EmpresaContext.empresaService.obtenerActiva().getCelular(); }
    private static String ruc() { return "R.U.C.: " + EmpresaContext.empresaService.obtenerActiva().getRuc(); }
    private static String logoUrl() { return EmpresaContext.empresaService.obtenerActiva().getLogoSmallUrl(); }

    private static final String BASE_URL  = "https://inmobiliariaivan.onrender.com/api/contratos";

    public static byte[] generar(PagoInicial pago, String rolUsuario) {
        return generar(pago, rolUsuario, Collections.emptyList());
    }

    public static byte[] generar(PagoInicial pago, String rolUsuario, List<Voucher> vouchers) {

        // Cálculo dinámico de márgenes — A5 landscape 595x420 pts
        float fs      = 9.5f;
        float lineH   = fs * 1.55f;
        float cH      = (5f * lineH) + (3f * 2f) + (4f * 3f);
        float tot     = 118f + 3f + 33f + cH + 78f;
        float margin  = Math.min(25f, Math.max(8f, (420f - tot) / 2f));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc   = new Document(pdf, PageSize.A5.rotate());
            doc.setMargins(margin, 18, margin, 52);

            Contrato contrato = pago.getContrato();

            Moneda monedaContrato = (contrato.getMoneda() != null) ? contrato.getMoneda() : Moneda.USD;
            String  simbolo       = (monedaContrato == Moneda.PEN) ? "S/" : "$";

            // Tipo y número de comprobante
            TipoComprobante tipoComp = (pago.getComprobante() != null)
                    ? pago.getComprobante().getTipoComprobante() : null;

            String tituloPrincipal = (tipoComp == TipoComprobante.BOLETA)
                    ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";

            String numComp = (pago.getComprobante() != null
                    && pago.getComprobante().getNumeroCompleto() != null)
                    ? pago.getComprobante().getNumeroCompleto() : "----------";

            // Info del lote
            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().iterator().next();
                loteInfo = "Mz. " + cl.getLote().getManzana()
                        + " Lt. " + cl.getLote().getNumeroLote()
                        + " - " + cl.getLote().getPrograma().getNombrePrograma();
            }

            // Datos del responsable del contrato
            String usuarioRegistro = "-";
            if (contrato.getUsuario() != null) {
                usuarioRegistro = contrato.getUsuario().getNombres()
                        + " " + contrato.getUsuario().getApellidos();
            }

            // Textos del cuerpo
            String concepto = "Pago de Inicial"
                    + "  -  Contrato N\u00b0 " + contrato.getIdContrato()
                    + "  -  " + loteInfo;

            String montoEnLetras = NumeroALetras.convertir(pago.getImportePagado(), monedaContrato);

            String fechaPagoStr = pago.getFechaPago() != null
                    ? pago.getFechaPago().format(FMT) : "-";

            String medioPagoStr = pago.getMedioPago() != null
                    ? pago.getMedioPago().name() : "-";

            String numOp = (pago.getNumeroOperacion() != null
                    && !pago.getNumeroOperacion().isBlank())
                    ? "   N\u00b0 Op: " + pago.getNumeroOperacion() : "";

            String observaciones = (pago.getObservaciones() != null
                    && !pago.getObservaciones().isBlank())
                    ? pago.getObservaciones() : "-";

            // ── QR ───────────────────────────────────────────────────────────
            String urlQr = BASE_URL + "/" + contrato.getIdContrato() + "/pago-inicial/comprobante-pdf";
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(52).setHeight(52)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ── Logo ─────────────────────────────────────────────────────────
            ImageData logoData = LogoCacheService.logoImageData();
            Image logoImg = (logoData != null
                    ? new Image(logoData)
                    : new Image(ImageDataFactory.create(new URL(logoUrl()))))
                    .setWidth(70).setHeight(70)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ── ENCABEZADO ──────────────────────────────────────────────────
            Table encabezado = new Table(
                    UnitValue.createPercentArray(new float[]{0.18f, 1, 0.22f}))
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

            // ── FILA: Recibi de + Caja monto ────────────────────────────────
            String clientes = construirTextoClientes(contrato);
            float fuenteCliente = clientes.length() > 120 ? 7.5f
                                : clientes.length() > 100 ? 8.5f
                                : clientes.length() > 80  ? 9f : 10f;

            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(3);
            filaRecibo.addCell(
                    construirCeldaClientes(contrato, courier, courierBold, fuenteCliente, 6f));
            filaRecibo.addCell(new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderBottom(Border.NO_BORDER)
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simbolo + " " + DF.format(pago.getImportePagado()))
                            .setFont(courierBold).setFontSize(15)
                            .setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // ── CUERPO ──────────────────────────────────────────────────────
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setPaddingTop(3).setPaddingBottom(3);

            celdaCuerpo.add(lineaDato("La cantidad de: ",  montoEnLetras,  courier, courierBold, 9.5f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Por concepto de: ", concepto,        courier, courierBold, 9.5f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Medio de pago: ",   medioPagoStr + numOp, courier, courierBold, 9.5f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Observaciones: ",   observaciones,  courier, courierBold, 9.5f));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // ── PIE ─────────────────────────────────────────────────────────
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaNombre = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaNombre.add(new Paragraph(usuarioRegistro)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(2).setMarginBottom(1));
            celdaNombre.add(new Paragraph(rolUsuario != null ? rolUsuario.toUpperCase() : "SECRETARIA")
                    .setFont(courier).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaNombre);

            String[] pf  = fechaPagoStr.split("/");
            String dia   = pf.length > 0 ? pf[0] : "--";
            String mes   = pf.length > 1 ? pf[1] : "--";
            String anio  = pf.length > 2 ? pf[2] : "----";

            Cell celdaFecha = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(8).setPaddingRight(8)
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
            tablaFecha.addCell(celdaFechaBox(dia,  courierBold));
            tablaFecha.addCell(celdaFechaBox(mes,  courierBold));
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

            // ── Línea de costura lateral ───────────────────────────────────
            PdfPage page = pdf.getFirstPage();
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.setStrokeColor(new DeviceGray(0.55f))
                  .setLineWidth(1.2f)
                  .moveTo(0, 210f)
                  .lineTo(28, 210f)
                  .stroke();
            canvas.release();

            // ── REVERSO: vouchers adjuntos (informativo, no va a SUNAT) ──
            agregarPaginaReverso(doc, pdf, vouchers, courierBold);

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante pago inicial PDF: " + e.getMessage(), e);
        }

        return eliminarPaginasEnBlanco(out.toByteArray());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

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

    private static String construirTextoClientes(Contrato contrato) {
        if (contrato.getClientes() == null || contrato.getClientes().isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (ContratoCliente cc : contrato.getClientes()) {
            com.Inmobiliaria.demo.entity.Cliente c = cc.getCliente();
            String nombre = c.getNombre() + " " + c.getApellidos();
            String doc    = (c.getNumDoc() != null && !c.getNumDoc().isBlank())
                            ? c.getNumDoc() : "--------";
            if (sb.length() > 0) sb.append(" / ");
            sb.append(nombre.toUpperCase()).append(" (DNI: ").append(doc).append(")");
        }
        return sb.toString();
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

        java.util.Collection<ContratoCliente> clientes = contrato.getClientes();
        boolean esPrimero = true;
        for (ContratoCliente cc : clientes) {
            com.Inmobiliaria.demo.entity.Cliente c = cc.getCliente();
            String nombre = c.getNombre() + " " + c.getApellidos();
            String doc    = (c.getNumDoc() != null && !c.getNumDoc().isBlank())
                            ? c.getNumDoc() : "--------";
            String linea  = nombre.toUpperCase() + " (DNI: " + doc + ")";

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
                ComprobantePagoInicialPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}
