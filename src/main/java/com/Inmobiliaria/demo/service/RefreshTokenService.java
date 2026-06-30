package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Integer usuarioId);
    LoginResponse refreshAccessToken(String refreshToken);
    void revokeRefreshToken(String token);
    Integer findUsuarioIdByToken(String token);
}
