package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.service.ContratoService;
import com.Inmobiliaria.demo.service.DistritoService;
import com.Inmobiliaria.demo.service.LetraCambioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/letras")
public class LetrasCambioController {

    private static final Logger logger = LoggerFactory.getLogger(LetrasCambioController.class);

    @Autowired
    private LetraCambioService letraCambioService;

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private DistritoService distritoService;
    
    @GetMapping("/contrato/{idContrato}/generar")
    public String redirigirGenerar(@PathVariable("idContrato") Integer idContrato) {
        return "redirect:/letras/contrato/" + idContrato;
    }

    @GetMapping("/contrato/{idContrato}")
    public String verYGenerarLetras(@PathVariable("idContrato") Integer idContrato, Model model) {
        List<LetraCambio> letras = letraCambioService.listarPorContrato(idContrato);
        model.addAttribute("listaLetras", letras);
        model.addAttribute("idContrato", idContrato);
        model.addAttribute("listaDistritos", distritoService.listarDistritos());
        return "letras/letrasCambio";
    }

    @PostMapping("/contrato/{idContrato}")
    public String generarLetras(
            @PathVariable("idContrato") Integer idContrato,
            @RequestParam("idDistrito") Integer idDistrito,
            @RequestParam("fechaGiro") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaGiro,
            @RequestParam("fechaVencimientoInicial") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimientoInicial,
            @RequestParam("importe") String importeStr,
            @RequestParam("importeLetras") String importeLetras,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Normalizar importe (quita símbolo de dólar y comas)
            String limpio = importeStr.replaceAll("[$,]", "").trim();
            Double importe = Double.parseDouble(limpio);

            Contrato contrato = contratoService.buscarPorId(idContrato);
            Distrito distrito = distritoService.obtenerPorId(idDistrito);

            Date giroDate = Date.from(fechaGiro.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date vencimientoDate = Date.from(fechaVencimientoInicial.atStartOfDay(ZoneId.systemDefault()).toInstant());

            letraCambioService.generarLetrasDesdeContrato(contrato, distrito, giroDate, vencimientoDate, importe, importeLetras);

            redirectAttributes.addFlashAttribute("mensaje", "Letra generada correctamente.");
        } catch (Exception e) {
            logger.error("Error generando letra para contrato {}: {}", idContrato, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al generar la letra.");
        }
        return "redirect:/letras/contrato/" + idContrato;
    }
}
