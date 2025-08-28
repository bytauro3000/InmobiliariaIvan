package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Parcelero;
import com.Inmobiliaria.demo.repository.ParceleroRepository;
import com.Inmobiliaria.demo.service.ParceleroService;

@Service
public class ParceleroServiceImpl implements ParceleroService {

    @Autowired
    private ParceleroRepository parceleroRepository;

    @Override
    public List<Parcelero> listarParceleros() {
        return parceleroRepository.findAll();
    }

    @Override
    public Optional<Parcelero> obtenerParceleroPorId(Integer id) {
        return parceleroRepository.findById(id);
    }

    @Override
    public Parcelero guardarParcelero(Parcelero parcelero) {
        return parceleroRepository.save(parcelero);
    }

    @Override
    public Parcelero actualizarParcelero(Integer id, Parcelero parcelero) {
        return parceleroRepository.findById(id)
                .map(p -> {
                    p.setNombres(parcelero.getNombres());
                    p.setApellidos(parcelero.getApellidos());
                    p.setDni(parcelero.getDni());
                    p.setCelular(parcelero.getCelular());
                    p.setDireccion(parcelero.getDireccion());
                    p.setEmail(parcelero.getEmail());
                    p.setDistrito(parcelero.getDistrito());
                    return parceleroRepository.save(p);
                })
                .orElseThrow(() -> new RuntimeException("Parcelero no encontrado con id " + id));
    }

    @Override
    public void eliminarParcelero(Integer id) {
        parceleroRepository.deleteById(id);
    }
}
