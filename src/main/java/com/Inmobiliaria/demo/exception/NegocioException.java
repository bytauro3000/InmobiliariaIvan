package com.Inmobiliaria.demo.exception;

// Excepción para errores de negocio — su mensaje SÍ se muestra al cliente
// Usar cuando el error es causado por datos incorrectos del usuario o reglas de negocio
// Ejemplos: lote ya vendido, separación no encontrada, comprobante duplicado
public class NegocioException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NegocioException(String mensaje) {
        super(mensaje);
    }
}