package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.client.ReciboClient;
import com.Inmobiliaria.demo.dto.LecturaServicioDTO;
import com.Inmobiliaria.demo.dto.ReciboDTO;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway/recibos")
public class ReciboGatewayController {

    @Autowired
    private ReciboClient reciboClient;

    @Autowired
    private InscripcionClient inscripcionClient;

    @Autowired
    private ContratoRepository contratoRepository;

    @GetMapping("/preparar-planilla")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<List<LecturaServicioDTO>> prepararPlanilla(@RequestParam String tipo) {
        // 1. Obtener contratos inscritos
        List<Integer> idsContratos = inscripcionClient.obtenerContratosPorServicio(tipo);

        // 2. Unir con datos del Monolito
        List<LecturaServicioDTO> planilla = idsContratos.stream().map(id -> {
            Contrato contrato = contratoRepository.findById(id).orElse(null);
            if (contrato == null) return null;

            LecturaServicioDTO dto = new LecturaServicioDTO();
            dto.setIdContrato(id);
            
            // Nombre y ubicación
            var cliente = contrato.getClientes().get(0).getCliente();
            dto.setClienteNombre(cliente.getNombre() + " " + cliente.getApellidos());
            var lote = contrato.getLotes().get(0).getLote();
            dto.setManzana(lote.getManzana());
            dto.setLote(lote.getNumeroLote());

            // Buscar última lectura para ponerla como anterior
            try {
                List<ReciboDTO> historial = reciboClient.listarTodos();
                Double ultima = historial.stream()
                    .filter(r -> r.getIdContrato().equals(id) && r.getTipoServicio().equals(tipo))
                    .map(ReciboDTO::getLecturaActual)
                    .reduce((f, s) -> s).orElse(0.0);
                dto.setLecturaAnterior(ultima);
            } catch (Exception e) { dto.setLecturaAnterior(0.0); }

            return dto;
        }).filter(d -> d != null).collect(Collectors.toList());

        return ResponseEntity.ok(planilla);
    }

    @PostMapping("/guardar-planilla")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<?> guardarPlanilla(@RequestBody List<ReciboDTO> recibos) {
        try {
            for (ReciboDTO r : recibos) {
                r.setFechaGiro(LocalDate.now());
                reciboClient.registrarLectura(r);
            }
            return ResponseEntity.ok("Planilla procesada");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}