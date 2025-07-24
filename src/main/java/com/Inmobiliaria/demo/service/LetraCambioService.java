package com.Inmobiliaria.demo.service;

import java.util.Date;
import java.util.List;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;

public interface LetraCambioService {
	List<LetraCambio> listarPorContrato(Integer idContrato);
	void generarLetrasDesdeContrato(Contrato contrato, Distrito distrito, Date fechaGiro, Date fechaVencimientoInicial, Double importe, String importeLetras);
}
