package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.repository.ClienteRepository;
import com.Inmobiliaria.demo.service.ClienteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ClienteServiceImpl implements ClienteService {

    
    private final ClienteRepository clienteRepository;

    @Override
    public Cliente guardarCliente(Cliente cliente) { 
        //Validar duplicados con el nuevo campo 'numDoc'
    	 if (cliente.getNumDoc() != null && clienteRepository.findByNumDoc(cliente.getNumDoc()) != null) {
             throw new IllegalArgumentException("Cliente ya Registrado.");
        }
        
        return clienteRepository.save(cliente);
    }

    @Override
    @CacheEvict(value = "contratos", allEntries = true)
    public Cliente editarCliente(Cliente cliente) {
        clienteRepository.save(cliente);
        return clienteRepository.findByIdWithDistrito(cliente.getIdCliente());
    }

    @Override
    public void eliminarClienteById(Integer idCliente) {
        clienteRepository.deleteById(idCliente);
    }

    @Override
    public List<Cliente> listarClientes() {
    	return clienteRepository.findAllByOrderByIdClienteDesc();
    }

    @Override
    public Page<Cliente> listarClientesPaginado(Pageable pageable) {
        return clienteRepository.findAllPaginado(pageable);
    }
    
    @Override
    public Cliente buscarClientePorId(Integer idCliente) {
        return clienteRepository.findByIdWithDistrito(idCliente);
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
        //Implementar el nuevo método de la interfaz
        return clienteRepository.findByNumDoc(numDoc);
    }
}