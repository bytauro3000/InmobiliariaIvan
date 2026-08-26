package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.entity.Empresa;

/**
 * Utilidades de la empresa para las plantillas PDF.
 * Componer la dirección completa (calle + distrito + provincia + departamento)
 * a partir de los campos separados de la tabla empresa.
 * Se lee la empresa directamente del repositorio (sin pasar por la caché de
 * EmpresaService) para que los PDFs siempre reflejen el dato actual de la BD.
 */
public class EmpresaPdfUtil {

	private EmpresaPdfUtil() {
	}

	private static Empresa empresaFresca() {
		return EmpresaContext.empresaRepository.findByActivaTrue()
				.orElseThrow(() -> new RuntimeException("No hay una empresa activa configurada"));
	}

	/**
	 * Dirección completa de la empresa: "calle, Distrito de X, Provincia de Y, Departamento de Z".
	 * Si los campos de ubicación están vacíos, se omite la parte faltante.
	 */
	public static String direccionCompleta() {
		var e = empresaFresca();

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

	/**
	 * Dirección de la empresa para CONTRATOS, en el formato del modelo:
	 * "calle, Distrito de X, Provincia y Departamento de Y".
	 * Ej: "Av. Alfredo Mendiola N°3623 - ..., Distrito de Los Olivos, Provincia y Departamento de Lima"
	 */
	public static String direccionContrato() {
		var e = empresaFresca();

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
			sb.append("Provincia y Departamento de ").append(e.getProvincia().trim());
		} else if (e.getDepartamento() != null && !e.getDepartamento().isBlank()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append("Provincia y Departamento de ").append(e.getDepartamento().trim());
		}
		return sb.toString();
	}
}