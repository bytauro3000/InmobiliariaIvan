package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ReciboConClienteDTO;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import com.Inmobiliaria.demo.config.EmpresaContext;

public class ReciboServiciosPdfGenerator {

    private static String empresaNombre() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
    private static String empresaDireccion() { return EmpresaPdfUtil.direccionCompleta(); }
    private static String empresaTelefono() { return "Telf.: " + EmpresaContext.empresaService.obtenerActiva().getTelefono(); }
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    // Clase interna para agrupar las fuentes
    private static class Fuentes {
        PdfFont normal;
        PdfFont bold;
        Fuentes(PdfFont normal, PdfFont bold) {
            this.normal = normal;
            this.bold = bold;
        }
    }

    // Método que carga las fuentes cada vez que se invoca
    private static Fuentes cargarFuentes() {
        try {
            byte[] nBytes = ReciboServiciosPdfGenerator.class.getClassLoader()
                    .getResourceAsStream("fonts/ARIAL.TTF").readAllBytes();
            PdfFont normal = PdfFontFactory.createFont(nBytes, "Cp1252");
            byte[] bBytes = ReciboServiciosPdfGenerator.class.getClassLoader()
                    .getResourceAsStream("fonts/ARIALBD.TTF").readAllBytes();
            PdfFont bold = PdfFontFactory.createFont(bBytes, "Cp1252");
            return new Fuentes(normal, bold);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando fuentes para recibos", e);
        }
    }

    public static byte[] generarRecibo(ReciboConClienteDTO recibo) {
        if ("LUZ".equalsIgnoreCase(recibo.getTipoServicio())) {
            return generarReciboLuz(recibo);
        } else {
            return generarReciboAgua(recibo);
        }
    }

    private static byte[] generarReciboLuz(ReciboConClienteDTO recibo) {
        Fuentes fuentes = cargarFuentes(); // Fuentes frescas
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A5);
        document.setMargins(20, 20, 20, 20);

        // Encabezado
        Paragraph empresa = new Paragraph(empresaNombre())
                .setFont(fuentes.bold)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(empresa);

        Paragraph servicio = new Paragraph("SERVICIO DE LUZ")
                .setFont(fuentes.bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2);
        document.add(servicio);

