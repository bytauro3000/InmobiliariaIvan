package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.LoginRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.RefreshToken;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.RefreshTokenService;
import com.Inmobiliaria.demo.service.SesionActivaService;
import com.Inmobiliaria.demo.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final SesionActivaService sesionActivaService;

    @Value("${jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getCorreo(), loginRequest.getContrasena())
        );

        Usuario usuario = usuarioService.buscarByUsuario(loginRequest.getCorreo());

        String token = jwtUtil.generateToken(authentication, usuario);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario.getId());
        agregarCookieRefreshToken(response, refreshToken.getToken());

        // Registrar sesión activa
        String ip = obtenerIpCliente(request);
        String userAgent = request.getHeader("User-Agent");
        sesionActivaService.registrarLogin(usuario, ip, userAgent);

        // El refresh token NO se devuelve en el body, solo en la cookie HttpOnly
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshAccessToken(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                                            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        // Actualizar última actividad de la sesión antes de rotar el token
        Integer usuarioId = refreshTokenService.findUsuarioIdByToken(refreshToken);
        if (usuarioId != null) {
            sesionActivaService.actualizarRefresh(usuarioId);
        }

        LoginResponse loginResponse = refreshTokenService.refreshAccessToken(refreshToken);
        agregarCookieRefreshToken(response, loginResponse.getRefreshToken());

        // El nuevo refresh token se envía por cookie, no por body
        return ResponseEntity.ok(new LoginResponse(loginResponse.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            Integer usuarioId = refreshTokenService.findUsuarioIdByToken(refreshToken);
            if (usuarioId != null) {
                sesionActivaService.desactivarSesion(usuarioId);
            }
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
        eliminarCookieRefreshToken(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestParam(value = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(true);
    }

    private void agregarCookieRefreshToken(HttpServletResponse response, String refreshToken) {
        // Eliminar cookie vieja con path /api/auth (de deploy anterior)
        Cookie oldCookie = new Cookie("refresh_token", null);
        oldCookie.setHttpOnly(true);
        oldCookie.setSecure(cookieSecure);
        oldCookie.setPath("/api/auth");
        oldCookie.setMaxAge(0);
        oldCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(oldCookie);

        // Setear cookie nueva con path /
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 días (coincide con refresh token en BD)
        cookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(cookie);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void eliminarCookieRefreshToken(HttpServletResponse response) {
        // Eliminar cookie con path /
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(cookie);

        // Eliminar cookie vieja con path /api/auth
        Cookie oldCookie = new Cookie("refresh_token", null);
        oldCookie.setHttpOnly(true);
        oldCookie.setSecure(cookieSecure);
        oldCookie.setPath("/api/auth");
        oldCookie.setMaxAge(0);
        oldCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(oldCookie);
    }
}