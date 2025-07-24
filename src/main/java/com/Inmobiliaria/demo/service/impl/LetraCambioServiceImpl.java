package com.Inmobiliaria.demo.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoLetra;

import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.service.LetraCambioService;

import jakarta.transaction.Transactional;

@Service
public class LetraCambioServiceImpl implements LetraCambioService {

    @Autowired
    private LetraCambioRepository letraCambioRepository;
  

    @Override
    public List<LetraCambio> listarPorContrato(Integer idContrato) {
        return letraCambioRepository.findByContratoIdContrato(idContrato);
    }
    
    @Override
    @Transactional
    public void generarLetrasDesdeContrato(Contrato contrato, Distrito distrito, Date fechaGiro, Date fechaVencimientoInicial, Double importe, String importeLetras) {
        int cantidad = contrato.getCantidadLetras();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fechaVencimientoInicial);

        int diaVencimientoOriginal = calendar.get(Calendar.DAY_OF_MONTH); // Por ejemplo 31, 30, 28...

        for (int i = 1; i <= cantidad; i++) {
            // Corregir fecha para el mes actual
            int anio = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH); // 0 = enero, ..., 11 = diciembre
            int ultimoDiaDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            // Si el día original no cabe en el mes actual, usar el último día
            int diaAUsar = Math.min(diaVencimientoOriginal, ultimoDiaDelMes);
            calendar.set(Calendar.DAY_OF_MONTH, diaAUsar);

            LetraCambio letra = new LetraCambio();
            letra.setContrato(contrato);
            letra.setDistrito(distrito);
            letra.setFechaGiro(fechaGiro);
            letra.setFechaVencimiento(calendar.getTime());
            letra.setImporte(importe);
            letra.setImporteLetras(importeLetras);
            letra.setEstado(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);

            // Estos campos solo se llenan cuando el cliente a realizado el pago de su letra 
            letra.setFechaPago(null);
            letra.setTipoComprobante(null);
            letra.setNumeroComprobante(""); 
            letra.setObservaciones("");     
            letraCambioRepository.save(letra);

            // Sumar 1 mes para la siguiente letra
            calendar.add(Calendar.MONTH, 1);
        }
    }
    
}
