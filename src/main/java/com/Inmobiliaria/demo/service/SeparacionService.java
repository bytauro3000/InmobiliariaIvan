package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;

public interface SeparacionService {
    List<SeparacionDTO> buscarPorDniOApellido(String filtro);
    
<<<<<<< HEAD
 // Nuevo método para buscar por id
    Separacion buscarPorId(Integer idSeparacion);
=======
    List<Separacion> listadoSeparacion();
    
    Separacion obtenerPorId(Integer id);
    
    Separacion crearSeparacion(Separacion reg);
    
    void eliminarSeparacion(Integer id);
    
    Separacion actualizarSeparacion(Separacion reg);
    
>>>>>>> branch 'main' of https://github.com/bytauro3000/InmobiliariaIvan
}
