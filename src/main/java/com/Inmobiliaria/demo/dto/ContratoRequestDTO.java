package com.Inmobiliaria.demo.dto;

import java.util.List;
import com.Inmobiliaria.demo.entity.Contrato;
import lombok.Data;
import lombok.NoArgsConstructor; 
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequestDTO {
    
    private Contrato contrato;
    private List<Integer> idClientes;
    private List<Integer> idLotes;
    private Integer idSeparacion;
}