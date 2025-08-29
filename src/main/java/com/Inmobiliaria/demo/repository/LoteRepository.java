package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.enums.EstadoLote;
//Repo
@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer>{

	 //Método para obtener lotes por programa (sin filtrar por estado)
    List<Lote> findByProgramaIdPrograma(Integer idPrograma);

    //Este es el método que necesitas para filtrar por programa Y estado
    List<Lote> findByProgramaIdProgramaAndEstadoEquals(Integer idPrograma, EstadoLote estado);
}
