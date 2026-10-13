package com.Inmobiliaria.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.Inmobiliaria.demo.enums.EstadoCivil;
import com.Inmobiliaria.demo.enums.EstadoCliente;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.security.crypto.AesGcmConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", length = 100)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCivil", nullable = false)
    private EstadoCivil estadoCivil = EstadoCivil.Soltero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoCliente", nullable = false)
    private TipoCliente tipoCliente;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "numDocumento", unique = true, length = 128)
    private String numDoc;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "celular", nullable = false, length = 128)
    private String celular;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "telefono", length = 128)
    private String telefono;

    @Column(name = "direccion", nullable = false, length = 150)
    private String direccion;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "fechaRegistro", nullable = false)
    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", length = 20, nullable = false)
    private Genero genero;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCliente estado = EstadoCliente.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_distrito")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Distrito distrito;
    
    @Column(name = "nacionalidad", length = 50)
    private String nacionalidad;
}