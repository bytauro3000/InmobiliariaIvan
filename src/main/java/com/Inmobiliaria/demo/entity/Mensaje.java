package com.Inmobiliaria.demo.entity;

import java.time.LocalDateTime;

import com.Inmobiliaria.demo.enums.EstadoMensaje;

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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "mensaje")
public class Mensaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	
	private Long remitenteId;
	private Long destinatarioId;
	
	@Column(columnDefinition = "TEXT")
    private String contenido;

    private String nombreArchivo;
    private String tipoArchivo;
    private String urlArchivo;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private EstadoMensaje estado;

	
}
