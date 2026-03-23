package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.enums.EstadoLote;

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
    
    //método de búsqueda combinada con ordenamiento
    List<Lote> findByProgramaIdProgramaAndManzanaContainingAndNumeroLoteContainingOrderByManzanaAscNumeroLoteAsc(
        Integer idPrograma, 
        String manzana, 
        String numeroLote
    );
    
    // Método para validar duplicados exactos
    boolean existsByProgramaIdProgramaAndManzanaAndNumeroLote(Integer idPrograma, String manzana, String numeroLote);

    // Metodo para validar al editar (excluyendo el ID actual)
    boolean existsByProgramaIdProgramaAndManzanaAndNumeroLoteAndIdLoteNot(Integer idPrograma, String manzana, String numeroLote, Integer idLote);

    // Reporte: todos los lotes ordenados por programa, manzana y numero de lote
    @Query("SELECT l FROM Lote l " +
           "JOIN FETCH l.programa p " +
           "ORDER BY p.nombrePrograma ASC, l.manzana ASC, l.numeroLote ASC")
    List<Lote> findAllParaReporteOrdenado();
}