        Paragraph direccion = new Paragraph(empresaDireccion())
                .setFont(fuentes.normal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(direccion);

        Paragraph telefono = new Paragraph(empresaTelefono())
                .setFont(fuentes.normal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(telefono);

        // Datos del usuario
        Table datosUsuario = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addRow(datosUsuario, "NOMBRE DEL USUARIO:", recibo.getNombreCliente(), fuentes.bold, fuentes.normal);
        addRow(datosUsuario, "DIRECCION DE SUMINISTRO:", "Mz. " + recibo.getManzana() + " Lte. " + recibo.getLote(), fuentes.bold, fuentes.normal);
        addRow(datosUsuario, "Programa:", recibo.getNombrePrograma() != null ? recibo.getNombrePrograma() : "", fuentes.bold, fuentes.normal);
        document.add(datosUsuario);

        // Fechas
        Table fechas = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addRow(fechas, "FECHA DE EMISION:", recibo.getFechaGiro() != null ? recibo.getFechaGiro().toString() : "", fuentes.bold, fuentes.normal);
        addRow(fechas, "FECHA DE VENCIMIENTO:", recibo.getFechaVencimiento() != null ? recibo.getFechaVencimiento().toString() : "", fuentes.bold, fuentes.normal);
        document.add(fechas);

        // Información general
        document.add(new Paragraph("INFORMACION GENERAL:")
                .setFont(fuentes.bold)
                .setFontSize(10)
                .setMarginBottom(5));

        Paragraph programa = new Paragraph("PROGRAMA: " + (recibo.getNombrePrograma() != null ? recibo.getNombrePrograma() : ""))
                .setFont(fuentes.normal)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(programa);

        // Tabla de lecturas
        Table lecturas = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addHeaderCell(lecturas, "Lectura anterior", fuentes.bold);
        addHeaderCell(lecturas, "Lectura actual", fuentes.bold);
        addHeaderCell(lecturas, "Consumo KWH", fuentes.bold);
        addCell(lecturas, recibo.getLecturaAnterior() != null ? recibo.getLecturaAnterior().toString() : "", fuentes.normal);
        addCell(lecturas, recibo.getLecturaActual() != null ? recibo.getLecturaActual().toString() : "", fuentes.normal);
        addCell(lecturas, recibo.getConsumoMes() != null ? recibo.getConsumoMes().toString() : "", fuentes.normal);
        document.add(lecturas);

        // Importe total
        Paragraph importe = new Paragraph("Importe total a pagar: S/ " + df.format(recibo.getImporteTotal()))
                .setFont(fuentes.bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(15);
        document.add(importe);

        // Información adicional
        document.add(new Paragraph("PARA TENER EN CUENTA")
                .setFont(fuentes.bold)
                .setFontSize(10)
                .setMarginBottom(5));

        String[] puntos = {
                "PAGO DE RECIBOS:",
                "- Exija sello y/o comprobante de pago al cancelar su recibo.",
                "- Si paga por Internet, tome nota del número de operación y por precaución imprima el comprobante o Voucher.",
                "- Todo recibo cancelado fuera de fecha, genera moras calculadas con la tasa Activa Moneda Nacional.",
                "- De no cancelar su deuda morosa ésta será ingresada a las Centrales de Riesgo.",
                "CORTE Y RECONEXIÓN:",
                "- LA EMPRESA INMOBILIARIA le suspenderá el servicio ante la falta de pago de la tarifa de dos (02) meses, o de una (1) cuota del Acuerdo de Financiamiento."
        };
        for (String punto : puntos) {
            document.add(new Paragraph(punto).setFont(fuentes.normal).setFontSize(8).setMarginLeft(10).setMarginBottom(2));
        }

        document.close();
        return baos.toByteArray();
    }

    private static byte[] generarReciboAgua(ReciboConClienteDTO recibo) {
        Fuentes fuentes = cargarFuentes(); // Fuentes frescas
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A5);
        document.setMargins(20, 20, 20, 20);

        // Encabezado
        Paragraph empresa = new Paragraph(empresaNombre())
                .setFont(fuentes.bold)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(empresa);

        Paragraph servicio = new Paragraph("SERVICIO DE AGUA POTABLE")
                .setFont(fuentes.bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2);
        document.add(servicio);

        Paragraph direccion = new Paragraph(empresaDireccion())
                .setFont(fuentes.normal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(direccion);

        Paragraph telefono = new Paragraph(empresaTelefono())
                .setFont(fuentes.normal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(telefono);

        // Datos del usuario
        Table datosUsuario = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addRow(datosUsuario, "NOMBRE DEL USUARIO:", recibo.getNombreCliente(), fuentes.bold, fuentes.normal);
        addRow(datosUsuario, "DIRECCION DE SUMINISTRO:", "Mz. " + recibo.getManzana() + " Lte. " + recibo.getLote(), fuentes.bold, fuentes.normal);
        addRow(datosUsuario, "Programa:", recibo.getNombrePrograma() != null ? recibo.getNombrePrograma() : "", fuentes.bold, fuentes.normal);
        document.add(datosUsuario);

        // Fechas
        Table fechas = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addRow(fechas, "FECHA DE EMISION:", recibo.getFechaGiro() != null ? recibo.getFechaGiro().toString() : "", fuentes.bold, fuentes.normal);
        addRow(fechas, "FECHA DE VENCIMIENTO:", recibo.getFechaVencimiento() != null ? recibo.getFechaVencimiento().toString() : "", fuentes.bold, fuentes.normal);
        document.add(fechas);

        // Información general
        document.add(new Paragraph("INFORMACION GENERAL:")
                .setFont(fuentes.bold)
                .setFontSize(10)
                .setMarginBottom(5));

        Paragraph programa = new Paragraph("PROGRAMA: " + (recibo.getNombrePrograma() != null ? recibo.getNombrePrograma() : ""))
                .setFont(fuentes.normal)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(programa);

        // Tabla de lecturas
        Table lecturas = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);
        addHeaderCell(lecturas, "Lectura anterior", fuentes.bold);
        addHeaderCell(lecturas, "Lectura actual", fuentes.bold);
        addHeaderCell(lecturas, "Consumo m³", fuentes.bold);
        addCell(lecturas, recibo.getLecturaAnterior() != null ? recibo.getLecturaAnterior().toString() : "", fuentes.normal);
        addCell(lecturas, recibo.getLecturaActual() != null ? recibo.getLecturaActual().toString() : "", fuentes.normal);
        addCell(lecturas, recibo.getConsumoMes() != null ? recibo.getConsumoMes().toString() : "", fuentes.normal);
        document.add(lecturas);

        // Importe total
        Paragraph importe = new Paragraph("Importe total a pagar: S/ " + df.format(recibo.getImporteTotal()))
                .setFont(fuentes.bold)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(15);
        document.add(importe);

        // Información adicional
        document.add(new Paragraph("PARA TENER EN CUENTA")
                .setFont(fuentes.bold)
                .setFontSize(10)
                .setMarginBottom(5));

        String[] puntos = {
                "PAGO DE RECIBOS:",
                "- Exija sello y/o comprobante de pago al cancelar su recibo.",
                "- Si paga por Internet, tome nota del número de operación y por precaución imprima el comprobante o Voucher.",
                "- Todo recibo cancelado fuera de fecha, genera moras calculadas con la tasa Activa Moneda Nacional.",
                "- De no cancelar su deuda morosa ésta será ingresada a las Centrales de Riesgo.",
                "CORTE Y RECONEXIÓN:",
                "- LA EMPRESA INMOBILIARIA le suspenderá el servicio ante el atrasó de un mes de pago.",
                "- En caso de corte se cobrará reconexión."
        };
        for (String punto : puntos) {
            document.add(new Paragraph(punto).setFont(fuentes.normal).setFontSize(8).setMarginLeft(10).setMarginBottom(2));
        }

        document.close();
        return baos.toByteArray();
    }

    // Métodos auxiliares con fuentes como parámetros
    private static void addRow(Table table, String label, String value, PdfFont bold, PdfFont normal) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(8)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "").setFont(normal).setFontSize(8)).setBorder(null));
    }

    private static void addHeaderCell(Table table, String text, PdfFont bold) {
        table.addCell(new Cell().add(new Paragraph(text).setFont(bold).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                .setBorder(null).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
    }

    private static void addCell(Table table, String text, PdfFont normal) {
        table.addCell(new Cell().add(new Paragraph(text).setFont(normal).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                .setBorder(null));
    }
}