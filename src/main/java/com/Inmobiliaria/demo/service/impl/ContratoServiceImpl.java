// ContratoServiceImpl.java
package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.ContratoService;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired
    private ContratoRepository contratoRepository;

    @Override
    public Contrato guardarContrato(Contrato contrato) {
        if (contrato.getTipoContrato() == TipoContrato.CONTADO) {
            if (contrato.getCantidadLetras() == null) {
                contrato.setCantidadLetras(0);
            }
            if (contrato.getInicial() == null) {
                contrato.setInicial(BigDecimal.ZERO);
            }
            if (contrato.getSaldo() == null) {
                contrato.setSaldo(BigDecimal.ZERO);
            }
        }
        return contratoRepository.save(contrato);
    }


    @Override
    public List<Contrato> listarContratos() {
        return contratoRepository.findAll();
    }

    @Override
    public Contrato buscarPorId(Integer idContrato) {
        return contratoRepository.findById(idContrato).orElse(null);
    }
}
