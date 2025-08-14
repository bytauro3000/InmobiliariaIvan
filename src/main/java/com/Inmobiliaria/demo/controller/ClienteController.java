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
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // CREAR (POST)
    @PostMapping
    public ResponseEntity<Cliente> crearCliente(@RequestBody Cliente cliente) {
        Cliente nuevoCliente = clienteService.guardarCliente(cliente);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }

    // LISTAR TODOS (GET)
    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes() {
        List<Cliente> clientes = clienteService.listarClientes();
        return ResponseEntity.ok(clientes);
    }

    // ACTUALIZAR (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(
            @PathVariable Integer id,
            @RequestBody Cliente cliente
    ) {
        cliente.setIdCliente(id); // Asegurar que el ID coincida
        Cliente clienteActualizado = clienteService.editarCliente(cliente);
        return ResponseEntity.ok(clienteActualizado);
    }

    // ELIMINAR (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarClienteById(id);
        return ResponseEntity.noContent().build();
    }
}