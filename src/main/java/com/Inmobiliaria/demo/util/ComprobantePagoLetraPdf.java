package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.Moneda;
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
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ComprobantePagoLetraPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private static final DeviceGray GRIS_OSCURO = new DeviceGray(0.15f);
    private static final DeviceGray GRIS_MEDIO  = new DeviceGray(0.45f);

    private static final String EMPRESA   = "INMOBILIARIA CONSTRUCTORA \"IVAN\" E.I.R.L.";
    private static final String DIRECCION = "Av. Alfredo Mendiola N 3623  3er. Piso Of. 301 - Urb. Panamericana Norte - Los Olivos - Lima";
    private static final String TELEFONO  = "Telf.: (01) 413-8679";
    private static final String RUC       = "R.U.C.: 20537853108";
    private static final String BASE_URL  = "https://inmobiliariaivan.onrender.com/api/pagos";

    public static byte[] generar(PagoLetras pago) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            // A5 horizontal: 595 x 420 pt  (210 x 148 mm)
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            PageSize a5h = PageSize.A5.rotate();
            Document doc = new Document(pdf, a5h);

            // Margen izquierdo grande (30pt) para la perforadora
            // Márgenes: top, right, bottom, left
            doc.setMargins(35, 18, 35, 32);

            LetraCambio letra    = pago.getLetra();
            Contrato    contrato = letra.getContrato();

            String tituloPrincipal = (pago.getTipoComprobante() == TipoComprobante.BOLETA)
                    ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";

            String numComp = (pago.getTipoComprobante() != null && pago.getNumeroComprobante() != null)
                    ? pago.getNumeroComprobante() : "----------";

            String clientes = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                clientes = contrato.getClientes().stream()
                        .map(cc -> cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos())
                        .collect(Collectors.joining(" / "));
            }

            String nombreFirmante = "-";
            String docFirmante    = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                var c = contrato.getClientes().get(0).getCliente();
                nombreFirmante = c.getNombre() + " " + c.getApellidos();
                docFirmante    = c.getNumDoc() != null ? c.getNumDoc() : "-";
            }

            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().get(0);
                loteInfo = "Mz. " + cl.getLote().getManzana()
                        + " Lt. " + cl.getLote().getNumeroLote()
                        + " - " + cl.getLote().getPrograma().getNombrePrograma();
            }

            String usuarioRegistro = "-";
            if (contrato.getUsuario() != null) {
                usuarioRegistro = contrato.getUsuario().getNombres()
                        + " " + contrato.getUsuario().getApellidos();
            }

            String importeTexto = letra.getImporteLetras() != null ? letra.getImporteLetras() : "-";
            String fechaPagoStr = pago.getFechaPago() != null ? pago.getFechaPago().format(FMT) : "-";
            String fechaVencStr = letra.getFechaVencimiento() != null ? letra.getFechaVencimiento().format(FMT) : "-";
            String medioPago    = pago.getMedioPago() != null ? pago.getMedioPago().name() : "-";
            String numOp        = (pago.getNumeroOperacion() != null && !pago.getNumeroOperacion().isBlank())
                                  ? "   N Op: " + pago.getNumeroOperacion() : "";
            // Símbolo de moneda según el contrato
            Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
            String simboloMoneda  = (monedaContrato == Moneda.PEN) ? "S/" : "$";

            String numLetraFormateado = letra.getNumeroLetra() != null
                    ? letra.getNumeroLetra().replace("/", " de ") : "-";
            String concepto     = "Pago de la Letra N " + numLetraFormateado
                    + "  -  Contrato N " + contrato.getIdContrato() + "  -  " + loteInfo;

            // QR
            String urlQr = BASE_URL + "/" + pago.getIdPago() + "/comprobante-pdf";
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(52).setHeight(52)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // Logo desde Cloudinary optimizado: 80x80px, calidad auto, formato auto
            String logoUrl = "https://res.cloudinary.com/dlgqaifrk/image/upload/e_grayscale,w_200,h_200,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png";
            Image logoImg = new Image(com.itextpdf.io.image.ImageDataFactory.create(new java.net.URL(logoUrl)))
                    .setWidth(70).setHeight(70)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // 1. ENCABEZADO — tres columnas: logo | datos empresa+título | QR
            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{0.18f, 1, 0.22f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Columna logo
            Cell celdaLogo = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(8)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER);
            celdaLogo.add(logoImg);
            encabezado.addCell(celdaLogo);

            // Columna central: empresa + título
            Cell celdaEmpresa = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setBorderRight(com.itextpdf.layout.borders.Border.NO_BORDER)
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
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            celdaEmpresa.add(new Paragraph("N  " + numComp)
                    .setFont(courierBold).setFontSize(10)
                    .setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(0));
            encabezado.addCell(celdaEmpresa);

            // Columna derecha: QR
            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea\ntu recibo")
                    .setFont(arial).setFontSize(7f)
                    .setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2));
            encabezado.addCell(celdaQr);

            doc.add(encabezado);

            // 2. FILA: Recibi de + Caja monto
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);
            // Fuente dinámica: si el nombre es largo, reduce el tamaño
            float fuenteCliente = clientes.length() > 80 ? 7f : clientes.length() > 60 ? 8f : 9f;
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPadding(8)
                    .add(lineaDato("Recibi de:  ", clientes, courier, courierBold, fuenteCliente)));
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f))
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simboloMoneda + " " + DF.format(pago.getImportePagado()))
                            .setFont(courierBold).setFontSize(15)
                            .setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // 3. CUERPO
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setPaddingTop(7).setPaddingBottom(7);
            celdaCuerpo.add(lineaDato("La cantidad de:     ", importeTexto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Por concepto de:    ", concepto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Medio de pago:      ", medioPago + numOp, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Fecha venc. letra:  ", fechaVencStr, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // 4. PIE: Nombre/DNI + Fecha + RECIBE CONFORME
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1.4f, 0.6f, 0.7f}))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell celdaNombre = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8);
            float fuenteNombre = nombreFirmante.length() > 50 ? 7f : nombreFirmante.length() > 38 ? 8f : 9f;
            celdaNombre.add(new Paragraph("Nombre:")
                    .setFont(courierBold).setFontSize(9f).setMarginBottom(3));
            celdaNombre.add(new Paragraph(nombreFirmante)
                    .setFont(courier).setFontSize(fuenteNombre).setMarginBottom(5));
            celdaNombre.add(new Paragraph("D.N.I.:   " + docFirmante)
                    .setFont(courier).setFontSize(9f));
            pie.addCell(celdaNombre);

            String[] pf = fechaPagoStr.split("/");
            String dia  = pf.length > 0 ? pf[0] : "--";
            String mes  = pf.length > 1 ? pf[1] : "--";
            String anio = pf.length > 2 ? pf[2] : "----";

            Cell celdaFecha = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaFecha.add(new Paragraph("DIA    MES    ANNO")
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
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(10));
            celdaFirma.add(new Paragraph(usuarioRegistro)
                    .setFont(courierBold).setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaFirma.add(new Paragraph("SECRETARIA")
                    .setFont(courier).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaFirma);

            doc.add(pie);
            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }


    /**
     * Genera un comprobante consolidado para pagos múltiples con el mismo número de comprobante.
     * Suma los importes, muestra todas las letras en el concepto.
     */
    public static byte[] generarMultiple(List<PagoLetras> pagos) {
        if (pagos == null || pagos.isEmpty()) throw new RuntimeException("Lista de pagos vacía");
        if (pagos.size() == 1) return generar(pagos.get(0));

        // Usamos el primer pago como referencia para datos del contrato
        PagoLetras primero   = pagos.get(0);
        LetraCambio letraRef = primero.getLetra();
        Contrato    contrato = letraRef.getContrato();

        // Sumar importes
        java.math.BigDecimal totalImporte = pagos.stream()
                .map(PagoLetras::getImportePagado)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Armar números de letra: "1, 2 y 3 de 130"
        List<String> numerosLetra = pagos.stream()
                .map(p -> p.getLetra().getNumeroLetra() != null
                        ? p.getLetra().getNumeroLetra().split("/")[0].trim()
                        : "?")
                .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)))
                .collect(Collectors.toList());

        String totalLetras = letraRef.getNumeroLetra() != null
                ? letraRef.getNumeroLetra().split("/")[1].trim() : "?";

        String letrasStr;
        if (numerosLetra.size() == 1) {
            letrasStr = "N° " + numerosLetra.get(0);
        } else {
            String ultimas = numerosLetra.get(numerosLetra.size() - 1);
            String anteriores = String.join(", ", numerosLetra.subList(0, numerosLetra.size() - 1));
            letrasStr = "N° " + anteriores + " y " + ultimas;
        }

        // Texto en letras del total
        Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
        String importeTexto   = NumeroALetras.convertir(totalImporte, monedaContrato);
        String simboloMoneda  = (monedaContrato == Moneda.PEN) ? "S/" : "$";

        // Lote y concepto
        String loteInfo = "-";
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            ContratoLote cl = contrato.getLotes().get(0);
            loteInfo = "Mz. " + cl.getLote().getManzana()
                    + " Lt. " + cl.getLote().getNumeroLote()
                    + " - " + cl.getLote().getPrograma().getNombrePrograma();
        }
        String concepto = "Pago de las Letras " + letrasStr + " de " + totalLetras
                + " Letras  -  Contrato N " + contrato.getIdContrato() + "  -  " + loteInfo;

        // Resto de datos del primer pago
        String clientes = "-";
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            clientes = contrato.getClientes().stream()
                    .map(cc -> cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos())
                    .collect(Collectors.joining(" / "));
        }
        String nombreFirmante = "-", docFirmante = "-";
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            var c = contrato.getClientes().get(0).getCliente();
            nombreFirmante = c.getNombre() + " " + c.getApellidos();
            docFirmante    = c.getNumDoc() != null ? c.getNumDoc() : "-";
        }
        String usuarioRegistro = "-";
        if (contrato.getUsuario() != null) {
            usuarioRegistro = contrato.getUsuario().getNombres() + " " + contrato.getUsuario().getApellidos();
        }

        String tituloPrincipal = (primero.getTipoComprobante() == TipoComprobante.BOLETA)
                ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";
        String numComp = (primero.getTipoComprobante() != null && primero.getNumeroComprobante() != null)
                ? primero.getNumeroComprobante() : "----------";
        String fechaPagoStr = primero.getFechaPago() != null ? primero.getFechaPago().format(FMT) : "-";
        String medioPago    = primero.getMedioPago() != null ? primero.getMedioPago().name() : "-";
        String numOp        = (primero.getNumeroOperacion() != null && !primero.getNumeroOperacion().isBlank())
                              ? "   N Op: " + primero.getNumeroOperacion() : "";
        String fechaVencStr = letraRef.getFechaVencimiento() != null
                ? letraRef.getFechaVencimiento().format(FMT) : "-";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc   = new Document(pdf, PageSize.A5.rotate());
            doc.setMargins(35, 18, 35, 32);

            // QR apunta al primer pago
            String urlQr = BASE_URL + "/" + primero.getIdPago() + "/comprobante-pdf";
            BarcodeQRCode qrCode = new BarcodeQRCode(urlQr);
            Image qrImage = new Image(qrCode.createFormXObject(pdf))
                    .setWidth(52).setHeight(52)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            String logoUrl = "https://res.cloudinary.com/dlgqaifrk/image/upload/e_grayscale,w_200,h_200,c_fit,f_auto,q_auto/v1773725974/logogrande_rfvxhu.png";
            Image logoImg  = new Image(com.itextpdf.io.image.ImageDataFactory.create(new java.net.URL(logoUrl)))
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
            celdaEmpresa.add(new Paragraph(EMPRESA).setFont(courierBold).setFontSize(11f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(DIRECCION).setFont(courier).setFontSize(8f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            celdaEmpresa.add(new Paragraph(TELEFONO + "          " + RUC).setFont(courier).setFontSize(8f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
            celdaEmpresa.add(new Paragraph(tituloPrincipal).setFont(courierBold).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            celdaEmpresa.add(new Paragraph("N  " + numComp).setFont(courierBold).setFontSize(10).setFontColor(GRIS_OSCURO).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(0));
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER).setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea\ntu recibo").setFont(arial).setFontSize(7f).setFontColor(GRIS_MEDIO).setTextAlignment(TextAlignment.CENTER).setMarginTop(2));
            encabezado.addCell(celdaQr);
            doc.add(encabezado);

            // ── FILA RECIBI DE + MONTO TOTAL ──
            float fuenteCliente = clientes.length() > 80 ? 7f : clientes.length() > 60 ? 8f : 9f;
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);
            filaRecibo.addCell(new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .add(lineaDato("Recibi de:  ", clientes, courier, courierBold, fuenteCliente)));
            filaRecibo.addCell(new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f)).setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simboloMoneda + " " + DF.format(totalImporte))
                            .setFont(courierBold).setFontSize(15).setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // ── CUERPO ──
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8).setPaddingTop(7).setPaddingBottom(7);
            celdaCuerpo.add(lineaDato("La cantidad de:     ", importeTexto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Por concepto de:    ", concepto, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Medio de pago:      ", medioPago + numOp, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            celdaCuerpo.add(lineaDato("Fecha venc. última: ", fechaVencStr, courier, courierBold, 10f));
            celdaCuerpo.add(separadorLinea());
            cuerpo.addCell(celdaCuerpo);
            doc.add(cuerpo);

            // ── PIE ──
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1.4f, 0.6f, 0.7f}))
                    .setWidth(UnitValue.createPercentValue(100));
            float fuenteNombre = nombreFirmante.length() > 50 ? 7f : nombreFirmante.length() > 38 ? 8f : 9f;
            Cell celdaNombre = new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8);
            celdaNombre.add(new Paragraph("Nombre:").setFont(courierBold).setFontSize(9f).setMarginBottom(3));
            celdaNombre.add(new Paragraph(nombreFirmante).setFont(courier).setFontSize(fuenteNombre).setMarginBottom(5));
            celdaNombre.add(new Paragraph("D.N.I.:   " + docFirmante).setFont(courier).setFontSize(9f));
            pie.addCell(celdaNombre);

            String[] pf = fechaPagoStr.split("/");
            String dia = pf.length > 0 ? pf[0] : "--";
            String mes = pf.length > 1 ? pf[1] : "--";
            String anio = pf.length > 2 ? pf[2] : "----";
            Cell celdaFecha = new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            celdaFecha.add(new Paragraph("DIA    MES    ANNO").setFont(courierBold).setFontSize(9f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            Table tablaFecha = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).setWidth(UnitValue.createPercentValue(100));
            tablaFecha.addCell(celdaFechaBox(dia, courierBold));
            tablaFecha.addCell(celdaFechaBox(mes, courierBold));
            tablaFecha.addCell(celdaFechaBox(anio, courierBold));
            celdaFecha.add(tablaFecha);
            pie.addCell(celdaFecha);

            Cell celdaFirma = new Cell().setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(10));
            celdaFirma.add(new Paragraph(usuarioRegistro).setFont(courierBold).setFontSize(9f).setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            celdaFirma.add(new Paragraph("SECRETARIA").setFont(courier).setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            pie.addCell(celdaFirma);
            doc.add(pie);
            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante múltiple: " + e.getMessage(), e);
        }
        return out.toByteArray();
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
                ComprobantePagoLetraPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}