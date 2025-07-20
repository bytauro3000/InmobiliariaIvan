package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.repository.ClienteRepository;
import com.Inmobiliaria.demo.service.ClienteService;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public Cliente guardarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente actualizarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente buscarClientePorDni(String dni) {
        return clienteRepository.buscarClientePorDni(dni);
    }

    @Override
    public Cliente buscarClientePorId(Integer idCliente) {
        Optional<Cliente> cliente = clienteRepository.findById(idCliente);
        return cliente.orElse(null);
    }

    @Override
    public void eliminarClientePorId(Integer idCliente) {
        clienteRepository.deleteById(idCliente);
    }

    @Override
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }
}
