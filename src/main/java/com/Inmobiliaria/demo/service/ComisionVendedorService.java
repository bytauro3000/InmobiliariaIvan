package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ComisionVendedorDTO;
import com.Inmobiliaria.demo.dto.PagoComisionMensualDTO;
import com.Inmobiliaria.demo.dto.PagoComisionResultadoDTO;
import com.Inmobiliaria.demo.dto.RegistrarAdelantoRequest;
import com.Inmobiliaria.demo.dto.RegistrarPagosMensualesRequest;
import com.Inmobiliaria.demo.dto.ActualizarMontoComisionRequest;
import com.Inmobiliaria.demo.entity.ComisionVendedor;
import com.Inmobiliaria.demo.entity.Contrato;

import java.util.List;
import java.util.Map;

public interface ComisionVendedorService {

    /** Crea la comisión al guardar un contrato con vendedor (si aplica). */
    ComisionVendedor crearComisionSiAplica(Contrato contrato);

    /** Lista todas las comisiones (secretaría). */
    List<ComisionVendedorDTO> listarComisiones();

    /**
     * Migración (backfill): crea comisiones para los contratos FINANCIADO/CONTADO
     * existentes con vendedor y % &gt; 0 que aún no tienen comisión. No crea comisión
     * en contratos RENUNCIADOS/RESUELTOS/TRANSFERIDOS. Idempotente.
     */
    Map<String, Object> migrarComisiones();

    /** Pagos mensuales habilitados de una comisión (letras pagadas > 8 sin pago). */
    List<PagoComisionMensualDTO> pagosMensualesHabilitados(Integer idComision);

    /** Registra el adelanto de comisión y genera recibo de egresos EG01. */
    PagoComisionResultadoDTO registrarAdelanto(RegistrarAdelantoRequest request);

    /**
     * Actualiza el monto total de comisión acordado (negociado por el gerente).
     * Solo aplica mientras no haya pagos registrados. No puede superar el 3% del
     * monto total del contrato.
     */
    ComisionVendedorDTO actualizarMontoComision(ActualizarMontoComisionRequest request);

    /** Registra uno o varios pagos mensuales y genera un único recibo de egresos. */
    PagoComisionResultadoDTO registrarPagosMensuales(RegistrarPagosMensualesRequest request);

    /** Marca la comisión como ANULADA cuando el contrato se renuncia/resuelve. */
    void anularComisionSiExiste(Integer idContrato);

    /**
     * Sincroniza el vendedor de la comisión al editar el contrato (cambio de vendedor).
     * Recalcula %/montos solo si aún no hay pagos registrados. Si el nuevo vendedor
     * no tiene comisión y no hay pagos, anula la comisión.
     */
    void sincronizarVendedorComision(Contrato contrato);
}