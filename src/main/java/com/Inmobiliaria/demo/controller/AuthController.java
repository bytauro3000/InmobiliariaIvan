package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.Usuario; // ✅ Importa la clase Usuario
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.UsuarioService; // ✅ Importa el servicio de usuario
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
    private UsuarioService usuarioService; // ✅ Inyecta el servicio de usuario

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena())
        );

        // ✅ Obtener el objeto completo del usuario
        Usuario usuario = usuarioService.buscarByUsuario(loginRequest.getCorreo());

        // ✅ Generar un JWT con los datos adicionales
        String token = jwtUtil.generateToken(authentication, usuario); // Pasa el objeto usuario a JwtUtil

        return ResponseEntity.ok(new LoginResponse(token));
    }
}