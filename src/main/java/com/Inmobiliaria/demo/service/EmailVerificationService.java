package com.Inmobiliaria.demo.service;


public interface EmailVerificationService {

    String enviarPin(String email);

    boolean verificarPin(String email, String pin);

    boolean isEmailVerificado(String email);

    void limpiarVerificacion(String email);
}
