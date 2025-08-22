package com.Inmobiliaria.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCMatcher {
    public static void main(String[] args) {
        String contrasenaIngresada = "123456";
        String hashDesdeDB = "$2a$10$eQXvZnoUFb0ued37ClCzJuYjqlLX9dbtNgVHDlAla8xcssKm9thKu";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Internamente, el método .matches() extrae el salt y compara
        if (encoder.matches(contrasenaIngresada, hashDesdeDB)) {
            System.out.println("¡Contraseña correcta! La autenticación es exitosa.");
        } else {
            System.out.println("Contraseña incorrecta. La autenticación falló.");
        }
    }
}