package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.GrupoLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteCronogramaPagosClientesDTO;
import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.DistritoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.service.LetraCambioService;
import com.Inmobiliaria.demo.util.LetraCambioPdf;
import com.Inmobiliaria.demo.util.NumeroALetras;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LetraCambioServiceImpl implements LetraCambioService {

    private final LetraCambioRepository letraCambioRepository;
    private final ContratoRepository contratoRepository;
    private final DistritoRepository distritoRepository;
    private final ModelMapper modelMapper;

    private void recalcularEstadoContrato(Contrato contrato) {
        if (contrato.getTipoContrato() != TipoContrato.FINANCIADO) return;

        EstadoContrato estadoActual = contrato.getEstadoContrato();
        if (estadoActual == EstadoContrato.CANCELADO    ||
            estadoActual == EstadoContrato.RESUELTO      ||
            estadoActual == EstadoContrato.EN_RESOLUCION ||
            estadoActual == EstadoContrato.RENUNCIA      ||
            estadoActual == EstadoContrato.TRANSFERIDO   ||
            estadoActual == EstadoContrato.CARTA_NOTARIAL) return;

        List<LetraCambio> letras = letraCambioRepository
                .findByContratoIdContrato(contrato.getIdContrato());

        LocalDate hoy = LocalDate.now();

        Optional<LocalDate> fechaUltimaPagada = letras.stream()
                .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                .map(LetraCambio::getFechaVencimiento)
                .filter(f -> f != null)
                .max(Comparator.naturalOrder());

        List<LetraCambio> letrasParaActualizar = new ArrayList<>();

        for (LetraCambio letra : letras) {
            if (letra.getEstadoLetra() != EstadoLetra.PENDIENTE) continue;
            if (letra.getFechaVencimiento() == null) continue;
            if (!letra.getFechaVencimiento().isBefore(hoy)) continue;
            if (fechaUltimaPagada.isPresent() &&
                    !letra.getFechaVencimiento().isAfter(fechaUltimaPagada.get())) continue;
            letra.setEstadoLetra(EstadoLetra.VENCIDO);
            letrasParaActualizar.add(letra);
        }

        if (!letrasParaActualizar.isEmpty()) {
            letraCambioRepository.saveAll(letrasParaActualizar); // [3] batch en vez de N saves
        }

        long letrasVencidas = letras.stream()
                .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
                .count();
        letrasVencidas += letrasParaActualizar.size();

        EstadoContrato nuevoEstado = letrasVencidas == 0
                ? EstadoContrato.ACTIVO
                : EstadoContrato.MORA;

        if (nuevoEstado != estadoActual) {
            contrato.setEstadoContrato(nuevoEstado);
            contratoRepository.save(contrato);
        }
    }

    @Override
    @Transactional
    public List<LetraCambioDTO> listarPorContrato(Integer idContrato) {
        List<LetraCambio> letrasConClientes = letraCambioRepository
                .findByContratoIdContratoConClientes(idContrato);
        List<LetraCambio> letrasConPagos = letraCambioRepository
                .findByContratoIdContratoConPagos(idContrato);

        Map<Integer, List<PagoLetras>> pagosPorLetra = letrasConPagos.stream()
                .collect(Collectors.toMap(
                        LetraCambio::getIdLetra,
                        l -> l.getPagos() != null ? l.getPagos() : new ArrayList<>()
                ));

        Map<String, Long> conteoPorComprobante = letrasConPagos.stream()
                .flatMap(l -> l.getPagos() != null ? l.getPagos().stream() : java.util.stream.Stream.empty())
                .filter(p -> p.getComprobante() != null
                          && p.getComprobante().getNumeroCompleto() != null
                          && !p.getComprobante().getNumeroCompleto().isBlank())
                .collect(Collectors.groupingBy(
                        p -> p.getComprobante().getNumeroCompleto(),
                        Collectors.counting()));

        LocalDate hoy = LocalDate.now();

        return letrasConClientes.stream()
                .map(letra -> {
                    if (letra.getEstadoLetra() == EstadoLetra.PENDIENTE &&
                            letra.getFechaVencimiento().isBefore(hoy)) {
                        letra.setEstadoLetra(EstadoLetra.VENCIDO);
                    }
                    LetraCambioDTO dto = modelMapper.map(letra, LetraCambioDTO.class);
                    Contrato contrato = letra.getContrato();
                    if (contrato != null && contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                        ContratoCliente contratoCliente = contrato.getClientes().iterator().next();
                        if (contratoCliente.getCliente() != null) {
                            String nombreCompleto = contratoCliente.getCliente().getNombre()
                                    + " " + contratoCliente.getCliente().getApellidos();
                            dto.setNombreCliente(nombreCompleto.trim());
                        }
                    }
                    if (contrato != null) {
                        dto.setMonedaContrato(contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD);
                    }
                    List<PagoLetras> pagos = pagosPorLetra.getOrDefault(letra.getIdLetra(), new ArrayList<>());
                    if (!pagos.isEmpty()) {
                     
                        String numComp = (pagos.get(0).getComprobante() != null)
                                ? pagos.get(0).getComprobante().getNumeroCompleto()
                                : null;
                        dto.setNumeroComprobante(numComp);
                        if (numComp != null && !numComp.isBlank()) {
                            dto.setEsMultiple(conteoPorComprobante.getOrDefault(numComp, 0L) > 1);
                        }
                    }
                    // Mapear saldo pendiente para letras PARCIALES
                    if (letra.getSaldoPendiente() != null) {
                        dto.setSaldoPendiente(letra.getSaldoPendiente());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Boolean> existenLetrasPorContratos(List<Integer> ids) {
        List<Integer> conLetras = letraCambioRepository.findContratosConLetras(ids);
        Set<Integer> set = new HashSet<>(conLetras);
        return ids.stream().collect(Collectors.toMap(id -> id, set::contains));
    }
    
    @Override
    @Transactional
    public List<ReporteLetraCambioDTO> obtenerReportePorContrato(Integer idContrato) {
        List<Object[]> results = letraCambioRepository.obtenerReportePorContrato(idContrato);
        List<ReporteLetraCambioDTO> reportes = new ArrayList<>();
        for (Object[] row : results) {
            ReporteLetraCambioDTO dto = new ReporteLetraCambioDTO();
            dto.setNumeroLetra((String) row[0]);
            java.sql.Date sqlFechaGiro = (java.sql.Date) row[1];
            dto.setFechaGiro(sqlFechaGiro != null ? sqlFechaGiro.toLocalDate() : null);
            java.sql.Date sqlFechaVencimiento = (java.sql.Date) row[2];
            dto.setFechaVencimiento(sqlFechaVencimiento != null ? sqlFechaVencimiento.toLocalDate() : null);
            dto.setImporte((BigDecimal) row[3]);
            dto.setImporteLetras((String) row[4]);
            dto.setDistritoNombre((String) row[5]);
            dto.setCliente1Nombre((String) row[6]);
            dto.setCliente1Apellidos((String) row[7]);
            dto.setCliente1NumDocumento((String) row[8]);
            dto.setCliente2Nombre((String) row[9]);
            dto.setCliente2Apellidos((String) row[10]);
            dto.setCliente2NumDocumento((String) row[11]);
            dto.setCliente1Direccion((String) row[12]);
            dto.setCliente1Distrito((String) row[13]);
            reportes.add(dto);
        }
        return reportes;
    }

    @Override
    @Transactional
    public List<ReporteCronogramaPagosClientesDTO> obtenerReporteCronogramaPagosPorContrato(Integer idContrato) {
        List<Object[]> results = letraCambioRepository.obtenerCronogramaPagosPorContrato(idContrato);
        List<ReporteCronogramaPagosClientesDTO> reportes = new ArrayList<>();
        for (Object[] row : results) {
            ReporteCronogramaPagosClientesDTO dto = new ReporteCronogramaPagosClientesDTO();
            int i = 0;
            dto.setIdLetra((Integer) row[i++]);
            dto.setCantidadLetras((Integer) row[i++]);
            dto.setMontoTotal((BigDecimal) row[i++]);
            dto.setInicial((BigDecimal) row[i++]);
            dto.setSaldo((BigDecimal) row[i++]);
            dto.setVendedorNombre((String) row[i++]);
            dto.setVendedorApellidos((String) row[i++]);
            dto.setNumeroLetra((String) row[i++]);
            java.sql.Date sqlFechaVencimiento = (java.sql.Date) row[i++];
            dto.setFechaVencimiento(sqlFechaVencimiento != null ? sqlFechaVencimiento.toLocalDate() : null);
            dto.setImporte((BigDecimal) row[i++]);
            dto.setCliente1Nombre((String) row[i++]);
            dto.setCliente1Apellidos((String) row[i++]);
            dto.setCliente1NumDocumento((String) row[i++]);
            dto.setCliente1Celular((String) row[i++]);
            dto.setCliente1Telefono((String) row[i++]);
            dto.setCliente1Direccion((String) row[i++]);
            dto.setCliente1Distrito((String) row[i++]);
            dto.setCliente2Nombre((String) row[i++]);
            dto.setCliente2Apellidos((String) row[i++]);
            dto.setCliente2NumDocumento((String) row[i++]);
            dto.setAval1Nombre((String) row[i++]);
            dto.setAval1Apellidos((String) row[i++]);
            dto.setAval1NumDocumento((String) row[i++]);
            dto.setLote1Manzana((String) row[i++]);
            dto.setLote1NumeroLote((String) row[i++]);
            dto.setLote1Area((BigDecimal) row[i++]);
            dto.setLote2Manzana((String) row[i++]);
            dto.setLote2NumeroLote((String) row[i++]);
            dto.setLote2Area((BigDecimal) row[i++]);
            dto.setProgramaNombre((String) row[i++]);
            reportes.add(dto);
        }
        return reportes;
    }

    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));

        Distrito distrito = distritoRepository.findById(generarLetrasRequest.getIdDistrito())
                .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado con el ID: " + generarLetrasRequest.getIdDistrito()));

        int cantidad = contrato.getCantidadLetras();
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad de letras debe ser mayor a cero");

        BigDecimal saldo = contrato.getSaldo();
        if (saldo == null || saldo.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El saldo del contrato es invalido o cero");

        LocalDate fechaVencimientoInicial = generarLetrasRequest.getFechaVencimientoInicial();
        int diaOriginal = fechaVencimientoInicial.getDayOfMonth();
        boolean esUltimoDiaDeSuMes = diaOriginal == fechaVencimientoInicial.lengthOfMonth();
        boolean forzarUltimoDia = esUltimoDiaDeSuMes && generarLetrasRequest.isUsarUltimoDiaMes();

        Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
        List<LetraCambio> letrasAGuardar = new ArrayList<>(cantidad);

        // ── MODO GRUPOS: distintos montos por tramos ──────────────────────────
        if (generarLetrasRequest.isModoGrupos()) {
            List<GrupoLetrasRequest> grupos = generarLetrasRequest.getGrupos();

            if (grupos == null || grupos.isEmpty())
                throw new IllegalArgumentException("Debe definir al menos un grupo de letras.");

            // Validar que la suma de cantidades coincida con las del contrato
            int totalCantidad = grupos.stream()
                    .mapToInt(g -> g.getCantidad() != null ? g.getCantidad() : 0)
                    .sum();
            if (totalCantidad != cantidad)
                throw new IllegalArgumentException(
                    "La suma de letras en los grupos (" + totalCantidad +
                    ") no coincide con la cantidad del contrato (" + cantidad + ").");

            // Parsear y validar importes de cada grupo
            List<BigDecimal> importesGrupo = new ArrayList<>();
            for (int g = 0; g < grupos.size(); g++) {
                GrupoLetrasRequest grupo = grupos.get(g);
                if (grupo.getCantidad() == null || grupo.getCantidad() <= 0)
                    throw new IllegalArgumentException("El grupo " + (g + 1) + " tiene una cantidad invalida.");
                if (grupo.getImporte() == null || grupo.getImporte().isBlank())
                    throw new IllegalArgumentException("El grupo " + (g + 1) + " no tiene importe definido.");
                try {
                    String importeStr = grupo.getImporte()
                            .replace("$", "").replace("S/", "").replace(",", "").trim();
                    BigDecimal imp = new BigDecimal(importeStr).setScale(2, RoundingMode.HALF_UP);
                    if (imp.compareTo(BigDecimal.ZERO) <= 0)
                        throw new IllegalArgumentException("El importe del grupo " + (g + 1) + " debe ser mayor a cero.");
                    importesGrupo.add(imp);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Importe invalido en el grupo " + (g + 1) + ": " + grupo.getImporte());
                }
            }

            // Validar que la suma total de los grupos == saldo del contrato
            BigDecimal saldoRedondeado = saldo.setScale(2, RoundingMode.HALF_UP);
            BigDecimal sumaGrupos = BigDecimal.ZERO;
            for (int g = 0; g < grupos.size(); g++) {
                sumaGrupos = sumaGrupos.add(
                    importesGrupo.get(g).multiply(new BigDecimal(grupos.get(g).getCantidad()))
                );
            }
            if (sumaGrupos.compareTo(saldoRedondeado) != 0)
                throw new IllegalArgumentException(
                    "La suma total de los grupos (" + sumaGrupos.toPlainString() +
                    ") no coincide con el saldo del contrato (" + saldoRedondeado.toPlainString() + ")." +
                    " Diferencia: " + sumaGrupos.subtract(saldoRedondeado).toPlainString());

            // Generar letras grupo a grupo con numeración global correlativa
            int numeroLetraGlobal = 1;
            for (int g = 0; g < grupos.size(); g++) {
                GrupoLetrasRequest grupo = grupos.get(g);
                BigDecimal importeGrupo = importesGrupo.get(g);

                for (int i = 0; i < grupo.getCantidad(); i++) {
                    LocalDate fechaCalculada = fechaVencimientoInicial.plusMonths(numeroLetraGlobal - 1);
                    LocalDate fechaFinal = forzarUltimoDia
                            ? fechaCalculada.withDayOfMonth(fechaCalculada.lengthOfMonth())
                            : fechaCalculada.withDayOfMonth(Math.min(diaOriginal, fechaCalculada.lengthOfMonth()));

                    LetraCambio letra = new LetraCambio();
                    letra.setContrato(contrato);
                    letra.setDistrito(distrito);
                    letra.setFechaGiro(generarLetrasRequest.getFechaGiro());
                    letra.setFechaVencimiento(fechaFinal);
                    letra.setImporte(importeGrupo);
                    letra.setImporteLetras(NumeroALetras.convertir(importeGrupo, monedaContrato));
                    letra.setEstadoLetra(EstadoLetra.PENDIENTE);
                    letra.setNumeroLetra(numeroLetraGlobal + "/" + cantidad);
                    letrasAGuardar.add(letra);
                    numeroLetraGlobal++;
                }
            }

        // ── MODO AUTOMÁTICO: el backend calcula el importe dividiendo saldo / cantidad ──
        } else if (generarLetrasRequest.isModoAutomatico()) {
            BigDecimal importePorLetra;
            BigDecimal importeUltimaLetra;

            BigDecimal saldoEntero = saldo.setScale(0, RoundingMode.HALF_UP);
            importePorLetra = saldoEntero.divide(new BigDecimal(cantidad), 0, RoundingMode.DOWN);
            BigDecimal sumaParcial = importePorLetra.multiply(new BigDecimal(cantidad - 1));
            importeUltimaLetra = saldoEntero.subtract(sumaParcial).setScale(0, RoundingMode.HALF_UP);

            for (int i = 1; i <= cantidad; i++) {
                LocalDate fechaCalculada = fechaVencimientoInicial.plusMonths(i - 1);
                LocalDate fechaFinal = forzarUltimoDia
                        ? fechaCalculada.withDayOfMonth(fechaCalculada.lengthOfMonth())
                        : fechaCalculada.withDayOfMonth(Math.min(diaOriginal, fechaCalculada.lengthOfMonth()));

                LetraCambio letra = new LetraCambio();
                letra.setContrato(contrato);
                letra.setDistrito(distrito);
                letra.setFechaGiro(generarLetrasRequest.getFechaGiro());
                letra.setFechaVencimiento(fechaFinal);
                letra.setImporte(i < cantidad ? importePorLetra : importeUltimaLetra);
                letra.setImporteLetras(NumeroALetras.convertir(letra.getImporte(), monedaContrato));
                letra.setEstadoLetra(EstadoLetra.PENDIENTE);
                letra.setNumeroLetra(i + "/" + cantidad);
                letrasAGuardar.add(letra);
            }

        // ── MODO MANUAL: el usuario define un importe fijo, la última absorbe el residuo ──
        } else {
            BigDecimal importePorLetra;
            BigDecimal importeUltimaLetra;

            try {
                String importeStr = generarLetrasRequest.getImporte()
                        .replace("$", "").replace("S/", "").replace(",", "").trim();
                importePorLetra = new BigDecimal(importeStr).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Importe invalido: " + generarLetrasRequest.getImporte());
            }
            BigDecimal saldoRedondeado = saldo.setScale(2, RoundingMode.HALF_UP);
            BigDecimal sumaParcialManual = importePorLetra.multiply(new BigDecimal(cantidad - 1));
            BigDecimal ultimaLetraManual = saldoRedondeado.subtract(sumaParcialManual);
            if (ultimaLetraManual.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    "El importe de $ " + importePorLetra.toPlainString() +
                    " es demasiado alto: " + (cantidad - 1) + " letras ya superan el saldo de $ " +
                    saldoRedondeado.toPlainString() +
                    ". Reduzca el importe para que la ultima letra tenga un valor positivo."
                );
            }
            importeUltimaLetra = ultimaLetraManual;

            for (int i = 1; i <= cantidad; i++) {
                LocalDate fechaCalculada = fechaVencimientoInicial.plusMonths(i - 1);
                LocalDate fechaFinal = forzarUltimoDia
                        ? fechaCalculada.withDayOfMonth(fechaCalculada.lengthOfMonth())
                        : fechaCalculada.withDayOfMonth(Math.min(diaOriginal, fechaCalculada.lengthOfMonth()));

                LetraCambio letra = new LetraCambio();
                letra.setContrato(contrato);
                letra.setDistrito(distrito);
                letra.setFechaGiro(generarLetrasRequest.getFechaGiro());
                letra.setFechaVencimiento(fechaFinal);
                letra.setImporte(i < cantidad ? importePorLetra : importeUltimaLetra);
                letra.setImporteLetras(NumeroALetras.convertir(letra.getImporte(), monedaContrato));
                letra.setEstadoLetra(EstadoLetra.PENDIENTE);
                letra.setNumeroLetra(i + "/" + cantidad);
                letrasAGuardar.add(letra);
            }
        }

        // ── Persistir todas las letras (compartido por los 3 modos) ──────────
        letraCambioRepository.saveAll(letrasAGuardar);
        recalcularEstadoContrato(contrato);
    }
    
    
    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public LetraCambioDTO actualizarLetra(Integer id, LetraCambioDTO letraCambioDTO) {
        LetraCambio letraExistente = letraCambioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Letra de cambio no encontrada con el ID: " + id));

        letraExistente.setFechaGiro(letraCambioDTO.getFechaGiro());
        letraExistente.setFechaVencimiento(letraCambioDTO.getFechaVencimiento());
        letraExistente.setImporte(letraCambioDTO.getImporte());
        letraExistente.setImporteLetras(letraCambioDTO.getImporteLetras());
        letraExistente.setEstadoLetra(EstadoLetra.valueOf(letraCambioDTO.getEstadoLetra()));

        LetraCambio letraActualizada = letraCambioRepository.save(letraExistente);
        recalcularEstadoContrato(letraActualizada.getContrato());
        return modelMapper.map(letraActualizada, LetraCambioDTO.class);
    }

    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public void eliminarPorContrato(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado con el ID: " + idContrato));

        // [2] @Modifying @Query en el repository → 1 solo DELETE SQL
        letraCambioRepository.deleteByContratoIdContrato(idContrato);

        // [4] NO llamamos recalcularEstadoContrato porque ya no hay letras.
        // Sin letras no puede haber ninguna VENCIDA → el estado siempre es ACTIVO.
        // Esto evita el SELECT innecesario a la BD remota después del DELETE.
        if (contrato.getTipoContrato() == TipoContrato.FINANCIADO) {
            EstadoContrato estadoActual = contrato.getEstadoContrato();
            boolean estadoTerminal =
                estadoActual == EstadoContrato.CANCELADO    ||
                estadoActual == EstadoContrato.RESUELTO      ||
                estadoActual == EstadoContrato.EN_RESOLUCION ||
                estadoActual == EstadoContrato.RENUNCIA      ||
                estadoActual == EstadoContrato.TRANSFERIDO   ||
                estadoActual == EstadoContrato.CARTA_NOTARIAL;

            if (!estadoTerminal && estadoActual != EstadoContrato.ACTIVO) {
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);
                contratoRepository.save(contrato);
            }
        }
    }

    @Override
    @Transactional
    public byte[] generarPdfLetras(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contrato no encontrado con el ID: " + idContrato));

        String moneda = (contrato.getMoneda() != null)
                ? contrato.getMoneda().name()
                : Moneda.USD.name();

        List<ReporteLetraCambioDTO> reportes = obtenerReportePorContrato(idContrato);

        if (reportes.isEmpty()) {
            throw new IllegalStateException(
                    "El contrato " + idContrato + " no tiene letras generadas.");
        }

        return LetraCambioPdf.generar(reportes, moneda);
    }
}