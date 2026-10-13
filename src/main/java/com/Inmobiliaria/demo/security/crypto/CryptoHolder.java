package com.Inmobiliaria.demo.security.crypto;

import javax.crypto.SecretKey;

/**
 * Holder estático de la clave de cifrado.
 *
 * Existe como holder estático porque los {@code AttributeConverter} de JPA
 * son instanciados por Hibernate (fuera del contenedor Spring) y no pueden
 * recibir dependencias por DI. El bean {@link CryptoService} valida la
 * clave al arrancar la aplicación y la publica aquí antes de que Hibernate
 * procese la primera entidad.
 */
public final class CryptoHolder {

    private static volatile SecretKey clave;
    private static volatile String claveBase64;

    private CryptoHolder() {
    }

    public static void set(SecretKey nuevaClave, String base64) {
        clave = nuevaClave;
        claveBase64 = base64;
    }

    public static SecretKey getClave() {
        SecretKey k = clave;
        if (k == null) {
            throw new IllegalStateException(
                    "La clave de cifrado no está inicializada: falta la propiedad app.enc.key (env APP_ENC_KEY).");
        }
        return k;
    }

    public static String getClaveBase64() {
        String b = claveBase64;
        if (b == null) {
            throw new IllegalStateException(
                    "La clave de cifrado no está inicializada: falta la propiedad app.enc.key (env APP_ENC_KEY).");
        }
        return b;
    }

    public static boolean inicializada() {
        return clave != null;
    }
}
