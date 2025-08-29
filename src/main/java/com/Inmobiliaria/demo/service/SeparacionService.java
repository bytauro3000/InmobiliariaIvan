package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;

public interface SeparacionService {
    List<SeparacionDTO> buscarPorDniOApellido(String filtro);
    
 // Nuevo método para buscar por id
    Separacion buscarPorId(Integer idSeparacion);
}
