package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.ConfiguracionSistema;
import com.Inmobiliaria.demo.repository.ConfiguracionSistemaRepository;
import com.Inmobiliaria.demo.service.ConfiguracionSistemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionSistemaServiceImpl implements ConfiguracionSistemaService {

    private static final String CLAVE_TIPO_CAMBIO  = "tipo_cambio_respaldo";
    private static final BigDecimal VALOR_DEFECTO  = new BigDecimal("3.700");

    private final ConfiguracionSistemaRepository repo;

    // Al arrancar: si la tabla está vacía inserta el valor por defecto
    @EventListener(ApplicationReadyEvent.class)
    public void inicializar() {
        if (!repo.existsById(CLAVE_TIPO_CAMBIO)) {
            repo.save(new ConfiguracionSistema(
                CLAVE_TIPO_CAMBIO,
                VALOR_DEFECTO.toPlainString(),
                "Tipo de cambio USD a PEN de respaldo cuando la API externa no responde"
            ));
            log.info("Tipo de cambio de respaldo inicializado en {}", VALOR_DEFECTO);
        }
    }

    @Override
    public BigDecimal getTipoCambioRespaldo() {
        return repo.findById(CLAVE_TIPO_CAMBIO)
                .map(c -> new BigDecimal(c.getValor()))
                .orElse(VALOR_DEFECTO);
    }

    @Override
    public void setTipoCambioRespaldo(BigDecimal valor) {
        ConfiguracionSistema config = repo.findById(CLAVE_TIPO_CAMBIO)
                .orElse(new ConfiguracionSistema(CLAVE_TIPO_CAMBIO, "", ""));
        config.setValor(valor.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString());
        repo.save(config);
        log.info("Tipo de cambio de respaldo actualizado a {}", valor);
    }
}