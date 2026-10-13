package com.Inmobiliaria.demo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.Inmobiliaria.demo.enums.AccionAuditoria;

/**
 * Marca un método de controller como auditable.
 * El AuditoriaAspect registra la acción DESPUÉS de que el método termina
 * con éxito (si el negocio lanza excepción, no se audita como realizado).
 *
 * Uso:
 *   @Auditable(accion = AccionAuditoria.PAGAR, entidad = "PagoLetras")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    AccionAuditoria accion();

    /** Nombre lógico de la entidad principal que toca el método. */
    String entidad();

    /**
     * Índice (0-based) del argumento del método que contiene el id de la
     * entidad; -1 si no aplica (ej: LOGIN).
     */
    int indiceArgId() default -1;
}
