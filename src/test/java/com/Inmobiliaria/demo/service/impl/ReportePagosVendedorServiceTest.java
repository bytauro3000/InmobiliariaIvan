package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ReportePagosVendedorDTO;
import com.Inmobiliaria.demo.entity.ComisionVendedor;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoComisionVendedor;
import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.repository.ComisionVendedorRepository;
import com.Inmobiliaria.demo.repository.ContratoClienteRepository;
import com.Inmobiliaria.demo.repository.ContratoLoteRepository;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.PagoComisionVendedorRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.VendedorRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import com.Inmobiliaria.demo.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario del reporte de pagos a vendedores: construcción de filas,
 * totales por tipo (ADELANTO / MENSUAL) y normalización de filtros.
 */
@ExtendWith(MockitoExtension.class)
class ReportePagosVendedorServiceTest {

    @Mock private ComisionVendedorRepository comisionRepository;
    @Mock private PagoComisionVendedorRepository pagoComisionRepository;
    @Mock private LetraCambioRepository letraRepository;
    @Mock private ContratoLoteRepository contratoLoteRepository;
    @Mock private ContratoClienteRepository contratoClienteRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private PagoInicialRepository pagoInicialRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ReciboEgresoService reciboEgresoService;
    @Mock private VendedorRepository vendedorRepository;

    @InjectMocks
    private ComisionVendedorServiceImpl service;

    private Vendedor vendedor() {
        Vendedor v = new Vendedor();
        v.setIdVendedor(1);
        v.setNombre("Miguel");
        v.setApellidos("Paredes Gómez");
        v.setDni("65432103");
        return v;
    }

    private ComisionVendedor comision() {
        Contrato contrato = new Contrato();
        contrato.setIdContrato(10);

        ComisionVendedor c = new ComisionVendedor();
        c.setIdComision(100);
        c.setContrato(contrato);
        c.setVendedor(vendedor());
        c.setMoneda(Moneda.USD);
        return c;
    }

    private PagoComisionVendedor adelanto() {
        PagoComisionVendedor p = new PagoComisionVendedor();
        p.setIdPagoComision(2);
        p.setComision(comision());
        p.setTipo("ADELANTO");
        p.setMonto(new BigDecimal("100.00"));
        p.setFechaPago(LocalDateTime.of(2026, 10, 1, 10, 0));
        p.setMedioPago(MedioPago.EFECTIVO);
        p.setNumeroEgreso("EG01-1");
        return p; // letra == null
    }

    private PagoComisionVendedor cuota() {
        LetraCambio letra = new LetraCambio();
        letra.setIdLetra(55);
        letra.setNumeroLetra("8/120");

        PagoComisionVendedor p = new PagoComisionVendedor();
        p.setIdPagoComision(1);
        p.setComision(comision());
        p.setLetra(letra);
        p.setTipo("MENSUAL");
        p.setMonto(new BigDecimal("25.00"));
        p.setFechaPago(LocalDateTime.of(2026, 11, 1, 9, 30));
        p.setNumeroEgreso("EG01-2");
        return p;
    }

    @Test
    void debeConstruirFilasYTotalesPorTipo() {
        // El repositorio devuelve del más reciente al más antiguo
        when(pagoComisionRepository.buscarParaReportePagos(0, ""))
                .thenReturn(List.of(cuota(), adelanto()));
        when(contratoLoteRepository.findLotesByContratos(List.of(10)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{10, "A", "12"},
                        new Object[]{10, "A", "13"}));
        Programa programa = new Programa();
        programa.setNombrePrograma("Florida");
        when(contratoLoteRepository.findProgramasByContratos(List.of(10)))
                .thenReturn(List.<Object[]>of(new Object[]{10, programa}));

        ReportePagosVendedorDTO dto = service.listarPagosVendedor(null, "TODOS");

        assertTrue(dto.isTodosVendedores());
        assertEquals("TODOS", dto.getFiltroTipo());
        assertEquals(2, dto.getTotalPagos());
        assertEquals(0, new BigDecimal("100.00").compareTo(dto.getTotalAdelantos()));
        assertEquals(0, new BigDecimal("25.00").compareTo(dto.getTotalMensuales()));
        assertEquals(0, new BigDecimal("125.00").compareTo(dto.getTotalGeneral()));
        assertEquals("USD", dto.getMoneda());

        // Fila 1: cuota (letra 8) con MZ/LT combinados y programa
        ReportePagosVendedorDTO.FilaPago filaCuota = dto.getPagos().get(0);
        assertEquals(1, filaCuota.getNumero());
        assertEquals("MENSUAL", filaCuota.getTipo());
        assertEquals("8/120", filaCuota.getNumeroLetra());
        assertEquals("A", filaCuota.getManzana());
        assertEquals("12, 13", filaCuota.getNumeroLote());
        assertEquals("Florida", filaCuota.getPrograma());
        assertEquals(10, filaCuota.getIdContrato());
        assertEquals("Miguel Paredes Gómez", filaCuota.getNombreVendedor());
        assertEquals("EG01-2", filaCuota.getNumeroEgreso());

        // Fila 2: adelanto sin letra
        ReportePagosVendedorDTO.FilaPago filaAdelanto = dto.getPagos().get(1);
        assertEquals(2, filaAdelanto.getNumero());
        assertEquals("ADELANTO", filaAdelanto.getTipo());
        assertNull(filaAdelanto.getNumeroLetra());
        assertEquals(0, new BigDecimal("100.00").compareTo(filaAdelanto.getMonto()));
        assertEquals("EFECTIVO", filaAdelanto.getMedioPago());
    }

    @Test
    void debeFiltrarPorVendedorYTipoNormalizado() {
        when(pagoComisionRepository.buscarParaReportePagos(anyInt(), anyString()))
                .thenReturn(List.of());
        when(vendedorRepository.findById(1)).thenReturn(Optional.of(vendedor()));

        // Vendedor + tipo en minúsculas → se normaliza a ADELANTO
        service.listarPagosVendedor(1, "adelanto");
        verify(pagoComisionRepository).buscarParaReportePagos(1, "ADELANTO");

        // Sin vendedor + TODOS → sentinels 0 / ""
        service.listarPagosVendedor(null, "TODOS");
        verify(pagoComisionRepository).buscarParaReportePagos(0, "");

        // Tipo inválido → se trata como todos
        service.listarPagosVendedor(1, "OTROCOSA");
        verify(pagoComisionRepository).buscarParaReportePagos(1, "");
    }

    @Test
    void debeDevolverCabeceraDeVendedorSinPagos() {
        when(pagoComisionRepository.buscarParaReportePagos(1, "MENSUAL")).thenReturn(List.of());
        when(vendedorRepository.findById(1)).thenReturn(Optional.of(vendedor()));

        ReportePagosVendedorDTO dto = service.listarPagosVendedor(1, "MENSUAL");

        assertEquals(false, dto.isTodosVendedores());
        assertEquals("MENSUAL", dto.getFiltroTipo());
        assertEquals("Miguel", dto.getNombreVendedor());
        assertEquals("Paredes Gómez", dto.getApellidosVendedor());
        assertEquals("65432103", dto.getDniVendedor());
        assertEquals(0, dto.getTotalPagos());
        assertTrue(dto.getPagos().isEmpty());
    }
}
