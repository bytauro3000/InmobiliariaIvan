package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.repository.VendedorRepository;
import com.Inmobiliaria.demo.service.VendedorService;




@Service
public class VendedorServiceImpl implements VendedorService{
	@Autowired
	VendedorRepository vendedorRepository;

	@Override
	public List<Vendedor> listarVendedores() {
		
		return vendedorRepository.findAll();
	}

	@Override
    public Optional<Vendedor> obtenerVendedorPorId(Integer id) {
        return vendedorRepository.findById(id);
    }

    @Override
    public Vendedor guardarVendedor(Vendedor vendedor) {
        return vendedorRepository.save(vendedor);
    }

    @Override
    public Vendedor actualizarVendedor(Integer id, Vendedor vendedor) {
        return vendedorRepository.findById(id)
                .map(v -> {
                    v.setNombre(vendedor.getNombre());
                    v.setApellidos(vendedor.getApellidos());
                    v.setDni(vendedor.getDni());
                    v.setCelular(vendedor.getCelular());
                    v.setEmail(vendedor.getEmail());
                    v.setDireccion(vendedor.getDireccion());
                    v.setFechaNacimiento(vendedor.getFechaNacimiento());
                    v.setGenero(vendedor.getGenero());
                    v.setComision(vendedor.getComision());
                    v.setDistrito(vendedor.getDistrito());
                    return vendedorRepository.save(v);
                })
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con id " + id));
    }

    @Override
    public void eliminarVendedor(Integer id) {
        vendedorRepository.deleteById(id);
    }
    
   

}