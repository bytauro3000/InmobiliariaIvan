package com.Inmobiliaria.demo.service.impl;

import java.util.Comparator;
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
	@Cacheable(key = "'reporte'")
	public List<Lote> listarLotesParaReporte() {
		// Ordenados por programa, manzana y numero de lote para el reporte
		return loteRepository.findAllParaReporteOrdenado();
	}

	@Override
	@Cacheable(key = "'conteo'")
	public List<Object[]> obtenerConteoPorEstadoYPrograma() {
		return loteRepository.contarLotesPorProgramaYEstado();
	}

	@Override
	@Cacheable(key = "'prog_' + #idPrograma")
	public List<Lote> listarLotesPorProgramaGestion(Integer idPrograma) {
		// Orden natural: manzana primero y luego número de lote
		return loteRepository.findByProgramaIdProgramaOrderByManzanaAscNumeroLoteAsc(idPrograma)
				.stream().sorted(ORDEN_MANZANA_LOTE).toList();
	}

	@Override
	@Cacheable(key = "{#idPrograma, #manzana, #numeroLote}")
	public List<Lote> buscarLotesPorGestion(Integer idPrograma, String manzana, String numeroLote) {
		// Si no se envía texto en los filtros, usamos el método que ya tienes de listar
		// todos
		if ((manzana == null || manzana.isEmpty()) && (numeroLote == null || numeroLote.isEmpty())) {
			return listarLotesPorProgramaGestion(idPrograma);
		}
		// Si hay texto, filtramos (con orden natural manzana → lote)
		return loteRepository
				.findByProgramaIdProgramaAndManzanaContainingAndNumeroLoteContainingOrderByManzanaAscNumeroLoteAsc(
						idPrograma, manzana, numeroLote)
				.stream().sorted(ORDEN_MANZANA_LOTE).toList();
	}

	// Orden natural: "1","2","10" o "A","B","C" (no lexicográfico, que pondría "10" antes de "2")
	private static final Comparator<Lote> ORDEN_MANZANA_LOTE =
			Comparator.comparing(Lote::getManzana, LoteServiceImpl::compararNatural)
					.thenComparing(Lote::getNumeroLote, LoteServiceImpl::compararNatural);

	private static int compararNatural(String a, String b) {
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		String at = a.trim();
		String bt = b.trim();
		try {
			int na = Integer.parseInt(at);
			try {
				return Integer.compare(na, Integer.parseInt(bt));
			} catch (NumberFormatException e) {
				return -1; // numérico antes que no numérico
			}
		} catch (NumberFormatException e) {
			try {
				Integer.parseInt(bt);
				return 1;
			} catch (NumberFormatException e2) {
				return at.compareToIgnoreCase(bt);
			}
		}
	}

	@Override
	@Cacheable(key = "'prog_dto_' + #idPrograma")
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
	@Cacheable(key = "'lote_' + #id")
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