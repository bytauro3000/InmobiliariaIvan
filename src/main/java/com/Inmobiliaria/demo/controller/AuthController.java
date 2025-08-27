package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UsuarioService usuarioService; 

 // Controlador principal para manejar la autenticación (login)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena())
        );

        //Busca el usuario para obtener datos adicionales (nombre, apellidos)
        Usuario usuario = usuarioService.buscarByUsuario(loginRequest.getCorreo());

        // Genera el JWT si la autenticación es exitosa
        String token = jwtUtil.generateToken(authentication, usuario); // Pasa el objeto usuario a JwtUtil
        
        //Devuelve el token en la respuesta
        return ResponseEntity.ok(new LoginResponse(token));
    }
}