package com.Inmobiliaria.demo.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.Inmobiliaria.demo.entity.Cliente;

public interface ClienteService {

	 Cliente guardarCliente(Cliente cliente);
	 Cliente editarCliente(Cliente cliente);
	 void eliminarClienteById(Integer idCliente);
	 List<Cliente> listarClientes();  
	    
	 Cliente buscarClientePorId(Integer idCliente);
	 Cliente buscarClientePorNumDoc(String numDoc);
	
	// 🔹 Nuevos métodos para los filtros independientes
	 List<Cliente> buscarPorNombresYApellidos(String filtro);
	 List<Cliente> buscarPorDocumento(String filtro);

	 Page<Cliente> listarClientesPaginado(Pageable pageable);
}
