package com.Inmobiliaria.demo.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Inicializa la clave AES-256 usada por {@link AesGcmConverter} al arrancar
 * la aplicación (fail-fast: si falta la clave, el app no arranca).
 *
 * app.enc.key admite:
 *  - clave Base64 de 32+ bytes (se usan los primeros 32), o
 *  - cualquier passphrase: se deriva SHA-256(passphrase) como clave AES-256.
 */
@Component
public class CryptoService {

    @Value("${app.enc.key:}")
    private String clavePropiedad;

    @PostConstruct
    public void inicializar() {
        if (clavePropiedad == null || clavePropiedad.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave de cifrado: define la variable de entorno APP_ENC_KEY "
                    + "(propiedad app.enc.key) antes de arrancar. Ver README-FASE2-CIFRADO.");
        }

        byte[] material;
        try {
            byte[] decodificada = Base64.getDecoder().decode(clavePropiedad.trim());
            material = decodificada.length >= 32
                    ? java.util.Arrays.copyOf(decodificada, 32)
                    : sha256(clavePropiedad.trim());
        } catch (IllegalArgumentException noEsBase64) {
            material = sha256(clavePropiedad.trim());
        }

        SecretKey clave = new SecretKeySpec(material, "AES");
        CryptoHolder.set(clave, Base64.getEncoder().encodeToString(material));
    }

    /** Genera una clave Base64 de 32 bytes (uso one-off: generar APP_ENC_KEY). */
    public static String generarClaveBase64() {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(256);
            return Base64.getEncoder().encodeToString(gen.generateKey().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
