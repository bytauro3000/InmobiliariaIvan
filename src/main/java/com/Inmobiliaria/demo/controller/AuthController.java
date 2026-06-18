package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.dto.RefreshTokenRequest;
import com.Inmobiliaria.demo.entity.RefreshToken;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.RefreshTokenService;
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
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena())
        );

        Usuario usuario = usuarioService.buscarByUsuario(loginRequest.getCorreo());

        String token = jwtUtil.generateToken(authentication, usuario);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario.getId());

        return ResponseEntity.ok(new LoginResponse(token, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = refreshTokenService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestParam(value = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(true);
    }
}