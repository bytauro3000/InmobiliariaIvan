package com.Inmobiliaria.demo.util;
import java.util.Base64;

public class GenerarBase64 {
    public static void main(String[] args) {
        String clavePlana = "MiClaveSecret1234567890MiProyectoDeInmuebles";
        String base64 = Base64.getEncoder().encodeToString(clavePlana.getBytes());
        System.out.println("JWT_SECRET en Base64: " + base64);
    }
}