package com.Inmobiliaria.demo.service.impl;
import java.util.List;
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
    	// Validación adicional (opcional): Evitar DNI duplicado
        if (cliente.getDni() != null && clienteRepository.findByDni(cliente.getDni()) != null) {
            throw new RuntimeException("El DNI ya está registrado");
        }
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente editarCliente(Cliente cliente) {      
        return clienteRepository.save(cliente);
    }

    @Override
    public void eliminarClienteById(Integer idCliente) {
        clienteRepository.deleteById(idCliente);
    }

    @Override
    public Cliente buscarClientePorDni(String dni) {
        return clienteRepository.buscarPorDni(dni);
    }

    @Override
    public List<Cliente> listarClientes() {
        return clienteRepository.findAllByOrderByIdClienteAsc();
    }

    @Override
    public List<Cliente> buscarClientesPorApellidos(String apellidos) {
        return clienteRepository.buscarPorApellidos(apellidos);
    }
}