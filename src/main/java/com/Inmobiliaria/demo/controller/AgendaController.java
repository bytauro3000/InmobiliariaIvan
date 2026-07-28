package com.Inmobiliaria.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.entity.Agenda;
import com.Inmobiliaria.demo.enums.EstadoAgenda;
import com.Inmobiliaria.demo.service.AgendaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agendas")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping
    public ResponseEntity<List<Agenda>> listarTodos() {
        return ResponseEntity.ok(agendaService.listarTodos());
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<Agenda>> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendaService.listarPorFecha(fecha));
    }

    @GetMapping("/rango")
    public ResponseEntity<List<Agenda>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(agendaService.listarPorRango(inicio, fin));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Agenda>> listarPendientes() {
        return ResponseEntity.ok(agendaService.listarPendientes());
    }

    @GetMapping("/hoy")
    public ResponseEntity<List<Agenda>> listarHoy() {
        return ResponseEntity.ok(agendaService.listarPorFecha(LocalDate.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        Agenda agenda = agendaService.obtenerPorId(id);
        if (agenda == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "Evento no encontrado"));
        }
        return ResponseEntity.ok(agenda);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Agenda agenda,
            @RequestHeader(value = "X-Auth-User", required = false) String usuario) {
        try {
            Agenda nueva = agendaService.crear(agenda, usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Agenda agenda) {
        try {
            Agenda actualizada = agendaService.actualizar(id, agenda);
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            EstadoAgenda estado = EstadoAgenda.valueOf(body.get("estado"));
            Agenda actualizada = agendaService.cambiarEstado(id, estado);
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            agendaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}