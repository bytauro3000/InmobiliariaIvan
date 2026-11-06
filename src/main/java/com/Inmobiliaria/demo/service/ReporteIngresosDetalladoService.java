package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ReporteIngresosDetalladoDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;

import java.time.LocalDate;
import java.util.List;

public interface ReporteIngresosDetalladoService {

    /**
     * Reporte de ingresos detallado por comprobante (BOLETA/RECIBO) en un rango
     * de fechas, con receptor, moneda, estado (vigente / anulado por NC o por
     * pago anulado) y totales por moneda.
     *
     * @param tipos     tipos de comprobante a incluir (nunca FACTURA ni NOTA_CREDITO)
     * @param tipoFiltro valor original del filtro para reflejarlo en el DTO (TODOS|BOLETA|RECIBO)
     */
    ReporteIngresosDetalladoDTO obtenerReporteDetallado(
            LocalDate desde,
            LocalDate hasta,
            List<TipoComprobante> tipos,
            String tipoFiltro);
}
