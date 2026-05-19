package com.Inmobiliaria.demo.client;

import com.Inmobiliaria.demo.dto.InscripcionServicioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-servicios-basicos", url = "${microservice.servicios-basicos.url}/api/inscripciones")
public interface InscripcionClient {

    @PostMapping("/crear")
    InscripcionServicioDTO crearInscripcion(@RequestBody InscripcionServicioDTO inscripcion);

    @GetMapping("/contratos-por-servicio")
    List<Integer> obtenerContratosPorServicio(@RequestParam("tipo") String tipo);

    // ── NUEVO ────────────────────────────────────────────────────────────────
    /**
     * Llama a GET /api/inscripciones/resumen-servicios del microservicio.
     * Devuelve un mapa { idContrato → ["LUZ", "AGUA"] } en una sola petición,
     * evitando dos llamadas separadas para obtener LUZ y AGUA.
     */
    @GetMapping("/resumen-servicios")
    Map<Integer, List<String>> obtenerResumenServicios();
}