package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.entity.Cliente;

public interface ClienteService {

	 Cliente guardarCliente(Cliente cliente);
	 Cliente editarCliente(Cliente cliente);
	 void eliminarClienteById(Integer idCliente);
	 List<Cliente> listarClientes();  
	    
	 Cliente buscarClientePorId(Integer idCliente);
	 Cliente buscarClientePorNumDoc(String numDoc);
	// Nuevo método para buscar por apellidos y nombres concatenados
	 List<Cliente> buscarPorApellidosYNombres(String filtro);
}
