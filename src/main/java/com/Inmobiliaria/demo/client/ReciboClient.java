package com.Inmobiliaria.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.dto.ReciboDTO;

import java.util.List;

@FeignClient(name = "ms-servicios-basicos-recibos", url = "${microservice.servicios-basicos.url}/api/recibos")
public interface ReciboClient {

    @PostMapping("/registrar")
    ReciboDTO registrarLectura(@RequestBody ReciboDTO recibo);

    @GetMapping
    List<ReciboDTO> listarTodos();
    
    @GetMapping("/filtrar")
    List<ReciboDTO> filtrarPorMesYTipo(@RequestParam int mes, @RequestParam int anio, @RequestParam String tipoServicio);
    
    @GetMapping("/{id}")
    ReciboDTO obtenerPorId(@PathVariable Long id);
}