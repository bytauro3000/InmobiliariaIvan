package com.Inmobiliaria.demo.service.impl;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.repository.VendedorRepository;
import com.Inmobiliaria.demo.service.VendedorService;

@Service
public class VendedorServiceImpl implements VendedorService{
	@Autowired
	VendedorRepository vendedorRepository;

	@Override
	public List<Vendedor> listarVendedores() {
		
		return vendedorRepository.findAll();
	}
}
