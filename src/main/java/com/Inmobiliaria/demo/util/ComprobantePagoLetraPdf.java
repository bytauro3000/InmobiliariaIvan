package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
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
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Genera el comprobante de pago de letra en formato A5 HORIZONTAL (landscape).
 * - 1 sola copia por hoja
 * - Titulo dinamico: "BOLETA DE VENTA" o "RECIBO DE INGRESO" segun tipoComprobante
 * - QR Code que enlaza al PDF del comprobante online
 * - Disenio en blanco y negro, tipografia Courier
 */
public class ComprobantePagoLetraPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00");

    private static final DeviceGray GRIS_OSCURO = new DeviceGray(0.15f);
    private static final DeviceGray GRIS_MEDIO  = new DeviceGray(0.45f);

    private static final String EMPRESA   = "INMOBILIARIA CONSTRUCTORA \"IVAN\" E.I.R.L.";
    private static final String DIRECCION = "Av. Alfredo Mendiola N 3623  3er. Piso Of. 301 - Urb. Panamericana Norte - Los Olivos - Lima";
    private static final String TELEFONO  = "Telf.: (01) 413-8679";
    private static final String RUC       = "R.U.C.: 20537853108";

    // URL base del backend en produccion (Render)
    // El QR apunta a este endpoint para que el cliente descargue su comprobante
    private static final String BASE_URL  = "https://inmobiliariaivan.onrender.com/api/pagos-letras";

    public static byte[] generar(PagoLetras pago) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            // A5 HORIZONTAL: 210 x 148 mm
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf, PageSize.A5.rotate());
            doc.setMargins(15, 18, 15, 18);

            LetraCambio letra    = pago.getLetra();
            Contrato    contrato = letra.getContrato();

            // ── Titulo dinamico segun tipo de comprobante ─────────────
            String tituloPrincipal = (pago.getTipoComprobante() == TipoComprobante.BOLETA)
                    ? "BOLETA DE VENTA"
                    : "RECIBO DE INGRESO";

            String numComp = (pago.getTipoComprobante() != null && pago.getNumeroComprobante() != null)
                    ? pago.getNumeroComprobante()
                    : "----------";

            // ── Datos del cliente ─────────────────────────────────────
            String clientes = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                clientes = contrato.getClientes().stream()
                        .map(cc -> cc.getCliente().getNombre()
                                + " " + cc.getCliente().getApellidos())
                        .collect(Collectors.joining(" / "));
            }

            String nombreFirmante = "-";
            String docFirmante    = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                var primerCliente = contrato.getClientes().get(0).getCliente();
                nombreFirmante = primerCliente.getNombre() + " " + primerCliente.getApellidos();
                // Campo correcto segun la entidad Cliente: getNumDoc()
                docFirmante = primerCliente.getNumDoc() != null ? primerCliente.getNumDoc() : "-";
            }

            // ── Datos del lote y contrato ─────────────────────────────
            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().get(0);
                loteInfo = "Mz. " + cl.getLote().getManzana()
                        + " Lt. " + cl.getLote().getNumeroLote()
                        + " - " + cl.getLote().getPrograma().getNombrePrograma();
            }

            String importeTexto = letra.getImporteLetras() != null ? letra.getImporteLetras() : "-";
            String fechaPagoStr = pago.getFechaPago() != null ? pago.getFechaPago().format(FMT) : "-";
            String fechaVencStr = letra.getFechaVencimiento() != null
                    ? letra.getFechaVencimiento().format(FMT) : "-";
            String medioPago    = pago.getMedioPago() != null ? pago.getMedioPago().name() : "-";
            String numOp        = (pago.getNumeroOperacion() != null && !pago.getNumeroOperacion().isBlank())
                                  ? "   N Op: " + pago.getNumeroOperacion() : "";

            // Concepto: unico campo descriptivo (se elimino Observaciones para no duplicar)
            String concepto = "Pago de la Letra N " + letra.getNumeroLetra()
                    + "  -  Contrato N " + contrato.getIdContrato()
                    + "  -  " + loteInfo;

            // ── QR: URL de descarga del comprobante ───────────────────
            String urlQr = BASE_URL + "/" + pago.getIdPago() + "/comprobante-pdf";
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(62).setHeight(62)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // ══════════════════════════════════════════════════════════
            //  LAYOUT PRINCIPAL: contenido izquierdo + QR derecho
            // ══════════════════════════════════════════════════════════
            Table layoutPrincipal = new Table(UnitValue.createPercentArray(new float[]{1, 0.22f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // ─── COLUMNA IZQUIERDA: todo el comprobante ───────────────
            Cell colIzq = new Cell().setBorder(Border.NO_BORDER).setPadding(0);

            // 1. ENCABEZADO
            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaEnc = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setPadding(5).setTextAlignment(TextAlignment.CENTER);
            celdaEnc.add(new Paragraph(EMPRESA)
                    .setFont(courierBold).setFontSize(9.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEnc.add(new Paragraph(DIRECCION)
                    .setFont(courier).setFontSize(6f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaEnc.add(new Paragraph(TELEFONO + "          " + RUC)
                    .setFont(courier).setFontSize(6.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            celdaEnc.add(new Paragraph(tituloPrincipal)
                    .setFont(courierBold).setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEnc.add(new Paragraph("N  " + numComp)
                    .setFont(courierBold).setFontSize(9)
                    .setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(0));
            encabezado.addCell(celdaEnc);
            colIzq.add(encabezado);

            // 2. FILA: Recibi de + Caja monto
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.3f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(3);
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(5)
                    .add(lineaDato("Recibi de:  ", clientes, courier, courierBold, 8f)));
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph("$ " + DF.format(pago.getImportePagado()))
                            .setFont(courierBold).setFontSize(12)
                            .setTextAlignment(TextAlignment.CENTER)));
            colIzq.add(filaRecibo);

            // 3. CUERPO
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setPaddingTop(4).setPaddingBottom(4);

            celdaCuerpo.add(lineaDato("La cantidad de:     ", importeTexto, courier, courierBold, 8f));
            celdaCuerpo.add(separadorPunteado(courier));

            celdaCuerpo.add(lineaDato("Por concepto de:    ", concepto, courier, courierBold, 8f));
            celdaCuerpo.add(separadorPunteado(courier));

            celdaCuerpo.add(lineaDato("Medio de pago:      ", medioPago + numOp, courier, courierBold, 8f));
            celdaCuerpo.add(separadorPunteado(courier));

            // Fecha de vencimiento de la letra (reemplaza a Observaciones)
            celdaCuerpo.add(lineaDato("Fecha venc. letra:  ", fechaVencStr, courier, courierBold, 8f));
            celdaCuerpo.add(separadorPunteado(courier));

            cuerpo.addCell(celdaCuerpo);
            colIzq.add(cuerpo);

            // 4. PIE: Nombre/DNI + Fecha + Firma
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1.3f, 0.65f, 0.75f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaNombre = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(4);
            celdaNombre.add(new Paragraph("Nombre:")
                    .setFont(courierBold).setFontSize(7).setMarginBottom(2));
            celdaNombre.add(new Paragraph(nombreFirmante)
                    .setFont(courier).setFontSize(7.5f).setMarginBottom(3));
            celdaNombre.add(new Paragraph("D.N.I.:   " + docFirmante)
                    .setFont(courier).setFontSize(7.5f));
            pie.addCell(celdaNombre);

            String[] pf = fechaPagoStr.split("/");
            String dia  = pf.length > 0 ? pf[0] : "--";
            String mes  = pf.length > 1 ? pf[1] : "--";
            String anio = pf.length > 2 ? pf[2] : "----";

            Cell celdaFecha = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaFecha.add(new Paragraph("DIA    MES    ANNO")
                    .setFont(courierBold).setFontSize(6.5f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            Table tablaFecha = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));
            tablaFecha.addCell(celdaFechaBox(dia, courierBold));
            tablaFecha.addCell(celdaFechaBox(mes, courierBold));
            tablaFecha.addCell(celdaFechaBox(anio, courierBold));
            celdaFecha.add(tablaFecha);
            pie.addCell(celdaFecha);

            Cell celdaFirma = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(14));
            celdaFirma.add(new Paragraph("_____________________")
                    .setFont(courier).setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER));
            celdaFirma.add(new Paragraph("RECIBE CONFORME")
                    .setFont(courierBold).setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaFirma);
            colIzq.add(pie);

            layoutPrincipal.addCell(colIzq);

            // ─── COLUMNA DERECHA: QR ──────────────────────────────────
            Cell colQr = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPaddingLeft(8)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);
            colQr.add(qrImage);
            colQr.add(new Paragraph("Escanea para\nver tu recibo")
                    .setFont(arial).setFontSize(6f)
                    .setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(3));
            layoutPrincipal.addCell(colQr);

            doc.add(layoutPrincipal);
            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private static Paragraph lineaDato(String label, String valor,
                                        PdfFont normal, PdfFont bold, float size) {
        return new Paragraph()
                .add(new Text(label).setFont(bold).setFontSize(size))
                .add(new Text(valor).setFont(normal).setFontSize(size))
                .setMarginBottom(1);
    }

    private static Paragraph separadorPunteado(PdfFont courier) {
        return new Paragraph("............................................................................"
                + "............................................................................")
                .setFont(courier).setFontSize(5)
                .setFontColor(GRIS_MEDIO)
                .setMarginTop(0).setMarginBottom(2);
    }

    private static Cell celdaFechaBox(String valor, PdfFont bold) {
        return new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(3).setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(valor).setFont(bold).setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ComprobantePagoLetraPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}