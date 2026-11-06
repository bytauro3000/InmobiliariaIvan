package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.IngresoDetalladoItemDTO;
import com.Inmobiliaria.demo.dto.ReporteIngresosDetalladoDTO;
import com.Inmobiliaria.demo.dto.TotalesMonedaDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Genera el Excel del reporte de INGRESOS DETALLADO (BOLETA/RECIBO por rango
 * de fechas). Clase utilitaria estática, igual que ListaContratosExcel.
 *
 * Los datos de la empresa (Ivan / Merruic) se leen desde EmpresaContext, por
 * lo que el mismo código sirve para ambos despliegues.
 */
public class IngresosDetalladosExcel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }

    public static byte[] generar(ReporteIngresosDetalladoDTO dto) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ingreso Detallado");

            // Configurar página horizontal
            sheet.getPrintSetup().setPaperSize((short) 9); // A4
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            DataFormat dataFormat = workbook.createDataFormat();

            // ── Estilos ───────────────────────────────────────────────────────
            CellStyle empresaStyle = workbook.createCellStyle();
            Font empresaFont = workbook.createFont();
            empresaFont.setBold(true);
            empresaFont.setFontHeightInPoints((short) 10);
            empresaStyle.setFont(empresaFont);
            empresaStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle tituloStyle = workbook.createCellStyle();
            Font tituloFont = workbook.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 14);
            tituloStyle.setFont(tituloFont);
            tituloStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle filtroStyle = workbook.createCellStyle();
            Font filtroFont = workbook.createFont();
            filtroFont.setFontHeightInPoints((short) 10);
            filtroStyle.setFont(filtroFont);
            filtroStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // Variantes para filas anuladas (gris)
            CellStyle anuladoTextoStyle = workbook.createCellStyle();
            anuladoTextoStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            anuladoTextoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle anuladoCenterStyle = workbook.createCellStyle();
            anuladoCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            anuladoCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            anuladoCenterStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            anuladoCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle anuladoMoneyStyle = workbook.createCellStyle();
            anuladoMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            anuladoMoneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            anuladoMoneyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            anuladoMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle totalesLabelStyle = workbook.createCellStyle();
            Font totalesFont = workbook.createFont();
            totalesFont.setBold(true);
            totalesLabelStyle.setFont(totalesFont);

            CellStyle totalesMoneyStyle = workbook.createCellStyle();
            totalesMoneyStyle.setFont(totalesFont);
            totalesMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalesMoneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // ── Filas de encabezado ───────────────────────────────────────────
            int numCols = 7;

            Row empresaRow = sheet.createRow(0);
            empresaRow.createCell(0).setCellValue(empresa());
            empresaRow.getCell(0).setCellStyle(empresaStyle);

            sheet.createRow(1); // vacía

            Row tituloRow = sheet.createRow(2);
            tituloRow.createCell(0).setCellValue("REPORTE DE INGRESOS DETALLADO");
            tituloRow.getCell(0).setCellStyle(tituloStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, numCols - 1));

            String tipoTexto = switch (dto.getTipoFiltro() != null ? dto.getTipoFiltro() : "TODOS") {
                case "BOLETA" -> "BOLETAS";
                case "RECIBO" -> "RECIBOS";
                default       -> "BOLETAS Y RECIBOS";
            };
            Row filtroRow = sheet.createRow(3);
            filtroRow.createCell(0).setCellValue(
                    "Período: " + dto.getFechaDesde().format(FMT) + " al " + dto.getFechaHasta().format(FMT)
                            + "   |   Comprobantes: " + tipoTexto
                            + "   |   Emitidos: " + dto.getCantidadTotal()
                            + " (anulados: " + dto.getCantidadAnuladas() + ")");
            filtroRow.getCell(0).setCellStyle(filtroStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, numCols - 1));

            sheet.createRow(4); // vacía

            // ── Cabeceras ─────────────────────────────────────────────────────
            Row header = sheet.createRow(5);
            String[] columnas = {"NRO. CPE", "RECEPTOR", "IMPORTE TOTAL", "MONEDA",
                    "FECHA DE EMISIÓN", "NOTA DE CRÉDITO", "ESTADO"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Filas de datos ────────────────────────────────────────────────
            int rowIdx = 6;
            if (dto.getDetalle() != null) {
                for (IngresoDetalladoItemDTO item : dto.getDetalle()) {
                    boolean anulado = !"VIGENTE".equals(item.getEstado());
                    CellStyle cenStyle = anulado ? anuladoCenterStyle : centerStyle;
                    CellStyle moneyStl = anulado ? anuladoMoneyStyle  : moneyStyle;

                    Row row = sheet.createRow(rowIdx++);

                    Cell cpe = row.createCell(0);
                    cpe.setCellValue(item.getNumeroCompleto());
                    if (anulado) cpe.setCellStyle(anuladoTextoStyle);

                    Cell rec = row.createCell(1);
                    rec.setCellValue(item.getReceptor() != null ? item.getReceptor() : "-");
                    if (anulado) rec.setCellStyle(anuladoTextoStyle);

                    Cell imp = row.createCell(2);
                    imp.setCellValue(item.getMonto() != null ? item.getMonto().doubleValue() : 0);
                    imp.setCellStyle(moneyStl);

                    Cell mon = row.createCell(3);
                    mon.setCellValue(item.getMoneda() != null ? item.getMoneda() : "N/D");
                    mon.setCellStyle(cenStyle);

                    Cell fec = row.createCell(4);
                    fec.setCellValue(item.getFechaEmision() != null ? item.getFechaEmision().format(FMT) : "");
                    fec.setCellStyle(cenStyle);

                    Cell nc = row.createCell(5);
                    nc.setCellValue(item.getNcNumeroCompleto() != null ? item.getNcNumeroCompleto() : "");
                    nc.setCellStyle(cenStyle);

                    Cell est = row.createCell(6);
                    est.setCellValue(item.getEstado());
                    est.setCellStyle(cenStyle);
                }
            }

            if (dto.getDetalle() == null || dto.getDetalle().isEmpty()) {
                Row vacia = sheet.createRow(rowIdx++);
                vacia.createCell(0).setCellValue("No se encontraron comprobantes en el rango indicado.");
            }

            // ── Resumen por moneda ────────────────────────────────────────────
            rowIdx++; // fila vacía

            Row resumenTitulo = sheet.createRow(rowIdx++);
            resumenTitulo.createCell(0).setCellValue("RESUMEN POR MONEDA");
            resumenTitulo.getCell(0).setCellStyle(totalesLabelStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    rowIdx - 1, rowIdx - 1, 0, numCols - 1));

            Row resumenHeader = sheet.createRow(rowIdx++);
            String[] colResumen = {"MONEDA", "BRUTO", "ANULADO", "NETO"};
            for (int i = 0; i < colResumen.length; i++) {
                Cell cell = resumenHeader.createCell(i);
                cell.setCellValue(colResumen[i]);
                cell.setCellStyle(headerStyle);
            }

            if (dto.getTotales() != null) {
                for (TotalesMonedaDTO t : dto.getTotales()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(t.getMoneda());
                    row.getCell(0).setCellStyle(totalesLabelStyle);

                    Cell bruto = row.createCell(1);
                    bruto.setCellValue(t.getBruto() != null ? t.getBruto().doubleValue() : 0);
                    bruto.setCellStyle(totalesMoneyStyle);

                    Cell anulada = row.createCell(2);
                    anulada.setCellValue(t.getAnulada() != null ? t.getAnulada().doubleValue() : 0);
                    anulada.setCellStyle(totalesMoneyStyle);

                    Cell neto = row.createCell(3);
                    neto.setCellValue(t.getNeto() != null ? t.getNeto().doubleValue() : 0);
                    neto.setCellStyle(totalesMoneyStyle);
                }
            }

            // ── Auto-size ─────────────────────────────────────────────────────
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
