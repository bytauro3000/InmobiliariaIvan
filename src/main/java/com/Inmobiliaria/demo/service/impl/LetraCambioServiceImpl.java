package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.ContratoRepository; // Importa el repositorio del Contrato
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.service.LetraCambioService;

import jakarta.transaction.Transactional;

@Service
public class LetraCambioServiceImpl implements LetraCambioService {

    @Autowired
    private LetraCambioRepository letraCambioRepository;
    
    @Autowired
    private ContratoRepository contratoRepository; // Inyecta el repositorio del Contrato

    @Override
    public List<LetraCambio> listarPorContrato(Integer idContrato) {
        return letraCambioRepository.findByContratoIdContrato(idContrato);
    }
    
    @Override
    @Transactional
    public void generarLetrasDesdeContrato(Integer idContrato, Distrito distrito, Date fechaGiro, Date fechaVencimientoInicial, BigDecimal importe, String importeLetras) {
        // Busca la entidad Contrato por su ID dentro del servicio
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));
        
        int cantidad = contrato.getCantidadLetras();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fechaVencimientoInicial);

        int diaVencimientoOriginal = calendar.get(Calendar.DAY_OF_MONTH);

        for (int i = 1; i <= cantidad; i++) {
            int anio = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int ultimoDiaDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            int diaAUsar = Math.min(diaVencimientoOriginal, ultimoDiaDelMes);
            calendar.set(Calendar.DAY_OF_MONTH, diaAUsar);

            LetraCambio letra = new LetraCambio();
            letra.setContrato(contrato);
            letra.setDistrito(distrito);
            letra.setFechaGiro(fechaGiro);
            letra.setFechaVencimiento(calendar.getTime());
            letra.setImporte(importe);
            letra.setImporteLetras(importeLetras);
            letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);

            letra.setFechaPago(null);
            letra.setTipoComprobante(null);
            letra.setNumeroComprobante("");
            letra.setObservaciones("");
            letraCambioRepository.save(letra);

            calendar.add(Calendar.MONTH, 1);
        }
    }
}