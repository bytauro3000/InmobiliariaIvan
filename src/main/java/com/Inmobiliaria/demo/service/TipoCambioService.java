package com.Inmobiliaria.demo.service;
 
import java.math.BigDecimal;
 
public interface TipoCambioService {
 
    // Precio de mercado — directo de la API
    BigDecimal obtenerTipoCambioOficial();
 
    // Precio empresa para contratos (usado en formularios de contrato)
    BigDecimal obtenerTipoCambioEmpresa();
 
    // ✅ NUEVO: Compra  = oficial + 0.0206  (cliente trae soles, compra dólares)
    BigDecimal obtenerTipoCambioCompra();
 
    // ✅ NUEVO: Venta   = oficial - 0.0054  (cliente trae dólares, vende a la empresa)
    BigDecimal obtenerTipoCambioVenta();
}