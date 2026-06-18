package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.RefreshTokenRequest;
import com.Inmobiliaria.demo.dto.LoginResponse;
import com.Inmobiliaria.demo.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Integer usuarioId);
    LoginResponse refreshAccessToken(RefreshTokenRequest request);
    void revokeRefreshToken(String token);
}
