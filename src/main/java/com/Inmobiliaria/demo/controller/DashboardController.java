package com.Inmobiliaria.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.IngresoDiarioDTO;
import com.Inmobiliaria.demo.dto.IngresoMensualDTO;
import com.Inmobiliaria.demo.service.ClienteService;
import com.Inmobiliaria.demo.service.DashboardService;
import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.service.ParceleroService;
import com.Inmobiliaria.demo.service.ProgramaService;
import com.Inmobiliaria.demo.service.VendedorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final VendedorService    vendedorService;
    private final ParceleroService   parceleroService;
    private final ProgramaService    programaService;
    private final LoteService        loteService;
    private final ClienteService     clienteService;
    private final DashboardService   dashboardService;

    // ── Cliente Feign hacia ms-servicios-basicos ───────────────────────────────
    private final InscripcionClient    inscripcionClient;

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINT 1 — Totales generales (tarjetas y gráficos del dashboard)
    // GET /api/dashboard/totales
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/totales")
    public Map<String, Object> obtenerTotales() {
        Map<String, Object> respuesta = new HashMap<>();

        // 1. Conteos para las tarjetas superiores
        respuesta.put("vendedores", (long) vendedorService.listarVendedores().size());
        respuesta.put("parceleros", (long) parceleroService.listarParceleros().size());
        respuesta.put("programas",  (long) programaService.listProgramas().size());
        respuesta.put("lotes",      (long) loteService.listarLotes().size());
        respuesta.put("clientes",   (long) clienteService.listarClientes().size());

        // 2. Gráfico de Lotes (Disponible, Vendido, Separado)
        List<Object[]> resultadosLotes = loteService.obtenerConteoPorEstadoYPrograma();
        respuesta.put("graficoLotes", procesarResultadosParaGrafico(resultadosLotes));

        // 3. Gráfico de Contratos (CONTADO vs FINANCIADO)
        List<Object[]> resultadosContratos = dashboardService.contarContratosPorProgramaYTipo();
        respuesta.put("graficoContratos", procesarResultadosParaGrafico(resultadosContratos));

        return respuesta;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINT 2 — Ingresos diarios consolidados
    // GET /api/dashboard/ingresos-diarios
    // GET /api/dashboard/ingresos-diarios?fecha=2026-05-19
    //
    // Suma:
    //   ① pago_letra        → pagos de letras de cambio
    //   ② pago_mora         → pagos de moras sobre letras vencidas
    //   ③ pago_inicial      → inicial de contratos (CONTADO y FINANCIADO)
    //   ④ inscripciones_servicios (ms-servicios-basicos) → inscripción LUZ/AGUA
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/ingresos-diarios")
    public IngresoDiarioDTO obtenerIngresosDiarios(
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        // Si no se pasa fecha, se usa hoy
        LocalDate diaConsulta = (fecha != null) ? fecha : LocalDate.now();

        // ① Pago de letras
        BigDecimal totalLetras    = dashboardService.sumPagoLetrasByFecha(diaConsulta);
        long       cantidadLetras = dashboardService.countPagoLetrasByFecha(diaConsulta);

        // ② Pago de moras
        BigDecimal totalMoras    = dashboardService.sumPagoMorasByFecha(diaConsulta);
        long       cantidadMoras = dashboardService.countPagoMorasByFecha(diaConsulta);

        // ③ Pago de iniciales (contratos)
        BigDecimal totalIniciales    = dashboardService.sumPagoInicialesByFecha(diaConsulta);
        long       cantidadIniciales = dashboardService.countPagoInicialesByFecha(diaConsulta);

        // ④ Inscripciones de servicios básicos (microservicio externo)
        BigDecimal totalInscripciones    = BigDecimal.ZERO;
        long       cantidadInscripciones = 0L;

        try {
            Map<String, Object> respuestaMicroservicio =
                    inscripcionClient.obtenerIngresosDiarios(diaConsulta.toString());

            if (respuestaMicroservicio != null) {
                Object montoObj    = respuestaMicroservicio.get("totalMonto");
                Object cantidadObj = respuestaMicroservicio.get("cantidad");

                if (montoObj != null) {
                    totalInscripciones = new BigDecimal(montoObj.toString());
                }
                if (cantidadObj != null) {
                    cantidadInscripciones = Long.parseLong(cantidadObj.toString());
                }
            }
        } catch (Exception e) {
            // Si el microservicio no está disponible, registramos el error y continuamos
            // con 0 para no romper el dashboard principal
            log.warn("No se pudo obtener ingresos de servicios básicos para {}: {}", diaConsulta, e.getMessage());
        }

        // Gran total
        BigDecimal totalGeneral = totalLetras
                .add(totalMoras)
                .add(totalIniciales)
                .add(totalInscripciones);

        return IngresoDiarioDTO.builder()
                .totalPagoLetras(totalLetras)
                .cantidadPagoLetras(cantidadLetras)
                .totalPagoMoras(totalMoras)
                .cantidadPagoMoras(cantidadMoras)
                .totalPagoIniciales(totalIniciales)
                .cantidadPagoIniciales(cantidadIniciales)
                .totalInscripcionesServicios(totalInscripciones)
                .cantidadInscripcionesServicios(cantidadInscripciones)
                .totalGeneral(totalGeneral)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENDPOINT 3 — Ingresos mensuales (hasta 12 meses)
    // GET /api/dashboard/ingresos-por-mes
    // GET /api/dashboard/ingresos-por-mes?desde=2026-05-01&hasta=2026-07-01
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/ingresos-por-mes")
    public List<IngresoMensualDTO> obtenerIngresosPorMes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        if (desde == null) desde = LocalDate.of(2026, 5, 1);
        if (hasta == null) hasta = LocalDate.now();

        // Limitar a máximo 12 meses
        LocalDate limite = hasta.minusMonths(11);
        if (desde.isBefore(limite)) desde = limite;

        // Obtener datos locales (letras, moras, iniciales)
        List<IngresoMensualDTO> ingresos = dashboardService.getIngresosPorMes(desde, hasta);

        // Inscripciones de servicios básicos (microservicio)
        try {
            List<Map<String, Object>> inscripcionesPorMes =
                    inscripcionClient.obtenerIngresosPorMes(desde.toString(), hasta.toString());

            if (inscripcionesPorMes != null) {
                for (Map<String, Object> fila : inscripcionesPorMes) {
                    int mes = Integer.parseInt(fila.get("mes").toString());
                    int anio = Integer.parseInt(fila.get("anio").toString());
                    BigDecimal total = new BigDecimal(fila.get("total").toString());

                    for (IngresoMensualDTO dto : ingresos) {
                        if (dto.getMes() == mes && dto.getAnio() == anio) {
                            dto.setTotalInscripcionesServicios(total);
                            dto.setTotalGeneral(
                                dto.getTotalPagoLetras()
                                    .add(dto.getTotalPagoMoras())
                                    .add(dto.getTotalPagoIniciales())
                                    .add(total)
                            );
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener ingresos mensuales de servicios básicos: {}", e.getMessage());
        }

        return ingresos;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Método auxiliar genérico
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convierte una lista de Object[] en Map<Nombre, Map<Categoria, Cantidad>>
     * que requiere el Frontend para los gráficos de barras.
     */
    private Map<String, Map<String, Long>> procesarResultadosParaGrafico(List<Object[]> resultados) {
        Map<String, Map<String, Long>> mapaFinal = new HashMap<>();

        if (resultados != null) {
            for (Object[] fila : resultados) {
                String nombrePrograma = (String) fila[0];
                String categoria      = fila[1].toString();
                Long   cantidad       = (Long) fila[2];

                mapaFinal.putIfAbsent(nombrePrograma, new HashMap<>());
                mapaFinal.get(nombrePrograma).put(categoria, cantidad);
            }
        }
        return mapaFinal;
    }
}