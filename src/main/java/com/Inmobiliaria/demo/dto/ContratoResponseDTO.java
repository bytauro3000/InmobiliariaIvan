package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResponseDTO {
    private Integer idContrato;
    private Date fechaContrato;
    private TipoContrato tipoContrato;
    private EstadoContrato estadoContrato;
    private BigDecimal montoTotal;
    private BigDecimal inicial;
    private BigDecimal saldo;
    private Integer cantidadLetras;
    private String observaciones;
    private Moneda moneda;
    private List<ClienteResponseDTO> clientes;
    private List<LoteResponseDTO> lotes;
    private List<LetraResponseDTO> letras;
    private VendedorResponseDTO vendedor;
    private Long idComprobanteInicial;
    private TipoComprobante tipoComprobanteInicial;
    private String numeroComprobanteInicial;

    private PagoInicialResponseDTO pagoInicial;
}