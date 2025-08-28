package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.repository.ProgramaRepository;
import com.Inmobiliaria.demo.service.ProgramaService;

@Service
public class ProgramaServiceImpl implements ProgramaService {

    @Autowired
    ProgramaRepository programaRepository;

    @Override
    public List<Programa> listProgramas() {
        return programaRepository.findAll();
    }

    @Override
    public Optional<Programa> getProgramaById(Integer id) {
        return programaRepository.findById(id);
    }

    @Override
    public Programa savePrograma(Programa programa) {
        // Calcular costoTotal automáticamente si no lo envían
        if (programa.getAreaTotal() != null && programa.getPrecioM2() != null) {
            programa.setCostoTotal(programa.getAreaTotal().multiply(programa.getPrecioM2()));
        }
        return programaRepository.save(programa);
    }

    @Override
    public Programa updatePrograma(Integer id, Programa programa) {
        return programaRepository.findById(id).map(p -> {
            p.setNombrePrograma(programa.getNombrePrograma());
            p.setUbicacion(programa.getUbicacion());
            p.setAreaTotal(programa.getAreaTotal());
            p.setPrecioM2(programa.getPrecioM2());
            p.setParcelero(programa.getParcelero());
            p.setDistrito(programa.getDistrito());

            if (p.getAreaTotal() != null && p.getPrecioM2() != null) {
                p.setCostoTotal(p.getAreaTotal().multiply(p.getPrecioM2()));
            }

            return programaRepository.save(p);
        }).orElseThrow(() -> new RuntimeException("Programa no encontrado con id " + id));
    }

    @Override
    public void deletePrograma(Integer id) {
        if (!programaRepository.existsById(id)) {
            throw new RuntimeException("No existe el Programa con id " + id);
        }
        programaRepository.deleteById(id);
    }
}
