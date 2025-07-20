package com.Inmobiliaria.demo.controller;

import java.security.Principal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.service.*;
import java.util.List;


@Controller
@RequestMapping("/contrato")
public class ContratoController {

    @Autowired private ContratoService contratoService;
    @Autowired private ClienteService clienteService;
    @Autowired private LoteService loteService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ContratoClienteService contratoClienteService;
    @Autowired private ContratoLoteService contratoLoteService;
    @Autowired private SeparacionService separacionService;


    @GetMapping("/registrar")
    public String mostrarFormularioContrato(Model model) {
        model.addAttribute("contrato", new Contrato());
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("lotes", loteService.listarLotes());
        return "contrato/registrarContrato";
    }

    @PostMapping("/guardar")
    public String guardarContrato(
        @ModelAttribute("contrato") Contrato contrato,
        @RequestParam(value = "idClientes", required = false) List<Integer> idClientes,
        @RequestParam(value = "idLotes", required = false) List<Integer> idLotes,
        @RequestParam(value = "idSeparacion", required = false) Integer idSeparacion,
        Principal principal
    ) {
        // Obtener usuario autenticado
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);
        contrato.setFechaContrato(new Date());

        // Si se seleccionó una separación
        if (idSeparacion != null) {
            Separacion separacion = new Separacion();
            separacion.setIdSeparacion(idSeparacion);
            contrato.setSeparacion(separacion);
        }

        // Guardar contrato primero
        Contrato contratoGuardado = contratoService.guardarContrato(contrato);

        // Si se seleccionó separación, no se registran clientes/lotes manuales
        if (idSeparacion == null) {
            // Asociar clientes
            if (idClientes != null) {
                for (Integer idCliente : idClientes) {
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(idCliente);

                    ContratoClienteId contratoClienteId = new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente);
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(contratoClienteId);
                    cc.setContrato(contratoGuardado);
                    cc.setCliente(cliente);
                    cc.setTipoPropietario("Titular");

                    contratoClienteService.guardar(cc);
                }
            }

            // Asociar lotes
            if (idLotes != null) {
                for (Integer idLote : idLotes) {
                    Lote lote = new Lote();
                    lote.setIdLote(idLote);

                    ContratoLoteId contratoLoteId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                    ContratoLote cl = new ContratoLote();
                    cl.setId(contratoLoteId);
                    cl.setContrato(contratoGuardado);
                    cl.setLote(lote);

                    contratoLoteService.guardar(cl);
                }
            }
        }

        return "redirect:/contrato/listar";
    }


    @GetMapping("/listar")
    public String listarContratos(Model model) {
        List<Contrato> contratos = contratoService.listarContratos();
        model.addAttribute("listaContratos", contratos);
        return "contrato/listaContrato";
    }
    
    @GetMapping("/buscar-separaciones")
    @ResponseBody
    public List<SeparacionDTO> buscarSeparaciones(@RequestParam(value = "filtro", required = false) String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return List.of(); // devuelve lista vacía si no se escribe nada
        }
        return separacionService.buscarPorDniOApellido(filtro);
    }
}
