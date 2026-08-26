package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO;
import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO.FilaClienteMora;
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

import com.Inmobiliaria.demo.config.EmpresaContext;

/**
 * Genera el PDF del reporte de clientes con pagos atrasados (EN MORA).
 *
 * Clase utilitaria estática, igual que ComprobanteMoraPdf y ContratoFloridaPdf.
 * No tiene dependencias de Spring. Recibe los datos ya calculados desde
 * ReporteMoraServiceImpl y solo se encarga del armado visual del PDF.
 *
 * Formato: A4 horizontal (landscape), una sección por programa.
 */
public class ReporteClientesMoraPdf {

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

    /**
     * @param grupos Lista de programas con sus clientes en mora,
     *               tal como la devuelve ReporteMoraServiceImpl.obtenerClientesEnMora()
     * @return bytes del PDF listo para enviar como ResponseEntity
     */
    public static byte[] generar(List<ReporteClientesMoraDTO> grupos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // ── Fuentes (igual que ComprobanteMoraPdf) ────────────────────────
            PdfFont courier     = cargarFuente("fonts/COUR.TTF");
            PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

            PdfDocument pdf     = new PdfDocument(new PdfWriter(out));
            Document    doc     = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(28, 28, 28, 28);

            String fechaHoy = LocalDate.now().format(FMT);

            // ── ENCABEZADO DE EMPRESA ─────────────────────────────────────────
            doc.add(encabezadoEmpresa(courierBold, courier, fechaHoy));

            // ── TÍTULO DEL REPORTE ────────────────────────────────────────────
            doc.add(new Paragraph("REPORTE DE CLIENTES CON PAGOS ATRASADOS")
                    .setFont(courierBold)
                    .setFontSize(13)
                    .setFontColor(COLOR_AZUL_OSCURO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(10));

            if (grupos == null || grupos.isEmpty()) {
                doc.add(new Paragraph("No se encontraron contratos en mora a la fecha.")
                        .setFont(courier)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER));
                doc.close();
                return out.toByteArray();
            }

            // ── SECCIÓN POR PROGRAMA ──────────────────────────────────────────
            for (ReporteClientesMoraDTO grupo : grupos) {
                doc.add(encabezadoPrograma(grupo.getNombrePrograma(), courierBold));
                doc.add(tablaClientes(grupo.getClientes(), courier, courierBold));
                doc.add(totalPrograma(grupo.getClientes(), courierBold));
            }

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
            throw new RuntimeException("Error generando PDF de clientes en mora: " + e.getMessage(), e);
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

        // Celda izquierda: nombre empresa + dirección
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

        // Celda derecha: fecha de emisión
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

    /** Barra azul oscura con el nombre del programa */
    private static Paragraph encabezadoPrograma(String nombrePrograma, PdfFont bold) {
        return new Paragraph("  PROGRAMA: " + nombrePrograma.toUpperCase())
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(COLOR_AZUL_OSCURO)
                .setPaddingTop(4)
                .setPaddingBottom(4)
                .setMarginTop(10)
                .setMarginBottom(0);
    }

    /**
     * Tabla con una fila por cliente en mora.
     * Columnas: N° | CLIENTE(S) | MZ | LT | LETRAS ATRAS. | RANGO | IMPORTE | CELULAR | DESDE | CONTRATO
     *
     * MZ y LT pueden tener múltiples valores apilados verticalmente cuando
     * el contrato tiene más de un lote.
     */
    private static Table tablaClientes(List<FilaClienteMora> clientes,
                                        PdfFont normal, PdfFont bold) {
        // Anchos en porcentaje (suman 100)
        // N° | CLIENTE(S) | MZ | LT | LETRAS | RANGO | IMPORTE | CELULAR | DESDE | CONTRATO
        float[] anchos = {3, 25, 5, 5, 7, 11, 12, 10, 13, 9};
        Table tabla = new Table(UnitValue.createPercentArray(anchos))
                .useAllAvailableWidth()
                .setMarginBottom(2);

        // ── Cabeceras ─────────────────────────────────────────────────────────
        String[] headers = {
                "N°", "CLIENTE(S)", "MZ", "LT",
                "LETRAS ATRAS.", "RANGO LETRAS",
                "IMPORTE ATRAS.", "CELULAR", "DESDE", "CONTRATO"
        };
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

        // ── Filas de datos ────────────────────────────────────────────────────
        int numFila = 1;
        for (FilaClienteMora cliente : clientes) {

            // Color de fila: rojo si >= 3 letras, azul/blanco alternado si no
            DeviceRgb colorFila;
            if (cliente.getCantidadLetrasAtrasadas() >= 3) {
                colorFila = COLOR_ROJO_SUAVE;
            } else {
                colorFila = (numFila % 2 == 0) ? COLOR_AZUL_CLARO : COLOR_BLANCO;
            }

            // ── Símbolo de moneda en importe ──────────────────────────────────
            String simbolo = "USD".equalsIgnoreCase(cliente.getMoneda()) ? "$ " : "S/ ";
            String importe  = simbolo + DF.format(cliente.getImporteTotal());

            // ── Fecha de inicio de mora ───────────────────────────────────────
            String fechaDesde = cliente.getFechaVencimientoInicio() != null
                    ? cliente.getFechaVencimientoInicio().format(FMT)
                    : "";

            // ── MZ y LT: múltiples lotes apilados con salto de línea ──────────
            String mzTexto = String.join("\n",
                    cliente.getManzanas() != null ? cliente.getManzanas() : List.of(""));
            String ltTexto = String.join("\n",
                    cliente.getNumeroLotes() != null ? cliente.getNumeroLotes() : List.of(""));

            // ── Celdas simples (texto plano) ──────────────────────────────────
            agregarCelda(tabla, String.valueOf(numFila),                              TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, cliente.getNombreClientes(),                          TextAlignment.LEFT,   normal, colorFila);
            agregarCelda(tabla, mzTexto,                                              TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, ltTexto,                                              TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, String.valueOf(cliente.getCantidadLetrasAtrasadas()), TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, cliente.getRangoLetras(),                             TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, importe,                                              TextAlignment.RIGHT,  normal, colorFila);
            agregarCelda(tabla, cliente.getCelular() != null ? cliente.getCelular() : "", TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, fechaDesde,                                           TextAlignment.CENTER, normal, colorFila);
            agregarCelda(tabla, "N° " + cliente.getIdContrato(),                     TextAlignment.CENTER, normal, colorFila);

            numFila++;
        }

        return tabla;
    }

    /** Helper para agregar una celda estándar con texto y configuración consistente */
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

    /** Línea de totales al final de cada programa */
    private static Paragraph totalPrograma(List<FilaClienteMora> clientes, PdfFont bold) {
        long totalLetras   = clientes.stream()
                .mapToLong(FilaClienteMora::getCantidadLetrasAtrasadas).sum();
        int  totalClientes = clientes.size();

        return new Paragraph(
                String.format("Total clientes en mora: %d   |   Total letras atrasadas: %d",
                        totalClientes, totalLetras))
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(COLOR_AZUL_OSCURO)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(1)
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

    /** Carga una fuente TTF desde resources, igual que en ComprobanteMoraPdf */
    private static PdfFont cargarFuente(String path) throws Exception {
        byte[] bytes = StreamUtil.inputStreamToArray(
                ReporteClientesMoraPdf.class.getClassLoader().getResourceAsStream(path));
        return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
    }
}