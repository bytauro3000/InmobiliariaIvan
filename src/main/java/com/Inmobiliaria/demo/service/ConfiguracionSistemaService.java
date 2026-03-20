package com.Inmobiliaria.demo.service;

import java.math.BigDecimal;

public interface ConfiguracionSistemaService {
    BigDecimal getTipoCambioRespaldo();
    void setTipoCambioRespaldo(BigDecimal valor);
}