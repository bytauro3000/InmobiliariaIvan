package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CERTIFICADO DE CANCELACION DE TERRENO — hoja final reutilizable.
 * Se usa en:
 *  - Contrato al contado (ContratoContadoFloridaPdf): siempre (el pago ya se hizo).
 *  - Contrato financiado (ContratoFloridaPdf): solo cuando el contrato está CANCELADO
 *    (cliente pagó la última letra).
 */
public class CertificadoCancelacionPdf {

	private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
	private static String ruc() { return EmpresaContext.empresaService.obtenerActiva().getRuc(); }
	private static String partidaElectronica() { return EmpresaContext.empresaService.obtenerActiva().getPartidaElectronica(); }

	/**
	 * Agrega al documento la hoja del certificado de cancelación.
	 * Internamente calcula fecha, cliente(s), lote y dirección a partir del contrato.
	 */
	public static void agregarCertificado(Document document, ContratoResponseDTO contrato,
			PdfFont arialBold, PdfFont arialBoldItalic, PdfFont arialItalic) {

		document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		document.setMargins(122, 85, 57, 85);

		LocalDate fechaRegistro = contrato.getFechaContrato();
		if (fechaRegistro == null) fechaRegistro = LocalDate.now();

		String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
				"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
		String diaNum = String.format("%02d", fechaRegistro.getDayOfMonth());
		String mesNombre = nombresMeses[fechaRegistro.getMonthValue() - 1];
		int anioNum = fechaRegistro.getYear();
		DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		List<ClienteResponseDTO> clientes = contrato.getClientes();
		int numClientes = clientes.size();
		// Prioriza al TITULAR si viene el rol (defensivo); si no, el primero.
		ClienteResponseDTO titular = clientes.stream()
				.filter(c -> c.getTipoPropietario() != null
						&& c.getTipoPropietario() == com.Inmobiliaria.demo.enums.TipoPropietario.TITULAR)
				.findFirst()
				.orElse(clientes.get(0));
		LoteResponseDTO lote = contrato.getLotes().get(0);

		// Dirección real del titular
		String nombreDistrito = (titular.getDistrito() != null) ? titular.getDistrito().getNombre() : "";
		String domicilioCalle = (titular.getDireccion() != null) ? titular.getDireccion().toUpperCase() : "";
		String nombreDepartamento = (titular.getDistrito() != null && titular.getDistrito().getDepartamento() != null)
				? titular.getDistrito().getDepartamento().toUpperCase() : "";
		String ubicacionDinamica;
		if (nombreDepartamento.contains("CALLAO")) {
			ubicacionDinamica = ", Distrito de " + nombreDistrito + ", Provincia Constitucional del Callao";
		} else {
			String depFormatted = nombreDepartamento.isEmpty() ? "Lima"
					: nombreDepartamento.charAt(0) + nombreDepartamento.substring(1).toLowerCase();
			ubicacionDinamica = ", Distrito de " + nombreDistrito + ", Provincia y Departamento de " + depFormatted;
		}
		String direccionRealParaContrato = domicilioCalle + ubicacionDinamica;

		document.add(new Paragraph("CERTIFICADO DE CANCELACION DE TERRENO")
				.setFont(arialBoldItalic).setFontSize(14).setUnderline().setTextAlignment(TextAlignment.CENTER)
				.setMarginBottom(25));

		Paragraph certEmpresa = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.2f);

		certEmpresa.add("La Empresa ");
		certEmpresa.add(new Text(empresa()).setFont(arialBold));
		certEmpresa.add(", inscrito en la Ficha #" + partidaElectronica() + " del Registro Mercantil de Lima, con RUC # " + ruc() + ".");

		document.add(certEmpresa);

		document.add(new Paragraph("")
				.setMarginTop(20));

		document.add(new Paragraph("CERTIFICA:")
				.setFont(arialBoldItalic).setFontSize(12).setTextAlignment(TextAlignment.LEFT)
				.setMarginBottom(15));

