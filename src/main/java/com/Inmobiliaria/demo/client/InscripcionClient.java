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
 
    @PostMapping("/{idInscripcion}/pagos")
    Map<String, Object> registrarAbono(
            @PathVariable("idInscripcion") Integer idInscripcion,
            @RequestBody Map<String, Object> abono);
 
    @GetMapping("/{idInscripcion}/pagos/saldo")
    Map<String, Object> obtenerSaldo(@PathVariable("idInscripcion") Integer idInscripcion);
 
    @GetMapping("/{idInscripcion}/pagos")
    List<Map<String, Object>> listarAbonos(@PathVariable("idInscripcion") Integer idInscripcion);
 
    @GetMapping("/contratos-por-servicio")
    List<Integer> obtenerContratosPorServicio(@RequestParam("tipo") String tipo);
 
    /**
     * Devuelve un mapa: idContrato → lista de tipoServicio con estado ACTIVO/PENDIENTE_CONEXION.
     * Se usa para saber qué servicios ya tiene cada contrato.
     */
    @GetMapping("/resumen-servicios")
    Map<Integer, List<String>> obtenerResumenServicios();
 
    @DeleteMapping("/{id}")
    void eliminarInscripcion(@PathVariable("id") Integer id);
 
    /**
     * Endpoint existente: ingresos de un solo día (usado por el Dashboard).
     * GET /api/inscripciones/ingresos-diarios?fecha=2026-05-19
     */
    @GetMapping("/ingresos-diarios")
    Map<String, Object> obtenerIngresosDiarios(
            @RequestParam(value = "fecha", required = false) String fecha);
 
    @GetMapping("/pendientes/{idContrato}")
    List<Map<String, Object>> listarPendientesPorContrato(
            @PathVariable("idContrato") Integer idContrato);
 
    @GetMapping("/resumen-pendientes")
    Map<Integer, List<Map<String, Object>>> obtenerResumenPendientes();

    @PatchMapping("/{idInscripcion}/pagos/{idPagoInscripcion}/anular")
    Map<String, Object> anularAbono(
            @PathVariable("idInscripcion") Integer idInscripcion,
            @PathVariable("idPagoInscripcion") Long idPagoInscripcion,
            @RequestBody Map<String, Object> request);

    @GetMapping("/ingresos-rango")
    List<Map<String, Object>> obtenerIngresosPorRango(
            @RequestParam("desde") String desde,
            @RequestParam("hasta") String hasta);

    @GetMapping("/ingresos-por-mes")
    List<Map<String, Object>> obtenerIngresosPorMes(
            @RequestParam("desde") String desde,
            @RequestParam("hasta") String hasta);
}