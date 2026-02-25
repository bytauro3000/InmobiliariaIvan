package com.Inmobiliaria.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCMatcher {
    public static void main(String[] args) {
        String contrasenaIngresada = "Oper@dor2026";
        String hashDesdeDB = "$2a$10$0FTGrAqP/Fxr76Q.LtgTRuuFeRncxO5nXULYjiR0CQ/VXQXC0V3Bq";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Internamente, el método .matches() extrae el salt y compara
        if (encoder.matches(contrasenaIngresada, hashDesdeDB)) {
            System.out.println("¡Contraseña correcta! La autenticación es exitosa.");
        } else {
            System.out.println("Contraseña incorrecta. La autenticación falló.");
        }
    }
}