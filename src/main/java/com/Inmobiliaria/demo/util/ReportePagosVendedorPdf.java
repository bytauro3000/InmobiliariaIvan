package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ReportePagosVendedorDTO;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** PDF del reporte de pagos ya efectuados a vendedores (adelantos y cuotas). */
public class ReportePagosVendedorPdf {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    // ── Colores ──────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_AZUL_OSCURO = new DeviceRgb(30, 64, 110);
    private static final DeviceRgb COLOR_GRIS_HEADER = new DeviceRgb(60, 60, 60);
    private static final DeviceRgb COLOR_FILA_ALTERNA = new DeviceRgb(245, 248, 252);

    // ── Datos de empresa ─────────────────────────────────────────────
    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
    private static String ruc() { return "R.U.C.: " + EmpresaContext.empresaService.obtenerActiva().getRuc(); }
    private static String direccion() { return EmpresaPdfUtil.direccionCompleta(); }
    private static String telefono() { return "Cel.: " + EmpresaContext.empresaService.obtenerActiva().getCelular(); }
    private static String logoUrl() { return EmpresaContext.empresaService.obtenerActiva().getLogoSmallUrl(); }

    public static byte[] generar(ReportePagosVendedorDTO dto) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(20, 20, 20, 20);

            // ── ENCABEZADO ─────────────────────────────────────────────
            doc.add(encabezado(courierBold, courier));

            // ── TITULO ─────────────────────────────────────────────────
            doc.add(new Paragraph("REPORTE DE PAGOS A VENDEDORES")
                    .setFont(courierBold)
                    .setFontSize(13)
                    .setFontColor(COLOR_AZUL_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6)
                    .setMarginBottom(4));

            // ── FILTROS Y RESUMEN ──────────────────────────────────────
            doc.add(resumen(dto, courier, courierBold));

            // ── TABLA DE PAGOS ─────────────────────────────────────────
            List<ReportePagosVendedorDTO.FilaPago> pagos = dto.getPagos();
            if (pagos != null && !pagos.isEmpty()) {
                doc.add(tablaPagos(pagos, dto.isTodosVendedores(), courier, courierBold));
                doc.add(totales(dto, courierBold));
            } else {
                doc.add(new Paragraph("No hay pagos registrados para los filtros seleccionados.")
                        .setFont(courier).setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(20));
            }

