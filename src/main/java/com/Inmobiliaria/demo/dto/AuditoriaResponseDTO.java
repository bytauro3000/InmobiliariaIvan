package com.Inmobiliaria.demo.dto;

import java.time.LocalDateTime;

import com.Inmobiliaria.demo.entity.Auditoria;
import com.Inmobiliaria.demo.enums.AccionAuditoria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de salida de la bitácora de auditoría (expone solo campos seguros).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponseDTO {

    private Long idAuditoria;
    private LocalDateTime fechaHora;
    private String usuarioEmail;
    private String usuarioRol;
    private AccionAuditoria accion;
    private String entidad;
    private Integer idEntidad;
    private String descripcion;
    private String ipOrigen;

    public static AuditoriaResponseDTO desde(Auditoria a) {
        return new AuditoriaResponseDTO(
                a.getIdAuditoria(),
                a.getFechaHora(),
                a.getUsuarioEmail(),
                a.getUsuarioRol(),
                a.getAccion(),
                a.getEntidad(),
                a.getIdEntidad(),
                a.getDescripcion(),
                a.getIpOrigen());
    }
}
