package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.IngresoDetalladoItemDTO;
import com.Inmobiliaria.demo.dto.ReporteIngresosDetalladoDTO;
import com.Inmobiliaria.demo.dto.TotalesMonedaDTO;
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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera el PDF del reporte de INGRESOS DETALLADO (comprobantes BOLETA/RECIBO
 * por rango de fechas, con receptor, importe, fecha de emisión y NC).
 *
 * Clase utilitaria estática, igual que ReporteClientesMoraPdf. No tiene
 * dependencias de Spring: recibe el DTO ya calculado y solo arma el PDF.
 * Los datos de la empresa (Ivan / Merruic) se leen desde EmpresaContext,
 * por lo que el mismo código sirve para ambos despliegues.
 *
 * Formato: A4 horizontal (landscape).
 * Nota: solo se usa doc.add() (sin referencias explícitas a páginas) para
 * evitar el NPE de immediateFlush detectado en EG01-157.
 */
public class ReporteIngresosDetalladoPdf {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat     DF  = new DecimalFormat("#,##0.00",
            new DecimalFormatSymbols(Locale.US));

    // ── Datos de la empresa (dinámicos desde EmpresaContext) ──────────────────
    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
    private static String direccion() { return EmpresaPdfUtil.direccionCompleta(); }
    private static String telefono() { return "Cel.: " + EmpresaContext.empresaService.obtenerActiva().getCelular(); }
    private static String ruc() { return "R.U.C.: " + EmpresaContext.empresaService.obtenerActiva().getRuc(); }

