package com.Inmobiliaria.demo.service;

import java.util.List;
import java.util.Optional;

import com.Inmobiliaria.demo.entity.Programa;

public interface ProgramaService {
    List<Programa> listProgramas();
    Optional<Programa> getProgramaById(Integer id);
    Programa savePrograma(Programa programa);
    Programa updatePrograma(Integer id, Programa programa);
    void deletePrograma(Integer id);
}
