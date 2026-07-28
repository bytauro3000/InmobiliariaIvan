package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.Inmobiliaria.demo.enums.TipoCalculoMora;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empresa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_legal", nullable = false, length = 200)
    private String nombreLegal;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    @Column(name = "ruc", nullable = false, length = 11)
    private String ruc;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "celular", length = 50)
    private String celular;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_small_url", length = 500)
    private String logoSmallUrl;

    @Column(name = "pagina_web", length = 200)
    private String paginaWeb;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_calculo_mora", nullable = false, length = 30)
    private TipoCalculoMora tipoCalculoMora;

    @Column(name = "mora_porcentaje", precision = 5, scale = 4)
    private BigDecimal moraPorcentaje;

    @Column(name = "mora_monto_diario", precision = 10, scale = 2)
    private BigDecimal moraMontoDiario;

    @Column(name = "mora_tasa_diaria", precision = 7, scale = 6)
    private BigDecimal moraTasaDiaria;


    @Column(name = "representante_legal", length = 200)
    private String representanteLegal;

    @Column(name = "representante_dni", length = 20)
    private String representanteDni;

    @Column(name = "partida_electronica", length = 50)
    private String partidaElectronica;

    @Column(name = "ubigeo", length = 10)
    private String ubigeo;

    @Column(name = "distrito", length = 100)
    private String distrito;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "departamento", length = 100)
    private String departamento;

    @Column(name = "apisperu_environment", length = 20)
    private String apisperuEnvironment;

    @Column(name = "whatsapp_device_id", length = 100)
    private String whatsappDeviceId;

    @Column(name = "notificacion_email", length = 200)
    private String notificacionEmail;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
