package com.Inmobiliaria.demo.client;

import com.Inmobiliaria.demo.dto.InscripcionServicioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-servicios-basicos", url = "${microservice.servicios-basicos.url}/api/inscripciones")
public interface InscripcionClient {

    @PostMapping("/crear")
    InscripcionServicioDTO crearInscripcion(@RequestBody InscripcionServicioDTO inscripcion);

    @GetMapping("/contratos-por-servicio")
    List<Integer> obtenerContratosPorServicio(@RequestParam("tipo") String tipo);

    @GetMapping("/resumen-servicios")
    Map<Integer, List<String>> obtenerResumenServicios();

    /**
     * Elimina una inscripción en el microservicio por su ID.
     * El microservicio devuelve 204 No Content si fue exitoso.
     */
    @DeleteMapping("/{id}")
    void eliminarInscripcion(@PathVariable("id") Integer id);

    /**
     * Obtiene el total de ingresos por inscripciones de servicios básicos del día.
     *
     * @param fecha Fecha en formato yyyy-MM-dd (opcional, por defecto hoy en el microservicio).
     * @return Map con: totalMonto (BigDecimal), cantidad (long), fecha (String).
     */
    @GetMapping("/ingresos-diarios")
    Map<String, Object> obtenerIngresosDiarios(@RequestParam(value = "fecha", required = false) String fecha);
}