package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ListaContratoDTO;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.colors.ColorConstants;
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

import com.Inmobiliaria.demo.config.EmpresaContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReporteListaContratosPdf {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DeviceRgb COLOR_AZUL_OSCURO = new DeviceRgb(30, 64, 110);
    private static final DeviceRgb COLOR_GRIS_HEADER = new DeviceRgb(60, 60, 60);

    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }

    public static byte[] generar(List<ListaContratoDTO> lista) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfFont courier = cargarFuente("fonts/COUR.TTF");
        PdfFont courierBold = cargarFuente("fonts/COURBD.TTF");

        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(5, 20, 20, 20);

        // Encabezado empresa
        doc.add(encabezado(courierBold, courier));

        // Titulo
        doc.add(new Paragraph("LISTA DE CONTRATOS")
                .setFont(courierBold).setFontSize(13)
                .setFontColor(COLOR_AZUL_OSCURO)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4).setMarginBottom(4));

        doc.add(new Paragraph("EMISION: " + LocalDateTime.now().format(FMT_FECHA))
                .setFont(courier).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6));

        // Agrupar por programa
        Map<String, List<ListaContratoDTO>> porPrograma = new LinkedHashMap<>();
        for (ListaContratoDTO dto : lista) {
            String prog = dto.getNombrePrograma() != null ? dto.getNombrePrograma() : "SIN PROGRAMA";
            porPrograma.computeIfAbsent(prog, k -> new java.util.ArrayList<>()).add(dto);
        }

        for (Map.Entry<String, List<ListaContratoDTO>> entry : porPrograma.entrySet()) {
            // Titulo programa
            doc.add(new Paragraph("  PROGRAMA: " + entry.getKey().toUpperCase())
                    .setFont(courierBold).setFontSize(9)
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(COLOR_AZUL_OSCURO)
                    .setPaddingTop(3).setPaddingBottom(3)
                    .setMarginTop(8).setMarginBottom(0));

            // Tabla
            String[] headers = {"N°", "NOMBRE Y APELLIDOS", "MZ", "LT", "AREA", "CELULAR 1", "CELULAR 2"};
            Table t = new Table(UnitValue.createPercentArray(new float[]{0.05f, 0.30f, 0.08f, 0.10f, 0.12f, 0.17f, 0.18f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(2);

            // Header
            for (String h : headers) {
                t.addCell(new Cell()
                        .add(new Paragraph(h).setFont(courierBold).setFontSize(7.5f).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(COLOR_GRIS_HEADER)
                        .setPadding(3)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f)));
            }

            // Filas
            boolean alternate = false;
            DeviceRgb colorFila = new DeviceRgb(245, 248, 252);
            int numero = 1;
            for (ListaContratoDTO dto : entry.getValue()) {
                DeviceRgb bg = alternate ? colorFila : null;

                String nombre = dto.getNombreCliente1();
                if (dto.getNombreCliente2() != null && !dto.getNombreCliente2().isEmpty()) {
                    nombre += " / " + dto.getNombreCliente2();
                }

                StringBuilder mz = new StringBuilder();
                if (dto.getManzana() != null) mz.append(dto.getManzana());
                if (dto.getManzana2() != null && !dto.getManzana2().isEmpty()) {
                    mz.append("\n").append(dto.getManzana2());
                }

                StringBuilder lt = new StringBuilder();
                if (dto.getNumeroLote() != null) lt.append(dto.getNumeroLote());
                if (dto.getNumeroLote2() != null && !dto.getNumeroLote2().isEmpty()) {
                    lt.append("\n").append(dto.getNumeroLote2());
                }

                String area = dto.getAreaTotal() != null ? dto.getAreaTotal() + " m2" : "";

                t.addCell(celdaFilaCenter(String.valueOf(numero++), courier, bg));
                t.addCell(celdaFila(nombre, courier, bg));
                t.addCell(celdaFila(mz.toString(), courier, bg));
                t.addCell(celdaFila(lt.toString(), courier, bg));
                t.addCell(celdaFila(area, courier, bg));
                t.addCell(celdaFila(dto.getCelular1() != null ? dto.getCelular1() : "", courier, bg));
                t.addCell(celdaFila(dto.getCelular2() != null ? dto.getCelular2() : "", courier, bg));

                alternate = !alternate;
            }

            // Total programa
            Cell totalCell = new Cell(1, 7)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.RIGHT);
            totalCell.add(new Paragraph("TOTAL: " + entry.getValue().size() + " contratos").setFont(courierBold).setFontSize(8));
            t.addCell(totalCell);

            doc.add(t);
        }

        // Pie
        doc.add(new Paragraph("Generado por el Sistema de Gestion Inmobiliaria  |  " + empresa())
                .setFont(courier).setFontSize(7)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));

        doc.close();
        return out.toByteArray();
    }

    private static Table encabezado(PdfFont bold, PdfFont normal) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 0.3f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(0)
                .setMarginBottom(0);

        Cell izq = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setPaddingLeft(5)
                .setPaddingRight(5);
        izq.add(new Paragraph(empresa()).setFont(bold).setFontSize(9).setFontColor(COLOR_AZUL_OSCURO));
        t.addCell(izq);

        Cell der = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(0)
                .setPaddingBottom(0)
                .setPaddingLeft(5)
                .setPaddingRight(5);
        try {
            String url = EmpresaContext.empresaService.obtenerActiva().getLogoSmallUrl();
            if (url != null && !url.isBlank()) {
                com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(
                        com.itextpdf.io.image.ImageDataFactory.create(url));
                logo.scaleToFit(60, 60);
                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
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

    private static Cell celdaFila(String texto, PdfFont font, DeviceRgb bg) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.LEFT);
        if (bg != null) c.setBackgroundColor(bg);
        c.add(new Paragraph(texto != null ? texto : "").setFont(font).setFontSize(7.5f));
        return c;
    }

    private static Cell celdaFilaCenter(String texto, PdfFont font, DeviceRgb bg) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER);
        if (bg != null) c.setBackgroundColor(bg);
        c.add(new Paragraph(texto != null ? texto : "").setFont(font).setFontSize(7.5f));
        return c;
    }

    private static PdfFont cargarFuente(String path) throws Exception {
        try (var is = ReporteListaContratosPdf.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Fuente no encontrada: " + path);
            byte[] bytes = StreamUtil.inputStreamToArray(is);
            return PdfFontFactory.createFont(bytes, PdfEncodings.WINANSI);
        }
    }
}
