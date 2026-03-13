package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO devuelto al marcar un contrato como TRANSFERIDO.
 * Contiene los datos calculados para pre-llenar el nuevo contrato
 * que se creará a nombre del cliente receptor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaResponseDTO {

    private Integer idContratoOriginal;

    // Datos del lote(s) — para pre-llenar el nuevo contrato
    private List<LoteResponseDTO> lotes;
    private List<Integer> idLotes;

    // Datos del vendedor original
    private Integer idVendedor;
    private String nombreVendedor;

    // Cálculo financiero
    private BigDecimal montoTotal;          // El mismo monto total del contrato original
    private BigDecimal montoPagado;         // Suma de letras PAGADAS → sugerido como inicial
    private BigDecimal saldoPendiente;      // montoTotal - montoPagado → saldo del nuevo contrato
    private Integer letrasRestantes;        // Cantidad de letras aún PENDIENTES o VENCIDAS
    private Integer letrasOriginales;       // Total de letras del contrato original

    // Mensaje informativo para mostrar en el frontend
    private String resumen;
}