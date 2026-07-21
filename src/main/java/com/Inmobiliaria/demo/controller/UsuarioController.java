package com.Inmobiliaria.demo.controller;

import org.springframework.http.HttpStatus;

import java.util.List;


import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import com.Inmobiliaria.demo.dto.ConsultaDniDTO;
import com.Inmobiliaria.demo.dto.UsuarioListadoDTO;
import com.Inmobiliaria.demo.dto.UsuarioRegistroDTO;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.entity.RolUsuario;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.repository.DistritoRepository;
import com.Inmobiliaria.demo.service.ConsultaDniService;
import com.Inmobiliaria.demo.service.EmailVerificationService;
import com.Inmobiliaria.demo.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

   
    private final UsuarioService usuarioService;
    private final EmailVerificationService emailVerificationService;
    private final ConsultaDniService consultaDniService;
    private final DistritoRepository distritoRepository;

    // Endpoint para crear el usuario
    @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody UsuarioRegistroDTO dto) {
        try {
            if (!emailVerificationService.isEmailVerificado(dto.getCorreo())) {
                return new ResponseEntity<>("Debe verificar el correo electrónico antes de registrar.", HttpStatus.BAD_REQUEST);
            }
            Usuario nuevoUsuario = usuarioService.registrarUsuario(dto);
            emailVerificationService.limpiarVerificacion(dto.getCorreo());
            return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/enviar-pin")
    public ResponseEntity<?> enviarPinVerificacion(@RequestBody Map<String, String> body) {
        String correo = body.get("correo");
        if (correo == null || correo.isBlank()) {
            return new ResponseEntity<>("El correo es requerido.", HttpStatus.BAD_REQUEST);
        }
        try {
            emailVerificationService.enviarPin(correo);
            return new ResponseEntity<>(Map.of("mensaje", "Código enviado al correo."), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verificar-pin")
    public ResponseEntity<?> verificarPin(@RequestBody Map<String, String> body) {
        String correo = body.get("correo");
        String pin = body.get("pin");

        if (correo == null || correo.isBlank() || pin == null || pin.isBlank()) {
            return new ResponseEntity<>("Correo y PIN son requeridos.", HttpStatus.BAD_REQUEST);
        }

        boolean valido = emailVerificationService.verificarPin(correo, pin);
        if (valido) {
            return new ResponseEntity<>(Map.of("mensaje", "Correo verificado correctamente."), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("PIN inválido o expirado.", HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/listar")
    public ResponseEntity<List<UsuarioListadoDTO>> listarUsuarios() {
        try {
            List<UsuarioListadoDTO> lista = usuarioService.listarUsuarios();
            return new ResponseEntity<>(lista, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/editar/{id}")
    public ResponseEntity<?> editarUsuario(@PathVariable Integer id, @Valid @RequestBody UsuarioRegistroDTO dto) {
        try {
            Usuario actualizado = usuarioService.editarUsuario(id, dto);
            return new ResponseEntity<>(actualizado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/cambiar-estado/{id}")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id) {
        try {
            Usuario actualizado = usuarioService.cambiarEstadoUsuario(id);
            return new ResponseEntity<>(actualizado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolUsuario>> listarRoles() {
        try {
            List<RolUsuario> roles = usuarioService.listarRoles();
            return new ResponseEntity<>(roles, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/consultar-dni/{dni}")
    public ResponseEntity<ConsultaDniDTO> consultarDni(@PathVariable String dni) {
        ConsultaDniDTO resultado = consultaDniService.buscarEnReniec(dni);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/departamentos")
    public ResponseEntity<List<String>> listarDepartamentos() {
        return ResponseEntity.ok(distritoRepository.findDepartamentos());
    }

    @GetMapping("/provincias")
    public ResponseEntity<List<String>> listarProvincias(@RequestParam String departamento) {
        return ResponseEntity.ok(distritoRepository.findProvinciasByDepartamento(departamento));
    }

    @GetMapping("/distritos")
    public ResponseEntity<List<Distrito>> listarDistritos(
            @RequestParam String departamento,
            @RequestParam String provincia) {
        return ResponseEntity.ok(distritoRepository.findByDepartamentoAndProvinciaOrderByNombreAsc(departamento, provincia));
    }
}
