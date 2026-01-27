package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.enums.EstadoLote;
//Repo
@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer>{
	
	//Metodo implementado para mostrar el grafico de barras en el dashboard
	@Query("SELECT l.programa.nombrePrograma, l.estado, COUNT(l) " +
		       "FROM Lote l " +
		       "GROUP BY l.programa.nombrePrograma, l.estado")
		List<Object[]> contarLotesPorProgramaYEstado();

	//Método para obtener lotes por programa (sin filtrar por estado)
	//ordenar por Manzana y luego por Lote
	List<Lote> findByProgramaIdProgramaOrderByManzanaAscNumeroLoteAsc(Integer idPrograma);

    //Este es el método que necesitas para filtrar por programa Y estado
	//Cambiado para ordenar por Manzana y luego por Lote
    List<Lote> findByProgramaIdProgramaAndEstadoEqualsOrderByManzanaAscNumeroLoteAsc(Integer idPrograma, EstadoLote estado);
    
 // 🔹 Nuevo método de búsqueda combinada con ordenamiento
    List<Lote> findByProgramaIdProgramaAndManzanaContainingAndNumeroLoteContainingOrderByManzanaAscNumeroLoteAsc(
        Integer idPrograma, 
        String manzana, 
        String numeroLote
    );
}
