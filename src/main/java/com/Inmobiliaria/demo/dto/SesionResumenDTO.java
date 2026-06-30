package com.Inmobiliaria.demo.dto;

import java.util.List;

public class SesionResumenDTO {
    private long usuariosActivos;
    private long visitasHoy;
    private List<UsuarioActivoDTO> sesiones;

    public SesionResumenDTO(long usuariosActivos, long visitasHoy, List<UsuarioActivoDTO> sesiones) {
        this.usuariosActivos = usuariosActivos;
        this.visitasHoy = visitasHoy;
        this.sesiones = sesiones;
    }

    public long getUsuariosActivos() { return usuariosActivos; }
    public long getVisitasHoy() { return visitasHoy; }
    public List<UsuarioActivoDTO> getSesiones() { return sesiones; }
}
