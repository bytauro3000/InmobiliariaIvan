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

    // Construye una Cell con "Recibi de:" y cada cliente alineado correctamente.
    // Devuelve Cell en lugar de Paragraph para poder usar múltiples Paragraph dentro.
    private static Cell construirCeldaClientes(Contrato contrato,
                                               PdfFont normal, PdfFont bold,
                                               float size, float padding) {
        Cell celda = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setPadding(padding);

        if (contrato.getClientes() == null || contrato.getClientes().isEmpty()) {
            celda.add(lineaDato("Recibi de:", "-", normal, bold, size));
            return celda;
        }

        java.util.List<com.Inmobiliaria.demo.entity.ContratoCliente> clientes = contrato.getClientes();
        for (int i = 0; i < clientes.size(); i++) {
            com.Inmobiliaria.demo.entity.Cliente c = clientes.get(i).getCliente();
            String nombre = c.getNombre() + " " + c.getApellidos();
            String dni    = (c.getNumDoc() != null && !c.getNumDoc().isBlank())
                            ? c.getNumDoc() : "--------";
            String linea  = nombre.toUpperCase() + " (DNI: " + dni + ")";

            Paragraph p;
            if (i == 0) {
                // Primera línea: "Recibi de:" en bold + nombre en normal
                p = new Paragraph()
                        .add(new Text("Recibi de:").setFont(bold).setFontSize(size))
                        .add(new Text(linea).setFont(normal).setFontSize(size))
                        .setMarginBottom(0);
            } else {
                // Líneas siguientes: indent equivalente al ancho de "Recibi de:"
                // Usamos setMarginLeft con el ancho calculado en pts para Courier bold
                // Courier bold: ancho de carácter ≈ size * 0.6; "Recibi de:" = 10 chars
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
    // COMPROBANTE INDIVIDUAL
    // ─────────────────────────────────────────────────────────────────────────
    public static byte[] generar(PagoLetras pago, String rolUsuario) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            PageSize a5h = PageSize.A5.rotate();
            Document doc = new Document(pdf, a5h);
            doc.setMargins(35, 18, 35, 32);

            LetraCambio letra    = pago.getLetra();
            Contrato    contrato = letra.getContrato();

            String tituloPrincipal = (pago.getTipoComprobante() == TipoComprobante.BOLETA)
                    ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";

            String numComp = (pago.getTipoComprobante() != null && pago.getNumeroComprobante() != null)
                    ? pago.getNumeroComprobante() : "----------";

            String clientes = construirTextoClientes(contrato);

            String usuarioRegistro = "-";
            if (contrato.getUsuario() != null) {
                usuarioRegistro = contrato.getUsuario().getNombres()
                        + " " + contrato.getUsuario().getApellidos();
            }

            String loteInfo = "-";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                ContratoLote cl = contrato.getLotes().get(0);
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
            Image logoImg = new Image(com.itextpdf.io.image.ImageDataFactory.create(new java.net.URL(logoUrl)))
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
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.TOP);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea tu\ncomprobante")
                    .setFont(arial).setFontSize(7f)
                    .setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2));
            celdaQr.add(new Paragraph("N\u00b0 " + numComp)
                    .setFont(courierBold).setFontSize(9f)
                    .setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(4));
            encabezado.addCell(celdaQr);
            doc.add(encabezado);

            // ── FILA: Recibi de + Caja monto ──
            float fuenteCliente = clientes.length() > 100 ? 6.5f
                                : clientes.length() > 80  ? 7.5f
                                : clientes.length() > 60  ? 8.5f : 9f;
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(5);
            filaRecibo.addCell(construirCeldaClientes(contrato, courier, courierBold, fuenteCliente, 8f));
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f))
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
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                    .setPaddingLeft(8).setPaddingRight(8)
                    .setPaddingTop(7).setPaddingBottom(7);
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

            // ── PIE: Secretaria | Fecha de Pago (centro) | Gerente General ──
            // 1f | 0.8f | 1f  →  ambos lados exactamente del mismo ancho
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Celda izquierda: nombre y rol del usuario que registra
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

            // Celda central: Fecha de Pago
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

            // Celda derecha: Gerente General
            Cell celdaFirma = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(18));
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
            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // COMPROBANTE MÚLTIPLE
    // ─────────────────────────────────────────────────────────────────────────
    public static byte[] generarMultiple(List<PagoLetras> pagos, String rolUsuario) {
        if (pagos == null || pagos.isEmpty()) throw new RuntimeException("Lista de pagos vacía");
        if (pagos.size() == 1) return generar(pagos.get(0), rolUsuario);

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
            ContratoLote cl = contrato.getLotes().get(0);
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

        String tituloPrincipal = (primero.getTipoComprobante() == TipoComprobante.BOLETA)
                ? "BOLETA DE VENTA" : "RECIBO DE INGRESO";
        String numComp = (primero.getTipoComprobante() != null && primero.getNumeroComprobante() != null)
                ? primero.getNumeroComprobante() : "----------";
        String fechaPagoStr = primero.getFechaPago() != null ? primero.getFechaPago().format(FMT) : "-";
        String medioPago    = primero.getMedioPago() != null ? primero.getMedioPago().name() : "-";
        String numOp        = (primero.getNumeroOperacion() != null && !primero.getNumeroOperacion().isBlank())
                              ? "   N\u00b0 Op: " + primero.getNumeroOperacion() : "";
        // Para pago múltiple: fechas de vencimiento ordenadas por número de letra
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

        // Escala dinámica: reduce márgenes y fuentes si hay muchas letras (evita segunda página)
        int numLetras = pagos.size();
        float padCuerpo    = numLetras >= 8 ? 4f   : 7f;
        float fsCuerpo     = numLetras >= 8 ? 9f   : 10f;
        float marginTopRec = numLetras >= 8 ? 3f   : 5f;
        float padRecibo    = numLetras >= 8 ? 5f   : 8f;

        // Centrado vertical: calculamos cuantas lineas extra ocupa "Fechas de vencimiento"
        // para estimar el espacio libre y distribuirlo simetricamente arriba/abajo.
        // A5 apaisado: 595 x 420 pts. Margenes lat.: izq=32, der=18 -> ancho util ~545 pts.
        // "Fechas de vencimiento: " = 23 chars -> quedan ~77 chars para fechas en linea 1.
        // Lineas de continuacion tienen ~84 chars disponibles.
        int charsDisponiblesL1 = 77;
        int charsPerLineCont   = 84;
        int charsFechas = fechasVencStr.length();
        int lineasExtra = 0;
        if (charsFechas > charsDisponiblesL1) {
            lineasExtra = (int)Math.ceil((float)(charsFechas - charsDisponiblesL1) / charsPerLineCont);
        }
        // Altura base del contenido sin margenes (pts empiricos):
        // encabezado=100, marginTopRec, filaRecibo=28, cuerpo base (4 filas), pie=70
        float lineHeightExtra = fsCuerpo * 1.55f;
        float cuerpoBase  = numLetras >= 8 ? 118f : 134f;
        float contenidoH  = 100f + marginTopRec + 28f + cuerpoBase + (lineasExtra * lineHeightExtra) + 70f;
        float pageH       = 420f; // A5 rotado
        float libre       = pageH - contenidoH;
        // Clamp entre 10 y 30 para nunca generar segunda pagina
        float margenSim   = Math.min(40f, Math.max(10f, libre / 2f));
        float margenTop   = margenSim;
        float margenV     = margenSim;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");
            PdfFont arial       = cargarFuente("fonts/ARIAL.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc   = new Document(pdf, PageSize.A5.rotate());
            doc.setMargins(margenTop, 18, margenV, 32);

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
            encabezado.addCell(celdaEmpresa);

            Cell celdaQr = new Cell()
                    .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1f))
                    .setBorderLeft(Border.NO_BORDER).setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.TOP);
            celdaQr.add(qrImage);
            celdaQr.add(new Paragraph("Escanea tu\ncomprobante")
                    .setFont(arial).setFontSize(7f).setFontColor(GRIS_MEDIO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(2));
            celdaQr.add(new Paragraph("N\u00b0 " + numComp)
                    .setFont(courierBold).setFontSize(9f).setFontColor(GRIS_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
            encabezado.addCell(celdaQr);
            doc.add(encabezado);

            // ── FILA: Recibi de + Monto total ──
            float fuenteCliente = clientes.length() > 100 ? 6.5f
                                : clientes.length() > 80  ? 7.5f
                                : clientes.length() > 60  ? 8.5f : 9f;
            Table filaRecibo = new Table(UnitValue.createPercentArray(new float[]{1, 0.28f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(marginTopRec);
            filaRecibo.addCell(construirCeldaClientes(contrato, courier, courierBold, fuenteCliente, padRecibo));
            filaRecibo.addCell(new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f)).setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .add(new Paragraph(simboloMoneda + " " + DF.format(totalImporte))
                            .setFont(courierBold).setFontSize(15).setTextAlignment(TextAlignment.CENTER)));
            doc.add(filaRecibo);

            // ── CUERPO ──
            Table cuerpo = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .setWidth(UnitValue.createPercentValue(100));
            Cell celdaCuerpo = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
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

            // ── PIE: Secretaria | Fecha de Pago (centro) | Gerente General ──
            // 1f | 0.8f | 1f  →  ambos lados exactamente del mismo ancho
            Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 0.8f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100));

         // Celda izquierda: nombre y rol del usuario que registra
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

            // Celda central: Fecha de Pago
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

            // Celda derecha: Gerente General
            Cell celdaFirma = new Cell()
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f)).setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM);
            celdaFirma.add(new Paragraph(" ").setFont(courier).setFontSize(18));
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
            doc.close();
          
        } catch (Exception e) {
            throw new RuntimeException("Error generando comprobante m\u00faltiple: " + e.getMessage(), e);
        }
        return out.toByteArray();
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