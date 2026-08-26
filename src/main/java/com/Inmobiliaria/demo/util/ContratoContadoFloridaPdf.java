package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.config.EmpresaContext;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Plantilla de CONTRATO AL CONTADO para el Programa "LA FLORIDA DE TORRE BLANCA" (IVAN).
 * A diferencia de ContratoFloridaPdf (financiado), el contado:
 *  - No tiene letras de cambio, cuota inicial ni reserva de propiedad.
 *  - El precio se cancela a la suscripción (depósito/transferencia/efectivo).
 *  - Incluye el CERTIFICADO DE CANCELACION DE TERRENO al final.
 * La cláusula PRIMERA (PROPIEDAD) varía según la ETAPA del programa (1RA/2DA/3RA).
 */
public class ContratoContadoFloridaPdf {

	private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
	private static String ruc() { return EmpresaContext.empresaService.obtenerActiva().getRuc(); }
	private static String representanteLegal() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteLegal(); }
	private static String representanteDni() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteDni(); }
	private static String partidaElectronica() { return EmpresaContext.empresaService.obtenerActiva().getPartidaElectronica(); }
	private static String direccion() { return EmpresaContext.empresaService.obtenerActiva().getDireccion(); }

	public static byte[] generarContratoContadoFlorida(ContratoResponseDTO contrato) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(122, 85, 57, 85);

		// ── Carga de fuentes ───────────────────────────────────────────────────
		PdfFont arialBold;
		PdfFont arialBoldItalic;
		PdfFont arialItalic;
		try {
			byte[] bBytes = StreamUtil.inputStreamToArray(
					ContratoContadoFloridaPdf.class.getClassLoader().getResourceAsStream("fonts/ARIALBD.TTF"));
			arialBold = PdfFontFactory.createFont(bBytes, PdfEncodings.WINANSI);

			byte[] biBytes = StreamUtil.inputStreamToArray(
					ContratoContadoFloridaPdf.class.getClassLoader().getResourceAsStream("fonts/ARIALBI.TTF"));
			arialBoldItalic = PdfFontFactory.createFont(biBytes, PdfEncodings.WINANSI);

			byte[] iBytes = StreamUtil.inputStreamToArray(
					ContratoContadoFloridaPdf.class.getClassLoader().getResourceAsStream("fonts/ARIALI.TTF"));
			arialItalic = PdfFontFactory.createFont(iBytes, PdfEncodings.WINANSI);
		} catch (Exception e) {
			throw new RuntimeException("Error cargando las fuentes Arial desde resources/fonts/", e);
		}

		// ── Fecha del contrato ─────────────────────────────────────────────────
		LocalDate fechaRegistro = contrato.getFechaContrato();
		if (fechaRegistro == null) fechaRegistro = LocalDate.now();

		String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
				"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
		String diaNum = String.format("%02d", fechaRegistro.getDayOfMonth());
		String mesNombre = nombresMeses[fechaRegistro.getMonthValue() - 1];
		String mesNum = String.format("%02d", fechaRegistro.getMonthValue());
		int anioNum = fechaRegistro.getYear();

		// ── Datos del cliente ──────────────────────────────────────────────────
		List<ClienteResponseDTO> clientes = contrato.getClientes();
		int numClientes = clientes.size();
		ClienteResponseDTO titular = clientes.get(0);

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

		// Bloque de compradores
		Paragraph bloqueCompradores = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10);
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
			boolean esFemenino = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));

			String prefijo = esFemenino ? "la Sra. " : "el Sr. ";
			String nacionalidad = resolverNacionalidad(c);
			String identif = esFemenino ? "identificada" : "identificado";

			String estCivil = "";
			if (c.getEstadoCivil() != null) {
				estCivil = c.getEstadoCivil().toString().toLowerCase();
				if (esFemenino) {
					if (estCivil.equals("soltero")) estCivil = "soltera";
					if (estCivil.equals("casado")) estCivil = "casada";
					if (estCivil.equals("viudo")) estCivil = "viuda";
				}
			} else {
				estCivil = esFemenino ? "soltera" : "soltero";
			}

			bloqueCompradores.add(prefijo);
			bloqueCompradores.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setBold());
			bloqueCompradores.add(", " + nacionalidad + ", " + estCivil + ", " + identif + " con ");
			bloqueCompradores.add(new Text(etiquetaDocumento(c) + c.getNumDoc()).setBold());

			if (numClientes > 1 && i < numClientes - 1) {
				if (i == numClientes - 2) bloqueCompradores.add(", y ");
				else bloqueCompradores.add(", ");
			}
		}

		// Variables de concordancia
		String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES" : (titular.getGenero() != null && titular.getGenero().equals(Genero.Femenino) ? "LA COMPRADORA" : "EL COMPRADOR");
		String pronombreDenom = (numClientes > 1) ? "les" : "le";
		String verboDeclara = (numClientes > 1) ? "declaran" : "declara";
		String etiquetaDomicilio = (numClientes > 1) ? "ambos con domicilio común en " : "con domicilio en ";

		// ── Datos del lote ─────────────────────────────────────────────────────
		LoteResponseDTO lote = contrato.getLotes().get(0);
		BigDecimal areaTotal = BigDecimal.ZERO;
		for (LoteResponseDTO l : contrato.getLotes()) areaTotal = areaTotal.add(l.getArea());

		// ── Moneda y formato ───────────────────────────────────────────────────
		Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
		String prefMoneda = (monedaContrato == Moneda.PEN) ? "S/" : "US$";
		DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
		String montoTotalLetras = NumeroALetras.convertir(contrato.getMontoTotal(), monedaContrato);

		/* =====================================================================
		 * ENCABEZADO DEL CONTRATO
		 * ===================================================================== */
		document.add(new Paragraph("PROGRAMA DE VIVIENDA")
				.setFont(arialBoldItalic).setFontSize(18).setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(16).setMarginBottom(0));

		document.add(new Paragraph("“LA FLORIDA DE TORRE BLANCA”")
				.setFont(arialBoldItalic).setFontSize(18).setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(16).setMarginBottom(5));

		document.add(new Paragraph("CONTRATO PRIVADO DE COMPRA-VENTA DE TERRENO RUSTICO")
				.setFont(arialBoldItalic).setFontSize(11).setUnderline().setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(15));

		/* =====================================================================
		 * INTRODUCCION
		 * ===================================================================== */
		Paragraph intro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.5f);

		intro.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico que celebran de una parte ");

		intro.add(new Text("\u201c" + empresa() + "\u201d ").setFont(arialBold));
		intro.add("con ");
		intro.add(new Text("RUC N\u00ba " + ruc() + " ").setFont(arialBold));
		intro.add("con domicilio en " + direccion() + ", debidamente representado por su ");
		intro.add(new Text(representanteLegal() + " ").setFont(arialBold));
		intro.add("con ");
		intro.add(new Text("DNI No." + representanteDni() + " ").setFont(arialBold));
		intro.add("según consta del poder inscrito en la partida electrónica Nº ");
		intro.add(new Text(partidaElectronica() + " ").setFont(arialBold));
		intro.add("del Registro de Personas Jurídicas, a quien en adelante se le denominará ");
		intro.add(new Text("LA VENDEDORA").setFont(arialBold));
		intro.add("; y de la otra parte ");

		for (com.itextpdf.layout.element.IElement el : bloqueCompradores.getChildren()) {
			intro.add((com.itextpdf.layout.element.ILeafElement) el);
		}

		intro.add(", de ocupación independiente, " + etiquetaDomicilio + direccionRealParaContrato);
		intro.add(", a quien en adelante se " + pronombreDenom + " denominará ");
		intro.add(new Text(etiquetaComprador).setFont(arialBold).setUnderline());
		intro.add(" en los términos y condiciones de las cláusulas siguientes:");

		document.add(intro);

		/* =====================================================================
		 * PRIMERA: PROPIEDAD (VARIA SEGUN LA ETAPA DEL PROGRAMA)
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("PRIMERA: PROPIEDAD").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(10));

		Paragraph primeraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		primeraCuerpo.add(new Text("“LA VENDEDORA”").setFont(arialBoldItalic));

		String nombreProgFiltro = lote.getNombrePrograma().toUpperCase();

		if (nombreProgFiltro.contains("2DA ETAPA")) {
			// TEXTO PARA LA SEGUNDA ETAPA (15 Has - 74.54%)
			primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 150,000.00 m2 Equivalente a 15 Has., que corresponde al 74.543780% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de la Avenida Túpac Amaru el cual forma parte de un área superficial de 201,224.03m2 equivalente a 20has. 1,224.04m2, ubicado en el Distrito de Carabayllo, Provincia y Departamento De Lima, formando parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
			primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
			primeraCuerpo.add("\n\nFue adquirido mediante contrato Privado de Compra- Venta de Acciones y Derechos a plazos de un Predio Rustico de fecha ");
			primeraCuerpo.add(new Text("06/11/2019").setFont(arialBoldItalic));
			primeraCuerpo.add(". Que le otorgo su anterior Propietaria ");
			primeraCuerpo.add(new Text("INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificada con ");
			primeraCuerpo.add(new Text("RUC. N°20601878616").setFont(arialBoldItalic));
			primeraCuerpo.add(", debidamente representada por su Gerente General ");
			primeraCuerpo.add(new Text("DON JOSE ANTONIO ESPINOZA TENA").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificado con ");
			primeraCuerpo.add(new Text("DNI N°09403557").setFont(arialBoldItalic));
			primeraCuerpo.add(" y su SubGerente ");
			primeraCuerpo.add(new Text("DON JHAMPIERE ULISES COSTILLA ESTRADA").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificado con ");
			primeraCuerpo.add(new Text("DNI Nº4512577").setFont(arialBoldItalic));
			primeraCuerpo.add(". Sobre dicho terreno, ");
			primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			primeraCuerpo.add(" ha proyectado el Programa de Vivienda denominado ");
			primeraCuerpo.add(new Text("\u201cLA FLORIDA DE TORRE BLANCA - 2DA ETAPA\u201d").setFont(arialBoldItalic));
			primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización.");

		} else if (nombreProgFiltro.contains("3RA ETAPA")) {
			// TEXTO PARA LA TERCERA ETAPA
			primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
			primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
			primeraCuerpo.add("\n\nFue adquirido mediante la minuta de Compra- Venta de Acciones y Derechos de Predio Rustico de la fecha ");
			primeraCuerpo.add(new Text("06/11/2019").setFont(arialBoldItalic));
			primeraCuerpo.add(" (15 Has.) y con fecha ");
			primeraCuerpo.add(new Text("29/03/2021").setFont(arialBoldItalic));
			primeraCuerpo.add(" (51,224.03 m2). Que le otorgo su anterior Propietaria ");
			primeraCuerpo.add(new Text("INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificada con ");
			primeraCuerpo.add(new Text("RUC. N°20601878616").setFont(arialBoldItalic));
			primeraCuerpo.add(", debidamente representada por su Gerente General ");
			primeraCuerpo.add(new Text("DON JOSE ANTONIO ESPINOZA TENA").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificado con ");
			primeraCuerpo.add(new Text("DNI N°09403557").setFont(arialBoldItalic));
			primeraCuerpo.add(". Sobre dicho terreno, ");
			primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			primeraCuerpo.add(" ha proyectado el Programa de Vivienda denominado ");
			primeraCuerpo.add(new Text("\u201cLA FLORIDA DE TORRE BLANCA - 3RA ETAPA\u201d").setFont(arialBoldItalic));
			primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización.");

		} else if (nombreProgFiltro.contains("LA FLORIDA DE TORRE BLANCA")) {
			// TEXTO PARA LA PRIMERA ETAPA (20 Has - 100%)
			primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
			primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
			primeraCuerpo.add("\n\nFue adquirido mediante la minuta de Compra- Venta de Acciones y Derechos de Predio Rustico de la fecha ");
			primeraCuerpo.add(new Text("06/11/2019").setFont(arialBoldItalic));
			primeraCuerpo.add(" (15 Has.) y con fecha ");
			primeraCuerpo.add(new Text("29/03/2021").setFont(arialBoldItalic));
			primeraCuerpo.add(" (51,224.03 m2). Que le otorgo su anterior Propietaria ");
			primeraCuerpo.add(new Text("INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificada con ");
			primeraCuerpo.add(new Text("RUC. N°20601878616").setFont(arialBoldItalic));
			primeraCuerpo.add(", debidamente representada por su Gerente General ");
			primeraCuerpo.add(new Text("DON JOSE ANTONIO ESPINOZA TENA").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificado con ");
			primeraCuerpo.add(new Text("DNI N°09403557").setFont(arialBoldItalic));
			primeraCuerpo.add(". Sobre dicho terreno, ");
			primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			primeraCuerpo.add(" ha proyectado el Programa de Vivienda denominado ");
			primeraCuerpo.add(new Text("\u201cLA FLORIDA DE TORRE BLANCA - 1RA ETAPA\u201d").setFont(arialBoldItalic));
			primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización.");

		} else {
			// Texto por defecto para otros programas (San Javier, Villa Hermosa, etc.)
			primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
			primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
			primeraCuerpo.add("\n\nFue adquirido mediante la minuta de Compra- Venta de Acciones y Derechos de Predio Rustico de la fecha ");
			primeraCuerpo.add(new Text("06/11/2019").setFont(arialBoldItalic));
			primeraCuerpo.add(" (15 Has.) y con fecha ");
			primeraCuerpo.add(new Text("29/03/2021").setFont(arialBoldItalic));
			primeraCuerpo.add(" (51,224.03 m2). Que le otorgo su anterior Propietaria ");
			primeraCuerpo.add(new Text("INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificada con ");
			primeraCuerpo.add(new Text("RUC. N°20601878616").setFont(arialBoldItalic));
			primeraCuerpo.add(", debidamente representada por su Gerente General ");
			primeraCuerpo.add(new Text("DON JOSE ANTONIO ESPINOZA TENA").setFont(arialBoldItalic));
			primeraCuerpo.add(", identificado con ");
			primeraCuerpo.add(new Text("DNI N°09403557").setFont(arialBoldItalic));
			primeraCuerpo.add(". Sobre dicho terreno, ");
			primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			primeraCuerpo.add(" ha proyectado el Programa de Vivienda denominado ");
			primeraCuerpo.add(new Text("\u201c" + lote.getNombrePrograma() + "\u201d").setFont(arialBoldItalic));
			primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización.");
		}

		document.add(primeraCuerpo);

		/* =====================================================================
		 * SEGUNDA: OBJETO DEL CONTRATO
		 * ===================================================================== */
		verificarEspacioYSalto(document, pdf, 0.4f);

		document.add(new Paragraph()
				.add(new Text("SEGUNDA: OBJETO DEL CONTRATO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph segundaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		segundaIntro.add("Por el presente contrato ");
		segundaIntro.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaIntro.add(" se obliga a transferir un lote de terreno ubicado en la Manzana “");
		segundaIntro.add(new Text(lote.getManzana()).setFont(arialBoldItalic));
		segundaIntro.add("” y asignado, con el lote Nº ");
		segundaIntro.add(new Text(lote.getNumeroLote()).setFont(arialBoldItalic));
		segundaIntro.add(" del Programa de Vivienda ");
		segundaIntro.add(new Text("“LA FLORIDA DE TORRE BLANCA”").setFont(arialBoldItalic));
		segundaIntro.add(" con un área de ");
		segundaIntro.add(new Text(areaTotal + "M2.").setFont(arialBoldItalic));
		segundaIntro.add(" Encerrado dentro de los siguientes linderos y medidas perimétricas:");

		document.add(segundaIntro);

		// Tabla de linderos — mismo espacio arriba y abajo del bloque
		Table tablaLinderos = new Table(UnitValue.createPercentArray(new float[]{30f, 45f, 25f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginLeft(20)
				.setMarginTop(14)
				.setMarginBottom(14);

		agregarFilaLinderos(tablaLinderos, "Por el frente", lote.getColindanteNorte(), "Con    " + lote.getAncho1() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaLinderos, "Por la derecha", lote.getColindanteEste(), "Con  " + lote.getLargo1() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaLinderos, "Por la Izquierda", lote.getColindanteOeste(), "Con    " + lote.getLargo2() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaLinderos, "Por el fondo", lote.getColindanteSur(), "Con    " + lote.getAncho2() + "  m.l.", arialItalic);

		document.add(tablaLinderos);

		Paragraph segundaFinal = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		segundaFinal.add("Por el presente contrato ");
		segundaFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaFinal.add(" otorga en venta real un lote de terreno rústico con veredas, agua y luz provisional previo al pago de cada servicio brindado a ");
		segundaFinal.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		segundaFinal.add(" así mismo, correspondiéndole sus aires, usos, costumbres, entradas, salidas y todo cuanto de hecho y por derecho le corresponde sin reserva ni limitación alguna, toda vez que la finalidad del presente contrato es que surta todos sus efectos legales.");

		document.add(segundaFinal);

		/* =====================================================================
		 * TERCERA: PRECIO, LUGAR Y FORMA DE PAGO (CONTADO)
		 * ===================================================================== */
		verificarEspacioYSalto(document, pdf, 0.4f);

		document.add(new Paragraph()
				.add(new Text("PRECIO, LUGAR Y FORMA DE PAGO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph terceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		terceraCuerpo.add(new Text("CLAUSULA TERCERA: ").setFont(arialBoldItalic));
		terceraCuerpo.add("El precio del bien objeto de la prestación a cargo de ");
		terceraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceraCuerpo.add(" asciende a la suma de ");
		terceraCuerpo.add(new Text(prefMoneda + " " + df.format(contrato.getMontoTotal())).setFont(arialBoldItalic));
		terceraCuerpo.add(new Text(" (" + montoTotalLetras + ")").setFont(arialBoldItalic));
		terceraCuerpo.add(", que es cancelado a la suscripción del presente documento ");

		// Medio de pago dinámico desde el pago inicial
		MedioPago medioPago = contrato.getPagoInicial() != null
				? contrato.getPagoInicial().getMedioPago() : null;
		String numeroOperacion = contrato.getPagoInicial() != null
				? contrato.getPagoInicial().getNumeroOperacion() : null;

		if (medioPago == null || medioPago == MedioPago.EFECTIVO) {
			terceraCuerpo.add("en efectivo");
		} else {
			terceraCuerpo.add("mediante " + descripcionMedioPago(medioPago));
			if (numeroOperacion != null && !numeroOperacion.isBlank() && !"0".equals(numeroOperacion.trim())) {
				terceraCuerpo.add(" con número de operación " + numeroOperacion.trim());
			}
		}

		terceraCuerpo.add(", surtiendo sus efectos cancelatorios respecto del precio establecido en la presente clausula a favor de ");
		terceraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceraCuerpo.add(" por lo que en señal de conformidad con el pago total recibido a su favor en contraprestación por la venta del lote objeto del presente contrato, ");
		terceraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceraCuerpo.add(" suscribe el presente contrato.");

		document.add(terceraCuerpo);

		/* =====================================================================
		 * CUARTA: EQUIVALENCIA
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("EQUIVALENCIA").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph cuartaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		cuartaCuerpo.add(new Text("CLAUSULA CUARTA: ").setFont(arialBoldItalic));
		cuartaCuerpo.add("Las partes contratantes declaran que el precio total pactado es el que realmente corresponde a las acciones y derechos del predio objeto del presente contrato, existiendo entre aquellas y éste perfecta equivalencia, en consecuencia, se hacen recíproca donación de cualquier diferencia y renuncian a cualquier acción o excepción que por dicha causa pudieran interponer para invalidar los efectos del presente contrato, así como a los plazos para interponerla.");

		document.add(cuartaCuerpo);

		/* =====================================================================
		 * QUINTA: CARGAS Y GRAVAMENES
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("CARGAS Y GRAVAMENES").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph quintaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		quintaCuerpo.add(new Text("CLAUSULA QUINTA: ").setFont(arialBoldItalic));
		quintaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		quintaCuerpo.add(" declara que el bien objeto de la prestación a su cargo al momento de celebrarse este contrato se encuentra libre de toda carga, gravamen, derecho real de garantía, medida judicial o extrajudicial y en general de todo acto o circunstancia que impida, prive o limite la libre disponibilidad, y/o el derecho de propiedad, posesión o uso del bien. No obstante, ");
		quintaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		quintaCuerpo.add(" se obliga al saneamiento por evicción.");

		document.add(quintaCuerpo);

		/* =====================================================================
		 * SEXTA: GASTOS Y TRIBUTOS
		 * ===================================================================== */
		// Agrupamos título + cuerpo con setKeepTogether para que la cláusula
		// no se corte entre dos páginas (toda la cláusula salta junta).
		com.itextpdf.layout.element.Div sextaDiv = new com.itextpdf.layout.element.Div().setKeepTogether(true);

		sextaDiv.add(new Paragraph()
				.add(new Text("GASTOS Y TRIBUTOS").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph sextaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		sextaCuerpo.add(new Text("CLAUSULA SEXTA: ").setFont(arialBoldItalic));
		sextaCuerpo.add("Las partes contratantes acuerdan que todos los gastos y tributos que originen la celebración, formalización y ejecución del presente contrato serán asumidos por ");
		sextaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		sextaCuerpo.add(".");

		sextaDiv.add(sextaCuerpo);
		document.add(sextaDiv);

		/* =====================================================================
		 * SEPTIMA: ESCRITURA PÚBLICA
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("ESCRITURA PÚBLICA").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph septimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		septimaCuerpo.add(new Text("CLAUSULA SEPTIMA: ").setFont(arialBoldItalic));
		septimaCuerpo.add("Queda establecido que los derechos y/o transferencia de propiedad que otorga el presente contrato serán formalizados mediante Escritura Publica una vez que ");
		septimaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		septimaCuerpo.add(" haya concluido con todos los trámites pertinentes para la formalización de su derecho de propiedad, obligándose ");
		septimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		septimaCuerpo.add(" a asumir los gastos administrativos de la empresa y derecho de formalización, notariales, registrales e impuestos de ley que por imperio de la ley les corresponden pagar.");

		document.add(septimaCuerpo);

		/* =====================================================================
		 * OCTAVA: ENTREGA DEL BIEN OBJETO DEL PRESENTE CONTRATO
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("ENTREGA DEL BIEN OBJETO DEL PRESENTE CONTRATO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph octavaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		octavaCuerpo.add(new Text("CLAUSULA OCTAVA: ").setFont(arialBoldItalic));
		octavaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		octavaCuerpo.add(" declara que la entrega física y real del predio objeto del presente contrato se realizara a la suscripción y legalización del presente contrato, por lo que a partir de ello el cuidado, administración y conservación del bien lo asume ");
		octavaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		octavaCuerpo.add(". Así mismo, ");
		octavaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		octavaCuerpo.add(", " + verboDeclara + " conocer la situación física, real y legal del predio objeto de compraventa, el mismo que lo encuentra a su entera satisfacción, por tanto, renuncia a toda acción rescisoria por dolo, error, lesión y cualquiera que tienda a invalidar el presente contrato.");

		document.add(octavaCuerpo);

		/* =====================================================================
		 * NOVENA: COMPETENCIA TERRITORIAL Y DOMICILIO
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("COMPETENCIA TERRITORIAL Y DOMICILIO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph novenaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		novenaCuerpo.add(new Text("CLAUSULA NOVENA. - ").setFont(arialBoldItalic));
		novenaCuerpo.add("las partes contratantes renuncian expresamente al fuero de sus domicilios y se someten a la jurisdicción de los jueces y tribunales de la corte superior de justicia de lima norte, para todos los efectos del presente contrato que pudiera dar origen.");

		document.add(novenaCuerpo);

		/* =====================================================================
		 * DECIMA: APLICACIÓN SUPLETORIA DE LA LEY
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("APLICACIÓN SUPLETORIA DE LA LEY – COMPETENCIA JURISDICCIONAL:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph decimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		decimaCuerpo.add(new Text("CLAUSULA DECIMA.- ").setFont(arialBoldItalic));
		decimaCuerpo.add("en todo lo no previsto en el presente contrato ambas partes contratantes se someten a lo establecido por las normas del código civil y demás normas que sean aplicables, así mismo declaran que en el supuesto que surja cualquier diferencia o controversia en relación al presente contrato, tanto en su interpretación como en su ejecución, las partes tratarán de solucionarlo directamente y de común acuerdo, caso contrario las partes se someten a la competencia de los jueces y tribunales de lima norte.");

		document.add(decimaCuerpo);

		/* =====================================================================
		 * DECIMA PRIMERA: CONFORMIDAD DE LAS PARTES CONTRATANTES
		 * ===================================================================== */
		document.add(new Paragraph()
				.add(new Text("CONFORMIDAD DE LAS PARTES CONTRATANTES:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11).setMarginTop(15));

		Paragraph undecimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f);

		undecimaCuerpo.add(new Text("CLAUSULA DECIMA PRIMERA - ").setFont(arialBoldItalic));
		undecimaCuerpo.add("las partes declaran aceptar todas y cada una de las cláusulas contenidas en el presente contrato, expresando que suscriben la misma bajo libre expresión de su voluntad, no habiendo mediado presión, dolo, violencia u otro medio ilícito análogo, renunciando a cualquier acción legal ulterior destinado a enervar los efectos legales del presente contrato. ");

		// Fecha en letras
		BigDecimal diaLetrasBD = BigDecimal.valueOf(fechaRegistro.getDayOfMonth());
		String diaLetras = NumeroALetras.convertir(diaLetrasBD).split(" CON ")[0].trim().toLowerCase();
		BigDecimal anioBigDecimal = BigDecimal.valueOf(anioNum);
		String anioBase = NumeroALetras.convertir(anioBigDecimal).split(" CON ")[0].trim().toLowerCase();
		StringBuilder formatAnio = new StringBuilder();
		for (String palabra : anioBase.split(" ")) {
			if (palabra.length() > 0) {
				formatAnio.append(Character.toUpperCase(palabra.charAt(0)))
						.append(palabra.substring(1)).append(" ");
			}
		}
		String anioLetrasFinal = formatAnio.toString().trim();

		undecimaCuerpo.add("Una vez leído el texto completo, expresan su conformidad suscribiendo el mismo en dos ejemplares idénticas ");
		undecimaCuerpo.add(new Text("a los " + diaLetras + " (" + diaNum + ") días del mes de " + mesNombre + " (" + mesNum + ") del año " + anioLetrasFinal + " (" + anioNum + ").")
				.setFont(arialItalic));

		document.add(undecimaCuerpo);

		/* =====================================================================
		 * FIRMAS DEL CONTRATO
		 * ===================================================================== */
		document.add(new Paragraph("").setMarginTop(20f));
		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		/* =====================================================================
		 * DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO
		 * ===================================================================== */
		document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		document.setMargins(122, 85, 30, 85);

		document.add(new Paragraph()
				.add(new Text("DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO")
						.setFont(arialBoldItalic).setUnderline())
				.setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

		Paragraph introPosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.2f);

		introPosesion.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico que celebran de una parte ");

		introPosesion.add(new Text("\u201c" + empresa() + "\u201d ").setFont(arialBold));
		introPosesion.add("con RUC No. ");
		introPosesion.add(new Text(ruc() + " ").setFont(arialBold));
		introPosesion.add("con domicilio en " + direccion() + ", debidamente representado por su ");
		introPosesion.add(new Text(representanteLegal() + " ").setFont(arialBold));
		introPosesion.add("con ");
		introPosesion.add(new Text("DNI No." + representanteDni() + " ").setFont(arialBold));
		introPosesion.add("según consta del poder inscrito en la partida electrónica No. ");
		introPosesion.add(new Text(partidaElectronica() + " ").setFont(arialBold));
		introPosesion.add("del Registro de Personas Jurídicas, a quien en adelante se le denominara ");
		introPosesion.add(new Text("LA VENDEDORA; ").setFont(arialBold));

		introPosesion.add("y de la otra parte ");

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

			introPosesion.add(pref);
			introPosesion.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setFont(arialBold));
			introPosesion.add(", " + nacion + ", " + estCivTexto + ", " + ident + " con ");
			introPosesion.add(new Text(etiquetaDocumento(c) + c.getNumDoc()).setFont(arialBold));

			if (numClientes > 1 && i < numClientes - 1) {
				introPosesion.add(i == numClientes - 2 ? " y " : ", ");
			}
		}

		introPosesion.add(", de ocupación independiente, " + etiquetaDomicilio + direccionRealParaContrato);
		introPosesion.add(", a quien en adelante se les denominará ");
		introPosesion.add(new Text(etiquetaComprador).setFont(arialBold));
		introPosesion.add(" en los términos y condiciones de las cláusulas siguientes:");

		document.add(introPosesion);

		/* --- CLAUSULA PRIMERO: DETALLE DEL LOTE --- */
		Paragraph primeroCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMarginTop(10);

		primeroCuerpo.add(new Text("PRIMERO.").setFont(arialBold).setUnderline());
		primeroCuerpo.add(" - ");
		primeroCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		primeroCuerpo.add(" en virtud del presente contrato de Compra - Venta celebrado con ");
		primeroCuerpo.add(new Text("\"" + etiquetaComprador + "\"").setFont(arialBold));

		DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		primeroCuerpo.add(" con fecha ");
		primeroCuerpo.add(new Text(fechaRegistro.format(fmtFecha)).setFont(arialBold));
		primeroCuerpo.add(" dio en venta real un lote de terreno rústico de ");
		primeroCuerpo.add(new Text(lote.getArea() + "M2").setFont(arialBold));
		primeroCuerpo.add(". El mismo que se ubica en la Manzana “");
		primeroCuerpo.add(new Text(lote.getManzana()).setFont(arialBold));
		primeroCuerpo.add("” y se encuentra signado con el lote Nº ");
		primeroCuerpo.add(new Text(lote.getNumeroLote()).setFont(arialBold));
		primeroCuerpo.add(" correspondiente al Programa de Vivienda ");
		primeroCuerpo.add(new Text("\"LA FLORIDA DE TORRE BLANCA\"").setFont(arialBold));
		primeroCuerpo.add(" del Distrito de Carabayllo, Provincia y Departamento de Lima; cuyos linderos y medidas perimétricas son las siguientes:");

		document.add(primeroCuerpo);

		Table tablaPosesionLinderos = new Table(UnitValue.createPercentArray(new float[]{30f, 45f, 25f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginLeft(20)
				.setMarginTop(14)
				.setMarginBottom(14);

		agregarFilaLinderos(tablaPosesionLinderos, "Por el frente", lote.getColindanteNorte(), "Con    " + lote.getAncho1() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaPosesionLinderos, "Por la derecha", lote.getColindanteEste(), "Con  " + lote.getLargo1() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaPosesionLinderos, "Por la Izquierda", lote.getColindanteOeste(), "Con    " + lote.getLargo2() + "  m.l.", arialItalic);
		agregarFilaLinderos(tablaPosesionLinderos, "Por el fondo", lote.getColindanteSur(), "Con    " + lote.getAncho2() + "  m.l.", arialItalic);

		document.add(tablaPosesionLinderos);

		// Descripción del predio (etapa) en posesión
		Paragraph posesionPredio = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f).setMarginTop(10);

		posesionPredio.add("Dicho lote se encuentra ubicado en el predio denominado lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
		posesionPredio.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));

		document.add(posesionPredio);

		/* --- CLAUSULA SEGUNDO: POSESION EFECTIVA --- */
		Paragraph segundoCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMarginTop(15).setMultipliedLeading(1.0f);

		segundoCuerpo.add(new Text("SEGUNDO.").setFont(arialBold).setUnderline());
		segundoCuerpo.add(" - ");
		segundoCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		segundoCuerpo.add(" mediante el presente documento, da en posesión efectiva a ");
		segundoCuerpo.add(new Text("\"" + etiquetaComprador + "\"").setFont(arialBold));
		segundoCuerpo.add(" el lote de terreno señalado en la cláusula anterior, quien declara haberlo recepcionado a su entera y completa satisfacción.");

		document.add(segundoCuerpo);

		/* --- CLAUSULA TERCERA: PRECIO DE VENTA (CONTADO) --- */
		Paragraph terceroPosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMarginTop(15).setMultipliedLeading(1.0f);

		terceroPosesion.add(new Text("PRECIO DE VENTA").setFont(arialBoldItalic).setUnderline());
		terceroPosesion.add("\n");
		terceroPosesion.add(new Text("TERCERA. - ").setFont(arialBold));
		terceroPosesion.add("El precio del bien objeto de la prestación a cargo de ");
		terceroPosesion.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceroPosesion.add(" asciende a la suma de ");
		terceroPosesion.add(new Text(prefMoneda + " " + df.format(contrato.getMontoTotal())).setFont(arialBoldItalic));
		terceroPosesion.add(new Text(" (" + montoTotalLetras + ")").setFont(arialBoldItalic));
		terceroPosesion.add(", que es cancelado a la suscripción del presente documento ");

		if (medioPago == null || medioPago == MedioPago.EFECTIVO) {
			terceroPosesion.add("en efectivo");
		} else {
			terceroPosesion.add("mediante " + descripcionMedioPago(medioPago));
			if (numeroOperacion != null && !numeroOperacion.isBlank() && !"0".equals(numeroOperacion.trim())) {
				terceroPosesion.add(" con número de operación " + numeroOperacion.trim());
			}
		}

		terceroPosesion.add(", surtiendo sus efectos cancelatorios respecto del precio establecido en la presente clausula a favor de ");
		terceroPosesion.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceroPosesion.add(" por lo que en señal de conformidad con el pago total recibido a su favor en contraprestación por la venta del lote objeto del presente contrato, ");
		terceroPosesion.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceroPosesion.add(" suscribe el presente contrato.");

		document.add(terceroPosesion);

		/* --- CLAUSULA CUARTA: RESPONSABILIDAD DEL COMPRADOR --- */
		Paragraph cuartoPosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMarginTop(15).setMultipliedLeading(1.0f);

		cuartoPosesion.add(new Text("CUARTA.").setFont(arialBold).setUnderline());
		cuartoPosesion.add(" - Queda entendido que al entrar ");
		cuartoPosesion.add(new Text("\"" + etiquetaComprador + "\"").setFont(arialBold));
		cuartoPosesion.add(" en posesión del terreno signado en la fecha es de su responsabilidad absoluta a partir de la misma, el pago de impuestos, haciéndose cargo también de la defensa de su posesión frente a terceros por los medios permitidos por ley, respetando estrictamente sus linderos, medidas perimétricas y los derechos de posesión y de propiedad de sus vecinos, así como los estipulados por el Reglamento Nacional de Construcciones. Así mismo se compromete también a pagar proporcionalmente los gastos efectuados y por efectuarse respecto a las obras de urbanización como son: Obras de agua, Desagüe, Electrificación y otras que puedan introducirse en bien del Programa de vivienda.");

		document.add(cuartoPosesion);

		/* --- CIERRE DE SEÑALIZACION --- */
		Paragraph cierrePosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMarginTop(20).setMultipliedLeading(1.0f);

		cierrePosesion.add("Conformes ambas partes con el contenido del presente documento, lo firman por duplicado del día ");

		String fechaTextoCierre = diaNum + " de " + mesNombre.toLowerCase() + " del " + anioNum + ".";
		cierrePosesion.add(new Text(fechaTextoCierre).setFont(arialItalic));

		document.add(cierrePosesion);

		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		/* =====================================================================
		 * CERTIFICADO DE CANCELACION DE TERRENO
		 * ===================================================================== */
		document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		document.setMargins(122, 85, 57, 85);

		document.add(new Paragraph("CERTIFICADO DE CANCELACION DE TERRENO")
				.setFont(arialBoldItalic).setFontSize(14).setTextAlignment(TextAlignment.CENTER)
				.setMarginBottom(25));

		Paragraph certEmpresa = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.5f);

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
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.5f);

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
				.setFont(arialItalic).setFontSize(12).setMultipliedLeading(1.5f);

		certCierre.add("Se extiende el presente certificado a solicitud de La Compradora, para los usos que crea conveniente.");

		document.add(certCierre);

		document.add(new Paragraph("")
				.setMarginTop(25));

		Paragraph certFirmaCiudad = new Paragraph()
				.setTextAlignment(TextAlignment.CENTER)
				.setFont(arialItalic).setFontSize(12);

		certFirmaCiudad.add("Los Olivos, " + diaNum + " de " + mesNombre.toLowerCase() + " del año " + anioNum + ".");

		document.add(certFirmaCiudad);

		document.add(new Paragraph("")
				.setMarginTop(40));

		// Firma de la empresa en el certificado
		Table tablaCert = new Table(UnitValue.createPercentArray(new float[]{45f, 10f, 45f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER);

		Cell celdaCertFirma = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
		Paragraph pLineaCert = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
				.setWidth(220f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
		celdaCertFirma.add(pLineaCert);
		celdaCertFirma.add(new Paragraph(representanteLegal()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaCertFirma.add(new Paragraph("DNI N°" + representanteDni()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaCertFirma.add(new Paragraph("“LA VENDEDORA”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));

		tablaCert.addCell(celdaCertFirma);
		tablaCert.addCell(new Cell().setBorder(Border.NO_BORDER));

		document.add(tablaCert);

		document.close();
		return out.toByteArray();
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static void agregarFilaLinderos(Table tabla, String etiqueta, String colindante, String medidaCompleta, PdfFont font) {
		float leadingCompacto = 11f;

		tabla.addCell(new Cell()
				.add(new Paragraph(etiqueta).setFont(font).setFontSize(11)
						.setFixedLeading(leadingCompacto).setMarginBottom(0))
				.setBorder(Border.NO_BORDER).setPadding(0f));

		Paragraph pColindante = new Paragraph(colindante).setFont(font).setFontSize(11)
				.setFixedLeading(leadingCompacto).setMarginBottom(0);
		pColindante.setProperty(com.itextpdf.layout.properties.Property.OVERFLOW_WRAP,
				com.itextpdf.layout.properties.OverflowWrapPropertyValue.ANYWHERE);
		tabla.addCell(new Cell()
				.add(pColindante)
				.setBorder(Border.NO_BORDER).setPadding(0f));

		String numero = medidaCompleta.replace("Con", "").replace("m.l.", "").trim();

		Table subTablaMedida = new Table(UnitValue.createPercentArray(new float[]{25f, 45f, 30f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER);

		subTablaMedida.addCell(new Cell().add(new Paragraph("Con").setFont(font).setFontSize(11).setFixedLeading(leadingCompacto))
				.setBorder(Border.NO_BORDER).setPadding(0));
		subTablaMedida.addCell(new Cell().add(new Paragraph(numero).setFont(font).setFontSize(11).setFixedLeading(leadingCompacto).setTextAlignment(TextAlignment.RIGHT))
				.setBorder(Border.NO_BORDER).setPadding(0));
		subTablaMedida.addCell(new Cell().add(new Paragraph("m.l.").setFont(font).setFontSize(11).setFixedLeading(leadingCompacto))
				.setBorder(Border.NO_BORDER).setPaddingLeft(5f).setPaddingTop(0).setPaddingBottom(0));

		tabla.addCell(new Cell()
				.add(subTablaMedida)
				.setBorder(Border.NO_BORDER).setPadding(0f));
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

	private static String descripcionMedioPago(MedioPago medioPago) {
		if (medioPago == null) return "otro medio de pago bancario";
		return switch (medioPago) {
			case DEPOSITO      -> "depósito bancario";
			case TRANSFERENCIA -> "transferencia bancaria";
			case YAPE          -> "YAPE";
			case PLIN          -> "PLIN";
			case TARJETA       -> "tarjeta";
			default            -> "otro medio de pago bancario";
		};
	}

	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes, PdfFont arialBoldItalic) {
		Table contenedorPrincipal = new Table(1)
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginTop(50f)
				.setKeepTogether(true);

		Table fila1 = new Table(UnitValue.createPercentArray(new float[]{45f, 10f, 45f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER);

		ClienteResponseDTO c1 = clientes.get(0);
		Cell celdaC1 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);

		Paragraph pLineaC1 = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
				.setWidth(200f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

		celdaC1.add(pLineaC1);
		celdaC1.add(new Paragraph(c1.getNombre().toUpperCase() + " " + c1.getApellidos().toUpperCase()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaC1.add(new Paragraph(etiquetaDocumento(c1) + c1.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));

		if (clientes.size() == 1) {
			String etiqueta = (c1.getGenero() != null && c1.getGenero().equals(Genero.Femenino))
					? "“LA COMPRADORA”" : "“EL COMPRADOR”";
			celdaC1.add(new Paragraph(etiqueta).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
		}
		fila1.addCell(celdaC1);
		fila1.addCell(new Cell().setBorder(Border.NO_BORDER));

		Cell celdaV = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
		Paragraph pLineaV = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
				.setWidth(160f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

		celdaV.add(pLineaV);
		celdaV.add(new Paragraph("“LA VENDEDORA”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaV.add(new Paragraph("DNI N°" + representanteDni()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
		fila1.addCell(celdaV);

		contenedorPrincipal.addCell(new Cell().add(fila1).setBorder(Border.NO_BORDER));

		if (clientes.size() > 1) {
			for (int i = 1; i < clientes.size(); i++) {
				ClienteResponseDTO ci = clientes.get(i);

				Table tablaExtra = new Table(new float[]{45f})
						.setWidth(UnitValue.createPercentValue(45))
						.setBorder(Border.NO_BORDER)
						.setMarginTop(50f);

				Cell celdaExtra = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
				Paragraph pLineaExtra = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
						.setWidth(200f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

				celdaExtra.add(pLineaExtra);
				celdaExtra.add(new Paragraph(ci.getNombre().toUpperCase() + " " + ci.getApellidos().toUpperCase()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
				celdaExtra.add(new Paragraph(etiquetaDocumento(ci) + ci.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));

				if (i == clientes.size() - 1) {
					celdaExtra.add(new Paragraph("“LOS COMPRADORES”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
				}

				tablaExtra.addCell(celdaExtra);
				contenedorPrincipal.addCell(new Cell().add(tablaExtra).setBorder(Border.NO_BORDER));
			}
		}

		document.add(contenedorPrincipal);
	}

	private static void verificarEspacioYSalto(Document document, PdfDocument pdf, float porcentajeRequerido) {
		com.itextpdf.layout.renderer.IRenderer renderer = document.getRenderer().getNextRenderer();
		if (renderer instanceof com.itextpdf.layout.renderer.DocumentRenderer) {
			com.itextpdf.layout.layout.LayoutArea area = ((com.itextpdf.layout.renderer.DocumentRenderer) document.getRenderer()).getCurrentArea();

			if (area != null) {
				float altoPagina = pdf.getDefaultPageSize().getHeight();
				float espacioLibre = area.getBBox().getHeight();

				if (espacioLibre < (altoPagina * porcentajeRequerido)) {
					document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
				}
			}
		}
	}
}