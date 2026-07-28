package com.Inmobiliaria.demo.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.Inmobiliaria.demo.entity.Empresa;
import com.Inmobiliaria.demo.enums.TipoCalculoMora;
import com.Inmobiliaria.demo.repository.EmpresaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmpresaRepository empresaRepository;

    @Override
    public void run(String... args) {
        if (empresaRepository.count() > 0) {
            log.info("Ya existe empresa configurada, se omite la inicialización.");
            return;
        }

        Empresa e = new Empresa();
        e.setNombreLegal("INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.");
        e.setNombreComercial("Inmobiliaria IVAN E.I.R.L.");
        e.setRuc("20537853108");
        e.setDireccion("Av. Alfredo Mendiola N° 3623 - 3er. Piso Of. 301 - Urb. Panamericana Norte, Los Olivos - Lima");
        e.setTelefono("(01) 413-8679");
        e.setCelular("987 891 788");
        e.setEmail("inmobiliariaivan.eirl@gmail.com");
        e.setLogoUrl("https://res.cloudinary.com/dlgqaifrk/image/upload/f_auto,q_auto/v1785124181/logo3DIvan_hqmnav.png");
        e.setLogoSmallUrl("https://res.cloudinary.com/dlgqaifrk/image/upload/v1785129342/logoIVANg_1785129341505.png");
        e.setPaginaWeb("https://inmobiliaria-ivan.vercel.app");

        e.setTipoCalculoMora(TipoCalculoMora.PORCENTAJE_MAS_DIARIO);
        e.setMoraPorcentaje(new BigDecimal("0.05"));
        e.setMoraMontoDiario(new BigDecimal("1.00"));

        e.setRepresentanteLegal("OLMEDO SILVA LOPEZ");
        e.setRepresentanteDni("19404451");
        e.setPartidaElectronica("12561792");

        e.setUbigeo("150117");
        e.setDistrito("LOS OLIVOS");
        e.setProvincia("LIMA");
        e.setDepartamento("LIMA");

        e.setApisperuEnvironment("produccion");
        e.setWhatsappDeviceId("InmobiliariaIVAN");
        e.setNotificacionEmail("bytauro2016@gmail.com");

        e.setActiva(true);

        empresaRepository.save(e);
        log.info("Empresa inicializada con datos de INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.");
    }
}
