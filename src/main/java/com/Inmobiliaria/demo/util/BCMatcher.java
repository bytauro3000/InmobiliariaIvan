package com.Inmobiliaria.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCMatcher {
    public static void main(String[] args) {
        String contrasenaIngresada = "inmobiliari@2026";
        String hashDesdeDB = "$2a$10$t3OAOGUelbw/pvFVcI3fneOvoeqGkFFESWtY85yWcbKBWIV0c.LZq";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Internamente, el método .matches() extrae el salt y compara
        if (encoder.matches(contrasenaIngresada, hashDesdeDB)) {
            System.out.println("¡Contraseña correcta! La autenticación es exitosa.");
        } else {
            System.out.println("Contraseña incorrecta. La autenticación falló.");
        }
    }
}