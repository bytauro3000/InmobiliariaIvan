package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.service.ConfiguracionSistemaService;
import com.Inmobiliaria.demo.service.TipoCambioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipoCambioServiceImpl implements TipoCambioService {

    private static final String API_URL = "https://v6.exchangerate-api.com/v6/%s/latest/USD";

    // Margen para contratos (se mantiene igual que antes)
    private static final BigDecimal MARGEN_EMPRESA = new BigDecimal("0.02");

    // ✅ NUEVO: márgenes reales de compra y venta
    // Compra: cliente trae soles y compra dólares → precio sube   (+0.0206)
    // Venta:  cliente trae dólares y vende        → precio baja   (-0.0054)
    private static final BigDecimal MARGEN_COMPRA = new BigDecimal("0.0206");
    private static final BigDecimal MARGEN_VENTA  = new BigDecimal("0.0054");

    @Value("${exchangerate.api-key}")
    private String apiKey;

    private final ConfiguracionSistemaService configService;
    private final RestTemplate restTemplate = new RestTemplate();

    // Caché en memoria — se actualiza cada hora
    private BigDecimal tipoCambioCache = null;

    @Override
    public BigDecimal obtenerTipoCambioOficial() {
        return tipoCambioCache != null ? tipoCambioCache : configService.getTipoCambioRespaldo();
    }

    @Override
    public BigDecimal obtenerTipoCambioEmpresa() {
        return obtenerTipoCambioOficial()
                .add(MARGEN_EMPRESA)
                .setScale(3, RoundingMode.HALF_UP);
    }

    //oficial + 0.0206
    @Override
    public BigDecimal obtenerTipoCambioCompra() {
        return obtenerTipoCambioOficial()
                .add(MARGEN_COMPRA)
                .setScale(3, RoundingMode.HALF_UP);
    }

    //oficial - 0.0054
    @Override
    public BigDecimal obtenerTipoCambioVenta() {
        return obtenerTipoCambioOficial()
                .subtract(MARGEN_VENTA)
                .setScale(3, RoundingMode.HALF_UP);
    }

    // Se ejecuta al arrancar y luego cada hora
    @Scheduled(fixedRate = 3_600_000)
    public void actualizarTipoCambio() {
        try {
            String url   = String.format(API_URL, apiKey);
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && "success".equals(response.get("result"))) {
                Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");
                if (rates != null && rates.containsKey("PEN")) {
                    double pen = ((Number) rates.get("PEN")).doubleValue();
                    tipoCambioCache = BigDecimal.valueOf(pen).setScale(3, RoundingMode.HALF_UP);
                    log.info("Tipo de cambio actualizado desde API: 1 USD = {} PEN (oficial)",  tipoCambioCache);
                    log.info("  Compra: {}  |  Venta: {}  |  Empresa: {}",
                            obtenerTipoCambioCompra(),
                            obtenerTipoCambioVenta(),
                            obtenerTipoCambioEmpresa());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("API tipo de cambio no disponible, usando valor de respaldo: {}", e.getMessage());
        }
        tipoCambioCache = configService.getTipoCambioRespaldo();
        log.info("Tipo de cambio de respaldo activo: 1 USD = {} PEN", tipoCambioCache);
    }
}