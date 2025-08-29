package com.Inmobiliaria.demo.controller;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.entity.Parcelero;
import com.Inmobiliaria.demo.service.ParceleroService;

@RestController
@RequestMapping("/api/parceleros")
@CrossOrigin(origins = "http://localhost:4200")
public class ParceleroController {

    @Autowired
    private ParceleroService parceleroService;

    // LISTAR TODOS
    @GetMapping
    public List<Parcelero> listar() {
        return parceleroService.listarParceleros();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Parcelero> obtenerPorId(@PathVariable Integer id) {
        return parceleroService.obtenerParceleroPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREAR NUEVO
    @PostMapping
    public ResponseEntity<Parcelero> crear(@RequestBody Parcelero parcelero) {
        Parcelero nuevo = parceleroService.guardarParcelero(parcelero);
        return ResponseEntity.ok(nuevo);
    }

    // ACTUALIZAR EXISTENTE
    @PutMapping("/{id}")
    public ResponseEntity<Parcelero> actualizar(@PathVariable Integer id, @RequestBody Parcelero parcelero) {
        try {
            Parcelero actualizado = parceleroService.actualizarParcelero(id, parcelero);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        parceleroService.eliminarParcelero(id);
        return ResponseEntity.noContent().build();
    }

    // 📊 EXPORTAR EXCEL
    @GetMapping("/reporte-excel")
    public ResponseEntity<byte[]> exportarParcelerosExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parceleros");

            // 🔹 Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] columnas = {
                "ID", "Nombres", "Apellidos", "DNI", 
                "Celular", "Dirección", "Email", "Distrito"
            };

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            List<Parcelero> parceleros = parceleroService.listarParceleros();
            int rowIdx = 1;
            for (Parcelero p : parceleros) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getIdParcelero());
                row.createCell(1).setCellValue(p.getNombres());
                row.createCell(2).setCellValue(p.getApellidos());
                row.createCell(3).setCellValue(p.getDni());
                row.createCell(4).setCellValue(p.getCelular() != null ? p.getCelular() : "");
                row.createCell(5).setCellValue(p.getDireccion() != null ? p.getDireccion() : "");
                row.createCell(6).setCellValue(p.getEmail() != null ? p.getEmail() : "");
                row.createCell(7).setCellValue(
                    p.getDistrito() != null ? p.getDistrito().getNombre() : ""
                );
            }

            // Ajustar columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=parceleros.xlsx")
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
