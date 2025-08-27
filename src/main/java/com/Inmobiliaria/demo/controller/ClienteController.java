package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
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
  
    @GetMapping("/buscar/apellidos/{apellidos}")
    public List<Cliente> obtenerClientesPorApellidos(@PathVariable String apellidos) {
        return clienteService.findByApellidos(apellidos);
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