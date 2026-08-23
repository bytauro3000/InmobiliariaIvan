package com.Inmobiliaria.demo.security;

import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.repository.VendedorRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    private final String SECRET_KEY;
    private final VendedorRepository vendedorRepository;

    public JwtUtil(@Value("${jwt.secret-key}") String secretKey,
                   VendedorRepository vendedorRepository) {
        this.SECRET_KEY = secretKey;
        this.vendedorRepository = vendedorRepository;
    }

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 30; // 30 minutos

    // Único responsable de crear el token en el Login
    public String generateToken(Authentication authentication, Usuario usuario) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", userDetails.getAuthorities().iterator().next().getAuthority());
        claims.put("nombre", usuario.getNombres());     
        claims.put("apellidos", usuario.getApellidos()); 
        claims.put("id", usuario.getId());
        // Si el usuario tiene un vendedor asociado, se expone idVendedor en el token
        // para que el frontend filtre (p. ej. "mis separaciones") sin consultas extra.
        vendedorRepository.findByIdUsuario(usuario.getId()).ifPresent(v -> claims.put("idVendedor", v.getIdVendedor()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateTokenFromUsuario(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "ROLE_" + usuario.getRol().getRolUsuario().toUpperCase());
        claims.put("nombre", usuario.getNombres());
        claims.put("apellidos", usuario.getApellidos());
        claims.put("id", usuario.getId());
        vendedorRepository.findByIdUsuario(usuario.getId()).ifPresent(v -> claims.put("idVendedor", v.getIdVendedor()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getCorreo())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}