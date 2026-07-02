package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoListItemDTO {
    private Integer idContrato;
    private LocalDate fechaContrato;
    private TipoContrato tipoContrato;
    private EstadoContrato estadoContrato;
    private BigDecimal montoTotal;
    private BigDecimal inicial;
    private BigDecimal saldo;
    private Integer cantidadLetras;
    private Moneda moneda;
    private List<ClienteSimpleDTO> clientes;
    private List<LoteSimpleDTO> lotes;
    private boolean tieneLetras;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteSimpleDTO {
        private String nombre;
        private String apellidos;
        private String numDoc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteSimpleDTO {
        private String manzana;
        private String numeroLote;
        private String nombrePrograma;
    }
}
