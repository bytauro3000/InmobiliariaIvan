package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrato del cliente en el portal, con sus letras y deudas.
 * Los montos NO se suman entre monedas: cada contrato lleva la suya (USD/PEN).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalContratoDTO {

    private Integer idContrato;
    private String  tipoContrato;    // CONTADO | FINANCIADO
    private String  estadoContrato;  // ACTIVO | MORA | ...
    private String  moneda;          // USD | PEN
    private BigDecimal montoTotal;
    private BigDecimal saldo;
    private Integer cantidadLetras;
    private String  lote;            // "Manzana X - Lote Y (Programa)" o null

    private List<PortalLetraDTO> letras = new ArrayList<>();

    // Deudas derivadas de las letras (solo FINANCIADO tiene letras)
    private BigDecimal deudaTotal = BigDecimal.ZERO;      // saldo de letras no pagadas
    private BigDecimal deudaInmediata = BigDecimal.ZERO;  // letras vencidas
    private BigDecimal moraInmediata = BigDecimal.ZERO;   // mora acumulada de vencidas

    private Integer letrasPagadas = 0;
    private Integer letrasVencidas = 0;
    private Integer letrasProximas = 0;   // pendientes/parciales aún no vencidas
}
