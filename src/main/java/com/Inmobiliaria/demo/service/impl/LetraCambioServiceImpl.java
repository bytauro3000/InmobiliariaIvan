package com.Inmobiliaria.demo.service.impl;

import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
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

        return listaLetras.stream()
            .map(letra -> {
                LetraCambioDTO dto = modelMapper.map(letra, LetraCambioDTO.class);
                Contrato contrato = letra.getContrato();
                
                if (contrato != null && contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                    ContratoCliente contratoCliente = contrato.getClientes().get(0);
                    if (contratoCliente.getCliente() != null) {
                        String nombreCompleto = contratoCliente.getCliente().getNombre() + " " + contratoCliente.getCliente().getApellidos();
                        dto.setNombreCliente(nombreCompleto.trim());
                    }
                }
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));

        Distrito distrito = distritoRepository.findById(generarLetrasRequest.getIdDistrito())
            .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado con el ID: " + generarLetrasRequest.getIdDistrito()));

        BigDecimal importe;
        try {
            importe = new BigDecimal(generarLetrasRequest.getImporte().replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Importe inválido: " + generarLetrasRequest.getImporte());
        }

        int cantidad = contrato.getCantidadLetras();

        // Usamos LocalDate en lugar de Calendar
        LocalDate fechaVencimientoInicial = generarLetrasRequest.getFechaVencimientoInicial();
        int diaVencimientoOriginal = fechaVencimientoInicial.getDayOfMonth();

        for (int i = 1; i <= cantidad; i++) {
            LocalDate fechaCalculada = fechaVencimientoInicial.plusMonths(i - 1);
            int ultimoDiaDelMes = fechaCalculada.lengthOfMonth();
            int diaAUsar = Math.min(diaVencimientoOriginal, ultimoDiaDelMes);
            LocalDate fechaFinal = fechaCalculada.withDayOfMonth(diaAUsar);

            LetraCambio letra = new LetraCambio();
            letra.setContrato(contrato);
            letra.setDistrito(distrito);
            letra.setFechaGiro(generarLetrasRequest.getFechaGiro()); // LocalDate
            letra.setFechaVencimiento(fechaFinal);
            letra.setImporte(importe);
            letra.setImporteLetras(generarLetrasRequest.getImporteLetras());
            letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);

            letra.setFechaPago(null);
            letra.setTipoComprobante(null);
            letra.setNumeroComprobante("");
            letra.setObservaciones("");

            letraCambioRepository.save(letra);
        }
    }
}
