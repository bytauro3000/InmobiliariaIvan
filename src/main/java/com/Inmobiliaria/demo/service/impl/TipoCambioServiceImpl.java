package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.service.ConfiguracionSistemaService;
import com.Inmobiliaria.demo.service.TipoCambioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipoCambioServiceImpl implements TipoCambioService {

    private static final String API_URL = "https://estadisticas.bcrp.gob.pe/estadisticas/series/api/PD04639PD-PD04640PD/json/%s/%s";

    private static final BigDecimal MARGEN_EMPRESA = new BigDecimal("0.02");
    private static final BigDecimal MARGEN_COMPRA = new BigDecimal("0.0206");
    private static final BigDecimal MARGEN_VENTA  = new BigDecimal("0.0054");

    private final ConfiguracionSistemaService configService;
    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BigDecimal tipoCambioCache = null;

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }

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

    @Override
    public BigDecimal obtenerTipoCambioCompra() {
        return obtenerTipoCambioOficial()
                .add(MARGEN_COMPRA)
                .setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal obtenerTipoCambioVenta() {
        return obtenerTipoCambioOficial()
                .subtract(MARGEN_VENTA)
                .setScale(3, RoundingMode.HALF_UP);
    }

    @Scheduled(fixedRate = 3_600_000)
    @SuppressWarnings("unchecked")
    public void actualizarTipoCambio() {
        try {
            LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
            LocalDate inicio = hoy.minusDays(7);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String url = String.format(API_URL, inicio.format(formatter), hoy.format(formatter));

            // El BCRP devuelve el body en JSON pero con Content-Type: text/html,
            // por lo que RestTemplate no puede deserializar directo a Map.
            // Se pide como String y se parsea manualmente con Jackson.
            String rawJson = restTemplate.getForObject(url, String.class);
            if (rawJson == null || rawJson.isBlank()) {
                throw new RuntimeException("Respuesta vacía del BCRP");
            }

            Map<String, Object> response = objectMapper.readValue(rawJson, Map.class);

            List<Map<String, Object>> periods = (List<Map<String, Object>>) response.get("periods");
            if (periods == null || periods.isEmpty()) {
                throw new RuntimeException("No se encontraron períodos en la respuesta del BCRP");
            }

            BigDecimal compra = null;
            BigDecimal venta = null;

            for (int i = periods.size() - 1; i >= 0; i--) {
                Map<String, Object> period = periods.get(i);
                List<String> values = (List<String>) period.get("values");
                if (values != null && values.size() >= 2
                        && !"n.d.".equals(values.get(0))
                        && !"n.d.".equals(values.get(1))) {
                    compra = new BigDecimal(values.get(0));
                    venta = new BigDecimal(values.get(1));
                    break;
                }
            }

            if (compra == null || venta == null) {
                throw new RuntimeException("No se encontró un período válido con datos en los últimos 7 días");
            }

            tipoCambioCache = compra.add(venta)
                    .divide(new BigDecimal("2"), 3, RoundingMode.HALF_UP);

            log.info("Tipo de cambio BCRP actualizado: Compra={} | Venta={} | Oficial={}", compra, venta, tipoCambioCache);
            log.info("  Empresa={} | Compra client={} | Venta client={}",
                    obtenerTipoCambioEmpresa(),
                    obtenerTipoCambioCompra(),
                    obtenerTipoCambioVenta());
            return;

        } catch (Exception e) {
            log.warn("⚠️ BCRP no disponible - usando fallback DB: {}", e.getMessage());
        }
        tipoCambioCache = configService.getTipoCambioRespaldo();
        log.info("Tipo de cambio de respaldo activo: 1 USD = {} PEN", tipoCambioCache);
    }
}