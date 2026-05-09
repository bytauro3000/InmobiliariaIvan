package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteResponseDTO {

    private Long idComprobante;
    private TipoComprobante tipoComprobante;
    private String serie;
    private Integer numero;
    private String numeroCompleto;
    private LocalDate fechaEmision;
    private BigDecimal monto;
    private TipoOrigenComprobante tipoOrigen;  
    private Integer referenciaId;
    private boolean emailEnviado;
}