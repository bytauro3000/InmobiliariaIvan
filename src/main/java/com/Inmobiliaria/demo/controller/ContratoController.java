package com.Inmobiliaria.demo.controller;

import java.security.Principal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.service.*;
import java.util.List;


@RestController
@RequestMapping("/api/contratos")
@CrossOrigin(origins = "*")
public class ContratoController {

    @Autowired private ContratoService contratoService;
    @Autowired private ClienteService clienteService;
    @Autowired private LoteService loteService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ContratoClienteService contratoClienteService;
    @Autowired private ContratoLoteService contratoLoteService;
    @Autowired private SeparacionService separacionService;


    // Clase interna para mapear los datos de entrada del JSON
    private static class ContratoRequestDTO {
        public Contrato contrato;
        public List<Integer> idClientes;
        public List<Integer> idLotes;
        public Integer idSeparacion;
    }

    @PostMapping("/agregar")
    public ResponseEntity<Contrato> guardarContrato(
        @RequestBody ContratoRequestDTO requestDTO,
        Principal principal
    ) {
        Contrato contrato = requestDTO.contrato;
        
        // Asigna el usuario y la fecha actual antes de guardar el contrato
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);
        contrato.setFechaContrato(new Date());

        // Asocia la separación si el contrato proviene de una
        if (requestDTO.idSeparacion != null) {
            Separacion separacion = new Separacion();
            separacion.setIdSeparacion(requestDTO.idSeparacion);
            contrato.setSeparacion(separacion);
        }

        Contrato contratoGuardado = contratoService.guardarContrato(contrato);

        // Si no hay separación, asocia los clientes y lotes manualmente
        if (requestDTO.idSeparacion == null) {
            if (requestDTO.idClientes != null) {
                for (Integer idCliente : requestDTO.idClientes) {
                    Cliente cliente = clienteService.buscarClientePorId(idCliente);
                    if (cliente != null) {
                        ContratoClienteId contratoClienteId = new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente);
                        ContratoCliente cc = new ContratoCliente();
                        cc.setId(contratoClienteId);
                        cc.setContrato(contratoGuardado);
                        cc.setCliente(cliente);
                        cc.setTipoPropietario("Titular");
                        contratoClienteService.guardar(cc);
                    }
                }
            }

            if (requestDTO.idLotes != null) {
                for (Integer idLote : requestDTO.idLotes) {
                    Lote lote = loteService.obtenerLotePorId(idLote); // ✅ Línea corregida aquí
                    if (lote != null) {
                        ContratoLoteId contratoLoteId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                        ContratoLote cl = new ContratoLote();
                        cl.setId(contratoLoteId);
                        cl.setContrato(contratoGuardado);
                        cl.setLote(lote);
                        contratoLoteService.guardar(cl);
                    }
                }
            }
        }

        return new ResponseEntity<>(contratoGuardado, HttpStatus.CREATED);
    }

    @GetMapping("/listar")
    public List<Contrato> listarContratos() {
        return contratoService.listarContratos();
    }
    
    @GetMapping("/separaciones/buscar")
    public List<SeparacionDTO> buscarSeparaciones(@RequestParam(value = "filtro", required = false) String filtro) {
        // Devuelve una lista de separaciones basada en el DNI o apellido del cliente
        if (filtro == null || filtro.trim().isEmpty()) {
            return List.of();
        }
        return separacionService.buscarPorDniOApellido(filtro);
    }
}
