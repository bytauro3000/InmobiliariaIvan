package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para la pantalla de lista de pagos de inscripciones.
 * Combina datos del PagoInicial, su Comprobante y el Lote/Programa del contrato.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoInscripcionDTO {

    private Integer       idPagoInicial;
    private Integer       idContrato;
    private BigDecimal    importePagado;
    private LocalDate     fechaPago;
    private MedioPago     medioPago;
    private String        numeroOperacion;
    private String        observaciones;

    // Datos del comprobante
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;  // ej. "B001-00042"
    private LocalDate       fechaEmision;

    // Tipo de servicio extraído de observaciones ("LUZ" o "AGUA")
    private String tipoServicio;

    // Datos del lote asociado al contrato
    private String  manzana;
    private String  numeroLote;

    // Datos del programa al que pertenece el lote
    private Integer idPrograma;
    private String  nombrePrograma;
}