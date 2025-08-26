package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.service.ProgramaService;

@RestController
@RequestMapping("/api/programas")
public class ProgramaController {
@Autowired
ProgramaService programaService;
	
	@GetMapping
	public ResponseEntity<List<Programa>> listadoProgramas(){
	return  ResponseEntity.ok(programaService.listProgramas());

	}
}
