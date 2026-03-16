package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.dto.LoteProgramaResponseDTO;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.repository.LoteRepository;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.LoteService;

import lombok.RequiredArgsConstructor;

@Service
@CacheConfig(cacheNames = "lotes")
@RequiredArgsConstructor 
public class LoteServiceImpl implements LoteService {


	private final LoteRepository loteRepository;

	@Override
	@Cacheable(key = "'todos'")
	public List<Lote> listarLotes() {
		return loteRepository.findAll();
	}

	@Override
	@Cacheable(key = "'conteo'")
	public List<Object[]> obtenerConteoPorEstadoYPrograma() {
		return loteRepository.contarLotesPorProgramaYEstado();
	}

	@Override
	@Cacheable(key = "'prog_' + #idPrograma")
	public List<Lote> listarLotesPorProgramaGestion(Integer idPrograma) {
		// Llama al método que ya tienes en el Repository que no filtra por estado
		return loteRepository.findByProgramaIdProgramaOrderByManzanaAscNumeroLoteAsc(idPrograma);
	}

	@Override
	@Cacheable(key = "{#idPrograma, #manzana, #numeroLote}")
	public List<Lote> buscarLotesPorGestion(Integer idPrograma, String manzana, String numeroLote) {
		// Si no se envía texto en los filtros, usamos el método que ya tienes de listar
		// todos
		if ((manzana == null || manzana.isEmpty()) && (numeroLote == null || numeroLote.isEmpty())) {
			return listarLotesPorProgramaGestion(idPrograma);
		}
		// Si hay texto, filtramos
		return loteRepository
				.findByProgramaIdProgramaAndManzanaContainingAndNumeroLoteContainingOrderByManzanaAscNumeroLoteAsc(
						idPrograma, manzana, numeroLote);
	}

	@Override
	@Cacheable(key = "#idPrograma")
	public List<LoteProgramaResponseDTO> listarLotesPorPrograma(Integer idPrograma) {
		// Filtrar lotes disponibles por programa
		List<Lote> lotes = loteRepository.findByProgramaIdProgramaAndEstadoEqualsOrderByManzanaAscNumeroLoteAsc(
				idPrograma, EstadoLote.Disponible);

		return lotes.stream().map(lote -> {
			LoteProgramaResponseDTO dto = new LoteProgramaResponseDTO();
			dto.setIdLote(lote.getIdLote());
			dto.setManzana(lote.getManzana());
			dto.setNumeroLote(lote.getNumeroLote());
			dto.setArea(lote.getArea());
			dto.setPrecioM2(lote.getPrecioM2());

			if (lote.getPrograma() != null) {
				dto.setIdPrograma(lote.getPrograma().getIdPrograma());
				dto.setNombrePrograma(lote.getPrograma().getNombrePrograma());
			}
			return dto;
		}).toList();
	}

	@Override
	@CacheEvict(allEntries = true)
	public Lote actualizarLote(Lote lote) {
		Lote loteAct = obtenerLotePorId(lote.getIdLote());
		if (loteAct == null || loteAct.getEstado() == EstadoLote.Separado) {
			return null;
		}

		// Validar que al cambiar los datos de este lote, no choquen con otro ya
		// existente
		boolean existeOtro = loteRepository.existsByProgramaIdProgramaAndManzanaAndNumeroLoteAndIdLoteNot(
				lote.getPrograma().getIdPrograma(), lote.getManzana(), lote.getNumeroLote(), lote.getIdLote());

		if (existeOtro) {
			throw new NegocioException("No se puede actualizar: Los datos coinciden con otro lote ya registrado.");
		}

		return loteRepository.save(lote);
	}

	@Override
	@Cacheable(key = "#id")
	public Lote obtenerLotePorId(Integer id) {
		return loteRepository.findById(id).orElse(null);
	}

	@Override
	@CacheEvict(allEntries = true)
	public Lote crearLote(Lote reg) {
		// 1. Validar que los datos no vengan nulos
		if (reg.getPrograma() == null || reg.getManzana() == null || reg.getNumeroLote() == null) {
			throw new NegocioException("Datos incompletos para la validacion.");
		}

		// 2. Verificar si ya existe un lote igual en el mismo programa
		boolean existe = loteRepository.existsByProgramaIdProgramaAndManzanaAndNumeroLote(
				reg.getPrograma().getIdPrograma(), reg.getManzana(), reg.getNumeroLote());

		if (existe) {
			// Aquí lanzamos una excepción para que el controlador la capture
			throw new NegocioException("El lote " + reg.getNumeroLote() + " de la manzana " + reg.getManzana()
					+ " ya existe en este programa.");
		}

		return loteRepository.save(reg);
	}

	@Override
	@CacheEvict(allEntries = true)
	public void eliminarLote(Integer id) {
		loteRepository.deleteById(id);
	}

	// VERIFICAMOS LA EXISTECIA DEL LOTE
	@Override
	public boolean existeLote(Integer idPrograma, String manzana, String numeroLote) {
		return loteRepository.existsByProgramaIdProgramaAndManzanaAndNumeroLote(idPrograma, manzana, numeroLote);
	}
}