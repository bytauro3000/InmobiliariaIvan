package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.UsuarioService;
import com.Inmobiliaria.demo.service.impl.TokenBlacklistService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

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
    
    //exponer los tocken den la lista negra para que el microservicio no los use como validos para sus consultas
    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestParam("token") String token) {
        // Verificamos si está en la lista negra
        boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);
        // Si NO está en la lista negra, devolvemos true (es válido)
        return ResponseEntity.ok(!isBlacklisted);
    }
}