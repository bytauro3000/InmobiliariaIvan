package com.Inmobiliaria.demo.util;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ListaContratoDTO;

public class ListaContratosWord {

    private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }

    public static byte[] generar(List<ListaContratoDTO> lista) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            // Orientación vertical explícita
            CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
            CTPageSz pageSz = sectPr.addNewPgSz();
            pageSz.setOrient(STPageOrientation.PORTRAIT);
            pageSz.setW(java.math.BigInteger.valueOf(11906)); // A4 width in twips
            pageSz.setH(java.math.BigInteger.valueOf(16838)); // A4 height in twips

            // Encabezado empresa
            XWPFParagraph empresaPara = doc.createParagraph();
            empresaPara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun empresaRun = empresaPara.createRun();
            empresaRun.setBold(true);
            empresaRun.setFontSize(10);
            empresaRun.setText(empresa());

            // Espacio
            doc.createParagraph();

            // Titulo
            XWPFParagraph titulo = doc.createParagraph();
            titulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun runTitulo = titulo.createRun();
            runTitulo.setBold(true);
            runTitulo.setFontSize(14);
            runTitulo.setText("LISTA DE CONTRATOS");

            // Agrupar por programa
            Map<String, List<ListaContratoDTO>> porPrograma = new LinkedHashMap<>();
            for (ListaContratoDTO dto : lista) {
                String prog = dto.getNombrePrograma() != null ? dto.getNombrePrograma() : "SIN PROGRAMA";
                porPrograma.computeIfAbsent(prog, k -> new java.util.ArrayList<>()).add(dto);
            }

            int numero = 1;
            for (Map.Entry<String, List<ListaContratoDTO>> entry : porPrograma.entrySet()) {
                // Titulo del programa
                XWPFParagraph progPara = doc.createParagraph();
                progPara.setAlignment(ParagraphAlignment.LEFT);
                XWPFRun progRun = progPara.createRun();
                progRun.setBold(true);
                progRun.setFontSize(11);
                progRun.setText("PROGRAMA: " + entry.getKey());

                // Tabla
                XWPFTable table = doc.createTable();

                // Header
                XWPFTableRow headerRow = table.getRow(0);
                String[] headers = {"N°", "NOMBRE Y APELLIDOS", "MZ", "LT", "AREA", "CELULAR 1", "CELULAR 2"};
                for (int i = 0; i < headers.length; i++) {
                    XWPFTableCell cell = (i < headerRow.getTableCells().size())
                            ? headerRow.getTableCells().get(i)
                            : headerRow.addNewTableCell();
                    cell.setText(headers[i]);
                    cell.setColor("3C3C3C");
                }

                for (ListaContratoDTO dto : entry.getValue()) {
                    XWPFTableRow row = table.createRow();

                    row.getCell(0).setText(String.valueOf(numero++));

                    String nombre = dto.getNombreCliente1();
                    if (dto.getNombreCliente2() != null && !dto.getNombreCliente2().isEmpty()) {
                        nombre += " / " + dto.getNombreCliente2();
                    }
                    row.getCell(1).setText(nombre);

                    StringBuilder mz = new StringBuilder();
                    if (dto.getManzana() != null) mz.append(dto.getManzana());
                    if (dto.getManzana2() != null && !dto.getManzana2().isEmpty()) {
                        mz.append(" / ").append(dto.getManzana2());
                    }
                    row.getCell(2).setText(mz.toString());

                    StringBuilder lt = new StringBuilder();
                    if (dto.getNumeroLote() != null) lt.append(dto.getNumeroLote());
                    if (dto.getNumeroLote2() != null && !dto.getNumeroLote2().isEmpty()) {
                        lt.append(" / ").append(dto.getNumeroLote2());
                    }
                    row.getCell(3).setText(lt.toString());

                    String area = dto.getAreaTotal() != null ? dto.getAreaTotal() + " m2" : "";
                    row.getCell(4).setText(area);

                    row.getCell(5).setText(dto.getCelular1() != null ? dto.getCelular1() : "");
                    row.getCell(6).setText(dto.getCelular2() != null ? dto.getCelular2() : "");
                }

                // Espacio despues de cada tabla
                doc.createParagraph();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }
}
