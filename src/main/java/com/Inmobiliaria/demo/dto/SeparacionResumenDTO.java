package com.Inmobiliaria.demo.dto;

import java.util.Date;
import java.util.List;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeparacionResumenDTO {
    private Integer idSeparacion;
    private Double monto;
    private Date fechaSepara;
    private Date fechaLimite;
    private EstadoSeparacion estadoSeparacion;
    private String nomVendedor;
    
    private List<ClienteDetalleDTO> clientes;
    private List<LoteDetalleDTO> lotes;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClienteDetalleDTO {
        private String nombreCompleto;
        private String numDoc;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoteDetalleDTO {
        private String manzana;
        private String numeroLote;
    }
}