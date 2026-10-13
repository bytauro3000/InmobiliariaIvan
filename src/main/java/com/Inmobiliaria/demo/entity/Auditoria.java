package com.Inmobiliaria.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.Inmobiliaria.demo.enums.AccionAuditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro inmutable de auditoría: quién, qué, cuándo y desde dónde.
 * Nunca se modifica ni se borra salvo por el proceso de retención
 * (AuditoriaPurgaScheduler). Si el registro de auditoría falla, el
 * negocio continúa ( AuditoriaServiceImpl nunca propaga excepción).
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaHora;

    /** Email del usuario autenticado (header X-Auth-User del gateway). */
    @Column(name = "usuario_email", nullable = false, length = 100, updatable = false)
    private String usuarioEmail;

    @Column(name = "usuario_rol", length = 50, updatable = false)
    private String usuarioRol;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 20, updatable = false)
    private AccionAuditoria accion;

    /** Nombre lógico de la entidad afectada (ej: PagoLetras, Contrato). */
    @Column(name = "entidad", nullable = false, length = 60, updatable = false)
    private String entidad;

    /** Id de la entidad afectada (null si no aplica). */
    @Column(name = "id_entidad", updatable = false)
    private Integer idEntidad;

    @Column(name = "descripcion", length = 500, updatable = false)
    private String descripcion;

    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;
}
