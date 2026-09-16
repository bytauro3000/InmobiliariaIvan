package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ReporteComisionVendedorDTO;
import com.itextpdf.io.font.PdfEncodings;
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

import com.itextpdf.io.image.ImageDataFactory;
import com.Inmobiliaria.demo.config.EmpresaContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReporteComisionPdf {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    // ── Colores ──────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_AZUL_OSCURO = new DeviceRgb(30, 64, 110);
    private static final DeviceRgb COLOR_AZUL_CLARO  = new DeviceRgb(220, 230, 242);
    private static final DeviceRgb COLOR_GRIS_HEADER = new DeviceRgb(60, 60, 60);

    // ── Datos de empresa ─────────────────────────────────────────────
    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
    private static String ruc() { return "R.U.C.: " + EmpresaContext.empresaService.obtenerActiva().getRuc(); }
    private static String direccion() { return EmpresaPdfUtil.direccionCompleta(); }
    private static String telefono() { return "Cel.: " + EmpresaContext.empresaService.obtenerActiva().getCelular(); }
    private static String logoUrl() { return EmpresaContext.empresaService.obtenerActiva().getLogoSmallUrl(); }

    public static byte[] generar(ReporteComisionVendedorDTO dto) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(20, 20, 20, 20);

            // ── ENCABEZADO ─────────────────────────────────────────────
            doc.add(encabezado(dto, courierBold, courier));

            // ── TITULO ─────────────────────────────────────────────────
            String titulo = dto.isSoloPendientes()
                    ? "COMISIONES PENDIENTES DE PAGO"
                    : "HISTORIAL DE COMISIONES";
            doc.add(new Paragraph(titulo)
                    .setFont(courierBold)
                    .setFontSize(13)
                    .setFontColor(COLOR_AZUL_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6)
                    .setMarginBottom(4));

            // ── DATOS DEL VENDEDOR ─────────────────────────────────────
            doc.add(datosVendedor(dto, courier, courierBold));

            // ── TABLA POR PROGRAMA ─────────────────────────────────────
            if (dto.getProgramas() != null && !dto.getProgramas().isEmpty()) {
                for (ReporteComisionVendedorDTO.ProgramaComision programa : dto.getProgramas()) {
                    doc.add(encabezadoPrograma(programa.getNombrePrograma(), courierBold));
                    doc.add(tablaComision(programa, courier, courierBold));
                }
            } else {
                doc.add(new Paragraph("El vendedor no tiene comisiones registradas.")
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
            throw new RuntimeException("Error generando PDF de comisiones: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ── ENCABEZADO ───────────────────────────────────────────────────

    private static Table encabezado(ReporteComisionVendedorDTO dto, PdfFont bold, PdfFont normal) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.3f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);

        // Celda izquierda: empresa
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

        // Celda derecha: logo
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

    // ── DATOS DEL VENDEDOR ───────────────────────────────────────────

    private static Table datosVendedor(ReporteComisionVendedorDTO dto, PdfFont normal, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6)
                .setMarginBottom(4);

        // Fila 1: Nombres | DNI | ID Vendedor | Emision
        t.addCell(celdaNormal("VENDEDOR: " + dto.getNombreVendedor() + " " + dto.getApellidosVendedor(), normal, bold));
        t.addCell(celdaNormal("DNI: " + dto.getDniVendedor(), normal, bold));
        t.addCell(celdaNormal("ID: " + dto.getIdVendedor(), normal, bold));
        t.addCell(celdaNormal("EMISION: " + dto.getFechaEmision().format(FMT_FECHA), normal, bold));
        t.addCell(celdaNormal("CELULAR: " + (dto.getCelularVendedor() != null ? dto.getCelularVendedor() : ""), normal, bold));

        // Fila 2: Direccion | Distrito
        Cell dirCell = new Cell(1, 3)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4);
        dirCell.add(new Paragraph("DIRECCION: " + (dto.getDireccionVendedor() != null ? dto.getDireccionVendedor() : ""))
                .setFont(normal).setFontSize(8));
        t.addCell(dirCell);

        t.addCell(celdaNormal("DISTRITO: " + (dto.getDistritoVendedor() != null ? dto.getDistritoVendedor() : ""), normal, bold));
        t.addCell(celdaNormal("", normal, bold));

        // Fila 3: % Comision | Total Comision | Aporte | Saldo
        t.addCell(celdaNormal("% COMISION: " + (dto.getPorcentajeComision() != null ? dto.getPorcentajeComision() + "%" : "0%"), normal, bold));
        t.addCell(celdaNormal("TOTAL COMISION: " + sym(dto) + " " + DF.format(dto.getTotalComision() != null ? dto.getTotalComision() : 0), normal, bold));
        t.addCell(celdaNormal("APORTE: " + sym(dto) + " " + DF.format(dto.getAporteComision() != null ? dto.getAporteComision() : 0), normal, bold));
        t.addCell(celdaNormal("SALDO: " + sym(dto) + " " + DF.format(dto.getSaldoPendiente() != null ? dto.getSaldoPendiente() : 0), normal, bold));
        t.addCell(celdaNormal("", normal, bold));

        return t;
    }

    // ── ENCABEZADO PROGRAMA ──────────────────────────────────────────

    private static Paragraph encabezadoPrograma(String nombrePrograma, PdfFont bold) {
        return new Paragraph("  PROGRAMA: " + nombrePrograma.toUpperCase())
                .setFont(bold).setFontSize(9)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(COLOR_AZUL_OSCURO)
                .setPaddingTop(3).setPaddingBottom(3)
                .setMarginTop(8).setMarginBottom(0);
    }

    // ── TABLA DE COMISIONES ──────────────────────────────────────────

    private static Table tablaComision(ReporteComisionVendedorDTO.ProgramaComision programa,
                                       PdfFont normal, PdfFont bold) {
        String[] headers = {"N°", "MZ", "LT", "FECHA", "MONTO COMISION", "PAGOS REALIZADOS", "SALDO", "DEUDA"};
        Table t = new Table(UnitValue.createPercentArray(new float[]{0.05f, 0.07f, 0.10f, 0.09f, 0.17f, 0.17f, 0.17f, 0.18f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(2);

        // Header
        for (String h : headers) {
            t.addCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(7f).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_GRIS_HEADER)
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
        }

        // Filas
        boolean alternate = false;
        DeviceRgb colorFila = new DeviceRgb(245, 248, 252);
        for (ReporteComisionVendedorDTO.FilaComision fila : programa.getFilas()) {
            DeviceRgb bg = alternate ? colorFila : null;

            t.addCell(celdaFilaCenter(String.valueOf(fila.getNumero()), normal, bg));
            t.addCell(celdaFila(fila.getManzana(), normal, bg));
            t.addCell(celdaFila(fila.getNumeroLote(), normal, bg));
            t.addCell(celdaFilaCenter(fila.getFechaContrato() != null
                    ? fila.getFechaContrato().getDayOfMonth() + "/"
                    + fila.getFechaContrato().getMonthValue() + "/"
                    + fila.getFechaContrato().getYear() : "—", normal, bg));
            t.addCell(celdaFila(fila.getMoneda() + " " + DF.format(fila.getMontoComision()), normal, bg));
            t.addCell(celdaFila(fila.getMoneda() + " " + DF.format(fila.getPagosRealizados()), normal, bg));
            t.addCell(celdaFila(fila.getMoneda() + " " + DF.format(fila.getSaldoComision()), normal, bg));
            t.addCell(celdaFila(fila.getMoneda() + " " + DF.format(fila.getDeudaPorLote()), normal, bg));

            alternate = !alternate;
        }

        // Total deuda por programa
        Cell totalDeudaLabel = new Cell(1, 7)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT);
        totalDeudaLabel.add(new Paragraph("DEUDA PROGRAMA (" + programa.getTotalLotes() + " lotes):").setFont(bold).setFontSize(8));
        t.addCell(totalDeudaLabel);

        BigDecimal totalDeudaPrograma = programa.getFilas().stream()
                .map(f -> f.getDeudaPorLote() != null ? f.getDeudaPorLote() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String moneda = !programa.getFilas().isEmpty() ? programa.getFilas().get(0).getMoneda() : "USD";

        Cell totalDeudaVal = new Cell(1, 1)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4)
                .setTextAlignment(TextAlignment.LEFT);
        totalDeudaVal.add(new Paragraph(moneda + " " + DF.format(totalDeudaPrograma)).setFont(bold).setFontSize(8));
        t.addCell(totalDeudaVal);

        return t;
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private static Cell celdaNormal(String texto, PdfFont normal, PdfFont bold) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(4);
        c.add(new Paragraph(texto).setFont(bold).setFontSize(8));
        return c;
    }

    private static Cell celdaFila(String texto, PdfFont normal, DeviceRgb bg) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER);
        if (bg != null) c.setBackgroundColor(bg);
        c.add(new Paragraph(texto != null ? texto : "").setFont(normal).setFontSize(8));
        return c;
    }

    private static Cell celdaFilaCenter(String texto, PdfFont normal, DeviceRgb bg) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER);
        if (bg != null) c.setBackgroundColor(bg);
        c.add(new Paragraph(texto != null ? texto : "").setFont(normal).setFontSize(7.5f));
        return c;
    }

    private static String sym(ReporteComisionVendedorDTO dto) {
        return "PEN".equals(dto.getMoneda()) ? "S/." : "$";
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        try (var is = ReporteComisionPdf.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Fuente no encontrada: " + path);
            byte[] bytes = StreamUtil.inputStreamToArray(is);
            return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
        }
    }
}