    // ── Colores del reporte ───────────────────────────────────────────────────
    private static final DeviceRgb COLOR_AZUL_OSCURO = new DeviceRgb(30,  64,  110);
    private static final DeviceRgb COLOR_AZUL_CLARO  = new DeviceRgb(220, 230, 242);
    private static final DeviceRgb COLOR_ROJO_SUAVE  = new DeviceRgb(255, 235, 235);
    private static final DeviceRgb COLOR_GRIS_HEADER = new DeviceRgb(60,  60,  60);
    private static final DeviceRgb COLOR_BLANCO      = new DeviceRgb(255, 255, 255);
    private static final DeviceGray GRIS_SEPARADOR   = new DeviceGray(0.75f);

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODO PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] generar(ReporteIngresosDetalladoDTO dto) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document    doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(28, 28, 28, 28);

            String fechaHoy = LocalDate.now().format(FMT);

            // ── ENCABEZADO DE EMPRESA ─────────────────────────────────────────
            doc.add(encabezadoEmpresa(courierBold, courier, fechaHoy));

            // ── TÍTULO + FILTROS ──────────────────────────────────────────────
            doc.add(new Paragraph("REPORTE DE INGRESOS DETALLADO")
                    .setFont(courierBold)
                    .setFontSize(13)
                    .setFontColor(COLOR_AZUL_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(4));

            String tipoTexto = switch (dto.getTipoFiltro() != null ? dto.getTipoFiltro() : "TODOS") {
                case "BOLETA" -> "BOLETAS";
                case "RECIBO" -> "RECIBOS";
                default       -> "BOLETAS Y RECIBOS";
            };
            String rango = dto.getFechaDesde().format(FMT) + "  al  " + dto.getFechaHasta().format(FMT);
            doc.add(new Paragraph(
                    "Período: " + rango
                            + "   |   Comprobantes: " + tipoTexto
                            + "   |   Emitidos: " + dto.getCantidadTotal()
                            + " (anulados: " + dto.getCantidadAnuladas() + ")")
                    .setFont(courier)
                    .setFontSize(8.5f)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));

            if (dto.getCantidadAnuladas() > 0) {
                doc.add(new Paragraph(
                        "Filas sombreadas: comprobantes anulados (NC o pago anulado), excluidos del total neto.")
                        .setFont(courier)
                        .setFontSize(7.5f)
                        .setFontColor(ColorConstants.RED)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(6));
            }

            List<IngresoDetalladoItemDTO> detalle = dto.getDetalle();
            if (detalle == null || detalle.isEmpty()) {
                doc.add(new Paragraph("No se encontraron comprobantes en el rango indicado.")
                        .setFont(courier)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(10));
                doc.close();
                return out.toByteArray();
            }

            // ── TABLA + TOTALES ───────────────────────────────────────────────
            doc.add(tablaDetalle(detalle, courier, courierBold));
            doc.add(bloqueTotales(dto.getTotales(), courierBold));

            // ── PIE DEL DOCUMENTO ─────────────────────────────────────────────
            doc.add(separadorDelgado());
            doc.add(new Paragraph("Generado por el Sistema de Gestión Inmobiliaria  |  " + empresa())
                    .setFont(courier)
                    .setFontSize(7)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(4));

            doc.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de ingreso detallado: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUES DEL PDF
    // ─────────────────────────────────────────────────────────────────────────

    /** Bloque superior con datos de la empresa y fecha de emisión */
    private static Table encabezadoEmpresa(PdfFont bold, PdfFont normal, String fecha) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.3f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);

        Cell izq = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(6);
        izq.add(new Paragraph(empresa())
                .setFont(bold).setFontSize(10)
                .setFontColor(COLOR_AZUL_OSCURO)
                .setMarginBottom(2));
        izq.add(new Paragraph(direccion())
                .setFont(normal).setFontSize(7.5f)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(1));
        izq.add(new Paragraph(telefono() + "          " + ruc())
                .setFont(normal).setFontSize(7.5f)
                .setFontColor(ColorConstants.DARK_GRAY));
        t.addCell(izq);

        Cell der = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1f))
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER);
        der.add(new Paragraph("Fecha de emisión")
                .setFont(bold).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3));
        der.add(new Paragraph(fecha)
                .setFont(bold).setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
        t.addCell(der);

        return t;
    }

    /**
     * Tabla principal: una fila por comprobante.
     * Columnas: NRO. CPE | RECEPTOR | IMPORTE TOTAL | FECHA DE EMISIÓN | NOTA DE CRÉDITO
     */
    private static Table tablaDetalle(List<IngresoDetalladoItemDTO> detalle,
                                      PdfFont normal, PdfFont bold) {
        // Anchos en porcentaje (suman 100)
        float[] anchos = {12, 40, 15, 15, 18};
        Table tabla = new Table(UnitValue.createPercentArray(anchos))
                .useAllAvailableWidth()
                .setMarginBottom(2);

        String[] headers = {"NRO. CPE", "RECEPTOR", "IMPORTE TOTAL", "FECHA DE EMISIÓN", "NOTA DE CRÉDITO"};
        for (String h : headers) {
            tabla.addHeaderCell(
                    new Cell()
                            .add(new Paragraph(h).setFont(bold).setFontSize(7.5f)
                                    .setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(COLOR_GRIS_HEADER)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setBorder(new SolidBorder(ColorConstants.WHITE, 0.3f))
                            .setPaddingTop(3).setPaddingBottom(3)
                            .setPaddingLeft(3).setPaddingRight(3)
            );
        }

        int numFila = 1;
        for (IngresoDetalladoItemDTO item : detalle) {
            boolean anulado = !"VIGENTE".equals(item.getEstado());
            DeviceRgb colorFila = anulado
                    ? COLOR_ROJO_SUAVE
                    : ((numFila % 2 == 0) ? COLOR_AZUL_CLARO : COLOR_BLANCO);

            String importe = simbolo(item.getMoneda()) + DF.format(item.getMonto());
            String fecha   = item.getFechaEmision() != null ? item.getFechaEmision().format(FMT) : "";
            String nc      = item.getNcNumeroCompleto() != null ? item.getNcNumeroCompleto() : "—";

            agregarCelda(tabla, item.getNumeroCompleto(),                   TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, item.getReceptor() != null ? item.getReceptor() : "-", TextAlignment.LEFT, normal, colorFila);
            agregarCelda(tabla, importe,                                    TextAlignment.RIGHT,  normal, colorFila);
            agregarCelda(tabla, fecha,                                      TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, nc,                                         TextAlignment.CENTER, normal, colorFila);

            numFila++;
        }

        return tabla;
    }

    private static void agregarCelda(Table tabla, String texto, TextAlignment align,
                                     PdfFont font, DeviceRgb colorFondo) {
        tabla.addCell(
                new Cell()
                        .add(new Paragraph(texto != null ? texto : "")
                                .setFont(font).setFontSize(7.5f))
                        .setBackgroundColor(colorFondo)
                        .setTextAlignment(align)
                        .setBorder(new SolidBorder(GRIS_SEPARADOR, 0.3f))
                        .setPaddingTop(3).setPaddingBottom(3)
                        .setPaddingLeft(3).setPaddingRight(3)
        );
    }

    /** Totales por moneda (bruto / anulado / neto), una línea por moneda */
    private static Paragraph bloqueTotales(List<TotalesMonedaDTO> totales, PdfFont bold) {
        StringBuilder sb = new StringBuilder();
        if (totales != null) {
            for (TotalesMonedaDTO t : totales) {
                if (sb.length() > 0) sb.append("     ");
                String s = simbolo(t.getMoneda());
                sb.append(t.getMoneda())
                  .append("  —  Bruto: ").append(s).append(DF.format(t.getBruto()))
                  .append("  |  Anulado: ").append(s).append(DF.format(t.getAnulada()))
                  .append("  |  NETO: ").append(s).append(DF.format(t.getNeto()));
            }
        }

        return new Paragraph(sb.length() > 0 ? sb.toString() : "")
                .setFont(bold)
                .setFontSize(8.5f)
                .setFontColor(COLOR_AZUL_OSCURO)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(5)
                .setMarginBottom(4);
    }

    /** Línea gris fina de separación */
    private static Table separadorDelgado() {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(6).setMarginBottom(2);
        t.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS_SEPARADOR, 0.5f))
                .setPadding(0).setHeight(1));
        return t;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    private static String simbolo(String moneda) {
        if ("USD".equalsIgnoreCase(moneda)) return "$ ";
        if ("PEN".equalsIgnoreCase(moneda)) return "S/ ";
        return "";
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ReporteIngresosDetalladoPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}
