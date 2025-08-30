package com.Inmobiliaria.demo.service.impl;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.DistritoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.service.LetraCambioService;
import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;

import jakarta.transaction.Transactional;

@Service
public class LetraCambioServiceImpl implements LetraCambioService {

    @Autowired
    private LetraCambioRepository letraCambioRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private DistritoRepository distritoRepository;
    
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<LetraCambioDTO> listarPorContrato(Integer idContrato) {
        List<LetraCambio> listaLetras = letraCambioRepository.findByContratoIdContrato(idContrato);
        
        // Usamos ModelMapper para hacer la conversión de una manera sencilla
        return listaLetras.stream()
            .map(letra -> modelMapper.map(letra, LetraCambioDTO.class))
            .collect(Collectors.toList());
    }
    
    
    @Override
    @Transactional
    public void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest) {
        // Buscar Contrato por ID
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));

        // Buscar Distrito por ID
        Distrito distrito = distritoRepository.findById(generarLetrasRequest.getIdDistrito())
            .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado con el ID: " + generarLetrasRequest.getIdDistrito()));

        // Convertir importe a BigDecimal
        BigDecimal importe;
        try {
            importe = new BigDecimal(generarLetrasRequest.getImporte().replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Importe inválido: " + generarLetrasRequest.getImporte());
        }

        // Lógica de cantidad de letras según contrato
        int cantidad = contrato.getCantidadLetras();

        // Inicialización de calendario para gestionar fechas
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(generarLetrasRequest.getFechaVencimientoInicial());

        // Guardar la fecha del primer vencimiento
        int diaVencimientoOriginal = calendar.get(Calendar.DAY_OF_MONTH);

        // Generar las letras de cambio
        for (int i = 1; i <= cantidad; i++) {
            int anio = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int ultimoDiaDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            // Ajustar día de vencimiento para evitar fecha inválida
            int diaAUsar = Math.min(diaVencimientoOriginal, ultimoDiaDelMes);
            calendar.set(Calendar.DAY_OF_MONTH, diaAUsar);

            // Crear nueva Letra de Cambio
            LetraCambio letra = new LetraCambio();
            letra.setContrato(contrato);
            letra.setDistrito(distrito);
            letra.setFechaGiro(generarLetrasRequest.getFechaGiro());
            letra.setFechaVencimiento(calendar.getTime());
            letra.setImporte(importe);
            letra.setImporteLetras(generarLetrasRequest.getImporteLetras());
            letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);

            // Campos adicionales, si se requieren
            letra.setFechaPago(null);
            letra.setTipoComprobante(null);
            letra.setNumeroComprobante("");
            letra.setObservaciones("");

            // Guardar la Letra de Cambio
            letraCambioRepository.save(letra);

            // Avanzar al siguiente mes
            calendar.add(Calendar.MONTH, 1);
        }
    }
}