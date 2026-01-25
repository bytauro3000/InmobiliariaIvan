package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ConsultaDniDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.service.ClienteService;
import com.Inmobiliaria.demo.service.ConsultaDniService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//COMPLETO
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService clienteService;
    private final ConsultaDniService consultaDniService;

    public ClienteController(ClienteService clienteService, ConsultaDniService consultaDniService) {
        this.clienteService = clienteService;
        this.consultaDniService = consultaDniService;
    }

    @GetMapping("/listar")
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
    }
    
    @GetMapping("/buscar/{id}")
    public Cliente obtenerClientePorId(@PathVariable Integer id) {
        return clienteService.buscarClientePorId(id);
    }
   
    @GetMapping("/buscar/numDoc/{numDoc}")
    public Cliente obtenerClientePorNumDoc(@PathVariable String numDoc) {
        return clienteService.buscarClientePorNumDoc(numDoc);
    }
    
    @GetMapping("/externo/reniec/{dni}")
    public ResponseEntity<ConsultaDniDTO> consultarReniec(@PathVariable String dni) {
        ConsultaDniDTO resultado = consultaDniService.buscarEnReniec(dni);
        
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        } else {
            // Si falla el API (limite excedido o error), devolvemos 404 para que Angular permita ingreso manual
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
  
 // 🔹 NUEVO ENDPOINT DE BÚSQUEDA DINÁMICA
    @GetMapping("/buscar/filtro")
    public List<Cliente> buscarClientes(
            @RequestParam("termino") String termino, 
            @RequestParam("tipo") String tipo) {
        
        if ("documento".equals(tipo)) {
            // Si el select en Angular es 'documento'
            return clienteService.buscarPorDocumento(termino);
        } else {
            // Por defecto busca por nombres y apellidos
            return clienteService.buscarPorNombresYApellidos(termino);
        }
    }

    @PostMapping("/agregar")
    public ResponseEntity<Cliente> agregarCliente(@RequestBody Cliente cliente) {
        Cliente nuevoCliente = clienteService.guardarCliente(cliente);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }
 
    @PutMapping("/actualizar/{id}")
    public Cliente actualizarCliente(@PathVariable Integer id, @RequestBody Cliente cliente) {
        cliente.setIdCliente(id);
        return clienteService.editarCliente(cliente);
    }
 
    @DeleteMapping("/eliminar/{id}")
    public void eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarClienteById(id);
    }

    // ✅ Nuevo método para manejar la excepción y devolver el mensaje al frontend
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        // Devuelve un código de estado 409 (Conflict) y el mensaje de la excepción
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }
}