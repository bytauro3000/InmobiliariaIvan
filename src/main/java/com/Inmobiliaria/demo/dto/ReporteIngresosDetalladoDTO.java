package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteIngresosDetalladoDTO {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private String tipoFiltro;            // TODOS | BOLETA | RECIBO
    private long cantidadTotal;
    private long cantidadAnuladas;
    private List<TotalesMonedaDTO> totales;   // una entrada por moneda (USD, PEN, N/D)
    private List<IngresoDetalladoItemDTO> detalle;
}
