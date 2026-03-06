package com.Inmobiliaria.demo.service.impl;

import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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
import com.Inmobiliaria.demo.util.NumeroALetras;
import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteCronogramaPagosClientesDTO;
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
        LocalDate hoy = LocalDate.now();

        for (LetraCambio letra : listaLetras) {
            // Si la letra está PENDIENTE y su fecha de vencimiento es anterior a hoy
            if (letra.getEstadoLetra() == EstadoLetra.PENDIENTE && 
                letra.getFechaVencimiento().isBefore(hoy)) {
                letra.setEstadoLetra(EstadoLetra.VENCIDO);
                // No es necesario guardar explícitamente porque @Transactional lo hará al final
            }
        }

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
    public List<ReporteCronogramaPagosClientesDTO> obtenerReporteCronogramaPagosPorContrato(Integer idContrato) {
        List<Object[]> results = letraCambioRepository.obtenerCronogramaPagosPorContrato(idContrato);
        List<ReporteCronogramaPagosClientesDTO> reportes = new ArrayList<>();

        for (Object[] row : results) {
            ReporteCronogramaPagosClientesDTO dto = new ReporteCronogramaPagosClientesDTO();
            int i = 0; 

            dto.setIdLetra((Integer) row[i++]);
            dto.setCantidadLetras((Integer) row[i++]);
            dto.setMontoTotal((BigDecimal) row[i++]);
            dto.setInicial((BigDecimal) row[i++]);
            dto.setSaldo((BigDecimal) row[i++]);
            dto.setVendedorNombre((String) row[i++]);
            dto.setVendedorApellidos((String) row[i++]);
            dto.setNumeroLetra((String) row[i++]);

            java.sql.Date sqlFechaVencimiento = (java.sql.Date) row[i++];
            dto.setFechaVencimiento(sqlFechaVencimiento != null ? sqlFechaVencimiento.toLocalDate() : null);

            dto.setImporte((BigDecimal) row[i++]);
            dto.setCliente1Nombre((String) row[i++]);
            dto.setCliente1Apellidos((String) row[i++]);
            dto.setCliente1NumDocumento((String) row[i++]);
            dto.setCliente1Celular((String) row[i++]);
            dto.setCliente1Telefono((String) row[i++]);
            dto.setCliente1Direccion((String) row[i++]);
            dto.setCliente1Distrito((String) row[i++]);
            dto.setCliente2Nombre((String) row[i++]);
            dto.setCliente2Apellidos((String) row[i++]);
            dto.setCliente2NumDocumento((String) row[i++]);
            dto.setLote1Manzana((String) row[i++]);
            dto.setLote1NumeroLote((String) row[i++]);
            dto.setLote1Area((BigDecimal) row[i++]);
            dto.setLote2Manzana((String) row[i++]);
            dto.setLote2NumeroLote((String) row[i++]);
            dto.setLote2Area((BigDecimal) row[i++]);
            dto.setProgramaNombre((String) row[i++]);

            reportes.add(dto);
        }
        return reportes;
    }
   
    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
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

            BigDecimal saldoEntero = saldo.setScale(0, BigDecimal.ROUND_HALF_UP);
            importePorLetra = saldoEntero.divide(new BigDecimal(cantidad), 0, BigDecimal.ROUND_DOWN);
            BigDecimal sumaParcial = importePorLetra.multiply(new BigDecimal(cantidad - 1));
            importeUltimaLetra = saldoEntero.subtract(sumaParcial).setScale(0, BigDecimal.ROUND_HALF_UP);
        } else {
            try {

                String importeStr = generarLetrasRequest.getImporte()
                                    .replace("$", "")
                                    .replace(",", "")
                                    .trim();
                importePorLetra = new BigDecimal(importeStr).setScale(2, BigDecimal.ROUND_HALF_UP);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Importe inválido: " + generarLetrasRequest.getImporte());
            }
        }

        LocalDate fechaVencimientoInicial = generarLetrasRequest.getFechaVencimientoInicial();
 
        boolean esUltimoDia = fechaVencimientoInicial.getDayOfMonth() == fechaVencimientoInicial.lengthOfMonth();

        for (int i = 1; i <= cantidad; i++) {
            LocalDate fechaCalculada = fechaVencimientoInicial.plusMonths(i - 1);
            LocalDate fechaFinal;

            if (esUltimoDia) {
               
                fechaFinal = fechaCalculada.withDayOfMonth(fechaCalculada.lengthOfMonth());
            } else {
               
                int diaOriginal = fechaVencimientoInicial.getDayOfMonth();
                int ultimoDiaDelMes = fechaCalculada.lengthOfMonth();
                int diaAUsar = Math.min(diaOriginal, ultimoDiaDelMes);
                fechaFinal = fechaCalculada.withDayOfMonth(diaAUsar);
            }

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

            letra.setImporteLetras(NumeroALetras.convertir(letra.getImporte()));
            letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            letra.setNumeroLetra(i + "/" + cantidad);
            letraCambioRepository.save(letra);
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "contratos", key = "#letraCambioDTO.idContrato")
    public LetraCambioDTO actualizarLetra(Integer id, LetraCambioDTO letraCambioDTO) {
        LetraCambio letraExistente = letraCambioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Letra de cambio no encontrada con el ID: " + id));

        letraExistente.setFechaGiro(letraCambioDTO.getFechaGiro());
        letraExistente.setFechaVencimiento(letraCambioDTO.getFechaVencimiento());
        letraExistente.setImporte(letraCambioDTO.getImporte());
        letraExistente.setImporteLetras(letraCambioDTO.getImporteLetras());
        letraExistente.setEstadoLetra(EstadoLetra.valueOf(letraCambioDTO.getEstadoLetra()));
        

        LetraCambio letraActualizada = letraCambioRepository.save(letraExistente);
        return modelMapper.map(letraActualizada, LetraCambioDTO.class);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public void eliminarPorContrato(Integer idContrato) {
        contratoRepository.findById(idContrato)
            .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));
        
        letraCambioRepository.deleteByContratoIdContrato(idContrato);
    }
}