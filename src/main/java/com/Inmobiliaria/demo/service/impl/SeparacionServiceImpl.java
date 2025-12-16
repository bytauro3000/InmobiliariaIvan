package com.Inmobiliaria.demo.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.SeparacionService;

@Service
public class SeparacionServiceImpl implements SeparacionService {

    @Autowired
    private LoteRepository loteRepository; 
    
    @Autowired
    private SeparacionRepository separacionRepository;
    
    @Autowired
    private SeparacionClienteRepository separacionClienteRepository;
    
    @Autowired
    private SeparacionLoteRepository separacionLoteRepository;

    @Override
    public List<SeparacionDTO> buscarPorDniOApellido(String filtro) {
        return separacionRepository.buscarPorDniOApellido(filtro);
    }

    @Override
    public Separacion buscarPorId(Integer idSeparacion) {
        return separacionRepository.findById(idSeparacion).orElse(null);
    }

    @Override
    public List<Separacion> listadoSeparacion() {
        return separacionRepository.findAll();
    }

    @Override
    public Separacion obtenerPorId(Integer id) {
        return separacionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Separacion crearSeparacion(Separacion separacion) {
        try {
            // 1. Guardar la cabecera de la separación para obtener el ID
            separacion.setEstado(EstadoSeparacion.EN_PROCESO);
            separacion.setFechaSeparacion(new Date());
            
            // Guardamos temporalmente para tener el ID
            Separacion separacionGuardada = separacionRepository.save(separacion);

            // 2. Procesar Lotes (Maneja múltiples lotes)
            if (separacion.getLotes() != null && !separacion.getLotes().isEmpty()) {
                for (SeparacionLote sl : separacion.getLotes()) {
                    // Validar cada lote
                    Lote lote = loteRepository.findById(sl.getLote().getIdLote())
                        .orElseThrow(() -> new RuntimeException("Lote ID " + sl.getLote().getIdLote() + " no encontrado"));

                    if (!lote.getEstado().equals(EstadoLote.Disponible)) {
                        throw new RuntimeException("El lote " + lote.getNumeroLote() + " no está disponible");
                    }

                    // Actualizar estado del lote
                    lote.setEstado(EstadoLote.Separado);
                    loteRepository.save(lote);

                    // Configurar relación intermedia
                    sl.setId(new SeparacionLoteId(separacionGuardada.getIdSeparacion(), lote.getIdLote()));
                    sl.setSeparacion(separacionGuardada);
                }
                separacionLoteRepository.saveAll(separacion.getLotes());
            }

            // 3. Procesar Clientes (Maneja múltiples clientes)
            if (separacion.getClientes() != null && !separacion.getClientes().isEmpty()) {
                for (SeparacionCliente sc : separacion.getClientes()) {
                    sc.setId(new SeparacionClienteId(separacionGuardada.getIdSeparacion(), sc.getCliente().getIdCliente()));
                    sc.setSeparacion(separacionGuardada);
                }
                separacionClienteRepository.saveAll(separacion.getClientes());
            }

            return separacionGuardada;

        } catch (Exception e) {
            throw new RuntimeException("Error al crear separación: " + e.getMessage());
        }
    }

    @Override
    public void eliminarSeparacion(Integer id) {
        separacionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Separacion actualizarSeparacion(Separacion separacionEntrante) {
        Separacion existente = separacionRepository.findById(separacionEntrante.getIdSeparacion())
                .orElseThrow(() -> new RuntimeException("Separación no encontrada"));

        // Actualizar campos básicos
        existente.setMonto(separacionEntrante.getMonto());
        existente.setFechaLimite(separacionEntrante.getFechaLimite());
        existente.setEstado(separacionEntrante.getEstado());
        existente.setObservaciones(separacionEntrante.getObservaciones());
        existente.setVendedor(separacionEntrante.getVendedor());

        // Nota: La actualización de listas (clientes/lotes) suele requerir 
        // lógica adicional para borrar los anteriores y agregar nuevos.
        
        return separacionRepository.save(existente);
    }

    @Override
    public List<SeparacionResumenDTO> listarResumen() {
        return separacionRepository.obtenerResumenSeparaciones();
    }
}