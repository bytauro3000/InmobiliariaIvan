package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.EstadoMora;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MoraResponseDTO {

    private Integer idMora;

    // Datos de la letra que originó la mora
    private Integer idLetra;
    private String  numeroLetra;
    private BigDecimal importeLetra;
    private LocalDate fechaVencimientoLetra;

    // Pago de letra asociado (si se registró en el mismo acto)
    private Integer idPagoLetra;

    // Detalle del cálculo
    private Integer    diasMora;
    private BigDecimal porcentajeAplicado;   // 0.05
    private BigDecimal montoPorcentaje;      // importe * 5%
    private BigDecimal montoDiario;          // dias * $1
    private BigDecimal montoMoraTotal;       // total a cobrar

    // Metadatos
    private LocalDate fechaGeneracion;
    private EstadoMora estadoMora;

    // Pagos realizados sobre esta mora
    private List<PagoMoraResponseDTO> pagos;
}