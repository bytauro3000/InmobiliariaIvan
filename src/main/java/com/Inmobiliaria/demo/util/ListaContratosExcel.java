package com.Inmobiliaria.demo.util;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ListaContratoDTO;

public class ListaContratosExcel {

    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }

    public static byte[] generar(List<ListaContratoDTO> lista) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lista de Contratos");

            // Configurar página vertical
            sheet.getPrintSetup().setPaperSize((short) 9); // A4
            sheet.getPrintSetup().setLandscape(false);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            // Estilo encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Estilo centrado
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Estilo empresa (negrita, centrado)
            CellStyle empresaStyle = workbook.createCellStyle();
            Font empresaFont = workbook.createFont();
            empresaFont.setBold(true);
            empresaFont.setFontHeightInPoints((short) 10);
            empresaStyle.setFont(empresaFont);
            empresaStyle.setAlignment(HorizontalAlignment.LEFT);

            // Fila empresa
            Row empresaRow = sheet.createRow(0);
            Cell empresaCell = empresaRow.createCell(0);
            empresaCell.setCellValue(empresa());
            empresaCell.setCellStyle(empresaStyle);

            // Fila vacía
            sheet.createRow(1);

            // Fila título
            Row tituloRow = sheet.createRow(2);
            Cell tituloCell = tituloRow.createCell(0);
            tituloCell.setCellValue("LISTA DE CONTRATOS");
            CellStyle tituloStyle = workbook.createCellStyle();
            Font tituloFont = workbook.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 14);
            tituloStyle.setFont(tituloFont);
            tituloStyle.setAlignment(HorizontalAlignment.CENTER);
            tituloCell.setCellStyle(tituloStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 7));

            // Encabezados columna
            Row header = sheet.createRow(4);
            String[] columnas = {"N°", "NOMBRE Y APELLIDOS", "MZ", "LT", "AREA", "CELULAR 1", "CELULAR 2", "ESTADO"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Agrupar por programa
            Map<String, List<ListaContratoDTO>> porPrograma = new LinkedHashMap<>();
            for (ListaContratoDTO dto : lista) {
                String prog = dto.getNombrePrograma() != null ? dto.getNombrePrograma() : "SIN PROGRAMA";
                porPrograma.computeIfAbsent(prog, k -> new java.util.ArrayList<>()).add(dto);
            }

            int rowIdx = 5;
            int numero = 1;
            for (Map.Entry<String, List<ListaContratoDTO>> entry : porPrograma.entrySet()) {
                // Fila de programa
                Row progRow = sheet.createRow(rowIdx++);
                Cell progCell = progRow.createCell(0);
                progCell.setCellValue("PROGRAMA: " + entry.getKey());
                progCell.setCellStyle(headerStyle);

                for (ListaContratoDTO dto : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(numero++);

                    String nombre = dto.getNombreCliente1();
                    if (dto.getNombreCliente2() != null && !dto.getNombreCliente2().isEmpty()) {
                        nombre += "\n" + dto.getNombreCliente2();
                    }
                    row.createCell(1).setCellValue(nombre);

                    // MZ
                    StringBuilder mz = new StringBuilder();
                    if (dto.getManzana() != null) mz.append(dto.getManzana());
                    if (dto.getManzana2() != null && !dto.getManzana2().isEmpty()) {
                        mz.append("\n").append(dto.getManzana2());
                    }
                    Cell mzCell = row.createCell(2);
                    mzCell.setCellValue(mz.toString());
                    mzCell.setCellStyle(centerStyle);

                    // LT
                    StringBuilder lt = new StringBuilder();
                    if (dto.getNumeroLote() != null) lt.append(dto.getNumeroLote());
                    if (dto.getNumeroLote2() != null && !dto.getNumeroLote2().isEmpty()) {
                        lt.append("\n").append(dto.getNumeroLote2());
                    }
                    Cell ltCell = row.createCell(3);
                    ltCell.setCellValue(lt.toString());
                    ltCell.setCellStyle(centerStyle);

                    // Area
                    String area = dto.getAreaTotal() != null ? dto.getAreaTotal() + " m2" : "";
                    Cell areaCell = row.createCell(4);
                    areaCell.setCellValue(area);
                    areaCell.setCellStyle(centerStyle);

                    Cell cel1Cell = row.createCell(5);
                    cel1Cell.setCellValue(dto.getCelular1() != null ? dto.getCelular1() : "");
                    cel1Cell.setCellStyle(centerStyle);

                    Cell cel2Cell = row.createCell(6);
                    cel2Cell.setCellValue(dto.getCelular2() != null ? dto.getCelular2() : "");
                    cel2Cell.setCellStyle(centerStyle);
                    row.createCell(7).setCellValue(dto.getEstadoContrato() != null ? dto.getEstadoContrato() : "");
                }
            }

            // Auto-size columns
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
