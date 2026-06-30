package com.Inmobiliaria.demo.dto;

import java.time.LocalDateTime;

public class UsuarioActivoDTO {
    private Long sesionId;
    private Integer usuarioId;
    private String nombre;
    private String correo;
    private String ip;
    private String userAgent;
    private LocalDateTime desde;
    private LocalDateTime ultimoRefresh;

    public UsuarioActivoDTO(Long sesionId, Integer usuarioId, String nombre, String correo,
                            String ip, String userAgent, LocalDateTime desde, LocalDateTime ultimoRefresh) {
        this.sesionId = sesionId;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.correo = correo;
        this.ip = ip;
        this.userAgent = userAgent;
        this.desde = desde;
        this.ultimoRefresh = ultimoRefresh;
    }

    public Long getSesionId() { return sesionId; }
    public Integer getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getDesde() { return desde; }
    public LocalDateTime getUltimoRefresh() { return ultimoRefresh; }
}
