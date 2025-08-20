package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.service.ClienteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // 🔹 Listar todos los clientes
    @GetMapping("/listarClientes")
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
    }

    // 🔹 Buscar cliente por DNI
    @GetMapping("/obtenerClientePorDni/{dni}")
    public Cliente obtenerClientePorDni(@PathVariable String dni) {
        return clienteService.buscarClientePorDni(dni);
    }

    // 🔹 Buscar clientes por apellidos
    @GetMapping("/obtenerClientesPorApellidos/{apellidos}")
    public List<Cliente> obtenerClientesPorApellidos(@PathVariable String apellidos) {
        return clienteService.buscarClientesPorApellidos(apellidos);
    }

    // 🔹 Agregar nuevo cliente
    @PostMapping("/agregarCliente")
    public Cliente agregarCliente(@RequestBody Cliente cliente) {
        return clienteService.guardarCliente(cliente);
    }

    // 🔹 Actualizar cliente
    @PostMapping("/actualizarCliente/{id}")
    public Cliente actualizarCliente(@PathVariable Integer id, @RequestBody Cliente cliente) {
        cliente.setIdCliente(id);
        return clienteService.editarCliente(cliente);
    }

    // 🔹 Eliminar cliente
    @DeleteMapping("/eliminarCliente/{id}")
    public void eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarClienteById(id);
    }
}
