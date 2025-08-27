package com.Inmobiliaria.demo.dto;
import java.util.List;
import com.Inmobiliaria.demo.entity.Contrato;

public class ContratoRequestDTO {
	public Contrato contrato;
    public List<Integer> idClientes;
    public List<Integer> idLotes;
    public Integer idSeparacion;
}
