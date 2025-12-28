package com.Inmobiliaria.demo.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Transactional(readOnly = true)
    public List<SeparacionDTO> buscarPorDniOApellido(String filtro) {
        return separacionRepository.buscarPorDniOApellido(filtro);
    }

    @Override
    @Transactional(readOnly = true)
    public Separacion buscarPorId(Integer idSeparacion) {
        // Utilizamos findById que en el Repositorio tiene el @EntityGraph completo
        return separacionRepository.findById(idSeparacion).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Separacion> listadoSeparacion() {
        // Utilizamos findAll que en el Repositorio tiene el @EntityGraph para listados
        return separacionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Separacion obtenerPorId(Integer id) {
        // Crucial: llama al método con EntityGraph para cargar distritos y evitar error JSON
        return separacionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Separacion crearSeparacion(Separacion separacion) {
        try {
            // 1. Forzar datos de cabecera
            separacion.setIdSeparacion(null);
            separacion.setEstado(EstadoSeparacion.EN_PROCESO);
            if (separacion.getFechaSeparacion() == null) {
                separacion.setFechaSeparacion(new Date());
            }

            // 2. Extraer SETS para salvar la cabecera sola primero
            Set<SeparacionLote> lotesReq = separacion.getLotes();
            Set<SeparacionCliente> clientesReq = separacion.getClientes();
            
            separacion.setLotes(null);
            separacion.setClientes(null);
            
            // Guardar la cabecera para obtener ID real
            Separacion separacionGuardada = separacionRepository.save(separacion);

            // 3. Procesar Lotes
            if (lotesReq != null) {
                for (SeparacionLote sl : lotesReq) {
                    Lote loteDB = loteRepository.findById(sl.getLote().getIdLote())
                        .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

                    if (loteDB.getEstado() != EstadoLote.Disponible) {
                        throw new RuntimeException("El lote " + loteDB.getNumeroLote() + " ya no está disponible");
                    }

                    loteDB.setEstado(EstadoLote.Separado);
                    loteRepository.save(loteDB);

                    sl.setSeparacion(separacionGuardada);
                    sl.setLote(loteDB);
                    sl.setId(new SeparacionLoteId(separacionGuardada.getIdSeparacion(), loteDB.getIdLote()));
                }
                separacionLoteRepository.saveAll(lotesReq);
            }

            // 4. Procesar Clientes
            if (clientesReq != null) {
                for (SeparacionCliente sc : clientesReq) {
                    sc.setSeparacion(separacionGuardada);
                    sc.setId(new SeparacionClienteId(separacionGuardada.getIdSeparacion(), sc.getCliente().getIdCliente()));
                }
                separacionClienteRepository.saveAll(clientesReq);
            }

            // Devolvemos el objeto recargado desde el repositorio para asegurar que traiga las relaciones EAGER
            return this.obtenerPorId(separacionGuardada.getIdSeparacion());
            
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void eliminarSeparacion(Integer id) {
        Separacion sep = separacionRepository.findById(id).orElse(null);
        if (sep != null && sep.getLotes() != null) {
            for (SeparacionLote sl : sep.getLotes()) {
                Lote lote = sl.getLote();
                lote.setEstado(EstadoLote.Disponible);
                loteRepository.save(lote);
            }
        }
        separacionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Separacion actualizarSeparacion(Separacion separacionEntrante) {
        Separacion existente = separacionRepository.findById(separacionEntrante.getIdSeparacion())
                .orElseThrow(() -> new RuntimeException("Separación no encontrada"));

        existente.setMonto(separacionEntrante.getMonto());
        existente.setFechaLimite(separacionEntrante.getFechaLimite());
        existente.setEstado(separacionEntrante.getEstado());
        existente.setObservaciones(separacionEntrante.getObservaciones());
        existente.setVendedor(separacionEntrante.getVendedor());
        
        return separacionRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeparacionResumenDTO> listarResumen() {
        List<Separacion> separaciones = separacionRepository.findAll();

        return separaciones.stream().map(s -> {
            SeparacionResumenDTO dto = new SeparacionResumenDTO();
            dto.setIdSeparacion(s.getIdSeparacion());
            dto.setMonto(s.getMonto());
            dto.setFechaSepara(s.getFechaSeparacion());
            dto.setFechaLimite(s.getFechaLimite());
            dto.setEstadoSeparacion(s.getEstado());
            
            dto.setNomVendedor(s.getVendedor() != null ? 
                s.getVendedor().getNombre() + " " + s.getVendedor().getApellidos() : "Sin Vendedor");

            if (s.getClientes() != null) {
                dto.setClientes(s.getClientes().stream()
                    .map(sc -> new SeparacionResumenDTO.ClienteDetalleDTO(
                        sc.getCliente().getNombre() + " " + sc.getCliente().getApellidos(),
                        sc.getCliente().getNumDoc()))
                    .collect(Collectors.toList()));
            }

            if (s.getLotes() != null) {
                dto.setLotes(s.getLotes().stream()
                    .map(sl -> new SeparacionResumenDTO.LoteDetalleDTO(
                        sl.getLote().getManzana(),
                        sl.getLote().getNumeroLote()))
                    .collect(Collectors.toList()));
            }

            return dto;
        }).collect(Collectors.toList());
    }
}