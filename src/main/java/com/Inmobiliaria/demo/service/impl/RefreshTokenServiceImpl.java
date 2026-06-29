package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.RefreshToken;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.RefreshTokenRepository;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.security.JwtUtil;
import com.Inmobiliaria.demo.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new NegocioException("Usuario no encontrado"));

        refreshTokenRepository.deleteByUsuario(usuario);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUsuario(usuario);
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshExpiration));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse refreshAccessToken(String requestToken) {
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByToken(requestToken);

        if (optionalToken.isEmpty()) {
            throw new NegocioException("Refresh token no encontrado");
        }

        RefreshToken storedToken = optionalToken.get();

        if (storedToken.isRevoked()) {
            throw new NegocioException("Refresh token ya fue revocado");
        }

        if (storedToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(storedToken);
            throw new NegocioException("Refresh token expirado");
        }

        Usuario usuario = storedToken.getUsuario();
        String newAccessToken = jwtUtil.generateTokenFromUsuario(usuario);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshToken = createRefreshToken(usuario.getId());

        return new LoginResponse(newAccessToken, newRefreshToken.getToken());
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
