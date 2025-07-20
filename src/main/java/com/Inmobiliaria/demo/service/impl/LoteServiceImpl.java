package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.repository.LoteRepository;
import com.Inmobiliaria.demo.service.LoteService;

@Service
public class LoteServiceImpl implements LoteService{
	@Autowired
	private LoteRepository loterepository;

	@Override
	public List<Lote> listarLotes() {
		return loterepository.findAll();
	}
	
	
}
