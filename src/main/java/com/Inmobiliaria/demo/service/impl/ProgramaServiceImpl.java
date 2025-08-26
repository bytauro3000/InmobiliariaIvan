package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.repository.ProgramaRepository;
import com.Inmobiliaria.demo.service.ProgramaService;

@Service
public class ProgramaServiceImpl implements ProgramaService {
	@Autowired
	ProgramaRepository programaRepository;
	
	@Override
	public List<Programa> listProgramas() {
		return	programaRepository.findAll();
	}
}
