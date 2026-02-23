package com.Inmobiliaria.demo.mensajeria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long>{

	List<Mensaje> findByDestinatarioId(Long destinatarioId);
	List<Mensaje> findByRemitenteId(Long remitenteId);
	List<Mensaje> findByRemitenteIdAndDestinatarioId(Long remitenteId, Long destinatarioId);
	List<Mensaje> findByRemitenteIdAndDestinatarioIdOrRemitenteIdAndDestinatarioIdOrderByFechaAsc(
	        Long remitenteId1, Long destinatarioId1,
	        Long remitenteId2, Long destinatarioId2);
	
	
	
	//PARA MARCAR ESTADO MENSAJE LEIDO CUANDO EL DESTINATARIO ABRE EL MENSAJE
	
	@Transactional
	@Modifying
	@Query("UPDATE Mensaje m SET m.estado = 'LEIDO' " +
		       "WHERE m.remitenteId = :remitenteId " +
		       "AND m.destinatarioId = :destinatarioId " +
		       "AND m.estado = 'ENVIADO'")
	void marcarComoLeidos(Long remitenteId, Long destinatarioId);
}
