package com.Inmobiliaria.demo.service;

import java.util.List;
import java.util.Optional;

import com.Inmobiliaria.demo.entity.Parcelero;

public interface ParceleroService {

    List<Parcelero> listarParceleros();
    Optional<Parcelero> obtenerParceleroPorId(Integer id);
    Parcelero guardarParcelero(Parcelero parcelero);
    Parcelero actualizarParcelero(Integer id, Parcelero parcelero);
    void eliminarParcelero(Integer id);
}
