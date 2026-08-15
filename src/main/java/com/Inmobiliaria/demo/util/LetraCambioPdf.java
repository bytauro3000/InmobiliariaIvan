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

public class LetraCambioPdf {

    // ── Dimensiones de página (igual que el frontend: format:[216,110]) ────────
    private static final float PAGE_WIDTH_MM  = 216f;
    private static final float PAGE_HEIGHT_MM = 110f;

    // ── Factor de conversión mm → pt (1 mm = 2.8346 pt) ──────────────────────
    private static final float MM_TO_PT = 2.8346f;

    // ── Tamaño de fuente (igual que el frontend: doc.setFontSize(10)) ─────────
    private static final float FONT_SIZE = 10f;

    // ── Desplazamiento global del contenido en el eje Y (hacia ARRIBA) ────────
    // En PDF el origen Y está abajo y crece hacia arriba; para subir el contenido
    // hacia el borde superior se SUMA a la coordenada Y final de todos los textos.
    // Unidad: puntos PDF (1 pt = 1/72 in ≈ 0.353 mm).
    private static final float CONTENT_Y_SHIFT_PT = 3f;

    // ── Formateador de fechas: LocalDate → "dd/MM/yyyy" ───────────────────────
    // Igual que el frontend: `${dia}/${mes}/${anio}`
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Rutas de fuentes en el classpath (idénticas a ComprobantePagoLetraPdf) ─
    private static final String FONT_COURIER_BOLD = "fonts/COURBD.TTF";

    // ────────────────────────────────────────────────────────────────────────────
    // MÉTODO PRINCIPAL
    // ────────────────────────────────────────────────────────────────────────────

    public static byte[] generar(List<ReporteLetraCambioDTO> reportes, String moneda) {

        // Ordenar ascendente por número de letra (ej: "1/60", "2/60", ...)
        // Igual que el frontend:
        //   reportes.sort((a,b) => parseInt(a.numeroLetra) - parseInt(b.numeroLetra))
        reportes.sort((a, b) -> {
            int numA = Integer.parseInt(a.getNumeroLetra().split("/")[0]);
            int numB = Integer.parseInt(b.getNumeroLetra().split("/")[0]);
            return Integer.compare(numA, numB);
        });

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter   writer = new PdfWriter(out);
            PdfDocument pdf    = new PdfDocument(writer);

            // ── FIX ANTI-DESALINEACIÓN ────────────────────────────────────────
            // PrintScaling = NONE previene que el visor escale el PDF al imprimir.
            // El usuario verá "Ajustar página" desmarcado en el diálogo de impresión.
            // Esto elimina el desplazamiento en X e Y sobre la letra física.
            PdfViewerPreferences prefs = new PdfViewerPreferences();
            prefs.setPrintScaling(PdfViewerPreferences.PdfViewerPreferencesConstants.NONE);
            pdf.getCatalog().setViewerPreferences(prefs);

            // Tamaño de página personalizado en puntos
            PageSize pageSize = new PageSize(
                    toPoints(PAGE_WIDTH_MM),
                    toPoints(PAGE_HEIGHT_MM)
            );

            // ── FUENTES COURIER EMBEBIDAS ─────────────────────────────────────
            // Mismas fuentes que usan los recibos (ComprobantePagoLetraPdf).
            // PdfEncodings.WINANSI garantiza correcta representación de caracteres
            // latinos (ñ, tildes, etc.) en todos los sistemas operativos.
            PdfFont fontBold = cargarFuente(FONT_COURIER_BOLD);

            // ── SÍMBOLO DE MONEDA ─────────────────────────────────────────────
            // El frontend usa el template literal:
            //   `${moneda === 'PEN' ? 'S/ ' : '$ '} ${importeFormateado}`
            //    → "S/ " + " " + importe = "S/  1,234.56"  (2 espacios)
            //    → "$ "  + " " + importe = "$  1,234.56"   (2 espacios)
            // Se replica aquí para igualar la posición visual del importe.
            String simboloMoneda = "PEN".equals(moneda) ? "S/. " : "$ ";

            for (ReporteLetraCambioDTO reporte : reportes) {
                PdfPage   page   = pdf.addNewPage(pageSize);
                PdfCanvas canvas = new PdfCanvas(page);

                // ── FILA 1 ────────────────────────────────────────────────────

                // Número de letra
                // frontend → doc.text(reporte.numeroLetra, 50, 22)
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getNumeroLetra(),
                        46, 22);

                // Fecha de Giro
                // frontend → doc.text(formatearFechaVista(reporte.fechaGiro), 99, 24)
                escribir(canvas, fontBold, FONT_SIZE,
                        formatearFecha(reporte.getFechaGiro()),
                        99, 23);

                // Distrito
                // frontend → doc.text(reporte.distritoNombre, 129, 22)
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getDistritoNombre(),
                        127, 22);

