package com.Inmobiliaria.demo.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.ContratoRequestDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.service.ContratoService;


@RestController
@RequestMapping("/api/contratos")
@CrossOrigin(origins = "*")
public class ContratoController {

    @Autowired
    private ContratoService contratoService;
   
    @PostMapping("/agregar")
    public ResponseEntity<ContratoResponseDTO> guardarContrato(
        @RequestBody ContratoRequestDTO requestDTO,
        Principal principal
    ) {
        ContratoResponseDTO contratoGuardado = contratoService.guardarContrato(requestDTO, principal);
        return new ResponseEntity<>(contratoGuardado, HttpStatus.CREATED);
    }
    
    @GetMapping("/listar")
    public List<ContratoResponseDTO> listarContratos() {
        return contratoService.listarContratos();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarContratoPorId(@PathVariable Integer id) {
        ContratoResponseDTO contrato = contratoService.buscarPorId(id);
        if (contrato != null) {
            return new ResponseEntity<>(contrato, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/eliminar/{id}")
    public void eliminarContrato(@PathVariable Integer id) {
        contratoService.eliminarContrato(id);
    }
}