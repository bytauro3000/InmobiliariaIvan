package com.Inmobiliaria.demo.dto.sunat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletaRequest {
    private String tipoDocumento;
    private String serie;
    private Integer numero;
    private String fechaEmision;
    private String horaEmision;
    private String moneda;
    private ClienteDto cliente;
    private List<DetalleDto> detalles;
    private BigDecimal totalGravado;
    private BigDecimal totalIgv;
    private BigDecimal total;
    private String leyenda;
    private String montoLetras;
}
