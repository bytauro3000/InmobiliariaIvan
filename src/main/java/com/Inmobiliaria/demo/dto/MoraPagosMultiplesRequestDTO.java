package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Pago múltiple de moras: una sola operación (medio de pago, comprobante y
 * vouchers compartidos) que salda N moras del mismo contrato.
 * El descuento negociado se distribuye proporcionalmente entre las moras.
 */
@Data
public class MoraPagosMultiplesRequestDTO {

    private List<Item> pagos;

    private BigDecimal      descuentoNegociado;
    private String          motivoDescuento;

    private LocalDate       fechaPago;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate       fechaOperacion;

    /** Hora exacta del voucher (ISO: HH:mm o HH:mm:ss); si llega, se respeta. */
    private LocalTime       horaOperacion;

    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobantePersonalizado;
    private String          observaciones;

    @Data
    public static class Item {
        private Integer    idMora;
        private BigDecimal importePagado;
    }
}
