package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // 1. Valida credenciales contra la BD (llama a loadUserByUsername internamente → 1ra query)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena())
        );

        // 2. Obtiene el Usuario para armar el JWT
        //    → 1er login: hace query a BD y guarda en caché
        //    → Logins siguientes: va directo a memoria, sin query adicional
        Usuario usuario = usuarioService.buscarByUsuario(loginRequest.getCorreo());

        // 3. Genera el token con nombre, apellidos, rol e id incluidos
        String token = jwtUtil.generateToken(authentication, usuario);

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestParam(value = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        // Si el Gateway dejó pasar la petición hasta aquí, el token ya es válido.
        return ResponseEntity.ok(true);
    }
}