package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.entity.Cliente;

public interface ClienteService {

    Cliente guardarCliente(Cliente cliente);
    Cliente actualizarCliente(Cliente cliente);
    Cliente buscarClientePorDni(String dni);
    Cliente buscarClientePorId(Integer idCliente);
    void eliminarClientePorId(Integer idCliente);
    List<Cliente> listarClientes();
}
