package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.CalculoMoraDTO;
import com.Inmobiliaria.demo.dto.PortalContratoDTO;
import com.Inmobiliaria.demo.dto.PortalDatosDTO;
import com.Inmobiliaria.demo.dto.PortalLetraDTO;
import com.Inmobiliaria.demo.dto.PortalResumenDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ClienteRepository;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.security.UsuarioActualResolver;
import com.Inmobiliaria.demo.service.MoraService;
import com.Inmobiliaria.demo.service.PortalClienteService;
import com.Inmobiliaria.demo.util.MascaraUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del portal del cliente (solo lectura).
 *
 * Identidad: siempre resuelta por el email del token JWT (X-Auth-User del
 * gateway); el cliente NUNCA envía ids por parámetro.
 *
 * Transacciones readOnly: el portal jamás escribe (el estado "VENCIDA" se
 * calcula en el DTO, sin tocar la entidad, a diferencia del staff donde
 * listarPorContrato sí puede persistir el cambio por dirty checking).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalClienteServiceImpl implements PortalClienteService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ClienteRepository clienteRepository;
    private final ContratoRepository contratoRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final MoraService moraService;
    private final UsuarioActualResolver usuarioActualResolver;

    // ── Mis datos ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PortalDatosDTO obtenerMisDatos() {
        Cliente cliente = clientesActuales().get(0);

        PortalDatosDTO dto = new PortalDatosDTO();
        dto.setNombre(cliente.getNombre());
        dto.setApellidos(cliente.getApellidos());
        dto.setNumDocEnmascarado(MascaraUtil.enmascararDocumento(cliente.getNumDoc()));
        dto.setCelularEnmascarado(MascaraUtil.enmascararCelular(cliente.getCelular()));
        dto.setTelefonoEnmascarado(MascaraUtil.enmascararCelular(cliente.getTelefono()));
        dto.setEmail(cliente.getEmail());
        dto.setDireccion(cliente.getDireccion());
        dto.setGenero(cliente.getGenero() != null ? cliente.getGenero().name() : null);
        dto.setEstadoCivil(cliente.getEstadoCivil() != null ? cliente.getEstadoCivil().name() : null);
        dto.setTipoCliente(cliente.getTipoCliente() != null ? cliente.getTipoCliente().name() : null);

        if (cliente.getDistrito() != null) {
            dto.setDistrito(cliente.getDistrito().getNombre());
            dto.setProvincia(cliente.getDistrito().getProvincia());
            dto.setDepartamento(cliente.getDistrito().getDepartamento());
        }
        return dto;
    }

    // ── Resumen ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PortalResumenDTO obtenerResumen() {
        List<Cliente> clientes = clientesActuales();
        Cliente principal = clientes.get(0);

        PortalResumenDTO resumen = new PortalResumenDTO();
        resumen.setNombreCliente(concatenar(principal.getNombre(), principal.getApellidos()));

        for (PortalContratoDTO contrato : construirContratos()) {
            resumen.getContratos().add(contrato);

            boolean esUsd = Moneda.USD.name().equals(contrato.getMoneda());
            if (esUsd) {
                resumen.setDeudaTotalUsd(resumen.getDeudaTotalUsd().add(contrato.getDeudaTotal()));
                resumen.setDeudaInmediataUsd(resumen.getDeudaInmediataUsd().add(contrato.getDeudaInmediata()));
                resumen.setMoraInmediataUsd(resumen.getMoraInmediataUsd().add(contrato.getMoraInmediata()));
            } else {
                resumen.setDeudaTotalPen(resumen.getDeudaTotalPen().add(contrato.getDeudaTotal()));
                resumen.setDeudaInmediataPen(resumen.getDeudaInmediataPen().add(contrato.getDeudaInmediata()));
                resumen.setMoraInmediataPen(resumen.getMoraInmediataPen().add(contrato.getMoraInmediata()));
            }
            resumen.setLetrasPagadas(resumen.getLetrasPagadas() + contrato.getLetrasPagadas());
            resumen.setLetrasVencidas(resumen.getLetrasVencidas() + contrato.getLetrasVencidas());
            resumen.setLetrasProximas(resumen.getLetrasProximas() + contrato.getLetrasProximas());
        }
        return resumen;
    }

    // ── Letras ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PortalContratoDTO> obtenerLetras() {
        return construirContratos();
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    /** Carga contratos del cliente con letra a letra, deudas y contadores. */
    private List<PortalContratoDTO> construirContratos() {
        List<Cliente> clientes = clientesActuales();
        List<Integer> idsCliente = clientes.stream().map(Cliente::getIdCliente).toList();

        List<Contrato> contratos = contratoRepository.findByClientesIdClienteIn(idsCliente);
        LocalDate hoy = LocalDate.now(ZONA_LIMA);

        List<PortalContratoDTO> resultado = new ArrayList<>();
        for (Contrato contrato : contratos) {
            resultado.add(construirContrato(contrato, hoy));
        }
        // Sin contratos: se devuelve lista vacía (el frontend muestra el estado vacío)
        log.debug("Portal: {} contratos para cliente(s) {}", resultado.size(), idsCliente);
        return resultado;
    }

    private PortalContratoDTO construirContrato(Contrato contrato, LocalDate hoy) {
        PortalContratoDTO dto = new PortalContratoDTO();
        dto.setIdContrato(contrato.getIdContrato());
        dto.setTipoContrato(contrato.getTipoContrato() != null ? contrato.getTipoContrato().name() : null);
        dto.setEstadoContrato(contrato.getEstadoContrato() != null ? contrato.getEstadoContrato().name() : null);
        dto.setMoneda(contrato.getMoneda() != null ? contrato.getMoneda().name() : Moneda.USD.name());
        dto.setMontoTotal(contrato.getMontoTotal());
        dto.setSaldo(contrato.getSaldo());
        dto.setCantidadLetras(contrato.getCantidadLetras());
        dto.setLote(describirLote(contrato));

        BigDecimal deudaTotal = BigDecimal.ZERO;
        BigDecimal deudaInmediata = BigDecimal.ZERO;
        BigDecimal moraInmediata = BigDecimal.ZERO;
        int pagadas = 0, vencidas = 0, proximas = 0;

        for (LetraCambio letra : letraCambioRepository.findByContratoIdContrato(contrato.getIdContrato())) {
            String estado = estadoEfectivo(letra, hoy);
            boolean estaVencida = "VENCIDA".equals(estado);
            int diasVencida = estaVencida
                    ? (int) ChronoUnit.DAYS.between(letra.getFechaVencimiento(), hoy)
                    : 0;
            BigDecimal mora = estaVencida ? moraEnVivo(letra.getIdLetra()) : BigDecimal.ZERO;
            BigDecimal saldoLetra = saldoDeLetra(letra);

            PortalLetraDTO letraDto = new PortalLetraDTO(
                    letra.getIdLetra(),
                    contrato.getIdContrato(),
                    letra.getNumeroLetra(),
                    letra.getFechaVencimiento(),
                    letra.getImporte(),
                    letra.getSaldoPendiente(),
                    estado,
                    diasVencida,
                    mora);
            dto.getLetras().add(letraDto);

            switch (estado) {
                case "PAGADA" -> pagadas++;
                case "VENCIDA" -> {
                    vencidas++;
                    deudaInmediata = deudaInmediata.add(saldoLetra);
                    moraInmediata = moraInmediata.add(mora);
                }
                case "ANULADO" -> { /* no cuenta */ }
                default -> { // PENDIENTE o PARCIAL (aún no vencida)
                    proximas++;
                }
            }
            if (!"PAGADA".equals(estado) && !"ANULADO".equals(estado)) {
                deudaTotal = deudaTotal.add(saldoLetra);
            }
        }

        dto.setDeudaTotal(deudaTotal);
        dto.setDeudaInmediata(deudaInmediata);
        dto.setMoraInmediata(moraInmediata);
        dto.setLetrasPagadas(pagadas);
        dto.setLetrasVencidas(vencidas);
        dto.setLetrasProximas(proximas);
        return dto;
    }

    /**
     * Estado que el cliente debe VER hoy, calculado sin persistir.
     * Usa la misma gracia dominical que la mora (MoraServiceImpl).
     */
    private String estadoEfectivo(LetraCambio letra, LocalDate hoy) {
        if (letra.getEstadoLetra() == EstadoLetra.ANULADO) {
            return "ANULADO";
        }
        if (letra.getEstadoLetra() == EstadoLetra.PAGADO) {
            return "PAGADA";
        }
        LocalDate efectiva = MoraServiceImpl.aplicarGraciaDominical(letra.getFechaVencimiento());
        boolean vencida = letra.getEstadoLetra() == EstadoLetra.VENCIDO
                || (efectiva != null && efectiva.isBefore(hoy));
        return vencida ? "VENCIDA"
                : letra.getEstadoLetra() == EstadoLetra.PARCIAL ? "PARCIAL" : "PENDIENTE";
    }

    /** Mora acumulada "en vivo" (no persiste); 0 si la letra no está vencida. */
    private BigDecimal moraEnVivo(Integer idLetra) {
        try {
            CalculoMoraDTO calculo = moraService.calcularMora(idLetra);
            return calculo.getMontoMoraTotal() != null ? calculo.getMontoMoraTotal() : BigDecimal.ZERO;
        } catch (NegocioException noVencida) {
            return BigDecimal.ZERO;
        }
    }

    /** Saldo adeudado de la letra: parcial usa saldoPendiente; el resto, importe completo. */
    private static BigDecimal saldoDeLetra(LetraCambio letra) {
        if (letra.getEstadoLetra() == EstadoLetra.PARCIAL
                && letra.getSaldoPendiente() != null
                && letra.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            return letra.getSaldoPendiente();
        }
        return letra.getImporte() != null ? letra.getImporte() : BigDecimal.ZERO;
    }

    /** "Mz 3 - Lt 12 (Villa Real II)" si el contrato tiene lote; si no, null. */
    private static String describirLote(Contrato contrato) {
        if (contrato.getLotes() == null || contrato.getLotes().isEmpty()) {
            return null;
        }
        return contrato.getLotes().stream()
                .findFirst()
                .map(cl -> {
                    var lote = cl.getLote();
                    if (lote == null) return null;
                    String programa = lote.getPrograma() != null ? lote.getPrograma().getNombrePrograma() : null;
                    String base = "Mz " + lote.getManzana() + " - Lt " + lote.getNumeroLote();
                    return programa != null ? base + " (" + programa + ")" : base;
                })
                .orElse(null);
    }

    private List<Cliente> clientesActuales() {
        String email = usuarioActualResolver.email();
        List<Cliente> clientes = clienteRepository.findByEmailIgnoreCase(email);
        if (clientes.isEmpty()) {
            throw new NegocioException("No se encontró la cuenta de cliente para este correo. Contacte con la inmobiliaria.");
        }
        return clientes;
    }

    private static String concatenar(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return (a + " " + b).trim();
    }
}
