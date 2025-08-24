package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.TokenRequest;
import com.Inmobiliaria.demo.service.impl.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRequest tokenRequest) {
        tokenBlacklistService.blacklistToken(tokenRequest.getToken());
        return ResponseEntity.ok().body("Token revocado con éxito");
    }
}