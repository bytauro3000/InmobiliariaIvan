package com.Inmobiliaria.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {
        // La contraseña que quieres cifrar
        String plainPassword = "123456";

        // Crea una instancia del codificador de contraseñas de Spring Security
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Cifra la contraseña
        String encodedPassword = passwordEncoder.encode(plainPassword);

        // Imprime la contraseña en texto plano y su versión cifrada
        System.out.println("Contraseña original: " + plainPassword);
        System.out.println("Contraseña cifrada (BCrypt): " + encodedPassword);
    }
}