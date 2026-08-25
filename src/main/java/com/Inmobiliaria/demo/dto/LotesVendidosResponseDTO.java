package com.Inmobiliaria.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de "lotes vendidos por vendedor".
 * Se agrupa por programa; cada programa contiene sus lotes y un subtotal.
 */
@Data
@Builder
public class LotesVendidosResponseDTO {

    private List<ProgramaDTO> programas;
    private BigDecimal totalGeneral;
    private long cantidadLotes;

    @Data
    @Builder
    public static class ProgramaDTO {
        private String nombrePrograma;
        private List<LoteVendidoDTO> lotes;
        private BigDecimal totalPrograma;
        private long cantidadLotes;
    }

    @Data
    @Builder
    public static class LoteVendidoDTO {
        private String manzana;
        private String numeroLote;
        private BigDecimal area;
        private BigDecimal costoVenta;      // monto_total del contrato
        private String cliente;             // nombre completo del comprador
        private String vendedor;            // nombre completo del vendedor
        private LocalDate fechaContrato;
        private String estadoContrato;
        private Integer idContrato;
    }
}