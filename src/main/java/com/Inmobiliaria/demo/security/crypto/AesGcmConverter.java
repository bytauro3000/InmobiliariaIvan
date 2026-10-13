package com.Inmobiliaria.demo.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cifrado AES-256-GCM determinístico para campos sensibles de baja entropía
 * (DNI, celulares, teléfonos).
 *
 * - Formato en BD: {@code enc:v1:} + Base64(IV(12) || ciphertext || tag(16)).
 * - IV determinístico: primeros 12 bytes de SHA-256(claveBase64 "|" texto),
 *   lo que permite igualdad exacta en JPQL/Hibernate sin descifrar en memoria.
 * - Passthrough: valores ya prefijados no se re-cifran; valores sin prefijo
 *   (datos legibles pre-migración) se devuelven tal cual al leer.
 */
@Converter
public class AesGcmConverter implements AttributeConverter<String, String> {

    public static final String PREFIJO = "enc:v1:";

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int TAM_IV = 12;
    private static final int TAG_BITS = 128;

    // ── API estática para servicios (migración, pruebas) ─────────────────────

    public static String cifrar(String textoPlano) {
        if (textoPlano == null || textoPlano.isBlank() || estaCifrado(textoPlano)) {
            return textoPlano;
        }
        try {
            byte[] iv = ivDeterministico(textoPlano);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, CryptoHolder.getClave(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] salida = new byte[TAM_IV + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, TAM_IV);
            System.arraycopy(cifrado, 0, salida, TAM_IV, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(salida);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el campo sensible.", e);
        }
    }

    public static String descifrar(String valor) {
        if (valor == null || valor.isBlank() || !estaCifrado(valor)) {
            return valor;
        }
        try {
            byte[] completo = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            if (completo.length < TAM_IV + 16) {
                throw new IllegalStateException("Valor cifrado inválido (longitud).");
            }
            byte[] iv = new byte[TAM_IV];
            System.arraycopy(completo, 0, iv, 0, TAM_IV);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, CryptoHolder.getClave(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plano = cipher.doFinal(completo, TAM_IV, completo.length - TAM_IV);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el campo sensible.", e);
        }
    }

    public static boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    private static byte[] ivDeterministico(String textoPlano) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update((CryptoHolder.getClaveBase64() + "|" + textoPlano).getBytes(StandardCharsets.UTF_8));
        byte[] hash = sha.digest();
        byte[] iv = new byte[TAM_IV];
        System.arraycopy(hash, 0, iv, 0, TAM_IV);
        return iv;
    }

    // ── AttributeConverter (Hibernate) ───────────────────────────────────────

    @Override
    public String convertToDatabaseColumn(String atributo) {
        return cifrar(atributo);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return descifrar(dbData);
    }
}
