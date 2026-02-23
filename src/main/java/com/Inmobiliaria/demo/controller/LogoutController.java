package com.Inmobiliaria.demo.controller;

import org.springframework.web.bind.annotation.*;

//Controlador para el cierre de sesión (logout)
@RestController
//Endpoint para cerrar sesión
@RequestMapping("/api/auth")
public class LogoutController {
/*
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRequest tokenRequest) {
    	//Añade el token a la lista negra
        tokenBlacklistService.blacklistToken(tokenRequest.getToken());
        //Devuelve una respuesta de éxito
        return ResponseEntity.ok().body("Token revocado con éxito");
    }
    */
}