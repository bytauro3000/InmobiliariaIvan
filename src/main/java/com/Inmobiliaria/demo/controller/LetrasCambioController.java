package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.service.ContratoService;
import com.Inmobiliaria.demo.service.DistritoService;
import com.Inmobiliaria.demo.service.LetraCambioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/letras")
public class LetrasCambioController {

    @Autowired
    private LetraCambioService letraCambioService;

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private DistritoService distritoService;

    // ✅ Mostrar letras de un contrato específico
    @GetMapping("/contrato/{idContrato}")
    public String verLetrasPorContrato(@PathVariable("idContrato") Integer idContrato, Model model) {
        List<LetraCambio> letras = letraCambioService.listarPorContrato(idContrato);
        model.addAttribute("listaLetras", letras);
        model.addAttribute("idContrato", idContrato);
        return "letras/listarLetrasCambio"; // ← Asegúrate de tener esta vista
    }
    
    @GetMapping("/contrato/{idContrato}/generar")
    public String mostrarFormularioGenerar(@PathVariable("idContrato") Integer idContrato, Model model) {
        model.addAttribute("idContrato", idContrato);
        model.addAttribute("listaDistritos", distritoService.listarDistritos());
        return "letras/generarLetrasCambio";
    }


    // ✅ Generar letras desde un formulario
    @PostMapping("/generar")
    public String generarLetras(
            @RequestParam("idContrato") Integer idContrato,
            @RequestParam("idDistrito") Integer idDistrito,
            @RequestParam("fechaGiro") String fechaGiroStr,
            @RequestParam("fechaVencimientoInicial") String fechaVencimientoStr,
            @RequestParam("importe") Double importe,
            @RequestParam("importeLetras") String importeLetras
    ) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fechaGiro = sdf.parse(fechaGiroStr);
            Date fechaVencimientoInicial = sdf.parse(fechaVencimientoStr);

            Contrato contrato = contratoService.buscarPorId(idContrato);
            Distrito distrito = distritoService.obtenerPorId(idDistrito);

            letraCambioService.generarLetrasDesdeContrato(contrato, distrito, fechaGiro, fechaVencimientoInicial, importe, importeLetras);

            return "redirect:/letras/contrato/" + idContrato + "?success=true";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/letras/contrato/" + idContrato + "?error=true";
        }
    }
}
