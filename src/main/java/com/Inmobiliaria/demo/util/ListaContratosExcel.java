package com.Inmobiliaria.demo.util;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.Inmobiliaria.demo.dto.ListaContratoDTO;

public class ListaContratosExcel {

    public static byte[] generar(List<ListaContratoDTO> lista) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lista de Contratos");

            // Estilo encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Encabezados
            Row header = sheet.createRow(0);
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

            int rowIdx = 1;
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
                        nombre += " / " + dto.getNombreCliente2();
                    }
                    row.createCell(1).setCellValue(nombre);

                    // MZ
                    StringBuilder mz = new StringBuilder();
                    if (dto.getManzana() != null) mz.append(dto.getManzana());
                    if (dto.getManzana2() != null && !dto.getManzana2().isEmpty()) {
                        mz.append("\n").append(dto.getManzana2());
                    }
                    row.createCell(2).setCellValue(mz.toString());

                    // LT
                    StringBuilder lt = new StringBuilder();
                    if (dto.getNumeroLote() != null) lt.append(dto.getNumeroLote());
                    if (dto.getNumeroLote2() != null && !dto.getNumeroLote2().isEmpty()) {
                        lt.append("\n").append(dto.getNumeroLote2());
                    }
                    row.createCell(3).setCellValue(lt.toString());

                    // Area
                    String area = dto.getAreaTotal() != null ? dto.getAreaTotal() + " m2" : "";
                    row.createCell(4).setCellValue(area);

                    row.createCell(5).setCellValue(dto.getCelular1() != null ? dto.getCelular1() : "");
                    row.createCell(6).setCellValue(dto.getCelular2() != null ? dto.getCelular2() : "");
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
