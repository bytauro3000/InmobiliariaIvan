package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.enums.TipoPropietario;
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
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Plantilla de CONTRATO AL CONTADO para el PROGRAMA DE VIVIENDA VILLA REAL II
 * (Inmobiliaria Merruic). A diferencia de la financiada, el contado:
 * - No tiene letras de cambio, cuota inicial ni reserva de propiedad.
 * - El precio se cancela a la suscripción (depósito bancario).
 * - No tiene cláusula resolutoria, renuncia ni cláusula penal.
 * Soporta 1 o 2 compradores (titulares) y avales.
 */
public class ContratoVillaRealIIContadoMerruic {

	private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
	private static String ruc() { return EmpresaContext.empresaService.obtenerActiva().getRuc(); }
	private static String representanteLegal() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteLegal(); }
	private static String representanteDni() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteDni(); }
	private static String partidaElectronica() { return EmpresaContext.empresaService.obtenerActiva().getPartidaElectronica(); }
	private static String direccion() {
		var e = EmpresaContext.empresaService.obtenerActiva();
		return (e.getDireccion() != null && !e.getDireccion().isBlank()) ? e.getDireccion().trim() : "";
	}
	private static String distrito() { return EmpresaContext.empresaService.obtenerActiva().getDistrito(); }
	private static String departamento() { return EmpresaContext.empresaService.obtenerActiva().getDepartamento(); }

	public static byte[] generarContratoVillaRealIIContado(ContratoResponseDTO contrato) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(99, 73, 57, 99);

		PdfFont arialNarrow;
		PdfFont arialNarrowBold;

		try {
			byte[] nBytes = StreamUtil.inputStreamToArray(
					ContratoVillaRealIIContadoMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALN.TTF"));
			arialNarrow = PdfFontFactory.createFont(nBytes, PdfEncodings.WINANSI);

			byte[] nbBytes = StreamUtil.inputStreamToArray(
					ContratoVillaRealIIContadoMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALNB.TTF"));
			arialNarrowBold = PdfFontFactory.createFont(nbBytes, PdfEncodings.WINANSI);
		} catch (Exception e) {
			throw new RuntimeException("Error cargando las fuentes Arial Narrow desde resources/fonts/", e);
		}

		// ── FECHA DEL CONTRATO ──────────────────────────────────────────────
		LocalDate fechaRegistro = contrato.getFechaContrato();
		if (fechaRegistro == null) {
			fechaRegistro = LocalDate.now();
		}
		String diaNum = String.format("%02d", fechaRegistro.getDayOfMonth());
		String mesNum = String.format("%02d", fechaRegistro.getMonthValue());
		int anioNum = fechaRegistro.getYear();

		// ── PROCESAMIENTO DINÁMICO DE CLIENTES ─────────────────────────────
		List<ClienteResponseDTO> clientes = contrato.getClientes();
		List<ClienteResponseDTO> titulares = clientes.stream()
				.filter(c -> c.getTipoPropietario() == null
						|| c.getTipoPropietario() != TipoPropietario.AVAL)
				.collect(Collectors.toList());
		List<ClienteResponseDTO> avales = clientes.stream()
				.filter(c -> c.getTipoPropietario() != null
						&& c.getTipoPropietario() == TipoPropietario.AVAL)
				.collect(Collectors.toList());

		if (titulares.isEmpty() && !clientes.isEmpty()) {
			titulares = new ArrayList<>(clientes);
			avales = new ArrayList<>();
		}

		int numClientes = titulares.size();
		ClienteResponseDTO titular = titulares.get(0);

		String nombreDistrito = (titular.getDistrito() != null) ? titular.getDistrito().getNombre() : "";
		String domicilioCalle = (titular.getDireccion() != null) ? titular.getDireccion().toUpperCase() : "";
		String domicilioComprador = domicilioCalle + ", Distrito de " + nombreDistrito;

		// Bloque de compradores
		Paragraph bloqueCompradores = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12);
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = titulares.get(i);
			boolean esFemenino = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));
			String prefijo = esFemenino ? "la Sra. " : "el Sr. ";
			String identif = esFemenino ? "identificada" : "identificado";

			bloqueCompradores.add(prefijo);
			bloqueCompradores.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setFont(arialNarrowBold));
			bloqueCompradores.add(" " + identif + " con ");
			bloqueCompradores.add(new Text(etiquetaDocumento(c) + c.getNumDoc()).setFont(arialNarrowBold));

			if (numClientes > 1 && i < numClientes - 1) {
				bloqueCompradores.add(i == numClientes - 2 ? " y " : ", ");
			}
		}

		// Bloque de AVALES (garantes), si los hay
		Paragraph bloqueAvales = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12);
		if (!avales.isEmpty()) {
			bloqueAvales.add("; actuando como AVAL ");
			for (int i = 0; i < avales.size(); i++) {
				ClienteResponseDTO c = avales.get(i);
				boolean esFemenino = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));
				String prefijo = esFemenino ? "la Sra. " : "el Sr. ";
				String identif = esFemenino ? "identificada" : "identificado";
				bloqueAvales.add(prefijo);
				bloqueAvales.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setFont(arialNarrowBold));
				bloqueAvales.add(" " + identif + " con ");
				bloqueAvales.add(new Text(etiquetaDocumento(c) + c.getNumDoc()).setFont(arialNarrowBold));
				if (i < avales.size() - 1) {
					bloqueAvales.add(i == avales.size() - 2 ? " y " : ", ");
				}
			}
		}

		// Etiqueta del comprador según género y cantidad
		boolean unicaFemenino = (numClientes == 1)
				&& titular.getGenero() != null && titular.getGenero().equals(Genero.Femenino);
		String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES"
				: (unicaFemenino ? "LA COMPRADORA" : "EL COMPRADOR");

		// Verbos de concordancia singular/plural
		String verboSeObliga = (numClientes > 1) ? "se obligan" : "se obliga";
		String verboDeclara = (numClientes > 1) ? "declaran" : "declara";
		String verboCubrira = (numClientes > 1) ? "cubrirán" : "cubrirá";
		String verboComunicara = (numClientes > 1) ? "comunicarán" : "comunicará";
		String verboHara = (numClientes > 1) ? "harán" : "hará";

		// ── DATOS DEL LOTE ─────────────────────────────────────────────────
		LoteResponseDTO lote = contrato.getLotes().get(0);

		// ── ENCABEZADO ─────────────────────────────────────────────────────
		document.add(new Paragraph("PROGRAMA DE VIVIENDA")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(0));

		document.add(new Paragraph("VILLA REAL II")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(0));

		document.add(new Paragraph("CONTRATO PRIVADO DE COMPRAVENTA DE TERRENO RÚSTICO EN FUTURA HABILITACION URBANA")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(15));

		// ── INTRODUCCIÓN ──────────────────────────────────────────────────
		Paragraph intro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		intro.add("Conste por este documento, que celebran de una parte la empresa ");
		intro.add(new Text("\u201c" + empresa() + "\u201d ").setFont(arialNarrowBold));
		intro.add("con ");
		intro.add(new Text("RUC Nº " + ruc()).setFont(arialNarrowBold));
		intro.add(", inscrita en la partida electrónica N° ");
		intro.add(new Text(partidaElectronica()).setFont(arialNarrowBold));
		intro.add(" del registro de personas jurídicas de Lima – Sunarp, representado por su titular gerente don ");
		intro.add(new Text(representanteLegal()).setFont(arialNarrowBold));
		intro.add(", identificado con ");
		intro.add(new Text("DNI N° " + representanteDni()).setFont(arialNarrowBold));
		intro.add(", con domicilio en ");
		intro.add(new Text(direccion() + ", Distrito de " + distrito() + ", Provincia y Departamento de " + departamento()).setFont(arialNarrowBold));
		intro.add(", a quien en adelante se le denominará ");
		intro.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		intro.add(" y de la otra parte ");

		for (com.itextpdf.layout.element.IElement el : bloqueCompradores.getChildren()) {
			intro.add((com.itextpdf.layout.element.ILeafElement) el);
		}

		if (!avales.isEmpty()) {
			for (com.itextpdf.layout.element.IElement el : bloqueAvales.getChildren()) {
				intro.add((com.itextpdf.layout.element.ILeafElement) el);
			}
		}

		intro.add(" con domicilio en " + domicilioComprador);
		intro.add(", a quienes en lo sucesivo se les llamará ");
		intro.add(new Text(etiquetaComprador).setFont(arialNarrowBold).setUnderline());
		intro.add(", bajo los términos y condiciones siguientes:");

		document.add(intro);

		// ── PRIMERA: ANTECEDENTES ──────────────────────────────────────────
		agregarEncabezadoClausula(document, arialNarrowBold, "ANTECEDENTES:");

		Paragraph primeraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		primeraCuerpo.add(new Text("PRIMERO: ").setFont(arialNarrowBold).setUnderline());
		primeraCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		primeraCuerpo.add(" es copropietario del 80.12% de acciones y derechos del inmueble denominado PARCELA 5-B DE LA ZONA A DEL FUNDO SANTA INÉS, EX ESTABLO MARANGA, U.C. N° 02974 (PARTE) equivalente a un área física de 5,164.00m², situado a la altura del kilómetro 3 de la Av. José Saco Rojas, Distrito de Carabayllo, Provincia y Departamento de Lima. Cuyo predio fue adquirido mediante contrato privado de compraventa de terreno rustico de fecha 19/11/2019 celebrada con sus anteriores propietarios ");
		primeraCuerpo.add(new Text("OLGA CONSUELO VIVAS MENENDEZ").setFont(arialNarrowBold));
		primeraCuerpo.add(", identificada con DNI N° 06926190 y ");
		primeraCuerpo.add(new Text("LUÍS EMILIO VIVAS MENENDEZ").setFont(arialNarrowBold));
		primeraCuerpo.add(", identificado con DNI N° 41633683 (sucesión intestada de quienes en vida fueron ");
		primeraCuerpo.add(new Text("EMILIO VIVAS REYNOSO").setFont(arialNarrowBold));
		primeraCuerpo.add(" y ");
		primeraCuerpo.add(new Text("DOLORES LEONISA MENENDEZ LOPEZ VIUDA DE VIVAS").setFont(arialNarrowBold));
		primeraCuerpo.add("), ");
		primeraCuerpo.add(new Text("OSCAR GERARDO WILLIAMS MENENDEZ").setFont(arialNarrowBold));
		primeraCuerpo.add(", identificado con DNI N° 09671279, ");
		primeraCuerpo.add(new Text("ANA MARIA WILLIAMS MENENDEZ DE VILLAJUAN").setFont(arialNarrowBold));
		primeraCuerpo.add(", identificada con DNI N° 07993343, (sucesión intestada de quien en vida fue ");
		primeraCuerpo.add(new Text("DOLORES LEONISA MENENDEZ LOPEZ VIUDA DE VIVAS").setFont(arialNarrowBold));
		primeraCuerpo.add(").");

		document.add(primeraCuerpo);

		Paragraph segundaParteCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		segundaParteCuerpo.add("Sobre el referido terreno ");
		segundaParteCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		segundaParteCuerpo.add(" viene desarrollando el ");
		segundaParteCuerpo.add(new Text("PROGRAMA DE VIVIENDA VILLA REAL II").setFont(arialNarrowBold));
		segundaParteCuerpo.add(" la misma que se distribuye en lotes y manzanas con sus respectivas áreas conforme al plano de lotización.");
		document.add(segundaParteCuerpo);

		// ── SEGUNDA: OBJETO DEL CONTRATO ───────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.25f);
		Div divSegunda = new Div().setKeepTogether(true);

		agregarEncabezadoClausula(document, arialNarrowBold, "OBJETO DEL CONTRATO:");

		Paragraph segundaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		segundaIntro.add(new Text("SEGUNDA: ").setFont(arialNarrowBold));
		segundaIntro.add("Por medio del presente documento ");
		segundaIntro.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		segundaIntro.add(" da en venta real y enajenación perpetua a favor de ");
		segundaIntro.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		segundaIntro.add(" el lote de terreno rústico ubicado en el PROGRAMA DE VIVIENDA VILLA REAL II, Manzana ");
		segundaIntro.add(new Text("\u201c" + lote.getManzana() + "\u201d").setFont(arialNarrowBold));
		segundaIntro.add(", Lote ");
		segundaIntro.add(new Text("\u201c" + lote.getNumeroLote() + "\u201d").setFont(arialNarrowBold));
		segundaIntro.add(", con un área de ");
		segundaIntro.add(new Text(lote.getArea() + " m2").setFont(arialNarrowBold));
		segundaIntro.add(", situado en el Distrito de Carabayllo, Provincia y Departamento de Lima, encerrado dentro de los siguientes linderos y medidas perimétricas:");

		divSegunda.add(segundaIntro);

		Table tablaLinderos = new Table(UnitValue.createPercentArray(new float[]{30f, 45f, 25f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginLeft(20);

		agregarFilaLinderos(tablaLinderos, "Por el frente", lote.getColindanteNorte(), "Con    " + lote.getAncho1() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por la derecha", lote.getColindanteEste(), "Con  " + lote.getLargo1() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por la Izquierda", lote.getColindanteOeste(), "Con    " + lote.getLargo2() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por el fondo", lote.getColindanteSur(), "Con    " + lote.getAncho2() + "  m.l.", arialNarrow);

		divSegunda.add(tablaLinderos);

		Paragraph segundaFinal = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
				.setMarginTop(10);

		segundaFinal.add("La transferencia del lote de terreno rústico antes citado se realiza ");
		segundaFinal.add(new Text("\u201cad-corpus\u201d").setFont(arialNarrowBold));
		segundaFinal.add(", comprendida además del área señalada del terreno rústico, sus entradas, salidas, aires, usos, costumbres, servidumbres y todo cuanto de hecho y por derecho correspondan al referido lote de terreno.");

		divSegunda.add(segundaFinal);

		document.add(divSegunda);

		// ── TERCERA: PRECIO Y FORMA DE PAGO (CONTADO) ──────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "PRECIO Y FORMA DE PAGO:");

		DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
		Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
		String prefMoneda = (monedaContrato == Moneda.PEN) ? "S/." : "US$";

		String montoTotalLetras = NumeroALetras.convertir(contrato.getMontoTotal(), monedaContrato);

		Paragraph terceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		terceraCuerpo.add(new Text("TERCERO: ").setFont(arialNarrowBold));
		terceraCuerpo.add("Las partes de mutuo acuerdo establecen que, el precio del terreno rústico materia de venta asciende a la suma de ");
		terceraCuerpo.add(new Text(prefMoneda + " " + df.format(contrato.getMontoTotal())).setFont(arialNarrowBold));
		terceraCuerpo.add(new Text(" (" + montoTotalLetras + ")").setFont(arialNarrowBold));
		terceraCuerpo.add(", importe que se paga mediante depósito bancario a la cuenta ");
		terceraCuerpo.add(new Text("191-1041663-1-57").setFont(arialNarrowBold));
		terceraCuerpo.add(" ");
		terceraCuerpo.add(new Text("BANCO DE CRÉDITO DEL PERÚ").setFont(arialNarrowBold));
		terceraCuerpo.add(" perteneciente a la empresa.");
		document.add(terceraCuerpo);

		// ── CUARTA: EQUIVALENCIA ───────────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "EQUIVALENCIA:");

		Paragraph cuartaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuartaCuerpo.add(new Text("CUARTO: ").setFont(arialNarrowBold));
		cuartaCuerpo.add("Los contratantes declaran que entre el lote vendido y el precio pactado existe justa equivalencia y que si hubiera alguna diferencia del área del lote de terreno vendido se pagará el reintegro o devolución al precio actualizado. Asimismo, las partes acuerdan que, el precio pactado en este contrato es solamente por la venta del terreno rústico en el que no están incluidos los trámites de aprobación de proyectos, desarrollo urbano; obras de habilitación urbana, los impuestos de ley y afines.");
		document.add(cuartaCuerpo);

		// ── QUINTA: GRAVAMEN ───────────────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "GRAVAMEN:");

		Paragraph quintaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		quintaCuerpo.add(new Text("QUINTO: ").setFont(arialNarrowBold));
		quintaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		quintaCuerpo.add(" declara que, sobre el bien que enajena no pesa ningún gravamen, hipoteca, medida judicial o extrajudicial, y en general, ningún acto o contrato que prive, limite o restrinja el derecho de propiedad, posesión o uso del bien, obligándose no obstante a la evicción o saneamiento de Ley.");
		document.add(quintaCuerpo);

		// ── SEXTA: ESCRITURA PÚBLICA (CONTADO) ─────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ESCRITURA PÚBLICA:");

		Paragraph sextaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		sextaCuerpo.add(new Text("SEXTO: ").setFont(arialNarrowBold));
		sextaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		sextaCuerpo.add(" se obliga a perfeccionar la transferencia del lote de terreno vendido a favor de ");
		sextaCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		sextaCuerpo.add(" cuando el predio de mayor extensión haya sido inscrito en los registros públicos y que el lote de terreno materia de venta sea independizado.");
		document.add(sextaCuerpo);

		// ── SÉPTIMA: HABILITACIÓN URBANA ───────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "HABILITACIÓN URBANA:");

		Paragraph septimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		septimaCuerpo.add(new Text("SÉTIMO: ").setFont(arialNarrowBold));
		septimaCuerpo.add("Para la tramitación del cambio de uso, elaboración del proyecto de Habilitación Urbana e Independización del lote de terreno materia de venta, las partes acuerdan conceder a ");
		septimaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		septimaCuerpo.add(" la representación exclusiva en la elaboración de los mismos. Luego de cancelar el precio íntegro del lote de terreno, ");
		septimaCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		septimaCuerpo.add(" " + verboSeObliga + " a cubrir en la proporción que le corresponde los gastos de habilitación urbana e independización de lotes que conforman el programa, para este fin ");
		septimaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		septimaCuerpo.add(" " + verboComunicara + " por carta simple y/o notarial el costo individual a cobrar en cada caso lo que será materia de un contrato anexo al presente documento.");
		document.add(septimaCuerpo);

		verificarEspacioYSalto(document, pdf, 0.15f);
		Paragraph octavaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		octavaCuerpo.add(new Text("OCTAVO: ").setFont(arialNarrowBold));
		octavaCuerpo.add("Se acuerda que, los trámites del proyecto de habilitación urbana y ejecución de la misma correrá a cuenta de los clientes que conforman el programa de vivienda, obligándose ");
		octavaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		octavaCuerpo.add(" prestar el apoyo necesario en la emisión de documentos que resulten necesarios para cumplir dicho fin, siendo que, la independización del lote de terreno materia de venta será a cuenta de ");
		octavaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		octavaCuerpo.add(".");
		document.add(octavaCuerpo);

		// ── NOVENA: GASTOS Y TRIBUTOS ──────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "GASTOS Y TRIBUTOS:");

		Paragraph novenaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		novenaCuerpo.add(new Text("NOVENO: ").setFont(arialNarrowBold));
		novenaCuerpo.add("Las partes acuerdan que, todos los gastos y tributos que originen la celebración, formalización y ejecución del presente contrato serán asumidos íntegramente por ");
		novenaCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		novenaCuerpo.add(".");
		document.add(novenaCuerpo);

		// ── DÉCIMA: ENTREGA DEL TERRENO ────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ENTREGA DEL TERRENO:");

		Paragraph decimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		decimaCuerpo.add(new Text("DÉCIMO: ").setFont(arialNarrowBold));
		decimaCuerpo.add(new Text("EL VENDEDOR").setFont(arialNarrowBold));
		decimaCuerpo.add(" hace entrega física del lote de terreno vendido a favor de ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		decimaCuerpo.add(" en ese acto, con la sola suscripción de este contrato, en efecto, queda expresamente convenido que, ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		decimaCuerpo.add(" al entrar en posesión del terreno materia de venta, se hará responsable absoluto a partir de la fecha el pago de impuestos ante la municipalidad distrital correspondiente, haciéndose cargo también de la defensa de su posesión frente a terceros por los medios permitidos por ley, respetando estrictamente sus linderos, medidas perimétricas y los derechos de posesión y de propiedad de sus vecinos, en caso de realizar construcción alguna, obligándose efectuar dicha construcción dentro del marco del reglamento nacional de construcciones y afines.");
		document.add(decimaCuerpo);

		// ── DÉCIMO PRIMERA: DOMICILIO ──────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "DOMICILIO:");

		Paragraph decimoPrimeraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		decimoPrimeraCuerpo.add(new Text("DÉCIMO PRIMERO: ").setFont(arialNarrowBold));
		decimoPrimeraCuerpo.add("Para la validez de todas las comunicaciones y notificaciones a las partes, para las acciones legales que deriven del presente contrato, ambas partes señalan como sus respectivos domicilios los indicados en la introducción de este documento. El cambio de domicilio de cualquiera de las partes surtirá efecto después de cinco días útiles de efectuada la comunicación mediante carta notarial.");
		document.add(decimoPrimeraCuerpo);

		// ── DÉCIMO SEGUNDA: COMPETENCIA TERRITORIAL ────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "COMPETENCIA TERRITORIAL:");

		Paragraph decimoSegundaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		decimoSegundaCuerpo.add(new Text("DÉCIMO SEGUNDO: ").setFont(arialNarrowBold));
		decimoSegundaCuerpo.add("Las partes dejan expresa constancia que, para todas las acciones legales que deriven del presente contrato se someten exclusivamente a los Jueces y Salas de la Corte Superior de Justicia de Lima Norte, renunciando así al fuero de sus respectivos domicilios.");
		document.add(decimoSegundaCuerpo);

		// ── DÉCIMO TERCERA: APLICACIÓN SUPLETORIA DE LA LEY ────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "APLICACIÓN SUPLETORIA DE LA LEY:");

		Paragraph decimoTerceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		decimoTerceraCuerpo.add(new Text("DÉCIMO TERCERO: ").setFont(arialNarrowBold));
		decimoTerceraCuerpo.add("En todo lo no previsto por las partes en el presente contrato, ambas se someten a lo establecido por las normas del código civil y demás del sistema jurídico que resulten aplicables.");
		document.add(decimoTerceraCuerpo);

		// ── CIERRE Y FIRMAS ────────────────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);

		document.add(new Paragraph()
				.add(new Text("Carabayllo, " + diaNum + "/" + mesNum + "/" + anioNum + ".").setFont(arialNarrow))
				.setFontSize(12)
				.setTextAlignment(TextAlignment.RIGHT)
				.setMarginTop(5));

		agregarBloqueFirmas(document, titulares, avales, arialNarrowBold);

		document.close();
		return out.toByteArray();
	}

	/* ========================================================================
	 * MÉTODOS AUXILIARES
	 * ======================================================================== */

	private static void agregarFilaLinderos(Table tabla, String etiqueta, String colindante, String medidaCompleta, PdfFont font) {
		float leadingCompacto = 11f;

		tabla.addCell(new Cell()
				.add(new Paragraph(etiqueta).setFont(font).setFontSize(12)
						.setFixedLeading(leadingCompacto).setMarginBottom(0))
				.setBorder(Border.NO_BORDER).setPadding(0f));

		tabla.addCell(new Cell()
				.add(new Paragraph(colindante).setFont(font).setFontSize(12)
						.setFixedLeading(leadingCompacto).setMarginBottom(0))
				.setBorder(Border.NO_BORDER).setPadding(0f));

		String numero = medidaCompleta.replace("Con", "").replace("m.l.", "").trim();

		Table subTablaMedida = new Table(UnitValue.createPercentArray(new float[]{25f, 45f, 30f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER);

		subTablaMedida.addCell(new Cell().add(new Paragraph("Con").setFont(font).setFontSize(12).setFixedLeading(leadingCompacto))
				.setBorder(Border.NO_BORDER).setPadding(0));
		subTablaMedida.addCell(new Cell().add(new Paragraph(numero).setFont(font).setFontSize(12).setFixedLeading(leadingCompacto).setTextAlignment(TextAlignment.RIGHT))
				.setBorder(Border.NO_BORDER).setPadding(0));
		subTablaMedida.addCell(new Cell().add(new Paragraph("m.l.").setFont(font).setFontSize(12).setFixedLeading(leadingCompacto))
				.setBorder(Border.NO_BORDER).setPaddingLeft(5f).setPaddingTop(0).setPaddingBottom(0));

		tabla.addCell(new Cell()
				.add(subTablaMedida)
				.setBorder(Border.NO_BORDER).setPadding(0f));
	}

	private static String etiquetaDocumento(ClienteResponseDTO c) {
		if (c.getTipoCliente() == TipoCliente.CE) return "C.E. N°";
		return "DNI N°";
	}

	private static float calcularAnchoLineaFirma(String nombreCompleto) {
		float anchoColumna = 267.75f;
		int len = nombreCompleto.length();
		float proporcion;
		if (len <= 15) {
			proporcion = 0.50f;
		} else if (len <= 20) {
			proporcion = 0.60f;
		} else if (len <= 25) {
			proporcion = 0.70f;
		} else if (len <= 30) {
			proporcion = 0.80f;
		} else if (len <= 35) {
			proporcion = 0.90f;
		} else {
			proporcion = 1.00f;
		}
		return anchoColumna * proporcion;
	}

	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> titulares,
                                            List<ClienteResponseDTO> avales, PdfFont arialNarrowBold) {
		Table contenedorPrincipal = new Table(1)
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginTop(50f)
				.setKeepTogether(true);

		// Agrupar titulares de 2 en 2 para mostrar en la misma fila
		for (int i = 0; i < titulares.size(); i += 2) {
			ClienteResponseDTO c1 = titulares.get(i);
			ClienteResponseDTO c2 = (i + 1 < titulares.size()) ? titulares.get(i + 1) : null;

			Table fila = new Table(UnitValue.createPercentArray(new float[]{45f, 10f, 45f}))
					.useAllAvailableWidth()
					.setBorder(Border.NO_BORDER);

			// Columna izquierda: primer titular
			Cell celdaC1 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
			String nombreFirmaC1 = c1.getNombre().toUpperCase() + " " + c1.getApellidos().toUpperCase();
			float anchoLineaC1 = calcularAnchoLineaFirma(nombreFirmaC1);

			Paragraph pLineaC1 = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
					.setWidth(anchoLineaC1).setMarginBottom(2)
					.setHorizontalAlignment(HorizontalAlignment.CENTER);

			celdaC1.add(pLineaC1);
			celdaC1.add(new Paragraph(nombreFirmaC1).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
			celdaC1.add(new Paragraph(etiquetaDocumento(c1) + c1.getNumDoc()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
			fila.addCell(celdaC1);

			// Columna centro: espacio
			fila.addCell(new Cell().setBorder(Border.NO_BORDER));

			// Columna derecha: segundo titular (si existe)
			if (c2 != null) {
				Cell celdaC2 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
				String nombreFirmaC2 = c2.getNombre().toUpperCase() + " " + c2.getApellidos().toUpperCase();
				float anchoLineaC2 = calcularAnchoLineaFirma(nombreFirmaC2);

				Paragraph pLineaC2 = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
						.setWidth(anchoLineaC2).setMarginBottom(2)
						.setHorizontalAlignment(HorizontalAlignment.CENTER);

				celdaC2.add(pLineaC2);
				celdaC2.add(new Paragraph(nombreFirmaC2).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
				celdaC2.add(new Paragraph(etiquetaDocumento(c2) + c2.getNumDoc()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
				fila.addCell(celdaC2);
			} else {
				fila.addCell(new Cell().setBorder(Border.NO_BORDER));
			}

			contenedorPrincipal.addCell(new Cell().add(fila).setBorder(Border.NO_BORDER));
		}

		// --- AVALES (Garantes) — firman como "LA AVAL" ---
		for (ClienteResponseDTO aval : avales) {
			Table tablaAval = new Table(new float[]{45f})
					.setWidth(UnitValue.createPercentValue(45))
					.setBorder(Border.NO_BORDER)
					.setMarginTop(50f);

			Cell celdaAval = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
			String nombreFirmaAval = aval.getNombre().toUpperCase() + " " + aval.getApellidos().toUpperCase();
			float anchoLineaAval = calcularAnchoLineaFirma(nombreFirmaAval);
			Paragraph pLineaAval = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
					.setWidth(anchoLineaAval).setMarginBottom(2)
					.setHorizontalAlignment(HorizontalAlignment.CENTER);

			celdaAval.add(pLineaAval);
			celdaAval.add(new Paragraph(nombreFirmaAval).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
			celdaAval.add(new Paragraph(etiquetaDocumento(aval) + aval.getNumDoc()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
			celdaAval.add(new Paragraph("\u201cLA AVAL\u201d").setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f));

			tablaAval.addCell(celdaAval);
			contenedorPrincipal.addCell(new Cell().add(tablaAval).setBorder(Border.NO_BORDER));
		}

		document.add(contenedorPrincipal);
	}

	private static void verificarEspacioYSalto(Document document, PdfDocument pdf, float porcentajeRequerido) {
		PdfDocument pdfDoc = pdf != null ? pdf : document.getPdfDocument();
		if (pdfDoc == null) return;
		com.itextpdf.layout.renderer.IRenderer renderer = document.getRenderer().getNextRenderer();
		if (renderer instanceof com.itextpdf.layout.renderer.DocumentRenderer) {
			com.itextpdf.layout.layout.LayoutArea area = ((com.itextpdf.layout.renderer.DocumentRenderer) document.getRenderer()).getCurrentArea();
			if (area != null) {
				float altoPagina = pdfDoc.getDefaultPageSize().getHeight();
				float espacioLibre = area.getBBox().getHeight();
				if (espacioLibre < (altoPagina * porcentajeRequerido)) {
					document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
				}
			}
		}
	}

	private static void agregarEncabezadoClausula(Document document, PdfFont arialNarrowBold, String titulo) {
		document.add(new Paragraph().add(new Text(titulo).setFont(arialNarrowBold).setUnderline())
				.setFontSize(12).setFixedLeading(12).setMarginTop(12).setMarginBottom(0));
	}
}