                // Fecha de Vencimiento
                // frontend → doc.text(formatearFechaVista(reporte.fechaVencimiento), 155, 24)
                escribir(canvas, fontBold, FONT_SIZE,
                        formatearFecha(reporte.getFechaVencimiento()),
                        153, 23);

                // Importe
                // frontend → doc.text(`${símbolo} ${importeFormateado}`, 182, 22)
                escribir(canvas, fontBold, FONT_SIZE,
                        simboloMoneda + formatearImporte(reporte.getImporte()),
                        180, 22);

                // ── FILA 2 ────────────────────────────────────────────────────

                // Importe en letras
                // frontend → doc.text(reporte.importeLetras, 43, 38)
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getImporteLetras(),
                        43, 38);

                // ── FILA 3 ────────────────────────────────────────────────────

                // Cliente 1 (nombre + apellidos)
                // frontend:
                //   const cliente1Info = cliente1Apellidos
                //     ? cliente1Nombre + ' ' + cliente1Apellidos
                //     : cliente1Nombre;
                //   doc.text(cliente1Info, 54, 49)
                if (reporte.getCliente1Nombre() != null) {
                    String cliente1 = reporte.getCliente1Nombre();
                    if (reporte.getCliente1Apellidos() != null
                            && !reporte.getCliente1Apellidos().isBlank()) {
                        cliente1 += " " + reporte.getCliente1Apellidos();
                    }
                    escribir(canvas, fontBold, FONT_SIZE, cliente1, 54, 49);
                }

                // ── FILA 4 ────────────────────────────────────────────────────

                // Cliente 2 con DNI/RUC (solo si existe)
                // frontend → doc.text(cliente2Info + ' DNI/RUC: ' + numDoc, 44, 53)
                if (reporte.getCliente2Nombre() != null
                        && !reporte.getCliente2Nombre().isBlank()) {
                    String cliente2 = reporte.getCliente2Nombre();
                    if (reporte.getCliente2Apellidos() != null
                            && !reporte.getCliente2Apellidos().isBlank()) {
                        cliente2 += " " + reporte.getCliente2Apellidos();
                    }
                    cliente2 += " DNI/RUC:" + reporte.getCliente2NumDocumento();
                    escribir(canvas, fontBold, FONT_SIZE, cliente2, 44, 53);
                }

                // ── FILA 5 ────────────────────────────────────────────────────

                // Dirección cliente 1
                // frontend → doc.text(reporte.cliente1Direccion, 52, 58)
                if (reporte.getCliente1Direccion() != null) {
                    escribir(canvas, fontBold, FONT_SIZE,
                            reporte.getCliente1Direccion(),
                            52,58);
                }

                // ── FILA 6 ────────────────────────────────────────────────────

                // DNI cliente 1
                // frontend → doc.text(reporte.cliente1NumDocumento, 50, 64)
                escribir(canvas, fontBold, FONT_SIZE,
                        reporte.getCliente1NumDocumento(),
                        50, 64);

                // Distrito cliente 1
                // frontend → doc.text(`distrito: ${reporte.cliente1Distrito}`, 88, 62)
                escribir(canvas, fontBold, FONT_SIZE,
                        "distrito:" + reporte.getCliente1Distrito(),
                        88, 62);

                canvas.release();
            }

            pdf.close();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF de letras de cambio", e);
        }

        return out.toByteArray();
    }

    // ── MÉTODOS AUXILIARES ────────────────────────────────────────────────────

    private static void escribir(PdfCanvas canvas, PdfFont font, float fontSize,
                                 String texto, float xMm, float yMm) {
        if (texto == null || texto.isBlank()) return;

        float xPt = toPoints(xMm);
        float yPt = toPoints(PAGE_HEIGHT_MM - yMm) + CONTENT_Y_SHIFT_PT;

        canvas.beginText()
              .setFontAndSize(font, fontSize)
              .moveText(xPt, yPt)
              .showText(texto)
              .endText();
    }

    /** Convierte milímetros a puntos PDF (1 mm = 2.8346 pt) */
    private static float toPoints(float mm) {
        return mm * MM_TO_PT;
    }

    /**
     * Formatea LocalDate a "dd/MM/yyyy".
     * Igual que el frontend: `${dia}/${mes}/${anio}`
     */
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
            InputStream is = LetraCambioPdf.class.getClassLoader().getResourceAsStream(path);
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