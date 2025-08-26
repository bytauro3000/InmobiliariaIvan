package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.entity.Cliente;

public interface ClienteService {

	 Cliente guardarCliente(Cliente cliente);
	 Cliente editarCliente(Cliente cliente);
	 void eliminarClienteById(Integer idCliente);
	 List<Cliente> listarClientes();  
	    

	 Cliente buscarClientePorNumDoc(String numDoc);
	 List<Cliente> findByApellidos(String apellidos);
}
