package com.Inmobiliaria.demo.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;

public interface LetraCambioService {
	List<LetraCambio> listarPorContrato(Integer idContrato);
	// 👉 El método ahora recibe el ID del contrato en lugar de la entidad
		void generarLetrasDesdeContrato(Integer idContrato, Distrito distrito, Date fechaGiro, Date fechaVencimientoInicial, BigDecimal importe, String importeLetras);
}
