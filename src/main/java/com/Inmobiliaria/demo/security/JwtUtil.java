package com.Inmobiliaria.demo.security;

import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.service.impl.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // La clave secreta para firmar y verificar los tokens, inyectada desde application.properties
    private final String SECRET_KEY;

    @Autowired // Inyecta el servicio de lista negra
    private TokenBlacklistService tokenBlacklistService;

    // Constructor que inyecta la clave secreta
    public JwtUtil(@Value("${jwt.secret-key}") String secretKey) {
        this.SECRET_KEY = secretKey;
    }

    // Método para generar un nuevo token JWT
    public String generateToken(Authentication authentication, Usuario usuario) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        // Añade el rol, nombre y apellidos del usuario al 'payload' del token
        claims.put("rol", userDetails.getAuthorities().iterator().next().getAuthority());
        claims.put("nombre", usuario.getNombres());     
        claims.put("apellidos", usuario.getApellidos()); 
        
        return Jwts.builder()
                .setClaims(claims) // Incluye los datos del usuario
                .setSubject(userDetails.getUsername()) // Define el usuario
                .setIssuedAt(new Date(System.currentTimeMillis())) // Fecha de creación
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Fecha de expiración (10 horas)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Firma el token con la clave secreta
                .compact();
    }

    // Extrae el nombre de usuario del token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrae todas las 'claims' del token (datos del payload)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Obtiene la clave de firma a partir de la clave secreta en base64
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Método para validar si un token es válido, incluyendo la lista negra
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // La validación comprueba: 1) si el usuario coincide, 2) si no ha expirado y 3) si no está en la lista negra
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !tokenBlacklistService.isBlacklisted(token));
    }

    // Verifica si el token ha expirado
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extrae la fecha de expiración del token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
