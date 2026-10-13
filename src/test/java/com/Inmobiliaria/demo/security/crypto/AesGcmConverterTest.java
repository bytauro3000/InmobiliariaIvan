package com.Inmobiliaria.demo.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests UNITARIOS puros (sin contexto Spring) del cifrado AES-256-GCM.
 * Inicializa CryptoHolder manualmente.
 */
class AesGcmConverterTest {

    @BeforeAll
    static void setupClave() throws Exception {
        byte[] material = MessageDigest.getInstance("SHA-256")
                .digest("clave-de-prueba-unitaria".getBytes(StandardCharsets.UTF_8));
        SecretKey clave = new SecretKeySpec(material, "AES");
        CryptoHolder.set(clave, Base64.getEncoder().encodeToString(material));
    }

    @Test
    void cifrarYDescifrar_roundTrip() {
        String dni = "45678912";
        String cifrado = AesGcmConverter.cifrar(dni);

        assertTrue(AesGcmConverter.estaCifrado(cifrado));
        assertNotEquals(dni, cifrado);
        assertEquals(dni, AesGcmConverter.descifrar(cifrado));
    }

    @Test
    void cifradoEsDeterministico() {
        assertEquals(AesGcmConverter.cifrar("98765432"), AesGcmConverter.cifrar("98765432"));
        assertNotEquals(AesGcmConverter.cifrar("98765432"), AesGcmConverter.cifrar("98765431"));
    }

    @Test
    void nullYBlancoPasanSinCifrar() {
        assertNull(AesGcmConverter.cifrar(null));
        assertEquals("", AesGcmConverter.cifrar(""));
        assertNull(AesGcmConverter.descifrar(null));
        assertEquals("", AesGcmConverter.descifrar(""));
    }

    @Test
    void yaCifradoNoSeRecifra() {
        String cifrado = AesGcmConverter.cifrar("12345678");
        assertSame(cifrado, AesGcmConverter.cifrar(cifrado));
    }

    @Test
    void textoPlanoLegibleSeDevuelveTalCualAlLeer() {
        assertEquals("12345678", AesGcmConverter.descifrar("12345678"));
    }

    @Test
    void prefijoEsEncV1() {
        assertTrue(AesGcmConverter.cifrar("00000001").startsWith("enc:v1:"));
        assertFalse(AesGcmConverter.estaCifrado("12345678"));
    }

    @Test
    void celularesYTelefonos_roundTrip() {
        String celular = "987654321";
        String telefono = "015551234";
        assertEquals(celular, AesGcmConverter.descifrar(AesGcmConverter.cifrar(celular)));
        assertEquals(telefono, AesGcmConverter.descifrar(AesGcmConverter.cifrar(telefono)));
    }

    @Test
    void converterDelegaEnMetodosEstaticos() {
        AesGcmConverter converter = new AesGcmConverter();
        String db = converter.convertToDatabaseColumn("11223344");
        assertTrue(AesGcmConverter.estaCifrado(db));
        assertEquals("11223344", converter.convertToEntityAttribute(db));
        assertEquals("11223344", converter.convertToEntityAttribute("11223344"));
    }
}
