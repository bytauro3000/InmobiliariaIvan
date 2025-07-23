// ContratoService.java
package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.entity.Contrato;

public interface ContratoService {
    Contrato guardarContrato(Contrato contrato);
    List<Contrato> listarContratos();
    Contrato buscarPorId(Integer idContrato);
    
}
