package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.CuentasPorCobrarDTO;

public interface CuentasPorCobrarService {

    /**
     * Devuelve las cuentas por cobrar: total esperado (USD y PEN) de las letras
     * pendientes de pago de contratos financiados ACTIVO/MORA, agrupado por programa.
     */
    CuentasPorCobrarDTO obtenerCuentasPorCobrar();
}