package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaResponseDTO {
    private Long id;
    private String nombreLegal;
    private String nombreComercial;
    private String ruc;
    private String direccion;
    private String telefono;
    private String celular;
    private String email;
    private String logoUrl;
    private String logoSmallUrl;
    private String paginaWeb;
    private String whatsapp;
    private String representanteLegal;
    private String representanteDni;
    private String partidaElectronica;
    private String ubigeo;
    private String distrito;
    private String provincia;
    private String departamento;
    private String tipoCalculoMora;
    private BigDecimal moraPorcentaje;
    private BigDecimal moraMontoDiario;
    private BigDecimal moraTasaDiaria;

    private String apisperuEnvironment;
    private String whatsappDeviceId;
    private String notificacionEmail;
    private Boolean activa;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
}
