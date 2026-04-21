package com.Inmobiliaria.demo.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerarLetrasRequest {
    private Integer idDistrito;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimientoInicial;
    private String importe;
    private String importeLetras;
    private boolean modoAutomatico;
    private boolean modoGrupos;          // NUEVO: modo por grupos de montos distintos
    private List<GrupoLetrasRequest> grupos; // NUEVO: lista de grupos
    private boolean usarUltimoDiaMes;
}