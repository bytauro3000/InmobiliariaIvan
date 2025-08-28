package com.Inmobiliaria.demo.service;

import java.util.List;
import java.util.Optional;

import com.Inmobiliaria.demo.entity.Vendedor;

public interface VendedorService {

	List<Vendedor> listarVendedores();
	  Optional<Vendedor> obtenerVendedorPorId(Integer id);
	  Vendedor guardarVendedor(Vendedor vendedor);
	  Vendedor actualizarVendedor(Integer id, Vendedor vendedor);
	  void eliminarVendedor(Integer id);
}