            // ── PIE ────────────────────────────────────────────────────
            doc.add(new Paragraph("Generado por el Sistema de Gestion Inmobiliaria  |  " + empresa())
                    .setFont(courier).setFontSize(7)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10));

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de pagos a vendedores: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ── ENCABEZADO ───────────────────────────────────────────────────

    private static Table encabezado(PdfFont bold, PdfFont normal) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.3f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);

        Cell izq = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(5);
        izq.add(new Paragraph(empresa())
                .setFont(bold).setFontSize(9)
                .setFontColor(COLOR_AZUL_OSCURO)
                .setMarginBottom(1));
        izq.add(new Paragraph(direccion())
                .setFont(normal).setFontSize(7)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(1));
        izq.add(new Paragraph(telefono() + "     " + ruc())
                .setFont(normal).setFontSize(7)
                .setFontColor(ColorConstants.DARK_GRAY));
        t.addCell(izq);

        Cell der = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER);
        try {
            String url = logoUrl();
            if (url != null && !url.isBlank()) {
                Image logo = new Image(ImageDataFactory.create(url));
                logo.scaleToFit(50, 50);
                logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
                der.add(logo);
            } else {
                der.add(new Paragraph("[LOGO]").setFont(normal).setFontSize(8));
            }
        } catch (Exception e) {
            der.add(new Paragraph("[LOGO]").setFont(normal).setFontSize(8));
        }
        t.addCell(der);

        return t;
    }

    // ── FILTROS Y RESUMEN ────────────────────────────────────────────

    private static Table resumen(ReportePagosVendedorDTO dto, PdfFont normal, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6)
                .setMarginBottom(4);

        String vendedor = dto.isTodosVendedores()
                ? "TODOS LOS VENDEDORES"
                : (nullToVacio(dto.getNombreVendedor()) + " " + nullToVacio(dto.getApellidosVendedor())).trim();

        t.addCell(celdaInfo("VENDEDOR: " + vendedor, bold));
        t.addCell(celdaInfo("DNI: " + nullToVacio(dto.getDniVendedor()), bold));
        t.addCell(celdaInfo("TIPO DE PAGO: " + etiquetaTipo(dto.getFiltroTipo()), bold));
        t.addCell(celdaInfo("EMISION: " + (dto.getFechaEmision() != null
                ? dto.getFechaEmision().format(FMT_HORA) : "—"), bold));
        t.addCell(celdaInfo("TOTAL PAGOS: " + dto.getTotalPagos(), bold));

        t.addCell(celdaInfo("ADELANTOS: " + simbolo(dto.getMoneda()) + " " + DF.format(zeroIfNull(dto.getTotalAdelantos())), bold));
        t.addCell(celdaInfo("CUOTAS (mensual): " + simbolo(dto.getMoneda()) + " " + DF.format(zeroIfNull(dto.getTotalMensuales())), bold));
        t.addCell(celdaInfo("TOTAL GENERAL: " + simbolo(dto.getMoneda()) + " " + DF.format(zeroIfNull(dto.getTotalGeneral())), bold));
        t.addCell(new Cell(1, 2)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4));

        return t;
    }

    // ── TABLA DE PAGOS ───────────────────────────────────────────────

    private static Table tablaPagos(List<ReportePagosVendedorDTO.FilaPago> pagos,
                                    boolean mostrarVendedor,
                                    PdfFont normal, PdfFont bold) {
        Table t;
        List<String> headers = new ArrayList<>();

        if (mostrarVendedor) {
            t = new Table(UnitValue.createPercentArray(
                    new float[]{0.04f, 0.09f, 0.15f, 0.08f, 0.06f, 0.06f, 0.06f, 0.18f, 0.14f, 0.14f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(2);
            headers.add("N°"); headers.add("FECHA"); headers.add("VENDEDOR"); headers.add("TIPO");
            headers.add("LETRA"); headers.add("MZ"); headers.add("LT");
            headers.add("PROGRAMA"); headers.add("MONTO"); headers.add("EGRESO");
        } else {
            t = new Table(UnitValue.createPercentArray(
                    new float[]{0.04f, 0.10f, 0.09f, 0.07f, 0.07f, 0.07f, 0.24f, 0.16f, 0.16f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(2);
            headers.add("N°"); headers.add("FECHA"); headers.add("TIPO");
            headers.add("LETRA"); headers.add("MZ"); headers.add("LT");
            headers.add("PROGRAMA"); headers.add("MONTO"); headers.add("EGRESO");
        }

        for (String h : headers) {
            t.addCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(7f).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_GRIS_HEADER)
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
        }

        boolean alternate = false;
        for (ReportePagosVendedorDTO.FilaPago fila : pagos) {
            DeviceRgb bg = alternate ? COLOR_FILA_ALTERNA : null;

            t.addCell(celda(String.valueOf(fila.getNumero()), normal, bg));
            t.addCell(celda(fila.getFechaPago() != null ? fila.getFechaPago().format(FMT_FECHA) : "—", normal, bg));
            if (mostrarVendedor) {
                t.addCell(celda(nullToVacio(fila.getNombreVendedor()), normal, bg));
            }
            t.addCell(celda(etiquetaTipo(fila.getTipo()), normal, bg));
            t.addCell(celda(fila.getNumeroLetra() != null ? fila.getNumeroLetra() : "—", normal, bg));
            t.addCell(celda(nullToVacio(fila.getManzana()), normal, bg));
            t.addCell(celda(nullToVacio(fila.getNumeroLote()), normal, bg));
            t.addCell(celda(nullToVacio(fila.getPrograma()), normal, bg));
            t.addCell(celda(simbolo(fila.getMoneda()) + " " + DF.format(zeroIfNull(fila.getMonto())), normal, bg));
            t.addCell(celda(nullToVacio(fila.getNumeroEgreso()), normal, bg));

            alternate = !alternate;
        }

        return t;
    }

    // ── TOTALES ──────────────────────────────────────────────────────

    private static Table totales(ReportePagosVendedorDTO dto, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6);

        t.addCell(celdaTotal("TOTAL ADELANTOS: " + simbolo(dto.getMoneda()) + " "
                + DF.format(zeroIfNull(dto.getTotalAdelantos())), bold));
        t.addCell(celdaTotal("TOTAL CUOTAS: " + simbolo(dto.getMoneda()) + " "
                + DF.format(zeroIfNull(dto.getTotalMensuales())), bold));
        t.addCell(celdaTotal("TOTAL GENERAL (" + dto.getTotalPagos() + " pagos): "
                + simbolo(dto.getMoneda()) + " " + DF.format(zeroIfNull(dto.getTotalGeneral())), bold));

        return t;
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private static Cell celdaInfo(String texto, PdfFont bold) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4);
        c.add(new Paragraph(texto).setFont(bold).setFontSize(8));
        return c;
    }

    private static Cell celda(String texto, PdfFont normal, DeviceRgb bg) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER);
        if (bg != null) c.setBackgroundColor(bg);
        c.add(new Paragraph(texto != null ? texto : "").setFont(normal).setFontSize(7.5f));
        return c;
    }

    private static Cell celdaTotal(String texto, PdfFont bold) {
        return new Cell()
                .setBackgroundColor(COLOR_AZUL_OSCURO)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(texto).setFont(bold).setFontSize(8.5f)
                        .setFontColor(ColorConstants.WHITE));
    }

    /** "ADELANTO" → "ADELANTO", "MENSUAL" → "CUOTA", "TODOS" → "TODOS". */
    private static String etiquetaTipo(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo.toUpperCase()) {
            case "ADELANTO" -> "ADELANTO";
            case "MENSUAL" -> "CUOTA";
            case "TODOS" -> "TODOS";
            default -> tipo.toUpperCase();
        };
    }

    private static String simbolo(String moneda) {
        if ("PEN".equals(moneda)) return "S/.";
        return "$";
    }

    private static BigDecimal zeroIfNull(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String nullToVacio(String valor) {
        return valor != null ? valor : "";
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        try (var is = ReportePagosVendedorPdf.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Fuente no encontrada: " + path);
            byte[] bytes = StreamUtil.inputStreamToArray(is);
            return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
        }
    }
}