		Paragraph certCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.2f);

		certCuerpo.add("Que, ");
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
			boolean esFem = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));
			String pref = esFem ? "la Sra. " : "el Sr. ";
			String ident = esFem ? "identificada" : "identificado";
			String nacion = resolverNacionalidad(c);

			String estCivTexto = "";
			if (c.getEstadoCivil() != null) {
				estCivTexto = c.getEstadoCivil().toString().toLowerCase();
				if (esFem) {
					if (estCivTexto.equals("soltero")) estCivTexto = "soltera";
					else if (estCivTexto.equals("casado")) estCivTexto = "casada";
					else if (estCivTexto.equals("viudo")) estCivTexto = "viuda";
				}
			} else {
				estCivTexto = esFem ? "soltera" : "soltero";
			}

			certCuerpo.add(pref);
			certCuerpo.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setFont(arialBold));
			certCuerpo.add(", " + nacion + ", " + estCivTexto + ", " + ident + " con ");
			certCuerpo.add(new Text(etiquetaDocumento(c) + c.getNumDoc()).setFont(arialBold));
			if (numClientes > 1 && i < numClientes - 1) {
				certCuerpo.add(i == numClientes - 2 ? " y " : ", ");
			}
		}
		certCuerpo.add(", de ocupación independiente, con domicilio en " + direccionRealParaContrato);
		certCuerpo.add("; ha adquirido un lote de terreno rústico para fines de vivienda con un área de ");
		certCuerpo.add(new Text(lote.getArea() + "M2.").setFont(arialBold));
		certCuerpo.add(" En la Manzana “");
		certCuerpo.add(new Text(lote.getManzana()).setFont(arialBold));
		certCuerpo.add("” asignado con el lote Nº ");
		certCuerpo.add(new Text(lote.getNumeroLote()).setFont(arialBold));
		certCuerpo.add("; lote que forma parte de un predio de mayor extensión denominado lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
		certCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
		certCuerpo.add("; El cual se viene desarrollando el Programa de Vivienda ");
		certCuerpo.add(new Text("\"LA FLORIDA DE TORRE BLANCA\"").setFont(arialBoldItalic));
		certCuerpo.add(". El lote de terreno fue adquirido mediante contrato de Compra-Venta del ");
		certCuerpo.add(new Text(fechaRegistro.format(fmtFecha)).setFont(arialBoldItalic));
		certCuerpo.add(", encontrándose a la fecha cancelado el precio total de venta.");

		document.add(certCuerpo);

		document.add(new Paragraph("")
				.setMarginTop(15));

		Paragraph certCierre = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.2f);

		certCierre.add("Se extiende el presente certificado a solicitud de La Compradora, para los usos que crea conveniente.");

		document.add(certCierre);

		document.add(new Paragraph("")
				.setMarginTop(10));

		Paragraph certFirmaCiudad = new Paragraph()
				.setTextAlignment(TextAlignment.RIGHT)
				.setFont(arialItalic).setFontSize(12);

		certFirmaCiudad.add("Los Olivos, " + diaNum + " de " + mesNombre.toLowerCase() + " del año " + anioNum + ".");

		document.add(certFirmaCiudad);

		document.add(new Paragraph("")
				.setMarginTop(12));

		Paragraph certAtentamente = new Paragraph("Atentamente,")
				.setFont(arialItalic).setFontSize(12)
				.setTextAlignment(TextAlignment.CENTER)
				.setMarginTop(8);

		document.add(certAtentamente);
	}

	private static String resolverNacionalidad(ClienteResponseDTO c) {
		boolean esFemenino = c.getGenero() != null && c.getGenero().equals(Genero.Femenino);
		String nac = c.getNacionalidad();
		if (nac != null && !nac.isBlank()) return nac.toLowerCase().trim();
		return esFemenino ? "peruana" : "peruano";
	}

	private static String etiquetaDocumento(ClienteResponseDTO c) {
		if (c.getTipoCliente() == TipoCliente.CE) return "C.E. N°";
		return "DNI N°";
	}
}