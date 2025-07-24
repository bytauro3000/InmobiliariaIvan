package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.entity.Distrito;

public interface DistritoService {
    List<Distrito> listarDistritos();
    Distrito obtenerPorId(Integer idDistrito);
}
