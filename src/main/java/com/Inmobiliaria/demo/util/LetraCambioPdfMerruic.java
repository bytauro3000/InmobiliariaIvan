package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfViewerPreferences;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class LetraCambioPdfMerruic {

    private static final float PAGE_WIDTH_MM  = 220f;
    private static final float PAGE_HEIGHT_MM = 110f;
    private static final float MM_TO_PT = 2.8346f;
    private static final float FONT_SIZE = 11f;
    private static final float CONTENT_Y_SHIFT_PT = 3f;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String FONT_COURIER_BOLD = "fonts/COURBD.TTF";

    public static byte[] generar(List<ReporteLetraCambioDTO> reportes, String moneda) {

        reportes.sort((a, b) -> {
            int numA = Integer.parseInt(a.getNumeroLetra().split("/")[0]);
            int numB = Integer.parseInt(b.getNumeroLetra().split("/")[0]);
            return Integer.compare(numA, numB);
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter   writer = new PdfWriter(out);
            PdfDocument pdf    = new PdfDocument(writer);

            PdfViewerPreferences prefs = new PdfViewerPreferences();
            prefs.setPrintScaling(PdfViewerPreferences.PdfViewerPreferencesConstants.NONE);
            pdf.getCatalog().setViewerPreferences(prefs);

            PageSize pageSize = new PageSize(
                    toPoints(PAGE_WIDTH_MM),
                    toPoints(PAGE_HEIGHT_MM)
            );

            PdfFont fontBold = cargarFuente(FONT_COURIER_BOLD);

            String simboloMoneda = "PEN".equals(moneda) ? "S/. " : "$ ";

            for (ReporteLetraCambioDTO reporte : reportes) {
                PdfPage   page   = pdf.addNewPage(pageSize);
                PdfCanvas canvas = new PdfCanvas(page);

                // ── FILA 1 ────────────────────────────────────────────────────

                // Numero de letra
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getNumeroLetra(),
                        48, 21);

                // DISTRICTO (antes iba Fecha de Giro en Ivan)
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getDistritoNombre(),
                        101, 20);

                // FECHA DE GIRO (antes iba Distrito en Ivan)
                escribir(canvas, fontBold, FONT_SIZE,
                        formatearFecha(reporte.getFechaGiro()),
                        129, 22);

                // Fecha de Vencimiento
                escribir(canvas, fontBold, FONT_SIZE,
                        formatearFecha(reporte.getFechaVencimiento()),
                        155, 22);

                // Importe
                escribir(canvas, fontBold, FONT_SIZE,
                        simboloMoneda + formatearImporte(reporte.getImporte()),
                        182, 21);

                // ── FILA 2 ────────────────────────────────────────────────────

                // Importe en letras
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getImporteLetras(),
                        45, 39);

                // ── FILA 3 ────────────────────────────────────────────────────

                // Cliente 1 (nombre + apellidos)
                if (reporte.getCliente1Nombre() != null) {
                    String cliente1 = reporte.getCliente1Nombre();
                    if (reporte.getCliente1Apellidos() != null
                            && !reporte.getCliente1Apellidos().isBlank()) {
                        cliente1 += " " + reporte.getCliente1Apellidos();
                    }
                    escribir(canvas, fontBold, FONT_SIZE - 0.5f, cliente1, 57, 49.5f);
                }

                // ── FILA 4 ────────────────────────────────────────────────────

                // Cliente 2 con DNI/RUC (solo si existe)
                if (reporte.getCliente2Nombre() != null
                        && !reporte.getCliente2Nombre().isBlank()) {
                    String cliente2 = reporte.getCliente2Nombre();
                    if (reporte.getCliente2Apellidos() != null
                            && !reporte.getCliente2Apellidos().isBlank()) {
                        cliente2 += " " + reporte.getCliente2Apellidos();
                    }
                    cliente2 += " DNI/RUC:" + reporte.getCliente2NumDocumento();
                    escribir(canvas, fontBold, FONT_SIZE - 0.5f, cliente2, 46, 54.5f);
                }

                // ── FILA 5 ────────────────────────────────────────────────────

                // DNI/RUC Cliente 1
                escribir(canvas, fontBold, FONT_SIZE - 0.5f, reporte.getCliente1NumDocumento(), 58, 63);

                // Celular Cliente 1
                if (reporte.getCliente1Celular() != null
                        && !reporte.getCliente1Celular().isBlank()) {
                    escribir(canvas, fontBold, FONT_SIZE - 0.5f, reporte.getCliente1Celular(), 89, 63);
                }

                // ── FILA 6 ────────────────────────────────────────────────────

                // Domicilio (con salto de linea si es largo)
                if (reporte.getCliente1Direccion() != null) {
                    String direccion = reporte.getCliente1Direccion();
                    if (direccion.length() > 50) {
                        // Dividir en dos lineas
                        int midpoint = direccion.length() / 2;
                        int spaceBefore = direccion.lastIndexOf(' ', midpoint);
                        int spaceAfter = direccion.indexOf(' ', midpoint);
                        int splitPoint = (spaceBefore != -1 && (spaceAfter == -1 || midpoint - spaceBefore <= spaceAfter - midpoint))
                                ? spaceBefore : (spaceAfter != -1 ? spaceAfter : midpoint);
                        String linea1 = direccion.substring(0, splitPoint).trim();
                        String linea2 = direccion.substring(splitPoint).trim();
                        escribir(canvas, fontBold, FONT_SIZE - 0.5f, linea1, 58, 68.5f);
                        escribir(canvas, fontBold, FONT_SIZE - 0.5f, linea2, 58, 74.5f);
                    } else {
                        escribir(canvas, fontBold, FONT_SIZE - 0.5f, direccion, 58, 68.5f);
                    }
                }

                // ── FILA 7 ────────────────────────────────────────────────────

                // Distrito del cliente
                if (reporte.getCliente1Distrito() != null) {
                    escribir(canvas, fontBold, FONT_SIZE - 0.5f,
                            reporte.getCliente1Distrito(),
                            84, 73);
                }

                canvas.release();
            }

            pdf.close();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF de letras de cambio MERRUI", e);
        }

        return out.toByteArray();
    }

    // ── METODOS AUXILIARES ────────────────────────────────────────────────────

    private static void escribir(PdfCanvas canvas, PdfFont font, float fontSize,
                                 String texto, float xMm, float yMm) {
        if (texto == null || texto.isBlank()) return;

        float xPt = toPoints(xMm);
        float yPt = toPoints(PAGE_HEIGHT_MM - yMm) - CONTENT_Y_SHIFT_PT;

        canvas.beginText()
              .setFontAndSize(font, fontSize)
              .moveText(xPt, yPt)
              .showText(texto)
              .endText();
    }

    private static float toPoints(float mm) {
        return mm * MM_TO_PT;
    }

    private static String formatearFecha(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.format(DATE_FMT);
    }

    private static String formatearImporte(BigDecimal importe) {
        if (importe == null) return "0.00";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(importe);
    }

    private static PdfFont cargarFuente(String path) throws IOException {
        try {
            InputStream is = LetraCambioPdfMerruic.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new IOException("Fuente no encontrada en el classpath: " + path
                        + " — verifica que el archivo exista en src/main/resources/fonts/");
            }
            byte[] bytes = StreamUtil.inputStreamToArray(is);
            return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error al cargar la fuente '" + path + "': " + e.getMessage(), e);
        }
    }
}
