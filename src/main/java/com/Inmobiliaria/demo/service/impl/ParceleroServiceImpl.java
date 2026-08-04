package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Parcelero;
import com.Inmobiliaria.demo.repository.ParceleroRepository;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.ParceleroService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ParceleroServiceImpl implements ParceleroService {

    
    private final ParceleroRepository parceleroRepository;

    @Override
    public List<Parcelero> listarParceleros() {
        return parceleroRepository.findAll();
    }

    @Override
    public Optional<Parcelero> obtenerParceleroPorId(Integer id) {
        return parceleroRepository.findById(id);
    }

    @Override
    public Parcelero buscarPorDni(String dni) {
        if (dni == null || dni.isBlank()) return null;
        return parceleroRepository.findByDni(dni.trim()).orElse(null);
    }

    @Override
    public Parcelero guardarParcelero(Parcelero parcelero) {
        prepararDatos(parcelero);

        if (parcelero.getDni() != null && parceleroRepository.existsByDni(parcelero.getDni())) {
            throw new NegocioException("Ya existe un parcelero con el DNI " + parcelero.getDni() + ".");
        }
        if (parcelero.getEmail() != null && parceleroRepository.existsByEmail(parcelero.getEmail())) {
            throw new NegocioException("Ya existe un parcelero con el correo " + parcelero.getEmail() + ".");
        }

        return parceleroRepository.save(parcelero);
    }

    @Override
    public Parcelero actualizarParcelero(Integer id, Parcelero parcelero) {
        prepararDatos(parcelero);

        return parceleroRepository.findById(id)
                .map(p -> {
                    if (parcelero.getDni() != null
                            && parceleroRepository.existsByDniAndIdParceleroNot(parcelero.getDni(), id)) {
                        throw new NegocioException("Ya existe otro parcelero con el DNI " + parcelero.getDni() + ".");
                    }
                    if (parcelero.getEmail() != null
                            && parceleroRepository.existsByEmailAndIdParceleroNot(parcelero.getEmail(), id)) {
                        throw new NegocioException("Ya existe otro parcelero con el correo " + parcelero.getEmail() + ".");
                    }
                    p.setNombres(parcelero.getNombres());
                    p.setApellidos(parcelero.getApellidos());
                    p.setDni(parcelero.getDni());
                    p.setCelular(parcelero.getCelular());
                    p.setDireccion(parcelero.getDireccion());
                    p.setEmail(parcelero.getEmail());
                    p.setDistrito(parcelero.getDistrito());
                    return parceleroRepository.save(p);
                })
                .orElseThrow(() -> new NegocioException("Parcelero no encontrado con id " + id));
    }

    @Override
    public void eliminarParcelero(Integer id) {
        parceleroRepository.deleteById(id);
    }

    /**
     * Normaliza los datos antes de guardar/actualizar:
     * - Email vacío → null (la columna es UNIQUE y '' duplicado viola la restricción).
     */
    private void prepararDatos(Parcelero parcelero) {
        if (parcelero.getDni() != null) {
            parcelero.setDni(parcelero.getDni().trim());
        }
        if (parcelero.getEmail() == null || parcelero.getEmail().isBlank()) {
            parcelero.setEmail(null);
        } else {
            parcelero.setEmail(parcelero.getEmail().trim());
        }
    }
}