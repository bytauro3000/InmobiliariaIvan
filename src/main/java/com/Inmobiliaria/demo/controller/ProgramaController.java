package com.Inmobiliaria.demo.controller;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.service.ProgramaService;

@RestController
@RequestMapping("/api/programas")
@CrossOrigin(origins = "http://localhost:4200") // 👈 habilita Angular
public class ProgramaController {

    @Autowired
    ProgramaService programaService;

    // Listar todos
    @GetMapping
    public ResponseEntity<List<Programa>> listadoProgramas() {
        return ResponseEntity.ok(programaService.listProgramas());
    }

    // Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<Programa> obtenerPrograma(@PathVariable Integer id) {
        return programaService.getProgramaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear
    @PostMapping
    public ResponseEntity<Programa> crearPrograma(@RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.savePrograma(programa));
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Programa> actualizarPrograma(@PathVariable Integer id, @RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.updatePrograma(id, programa));
    }

    // Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPrograma(@PathVariable Integer id) {
        programaService.deletePrograma(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/reporte-excel")
    public ResponseEntity<byte[]> exportarProgramasExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Programas");

            // 🔹 Estilo encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] columnas = {
                "ID", "Nombre Programa", "Ubicación", 
                "Área Total", "Precio x m²", "Costo Total", 
                "Parcelero", "Distrito"
            };

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            List<Programa> programas = programaService.listProgramas();
            int rowIdx = 1;
            for (Programa p : programas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getIdPrograma());
                row.createCell(1).setCellValue(p.getNombrePrograma());
                row.createCell(2).setCellValue(p.getUbicacion() != null ? p.getUbicacion() : "");
                row.createCell(3).setCellValue(p.getAreaTotal() != null ? p.getAreaTotal().doubleValue() : 0);
                row.createCell(4).setCellValue(p.getPrecioM2() != null ? p.getPrecioM2().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getCostoTotal() != null ? p.getCostoTotal().doubleValue() : 0);
                
                // 🔹 Parcelero (Nombres + Apellidos)
                row.createCell(6).setCellValue(
                    p.getParcelero() != null 
                        ? p.getParcelero().getNombres() + " " + p.getParcelero().getApellidos() 
                        : ""
                );

                // 🔹 Distrito
                row.createCell(7).setCellValue(
                    p.getDistrito() != null ? p.getDistrito().getNombre() : ""
                );
            }

            // Ajustar tamaño columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=programas.xlsx")
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
