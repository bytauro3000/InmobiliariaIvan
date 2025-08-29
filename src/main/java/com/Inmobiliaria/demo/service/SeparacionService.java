package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;

public interface SeparacionService {
    List<SeparacionDTO> buscarPorDniOApellido(String filtro);
    

    Separacion buscarPorId(Integer idSeparacion);

    List<Separacion> listadoSeparacion();
    
    Separacion obtenerPorId(Integer id);
    
    Separacion crearSeparacion(Separacion reg);
    
    void eliminarSeparacion(Integer id);
    
    Separacion actualizarSeparacion(Separacion reg);
    
}
