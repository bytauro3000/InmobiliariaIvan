package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*") // 🔥 Permite llamadas desde frontend (React, Angular, etc.)
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // ✅ CREAR CLIENTE (POST)
    @PostMapping
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente) {
        try {
            Cliente nuevoCliente = clienteService.guardarCliente(cliente);
            return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // ✅ LISTAR TODOS LOS CLIENTES (GET)
    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes() {
        List<Cliente> clientes = clienteService.listarClientes();
        return ResponseEntity.ok(clientes);
    }

    // ✅ BUSCAR CLIENTE POR DNI (GET)
    @GetMapping("/dni/{dni}")
    public ResponseEntity<?> buscarPorDni(@PathVariable String dni) {
        Cliente cliente = clienteService.buscarClientePorDni(dni);
        if (cliente == null) {
            return new ResponseEntity<>("Cliente no encontrado", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(cliente);
    }

    // ✅ BUSCAR CLIENTES POR APELLIDOS (GET)
    @GetMapping("/apellidos/{apellidos}")
    public ResponseEntity<List<Cliente>> buscarPorApellidos(@PathVariable String apellidos) {
        List<Cliente> clientes = clienteService.buscarClientesPorApellidos(apellidos);
        return ResponseEntity.ok(clientes);
    }

    // ✅ ACTUALIZAR CLIENTE (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(@PathVariable Integer id, @RequestBody Cliente cliente) {
        cliente.setIdCliente(id);
        Cliente clienteActualizado = clienteService.editarCliente(cliente);
        return ResponseEntity.ok(clienteActualizado);
    }

    // ✅ ELIMINAR CLIENTE (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarClienteById(id);
        return ResponseEntity.ok("Cliente eliminado correctamente");
    }
}
