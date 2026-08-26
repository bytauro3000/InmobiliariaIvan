package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;

/**
 * Utilidades de la empresa para las plantillas PDF.
 * Componer la dirección completa (calle + distrito + provincia + departamento)
 * a partir de los campos separados de la tabla empresa.
 */
public class EmpresaPdfUtil {

	private EmpresaPdfUtil() {
	}

	/**
	 * Dirección completa de la empresa: "calle, Distrito de X, Provincia de Y, Departamento de Z".
	 * Si los campos de ubicación están vacíos, se omite la parte faltante.
	 */
	public static String direccionCompleta() {
		var e = EmpresaContext.empresaService.obtenerActiva();

		StringBuilder sb = new StringBuilder();
		if (e.getDireccion() != null && !e.getDireccion().isBlank()) {
			sb.append(e.getDireccion().trim());
		}
		if (e.getDistrito() != null && !e.getDistrito().isBlank()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append("Distrito de ").append(e.getDistrito().trim());
		}
		if (e.getProvincia() != null && !e.getProvincia().isBlank()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append("Provincia de ").append(e.getProvincia().trim());
		}
		if (e.getDepartamento() != null && !e.getDepartamento().isBlank()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append("Departamento de ").append(e.getDepartamento().trim());
		}
		return sb.toString();
	}
}