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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteServiceImpl.class);

    private final ComprobanteRepository       comprobanteRepository;
    private final SerieComprobanteRepository   serieComprobanteRepository;

    // ─── Serie por defecto para cada tipo ─────────────────────────────────────
    private String serieDefecto(TipoComprobante tipo) {
        return switch (tipo) {
            case BOLETA      -> "B001";
            case FACTURA     -> "F001";
            case RECIBO      -> "RB01";
            case NOTA_CREDITO -> "BB01";
        };
    }

    // ─── Formatea el número completo según el tipo ─────────────────────────────
    // BOLETA/FACTURA/RECIBO → "RB01-0001", "EB01-0001", "F001-0001"
    private String formatearNumeroCompleto(TipoComprobante tipo, String serie, Integer numero) {
        return String.format("%s-%d", serie, numero);
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
        if (fechaEmision != null) {
            comp.setFechaEmision(fechaEmision);
        } else {
            log.warn("fechaEmision es null para nuevo {}/{} — usando LocalDate.now() = {}",
                    tipoComprobante, serie, LocalDate.now());
            comp.setFechaEmision(LocalDate.now());
        }
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
        int numeroSolicitado;
        try {
            String soloNumero = numeroPersonalizado.contains("-")
                    ? numeroPersonalizado.substring(numeroPersonalizado.lastIndexOf('-') + 1)
                    : numeroPersonalizado.trim();
            numeroSolicitado = Integer.parseInt(soloNumero);
        } catch (NumberFormatException e) {
            throw new NegocioException(
                "El número de comprobante personalizado no es válido: \"" + numeroPersonalizado + "\". "
                + "Ingrese solo el número correlativo (ej: 45)."
            );
        }

        // ── RESOLVER EL NÚMERO BAJO LOCK PESIMISTA (SELECT FOR UPDATE) ──────
        // Todo el cálculo de correlatividad y unicidad ocurre DENTRO del lock,
        // de modo que si dos secretarias envían el mismo número a la vez, solo la
        // primera obtiene el que pidió y la segunda recibe automáticamente el
        // siguiente número disponible (sin duplicados ni rechazo de SUNAT).
        int numeroFinal = resolverNumeroBajoLock(tipoComprobante, serie, numeroSolicitado);

        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, numeroFinal);

        // Crear el comprobante con el número final resuelto bajo lock
        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(numeroFinal);
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
        int numeroSolicitado;

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
                numeroSolicitado = Integer.parseInt(numeroStr);
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
                numeroSolicitado = Integer.parseInt(numeroPersonalizado.trim());
            } catch (NumberFormatException e) {
                throw new NegocioException(
                    "El número de comprobante personalizado no es válido: \"" + numeroPersonalizado + "\". "
                    + "Ingrese solo el número correlativo (ej: 45)."
                );
            }
        }

        // ── RESOLVER EL NÚMERO BAJO LOCK PESIMISTA (SELECT FOR UPDATE) ──────
        // Decide el número final dentro del lock: si otra transacción ya usó el
        // número solicitado (dos secretarias a la vez), se asigna automáticamente
        // el siguiente número disponible. Nunca se duplica.
        int numeroFinal = resolverNumeroBajoLock(tipoComprobante, serie, numeroSolicitado);

        String numeroCompleto = formatearNumeroCompleto(tipoComprobante, serie, numeroFinal);

        // Crear el comprobante con el número final resuelto bajo lock
        Comprobante comp = new Comprobante();
        comp.setTipoComprobante(tipoComprobante);
        comp.setSerie(serie);
        comp.setNumero(numeroFinal);
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

        // El siguiente número se resuelve bajo lock pesimista (serializa la
        // asignación del número para esta serie ante concurrencia).
        int siguiente = resolverNumeroBajoLock(tipoComprobante, serie, Integer.MAX_VALUE);

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

    /**
     * Resuelve el número de comprobante de forma ATÓMICA usando un lock pesimista
     * (SELECT FOR UPDATE) sobre la fila de {@code serie_comprobante}.
     *
     * <p>Este método es la defensa principal contra la condición de carrera
     * cuando dos secretarias registran un pago al mismo tiempo: ambas reciben el
     * mismo número "sugerido" del preview, pero al persistir SOLO la primera
     * transacción se queda con ese número. La segunda, al obtener el lock
     * después, detecta que el número ya fue usado y asigna automáticamente el
     * siguiente número disponible.</p>
     *
     * <p>Reglas:
     * <ul>
     *   <li>Si el número solicitado está libre y es el siguiente esperado, se usa tal cual.</li>
     *   <li>Si el número solicitado ya existe (lo tomó otra transacción), se asigna el siguiente.</li>
     *   <li>Si el número solicitado NO es el siguiente correlativo para una serie SUNAT (B*),
     *       se ajusta al siguiente esperado (SUNAT exige numeración consecutiva).</li>
     *   <li>El contador de la serie queda adelantado al número final asignado.</li>
     * </ul></p>
     *
     * @return el número final asignado (diferente al solicitado si hubo conflicto)
     */
    private int resolverNumeroBajoLock(TipoComprobante tipoComprobante, String serie, int numeroSolicitado) {
        // 1. Bloquear la fila del contador (SELECT FOR UPDATE). Esto serializa
        //    todas las transacciones que piden número para la misma serie.
        SerieComprobante contador = serieComprobanteRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .orElseThrow(() -> new NegocioException(
                        "No existe serie configurada para el tipo de comprobante: "
                        + tipoComprobante + " / serie: " + (serie.isBlank() ? "(vacío)" : serie)
                        + ". Ejecute el SQL de inicialización de serie_comprobante."
                ));

        // 2. Calcular el siguiente esperado DENTRO del lock (máximo entre contador y BD real)
        int esperado = Math.max(contador.getUltimoNumero(),
                comprobanteRepository.findMaxNumeroByTipoAndSerie(tipoComprobante, serie) == null
                        ? 0
                        : comprobanteRepository.findMaxNumeroByTipoAndSerie(tipoComprobante, serie)) + 1;

        // 3. El número solicitado queda libre y es el esperado → usarlo.
        //    Si ya existe o no es el esperado para series SUNAT → usar el esperado.
        boolean numeroLibre = !comprobanteRepository.existsByNumeroCompleto(
                formatearNumeroCompleto(tipoComprobante, serie, numeroSolicitado));

        int numeroFinal;
        if (numeroLibre && (numeroSolicitado == esperado || !serie.startsWith("B"))) {
            // Número libre: se usa. Para series no-SUNAT (RB01, EB01) se respeta
            // cualquier número libre; para series SUNAT (B*) debe ser el esperado.
            numeroFinal = numeroSolicitado;
        } else {
            // Conflicto (otra secretaria usó el número) o número no correlativo:
            // asignar el siguiente número disponible bajo el lock.
            numeroFinal = esperado;
            log.info("Número {} no disponible para {} / {}: se asigna el siguiente {}",
                    numeroSolicitado, tipoComprobante, serie, esperado);
        }

        // 4. Adelantar el contador al número asignado (siempre que sea mayor).
        if (numeroFinal > contador.getUltimoNumero()) {
            contador.setUltimoNumero(numeroFinal);
            serieComprobanteRepository.save(contador);
        }

        return numeroFinal;
    }

    // ─── Generación de Nota de Crédito ────────────────────────────────────────

    @Override
    @Transactional
    public Comprobante generarNotaCredito(Comprobante comprobanteOriginal,
                                           String codigoMotivo,
                                           String motivoNotaCredito,
                                           String anuladoPor) {
        TipoComprobante tipo = TipoComprobante.NOTA_CREDITO;
        // BB01 para comprobantes SUNAT (B-series), RN01 para internos (recibos)
        String origSerie = comprobanteOriginal.getSerie();
        String serie = (origSerie != null && origSerie.startsWith("B")) ? "BB01" : "RN01";

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