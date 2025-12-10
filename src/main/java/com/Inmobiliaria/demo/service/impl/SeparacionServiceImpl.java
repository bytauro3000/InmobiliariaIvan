package com.Inmobiliaria.demo.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.Separacion;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.repository.LoteRepository;
import com.Inmobiliaria.demo.repository.SeparacionRepository;
import com.Inmobiliaria.demo.service.SeparacionService;

@Service
public class SeparacionServiceImpl implements SeparacionService {

    @Autowired
	private LoteRepository loteRepository; 
    @Autowired
    private SeparacionRepository separacionRepository;

    @Override
    public List<SeparacionDTO> buscarPorDniOApellido(String filtro) {
        return separacionRepository.buscarPorDniOApellido(filtro);
    }

    
    // Implementación del método buscarPorId
    @Override
    public Separacion buscarPorId(Integer idSeparacion) {
        return separacionRepository.findById(idSeparacion).orElse(null);
    }

	@Override
	public List<Separacion> listadoSeparacion(){
		return separacionRepository.findAll();
	}

	@Override
	public Separacion obtenerPorId(Integer id) {
		return separacionRepository.findById(id).orElse(null);
	}

	@Override
	public Separacion crearSeparacion(Separacion separacion) {
        try {
            // 1. Verificar que el lote esté disponible
            Lote lote = loteRepository.findById(separacion.getLote().getIdLote())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

            if (!lote.getEstado().equals(EstadoLote.Disponible)) {
                throw new RuntimeException("El lote no está disponible para separación");
            }

            // 2. Cambiar estado del lote a Separado
            lote.setEstado(EstadoLote.Separado);
            loteRepository.save(lote);

            // 3. Guardar la separación
            separacion.setEstado(EstadoSeparacion.EN_PROCESO);
            separacion.setFechaSeparacion(new Date());

            return separacionRepository.save(separacion);

        } catch (Exception e) {
            // Si algo falla, @Transactional deshace todo
            throw new RuntimeException("Error al crear separación: " + e.getMessage());
        }
	}

	@Override
	public void eliminarSeparacion(Integer id) {
		separacionRepository.deleteById(id);
		
	}

	@Override
	public Separacion actualizarSeparacion(Separacion separacionEntrante) {
	    // 1. Buscamos si existe en la BD
	    Separacion separacionExistente = separacionRepository.findById(separacionEntrante.getIdSeparacion()).orElse(null);

	    if (separacionExistente == null) {
	        return null; // O lanzar una excepción
	    }

	    // 2. Actualizamos SOLO los campos que queremos modificar
	    // (O usamos el objeto completo si esa es la lógica de tu negocio)
	    separacionExistente.setMonto(separacionEntrante.getMonto());
	    separacionExistente.setFechaLimite(separacionEntrante.getFechaLimite());
	    separacionExistente.setEstado(separacionEntrante.getEstado());
	    separacionExistente.setObservaciones(separacionEntrante.getObservaciones());
	    
	    // Si permites cambiar de lote o cliente, actualízalos también:
	    separacionExistente.setCliente(separacionEntrante.getCliente());
	    separacionExistente.setVendedor(separacionEntrante.getVendedor());
	    separacionExistente.setLote(separacionEntrante.getLote());

	    // 3. Guardamos los cambios
	    return separacionRepository.save(separacionExistente);
	}
	
	@Override
	public List<SeparacionResumenDTO> listarResumen() {
        return separacionRepository.obtenerResumenSeparaciones();}

}
