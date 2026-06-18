package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ComprobanteResponseDTO;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.SerieComprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.repository.SerieComprobanteRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {

    private final ComprobanteRepository       comprobanteRepository;
    private final SerieComprobanteRepository   serieComprobanteRepository;

    // ─── Serie por defecto para cada tipo ─────────────────────────────────────
    private String serieDefecto(TipoComprobante tipo) {
        return switch (tipo) {
            case BOLETA      -> "EB01";
            case FACTURA     -> "F001";
            case RECIBO      -> "RB01";
            case NOTA_CREDITO -> "BB01";
        };
    }

    // ─── Formatea el número completo según el tipo ─────────────────────────────
    // BOLETA/FACTURA/RECIBO → "RB01-0001", "EB01-0001", "F001-0001"
    private String formatearNumeroCompleto(TipoComprobante tipo, String serie, Integer numero) {
        return String.format("%s-%04d", serie, numero);
    }

    @Override
    @Transactional
    public Comprobante generarComprobante(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision) {

        String serie = serieDefecto(tipoComprobante);

        // ── 1. Bloquear la fila del contador (SELECT FOR UPDATE) ─────────────
        SerieComprobante contador = serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .orElseThrow(() -> new NegocioException(
                        "No existe serie configurada para el tipo de comprobante: "
                        + tipoComprobante + " / serie: " + (serie.isBlank() ? "(vacío)" : serie)
                        + ". Ejecute el SQL de inicialización de serie_comprobante."
                ));

        // ── 2. Incrementar el contador ────────────────────────────────────────
        int nuevoNumero = contador.getUltimoNumero() + 1;
        contador.setUltimoNumero(nuevoNumero);
        serieComprobanteRepository.save(contador);   // persiste antes de crear el comprobante

        // ── 3. Crear y persistir el comprobante ───────────────────────────────
        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, nuevoNumero);

        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(nuevoNumero);
        comp.setNumeroCompleto(numeroCompleto);
        comp.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());
        comp.setMonto(monto);
        comp.setTipoOrigen(tipoOrigen);
        comp.setReferenciaId(referenciaId);
        comp.setEmailEnviado(false);

        return comprobanteRepository.save(comp);
    }

    // ─── Generación con número personalizado (pagos históricos) ───────────────

    @Override
    @Transactional
    public Comprobante generarComprobanteConNumero(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision,
            String numeroPersonalizado) {

        // Si no viene número personalizado, usa el flujo automático normal
        if (numeroPersonalizado == null || numeroPersonalizado.isBlank()) {
            return generarComprobante(tipoComprobante, tipoOrigen, referenciaId, monto, fechaEmision);
        }

        String serie = serieDefecto(tipoComprobante);

        // Parsear el número correlativo del string ingresado
        // Acepta tanto "45" como "RB01-0045" o "EB01-0045" — extrae solo el número
        int numeroInt;
        try {
            String soloNumero = numeroPersonalizado.contains("-")
                    ? numeroPersonalizado.substring(numeroPersonalizado.lastIndexOf('-') + 1)
                    : numeroPersonalizado.trim();
            numeroInt = Integer.parseInt(soloNumero);
        } catch (NumberFormatException e) {
            throw new NegocioException(
                "El número de comprobante personalizado no es válido: \"" + numeroPersonalizado + "\". "
                + "Ingrese solo el número correlativo (ej: 45)."
            );
        }

        // Construir el número completo formateado
        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, numeroInt);

        // Verificar que no exista ya ese número (evitar duplicados)
        if (comprobanteRepository.existsByNumeroCompleto(numeroCompleto)) {
            throw new NegocioException(
                "Ya existe un comprobante con el número \"" + numeroCompleto + "\". "
                + "Verifique el número ingresado."
            );
        }

        // ── Actualizar el contador si el número manual es mayor al último registrado ──
        // Esto garantiza que el próximo número automático siempre sea consecutivo
        // al número más alto existente en la BD, sea manual o automático.
        serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .ifPresent(contador -> {
                    if (numeroInt > contador.getUltimoNumero()) {
                        contador.setUltimoNumero(numeroInt);
                        serieComprobanteRepository.save(contador);
                    }
                });

        // Crear el comprobante con el número personalizado
        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(numeroInt);
        comp.setNumeroCompleto(numeroCompleto);
        comp.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());
        comp.setMonto(monto);
        comp.setTipoOrigen(tipoOrigen);
        comp.setReferenciaId(referenciaId);
        comp.setEmailEnviado(false);

        return comprobanteRepository.save(comp);
    }

    // ─── Generación con número y serie personalizada ─────────────────────────

    @Override
    @Transactional
    public Comprobante generarComprobanteConNumeroY(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision,
            String numeroPersonalizado,
            String seriePersonalizada) {

        if (numeroPersonalizado == null || numeroPersonalizado.isBlank()) {
            if (seriePersonalizada == null || seriePersonalizada.isBlank()) {
                return generarComprobante(tipoComprobante, tipoOrigen, referenciaId, monto, fechaEmision);
            }
            return generarConSeriePersonalizada(tipoComprobante, tipoOrigen, referenciaId, monto, fechaEmision, seriePersonalizada);
        }

        String serie;
        int numeroInt;

        if (numeroPersonalizado.contains("-")) {
            String[] partes = numeroPersonalizado.split("-", 2);
            String serieExtraida = partes[0].trim().toUpperCase();
            String numeroStr = partes[1].trim();
            if (serieExtraida.isEmpty() || numeroStr.isEmpty()) {
                throw new NegocioException(
                    "El formato del comprobante no es válido: \"" + numeroPersonalizado + "\". "
                    + "Use el formato Serie-Número (ej: B001-1)."
                );
            }
            serie = (seriePersonalizada != null && !seriePersonalizada.isBlank())
                    ? seriePersonalizada.trim().toUpperCase()
                    : serieExtraida;
            try {
                numeroInt = Integer.parseInt(numeroStr);
            } catch (NumberFormatException e) {
                throw new NegocioException(
                    "El número correlativo no es válido: \"" + numeroStr + "\"."
                );
            }
        } else {
            serie = (seriePersonalizada != null && !seriePersonalizada.isBlank())
                    ? seriePersonalizada.trim().toUpperCase()
                    : serieDefecto(tipoComprobante);
            try {
                numeroInt = Integer.parseInt(numeroPersonalizado.trim());
            } catch (NumberFormatException e) {
                throw new NegocioException(
                    "El número de comprobante personalizado no es válido: \"" + numeroPersonalizado + "\". "
                    + "Ingrese solo el número correlativo (ej: 45)."
                );
            }
        }

        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, numeroInt);

        if (comprobanteRepository.existsByNumeroCompleto(numeroCompleto)) {
            throw new NegocioException(
                "Ya existe un comprobante con el número \"" + numeroCompleto + "\". "
                + "Verifique el número ingresado."
            );
        }

        serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .ifPresent(contador -> {
                    if (numeroInt > contador.getUltimoNumero()) {
                        contador.setUltimoNumero(numeroInt);
                        serieComprobanteRepository.save(contador);
                    }
                });

        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(numeroInt);
        comp.setNumeroCompleto(numeroCompleto);
        comp.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());
        comp.setMonto(monto);
        comp.setTipoOrigen(tipoOrigen);
        comp.setReferenciaId(referenciaId);
        comp.setEmailEnviado(false);

        return comprobanteRepository.save(comp);
    }

    @Transactional
    private Comprobante generarConSeriePersonalizada(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision,
            String seriePersonalizada) {

        String serie = seriePersonalizada.trim().toUpperCase();

        int desdeContador = serieComprobanteRepository
                .findByTipoComprobanteAndSerie(tipoComprobante, serie)
                .map(SerieComprobante::getUltimoNumero)
                .orElse(0);

        Integer desdeTablaRaw = comprobanteRepository
                .findMaxNumeroByTipoAndSerie(tipoComprobante, serie);
        int desdeTabla = (desdeTablaRaw != null) ? desdeTablaRaw : 0;

        int siguiente = Math.max(desdeContador, desdeTabla) + 1;

        serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .ifPresent(contador -> {
                    if (siguiente > contador.getUltimoNumero()) {
                        contador.setUltimoNumero(siguiente);
                        serieComprobanteRepository.save(contador);
                    }
                });

        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, siguiente);

        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(siguiente);
        comp.setNumeroCompleto(numeroCompleto);
        comp.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());
        comp.setMonto(monto);
        comp.setTipoOrigen(tipoOrigen);
        comp.setReferenciaId(referenciaId);
        comp.setEmailEnviado(false);

        return comprobanteRepository.save(comp);
    }

    // ─── Generación de Nota de Crédito ────────────────────────────────────────

    @Override
    @Transactional
    public Comprobante generarNotaCredito(Comprobante comprobanteOriginal,
                                           String codigoMotivo,
                                           String motivoNotaCredito,
                                           String anuladoPor) {
        TipoComprobante tipo = TipoComprobante.NOTA_CREDITO;
        String serie = serieDefecto(tipo);

        SerieComprobante contador = serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipo, serie)
                .orElseThrow(() -> new NegocioException(
                        "No existe serie configurada para NOTA_CREDITO / serie: " + serie
                        + ". Ejecute el SQL de inicialización de serie_comprobante."
                ));

        int nuevoNumero = contador.getUltimoNumero() + 1;
        contador.setUltimoNumero(nuevoNumero);
        serieComprobanteRepository.save(contador);

        String numeroCompleto = formatearNumeroCompleto(tipo, serie, nuevoNumero);

        Comprobante nc = new Comprobante();
        nc.setTipoComprobante(TipoComprobante.NOTA_CREDITO);
        nc.setSerie(serie);
        nc.setNumero(nuevoNumero);
        nc.setNumeroCompleto(numeroCompleto);
        nc.setFechaEmision(LocalDate.now());
        nc.setMonto(comprobanteOriginal.getMonto());
        nc.setTipoOrigen(comprobanteOriginal.getTipoOrigen());
        nc.setReferenciaId(comprobanteOriginal.getReferenciaId());
        nc.setEmailEnviado(false);
        nc.setComprobanteReferencia(comprobanteOriginal);
        nc.setCodigoMotivo(codigoMotivo);
        nc.setMotivoNotaCredito(motivoNotaCredito);
        nc.setEstadoSunat("PENDIENTE");

        return comprobanteRepository.save(nc);
    }

    // ─── Eliminación de comprobante + resincronización del contador ────────────

    @Override
    @Transactional
    public void eliminarComprobante(Long idComprobante) {
        Comprobante comp = comprobanteRepository.findById(idComprobante)
                .orElseThrow(() -> new NegocioException(
                        "Comprobante no encontrado con id: " + idComprobante));

        TipoComprobante tipo = comp.getTipoComprobante();
        String serie        = comp.getSerie();

        // 1. Eliminar el comprobante
        comprobanteRepository.deleteById(idComprobante);

        // 2. Recalcular el contador con el MAX real que quedó en la tabla
        //    tras la eliminación. Si no quedan comprobantes de ese tipo/serie,
        //    el contador vuelve a 0.
        serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipo, serie)
                .ifPresent(contador -> {
                    int maxReal = comprobanteRepository
                            .findMaxNumeroByTipoAndSerie(tipo, serie);
                    contador.setUltimoNumero(maxReal);
                    serieComprobanteRepository.save(contador);
                });
    }

    // ─── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ComprobanteResponseDTO obtenerPorId(Long idComprobante) {
        Comprobante comp = comprobanteRepository.findById(idComprobante)
                .orElseThrow(() -> new NegocioException("Comprobante no encontrado con id: " + idComprobante));
        return mapToDTO(comp);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteResponseDTO obtenerPorNumeroCompleto(String numeroCompleto) {
        Comprobante comp = comprobanteRepository.findByNumeroCompleto(numeroCompleto)
                .orElseThrow(() -> new NegocioException("Comprobante no encontrado: " + numeroCompleto));
        return mapToDTO(comp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteResponseDTO> listarPorTipo(TipoComprobante tipoComprobante) {
        return comprobanteRepository
                .findByTipoComprobanteOrderByNumeroDesc(tipoComprobante)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteResponseDTO> listarPorOrigen(TipoOrigenComprobante tipoOrigen) {
        return comprobanteRepository
                .findByTipoOrigenOrderByFechaEmisionDesc(tipoOrigen)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobanteResponseDTO> listarPorRangoFecha(LocalDate desde, LocalDate hasta) {
        return comprobanteRepository
                .findByFechaEmisionBetween(desde, hasta)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Devuelve el número que se emitiría a continuación SIN persistir nada.
     * Toma el MAYOR entre el contador interno y el número más alto en la BD,
     * garantizando que el preview sea correcto incluso si hubo números manuales
     * que superan el contador de serie_comprobante.
     */
    @Override
    @Transactional(readOnly = true)
    public String previewSiguienteNumero(TipoComprobante tipoComprobante) {
        String serie = serieDefecto(tipoComprobante);

        // Número más alto según el contador interno
        int desdeContador = serieComprobanteRepository
                .findByTipoComprobanteAndSerie(tipoComprobante, serie)
                .map(SerieComprobante::getUltimoNumero)
                .orElse(0);

        // Número más alto real en la tabla comprobante (incluye números manuales)
        Integer desdeTablaRaw = comprobanteRepository
                .findMaxNumeroByTipoAndSerie(tipoComprobante, serie);
        int desdeTabla = (desdeTablaRaw != null) ? desdeTablaRaw : 0;

        // El siguiente será el mayor de los dos + 1
        int siguiente = Math.max(desdeContador, desdeTabla) + 1;

        return formatearNumeroCompleto(tipoComprobante, serie, siguiente);
    }

    // ─── Mapper interno ────────────────────────────────────────────────────────

    private ComprobanteResponseDTO mapToDTO(Comprobante c) {
        ComprobanteResponseDTO dto = new ComprobanteResponseDTO();
        dto.setIdComprobante(c.getIdComprobante());
        dto.setTipoComprobante(c.getTipoComprobante());
        dto.setSerie(c.getSerie());
        dto.setNumero(c.getNumero());
        dto.setNumeroCompleto(c.getNumeroCompleto());
        dto.setFechaEmision(c.getFechaEmision());
        dto.setMonto(c.getMonto());
        dto.setTipoOrigen(c.getTipoOrigen());
        dto.setReferenciaId(c.getReferenciaId());
        dto.setEmailEnviado(c.isEmailEnviado());
        return dto;
    }
}