package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.SeparacionDTO;

public interface SeparacionService {
    List<SeparacionDTO> buscarPorDniOApellido(String filtro);
}
