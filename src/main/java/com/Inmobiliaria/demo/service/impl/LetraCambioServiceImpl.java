package com.Inmobiliaria.demo.service.impl;

import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import com.Inmobiliaria.demo.util.NumeroALetrasUtil;
import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;

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
    @Transactional
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
    public List<ReporteLetraCambioDTO> obtenerReportePorContrato(Integer idContrato) {
        List<Object[]> results = letraCambioRepository.obtenerReportePorContrato(idContrato);
        
        List<ReporteLetraCambioDTO> reportes = new ArrayList<>();
        for (Object[] row : results) {
            ReporteLetraCambioDTO dto = new ReporteLetraCambioDTO();
            dto.setNumeroLetra((String) row[0]);

            // Si la fecha es de tipo java.sql.Date, convertirla a LocalDate
            java.sql.Date sqlFechaGiro = (java.sql.Date) row[1];
            LocalDate fechaGiro = sqlFechaGiro != null ? sqlFechaGiro.toLocalDate() : null;
            dto.setFechaGiro(fechaGiro);

            java.sql.Date sqlFechaVencimiento = (java.sql.Date) row[2];
            LocalDate fechaVencimiento = sqlFechaVencimiento != null ? sqlFechaVencimiento.toLocalDate() : null;
            dto.setFechaVencimiento(fechaVencimiento);

            dto.setImporte((BigDecimal) row[3]);
            dto.setImporteLetras((String) row[4]);
            dto.setDistritoNombre((String) row[5]);
            dto.setCliente1Nombre((String) row[6]);
            dto.setCliente1Apellidos((String) row[7]);
            dto.setCliente1NumDocumento((String) row[8]);
            dto.setCliente2Nombre((String) row[9]);
            dto.setCliente2Apellidos((String) row[10]);
            dto.setCliente2NumDocumento((String) row[11]);
            dto.setCliente1Direccion((String) row[12]);
            dto.setCliente1Distrito((String) row[13]);
            reportes.add(dto);
        }
        return reportes;
    }
   
    @Override
    @Transactional
    public void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));

        Distrito distrito = distritoRepository.findById(generarLetrasRequest.getIdDistrito())
            .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado con el ID: " + generarLetrasRequest.getIdDistrito()));

        int cantidad = contrato.getCantidadLetras();
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de letras debe ser mayor a cero");
        }

        BigDecimal importePorLetra;
        BigDecimal importeUltimaLetra = null;

        if (generarLetrasRequest.isModoAutomatico()) {
            BigDecimal saldo = contrato.getSaldo();
            if (saldo == null || saldo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El saldo del contrato es inválido o cero");
            }

            // Trabajar con saldo entero (redondeado)
            BigDecimal saldoEntero = saldo.setScale(0, BigDecimal.ROUND_HALF_UP);

            importePorLetra = saldoEntero.divide(new BigDecimal(cantidad), 0, BigDecimal.ROUND_DOWN);
            BigDecimal sumaParcial = importePorLetra.multiply(new BigDecimal(cantidad - 1));
            // La última letra es el resto del saldo menos la suma parcial, redondeada al entero más cercano
            importeUltimaLetra = saldoEntero.subtract(sumaParcial).setScale(0, BigDecimal.ROUND_HALF_UP);
        } else {
            try {
                String importeStr = generarLetrasRequest.getImporte().replaceAll("[^\\d]", "");
                importePorLetra = new BigDecimal(importeStr).setScale(0, BigDecimal.ROUND_HALF_UP);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Importe inválido: " + generarLetrasRequest.getImporte());
            }
        }

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
            letra.setFechaGiro(generarLetrasRequest.getFechaGiro());
            letra.setFechaVencimiento(fechaFinal);

            if (generarLetrasRequest.isModoAutomatico()) {
                if (i < cantidad) {
                    letra.setImporte(importePorLetra);
                } else {
                    letra.setImporte(importeUltimaLetra);
                }
            } else {
                letra.setImporte(importePorLetra);
            }

            letra.setImporteLetras(NumeroALetrasUtil.convertir(letra.getImporte()));
            letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);
            letra.setFechaPago(null);
            letra.setTipoComprobante(null);
            letra.setNumeroComprobante("");
            letra.setObservaciones("");

            letraCambioRepository.save(letra);
        }
    }
    
    @Override
    @Transactional
    public LetraCambioDTO actualizarLetra(Integer id, LetraCambioDTO letraCambioDTO) {
        LetraCambio letraExistente = letraCambioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Letra de cambio no encontrada con el ID: " + id));

        // Actualizar campos
        letraExistente.setFechaGiro(letraCambioDTO.getFechaGiro());
        letraExistente.setFechaVencimiento(letraCambioDTO.getFechaVencimiento());
        letraExistente.setImporte(letraCambioDTO.getImporte());

        // Agregar esta línea para actualizar importeLetras:
        letraExistente.setImporteLetras(letraCambioDTO.getImporteLetras());

        letraExistente.setEstadoLetra(EstadoLetra.valueOf(letraCambioDTO.getEstadoLetra()));
        letraExistente.setNumeroComprobante(letraCambioDTO.getNumeroComprobante());
        letraExistente.setObservaciones(letraCambioDTO.getObservaciones());

        // Guardar y retornar
        LetraCambio letraActualizada = letraCambioRepository.save(letraExistente);
        return modelMapper.map(letraActualizada, LetraCambioDTO.class);
    }
    
    
    @Override
    @Transactional
    public void eliminarPorContrato(Integer idContrato) {
        // Verifica si el contrato existe antes de intentar eliminar
        contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));
        
        // Llama al nuevo método del repositorio para la eliminación masiva
        letraCambioRepository.deleteByContratoIdContrato(idContrato);
    }
}
