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
        // ✅ Validar duplicados con el nuevo campo 'numDoc'
    	 if (cliente.getNumDoc() != null && clienteRepository.findByNumDoc(cliente.getNumDoc()) != null) {
             throw new IllegalArgumentException("Cliente ya Registrado.");
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
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }
    
    @Override
    public Cliente buscarClientePorId(Integer idCliente) {
        return clienteRepository.findById(idCliente).orElse(null);
    }
    
    @Override
    public List<Cliente> buscarPorNombresYApellidos(String filtro) {
        // Llama al método del repositorio que hace el CONCAT de nombre y apellidos
        return clienteRepository.buscarPorNombresYApellidos(filtro);
    }

    @Override
    public List<Cliente> buscarPorDocumento(String filtro) {
        // Llama al método del repositorio que busca solo en numDoc
        return clienteRepository.buscarPorDocumento(filtro);
    }

    @Override
    public Cliente buscarClientePorNumDoc(String numDoc) {
        // ✅ Implementar el nuevo método de la interfaz
        return clienteRepository.findByNumDoc(numDoc);
    }
}