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

import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.service.VendedorService;

@RestController
@RequestMapping("/api/vendedores")
@CrossOrigin(origins = "http://localhost:4200") // permite peticiones desde Angular
public class VendedorController {

    @Autowired
    private VendedorService vendedorService;

    // LISTAR TODOS
    @GetMapping
    public List<Vendedor> listar() {
        return vendedorService.listarVendedores();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Vendedor> obtenerPorId(@PathVariable Integer id) {
        return vendedorService.obtenerVendedorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREAR NUEVO
    @PostMapping
    public ResponseEntity<Vendedor> crear(@RequestBody Vendedor vendedor) {
        Vendedor nuevo = vendedorService.guardarVendedor(vendedor);
        return ResponseEntity.ok(nuevo);
    }

    // ACTUALIZAR EXISTENTE
    @PutMapping("/{id}")
    public ResponseEntity<Vendedor> actualizar(@PathVariable Integer id, @RequestBody Vendedor vendedor) {
        try {
            Vendedor actualizado = vendedorService.actualizarVendedor(id, vendedor);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        vendedorService.eliminarVendedor(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/reporte-excel")
    public ResponseEntity<byte[]> exportarVendedoresExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vendedores");

            // 🔹 Estilo encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] columnas = {
                "ID", "Nombre", "Apellidos", "DNI", "Celular",
                "Email", "Dirección", "Nacimiento", "Género",
                "Comisión", "Distrito"
            };

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // 🔹 Obtener datos
            List<Vendedor> vendedores = vendedorService.listarVendedores();
            int rowIdx = 1;
            for (Vendedor v : vendedores) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(v.getIdVendedor());
                row.createCell(1).setCellValue(v.getNombre());
                row.createCell(2).setCellValue(v.getApellidos());
                row.createCell(3).setCellValue(v.getDni());
                row.createCell(4).setCellValue(v.getCelular() != null ? v.getCelular() : "");
                row.createCell(5).setCellValue(v.getEmail() != null ? v.getEmail() : "");
                row.createCell(6).setCellValue(v.getDireccion() != null ? v.getDireccion() : "");
                row.createCell(7).setCellValue(v.getFechaNacimiento() != null ? v.getFechaNacimiento().toString() : "");
                row.createCell(8).setCellValue(v.getGenero());
                row.createCell(9).setCellValue(v.getComision() != null ? v.getComision().doubleValue() : 0);
                row.createCell(10).setCellValue(v.getDistrito() != null ? v.getDistrito().getNombre() : "");
            }

            // 🔹 Ajustar columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=vendedores.xlsx")
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    
    

}
