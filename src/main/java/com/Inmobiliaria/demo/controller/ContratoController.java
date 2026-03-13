package com.Inmobiliaria.demo.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.ContratoRequestDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;
import com.Inmobiliaria.demo.service.ContratoService;
import com.Inmobiliaria.demo.scheduler.ContratoEstadoScheduler;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private ContratoEstadoScheduler contratoEstadoScheduler;
   
    @PostMapping("/agregar")
    public ResponseEntity<ContratoResponseDTO> guardarContrato(
        @RequestBody ContratoRequestDTO requestDTO,
        Principal principal
    ) {
        ContratoResponseDTO contratoGuardado = contratoService.guardarContrato(requestDTO, principal);
        return new ResponseEntity<>(contratoGuardado, HttpStatus.CREATED);
    }
    
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<ContratoResponseDTO> actualizarContrato(
        @PathVariable Integer id,
        @RequestBody ContratoRequestDTO requestDTO
    ) {
        ContratoResponseDTO actualizado = contratoService.actualizarContrato(id, requestDTO);
        return new ResponseEntity<>(actualizado, HttpStatus.OK);
    }
    
    @GetMapping("/listar")
    public List<ContratoResponseDTO> listarContratos() {
        return contratoService.listarContratos();
    }
    
    //Buscar contrato por ID del contrato 
    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarContratoPorId(@PathVariable Integer id) {
        ContratoResponseDTO contrato = contratoService.buscarPorId(id);
        if (contrato != null) {
            return new ResponseEntity<>(contrato, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    //Buscar contrato por  PROGRAMA-MZ-LT
    @GetMapping("/buscar-por-lote")
    public ResponseEntity<ContratoResponseDTO> buscarPorLote(
            @RequestParam Integer idPrograma,
            @RequestParam String manzana,
            @RequestParam String numeroLote) {
        
        ContratoResponseDTO contrato = contratoService.buscarPorProgramaManzanaLote(idPrograma, manzana, numeroLote);
        return ResponseEntity.ok(contrato);
    }
    
    @GetMapping("/buscar-por-cliente")
    public List<ContratoResponseDTO> buscarPorCliente(@RequestParam String termino) {
        return contratoService.buscarPorNombreCliente(termino);
    }
    
    @DeleteMapping("/eliminar/{id}")
    public void eliminarContrato(@PathVariable Integer id) {
        contratoService.eliminarContrato(id);
    }

    /**
     * Cambia el estado del contrato manualmente (secretaria).
     * Transiciones válidas:
     *   MORA → CARTA_NOTARIAL → EN_RESOLUCION → RESUELTO
     *   ACTIVO/MORA → CANCELADO
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ContratoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam String estado) {
        ContratoResponseDTO actualizado = contratoService.cambiarEstado(id, estado);
        return ResponseEntity.ok(actualizado);
    }
    
    /**
     * Registra la renuncia voluntaria del cliente.
     * Libera el lote a Disponible y cancela letras pendientes.
     * Válido desde estado ACTIVO o MORA.
     */
    @PatchMapping("/{id}/renuncia")
    public ResponseEntity<ContratoResponseDTO> registrarRenuncia(@PathVariable Integer id) {
        ContratoResponseDTO resultado = contratoService.registrarRenuncia(id);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Registra la transferencia del contrato a otro cliente.
     * Marca el contrato como TRANSFERIDO y devuelve los datos calculados
     * (monto pagado como inicial sugerido, saldo restante, letras pendientes)
     * para pre-llenar el nuevo contrato.
     */
    @PatchMapping("/{id}/transferencia")
    public ResponseEntity<TransferenciaResponseDTO> registrarTransferencia(@PathVariable Integer id) {
        TransferenciaResponseDTO datos = contratoService.registrarTransferencia(id);
        return ResponseEntity.ok(datos);
    }

    // Endpoint de Seguridad Legal: Genera el archivo real
    @GetMapping("/{id}/imprimir")
    public ResponseEntity<byte[]> descargarContratoPdf(@PathVariable Integer id) {
        byte[] pdfBytes = contratoService.generarPdf(id);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Contrato_LaFlorida_" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    /**
     * Ejecuta el scheduler manualmente sin esperar las 6 AM.
     * Útil para pruebas y para forzar actualización de estados.
     */
    @PostMapping("/scheduler/ejecutar")
    public ResponseEntity<String> ejecutarScheduler() {
        contratoEstadoScheduler.ejecutarManualmente();
        return ResponseEntity.ok("Scheduler ejecutado. Revisa los logs de Spring Boot.");
    }
}