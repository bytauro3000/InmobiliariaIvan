package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.config.EmpresaContext;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LetraResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.enums.TipoContrato;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plantilla de contrato para el PROGRAMA NAPOLE (Inmobiliaria Merruic).
 * Genera el PDF del contrato de compra-venta de terreno rústico en futura
 * habilitación urbana, tanto en su modalidad CONTADO como FINANCIADO.
 *
 * Los datos de la empresa (vendedor) se toman dinámicamente de la tabla
 * "empresa" vía EmpresaContext, por lo que la misma plantilla sirve para
 * cualquier empresa activa (Merruic en este caso).
 */
public class ContratoNapolePdfMerruic {

	private static String empresa() { return EmpresaContext.empresaService.obtenerActiva().getNombreLegal(); }
	private static String ruc() { return EmpresaContext.empresaService.obtenerActiva().getRuc(); }
	private static String representanteLegal() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteLegal(); }
	private static String representanteDni() { return EmpresaContext.empresaService.obtenerActiva().getRepresentanteDni(); }
	private static String partidaElectronica() { return EmpresaContext.empresaService.obtenerActiva().getPartidaElectronica(); }
	private static String direccion() { return EmpresaContext.empresaService.obtenerActiva().getDireccion(); }
	private static String distrito() { return EmpresaContext.empresaService.obtenerActiva().getDistrito(); }
	private static String departamento() { return EmpresaContext.empresaService.obtenerActiva().getDepartamento(); }

	public static byte[] generarContratoNapole(ContratoResponseDTO contrato, LetraCambio primeraLetraEntidad) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(122, 85, 57, 85);

		PdfFont arialNarrow;      // Arial Narrow regular (texto, SIN cursiva)
		PdfFont arialNarrowBold;  // Arial Narrow negrita (énfasis, SIN cursiva)

		try {
			byte[] nBytes = StreamUtil.inputStreamToArray(
					ContratoNapolePdfMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALN.TTF"));
			arialNarrow = PdfFontFactory.createFont(nBytes, PdfEncodings.WINANSI);

			byte[] nbBytes = StreamUtil.inputStreamToArray(
					ContratoNapolePdfMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALNB.TTF"));
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
		int numClientes = clientes.size();
		ClienteResponseDTO titular = clientes.get(0);

		String nombreDistrito = (titular.getDistrito() != null) ? titular.getDistrito().getNombre() : "";
		String domicilioCalle = (titular.getDireccion() != null) ? titular.getDireccion().toUpperCase() : "";
		String domicilioComprador = domicilioCalle + ", Distrito de " + nombreDistrito;

		// Bloque de compradores
		Paragraph bloqueCompradores = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(12);
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
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

		// Etiqueta del comprador según género y cantidad
		boolean unicaFemenino = (numClientes == 1)
				&& titular.getGenero() != null && titular.getGenero().equals(Genero.Femenino);
		String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES"
				: (unicaFemenino ? "LA COMPRADORA" : "EL COMPRADOR");

		// Verbos de concordancia singular/plural
		String verboSeObliga = (numClientes > 1) ? "se obligan" : "se obliga";
		String verboGira     = (numClientes > 1) ? "giran" : "gira";
		String verboDeclara  = (numClientes > 1) ? "declaran" : "declara";
		String verboHara     = (numClientes > 1) ? "harán" : "hará";
		String verboCubrira  = (numClientes > 1) ? "cubrirán" : "cubrirá";
		String verboComunicara = (numClientes > 1) ? "comunicarán" : "comunicará";

		// ── DATOS DEL LOTE ─────────────────────────────────────────────────
		LoteResponseDTO lote = contrato.getLotes().get(0);

		// ── ENCABEZADO ─────────────────────────────────────────────────────
		// Tres líneas centradas, todas subrayadas y con el mismo espaciado
		// compacto (igual al de los encabezados de cláusula).
		document.add(new Paragraph("PROGRAMA NAPOLE")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(0));

		document.add(new Paragraph("CONTRATO PRIVADO DE COMPRA Y VENTA DE TERRENO RÚSTICO EN FUTURA")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(0));

		document.add(new Paragraph("HABILITACIÓN URBANA")
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
		intro.add(" del registro de personas jurídicas de Lima – Sunarp, con domicilio en ");
		intro.add(new Text(direccion() + ", Distrito de " + distrito() + ", Provincia y Departamento de " + departamento()).setFont(arialNarrowBold));
		intro.add(", representado por su titular gerente don ");
		intro.add(new Text(representanteLegal()).setFont(arialNarrowBold));
		intro.add(", identificado con ");
		intro.add(new Text("DNI N° " + representanteDni()).setFont(arialNarrowBold));
		intro.add(", a quien en adelante se le denominará ");
		intro.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		intro.add(" y de la otra parte ");

		for (com.itextpdf.layout.element.IElement el : bloqueCompradores.getChildren()) {
			intro.add((com.itextpdf.layout.element.ILeafElement) el);
		}

		intro.add(" con domicilio en " + domicilioComprador);
		intro.add(", a quien en lo sucesivo se le llamará ");
		intro.add(new Text(etiquetaComprador).setFont(arialNarrowBold).setUnderline());
		intro.add(", bajo los términos y condiciones siguientes:");

		document.add(intro);

		// ── PRIMERA: ANTECEDENTES ──────────────────────────────────────────
		agregarEncabezadoClausula(document, arialNarrowBold, "ANTECEDENTES:");

		Paragraph primeraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		primeraCuerpo.add(new Text("PRIMERA: ").setFont(arialNarrowBold));
		primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		primeraCuerpo.add(" ostenta a su cargo la posesión física, continua, pública y pacífica de un terreno de 17,999.94 m2, PARCELA AGRICOLA N° 10 U.C. 11444, ZONA PARCELACION GALLINAZO PUENTE PIEDRA equivalente a un área física de 21,623.49 m2, cuya inscripción registral consta en la partida electrónica N°");
		primeraCuerpo.add(new Text("49014934").setFont(arialNarrowBold));
		primeraCuerpo.add(" del registro de propiedad inmuebles de Lima-SUNARP. CON CODIGO DE contribuyente Nº");
		primeraCuerpo.add(new Text("175516").setFont(arialNarrowBold));
		primeraCuerpo.add(", Distrito de Puente Piedra, Provincia y Departamento de Lima. ");
		primeraCuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		primeraCuerpo.add(" con fecha 24/02/2023 celebraron el CONTRATO PRIVADO DE COMPRA Y VENTA DE TERRENO RÚSTICO a fin que la empresa en mención realice la lotización del 50.42% de acciones y derechos que equivale a un área de 17,999.94m2 de terreno, la misma que encuentra dentro del área de mayor extensión a que hace referencia esta cláusula, en ese sentido, la empresa, sobre el referido terreno viene desarrollando el PROGRAMA DE VIVIENDA ");
		primeraCuerpo.add(new Text("\u201cNAPOLE\u201d").setFont(arialNarrowBold));
		primeraCuerpo.add(" la misma que se distribuye en lotes y manzanas con sus respectivas áreas conforme al plano de lotización.");

		document.add(primeraCuerpo);

		// ── SEGUNDA: OBJETO DEL CONTRATO ───────────────────────────────────
		// Se agrupa en un Div con keepTogether para que la cláusula completa
		// (encabezado + texto + linderos) no se parta entre páginas.
		verificarEspacioYSalto(document, pdf, 0.25f);
		Div divSegunda = new Div().setKeepTogether(true);

		agregarEncabezadoClausula(document, arialNarrowBold, "OBJETO DEL CONTRATO:");

		Paragraph segundaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		segundaIntro.add(new Text("SEGUNDA: ").setFont(arialNarrowBold));
		segundaIntro.add("Por medio del presente documento ");
		segundaIntro.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		segundaIntro.add(" da en venta real y enajenación perpetua a favor de ");
		segundaIntro.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		segundaIntro.add(" el lote de terreno rústico ubicado en el PROGRAMA DE VIVIENDA NAPOLE, Manzana ");
		segundaIntro.add(new Text("\u201c" + lote.getManzana() + "\u201d").setFont(arialNarrowBold));
		segundaIntro.add(", Lote ");
		segundaIntro.add(new Text("\u201c" + lote.getNumeroLote() + "\u201d").setFont(arialNarrowBold));
		segundaIntro.add(", con un área de ");
		segundaIntro.add(new Text(lote.getArea() + " m2").setFont(arialNarrowBold));
		segundaIntro.add(", situado en el Distrito de Puente Piedra, Provincia y Departamento de Lima, encerrado dentro de los siguientes linderos y medidas perimétricas:");

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

		// ── TERCERA: PRECIO Y FORMA DE PAGO ────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "PRECIO Y FORMA DE PAGO:");

		DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
		Moneda monedaContrato = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
		String prefMoneda = (monedaContrato == Moneda.PEN) ? "S/." : "US$";
		String prefMonedaSinPunto = (monedaContrato == Moneda.PEN) ? "S/" : "US$";

		String montoTotalLetras = NumeroALetras.convertir(contrato.getMontoTotal(), monedaContrato);

		Paragraph terceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		terceraCuerpo.add(new Text("TERCERO: ").setFont(arialNarrowBold));
		terceraCuerpo.add("Las partes de mutuo acuerdo establecen que, el precio del terreno rústico materia de venta asciende a la suma de ");
		terceraCuerpo.add(new Text(prefMoneda + " " + df.format(contrato.getMontoTotal())).setFont(arialNarrowBold));
		terceraCuerpo.add(new Text(" (" + montoTotalLetras + ")").setFont(arialNarrowBold));

		boolean esFinanciado = contrato.getTipoContrato() == TipoContrato.FINANCIADO
				|| (contrato.getLetras() != null && !contrato.getLetras().isEmpty());

		if (!esFinanciado) {
			// ── CONTADO: pago mediante depósito bancario ──
			terceraCuerpo.add(" importe que se paga mediante depósito bancario a la cuenta ");
			terceraCuerpo.add(new Text("191-1041663-1-57").setFont(arialNarrowBold));
			terceraCuerpo.add(" ");
			terceraCuerpo.add(new Text("BANCO DE CRÉDITO DEL PERÚ").setFont(arialNarrowBold));
			terceraCuerpo.add(" perteneciente a la empresa.");
			document.add(terceraCuerpo);
		} else {
			// ── FINANCIADO: cuota inicial + armadas ──
			List<LetraResponseDTO> listaLetras = contrato.getLetras();
			int totalLetras = contrato.getCantidadLetras() != null
					? contrato.getCantidadLetras()
					: listaLetras.size();
			LetraResponseDTO primeraLetra = listaLetras.get(0);

			terceraCuerpo.add(", importe que, ");
			terceraCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
			terceraCuerpo.add(" " + verboSeObliga + " cancelar de la siguiente forma:");
			document.add(terceraCuerpo);

			// 3.1 Cuota inicial
			Paragraph inicialPara = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
					.setMarginLeft(52).setFirstLineIndent(-12).setMarginTop(0);
			inicialPara.add(new Text("a. ").setFont(arialNarrowBold));
			inicialPara.add("Cuota inicial de ");
			BigDecimal inicial = contrato.getInicial() != null ? contrato.getInicial() : BigDecimal.ZERO;
			inicialPara.add(new Text(prefMoneda + " " + df.format(inicial)).setFont(arialNarrowBold));
			inicialPara.add(" cuyo importe se paga a la suscripción de este documento, sin más constancia que la firma y huella de las partes puestas al final de este contrato.");
			document.add(inicialPara);

			// 3.2 Saldo + armadas
			Paragraph saldoPara = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
					.setMarginLeft(52).setFirstLineIndent(-12).setMarginTop(0);

			saldoPara.add(new Text("b. ").setFont(arialNarrowBold));
			saldoPara.add("El saldo de ");
			saldoPara.add(new Text(prefMoneda + " " + df.format(contrato.getSaldo())).setFont(arialNarrowBold));
			saldoPara.add(" será cancelado en ");
			saldoPara.add(new Text(totalLetras + "").setFont(arialNarrowBold));
			saldoPara.add(" ARMADAS mensuales y consecutivas, a razón de ");

			Map<BigDecimal, Integer> gruposMonto = new LinkedHashMap<>();
			for (LetraResponseDTO letra : listaLetras) {
				BigDecimal importe = letra.getImporte();
				gruposMonto.put(importe, gruposMonto.getOrDefault(importe, 0) + 1);
			}

			if (gruposMonto.size() == 1) {
				BigDecimal montoUnico = gruposMonto.keySet().iterator().next();
				saldoPara.add(new Text("1").setFont(arialNarrowBold));
				saldoPara.add(" ARMADA de ");
				saldoPara.add(new Text(prefMonedaSinPunto + " " + df.format(montoUnico)).setFont(arialNarrowBold));
			} else {
				int indexGrupo = 0;
				for (Map.Entry<BigDecimal, Integer> entry : gruposMonto.entrySet()) {
					BigDecimal monto = entry.getKey();
					Integer cantidad = entry.getValue();
					saldoPara.add(new Text(cantidad + "").setFont(arialNarrowBold));
					saldoPara.add(cantidad == 1 ? " ARMADA de " : " ARMADAS de ");
					saldoPara.add(new Text(prefMonedaSinPunto + " " + df.format(monto)).setFont(arialNarrowBold));
					if (indexGrupo < gruposMonto.size() - 2) {
						saldoPara.add(", ");
					} else if (indexGrupo == gruposMonto.size() - 2) {
						saldoPara.add(" y ");
					}
					indexGrupo++;
				}
			}

			saldoPara.add(" con fecha de vencimiento de la primera letra es ");
			saldoPara.add(new Text(primeraLetra.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(arialNarrowBold));
			saldoPara.add(".");
			document.add(saldoPara);

			// 3.3 Lugar de pago
			Paragraph lugarPago = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
					.setMarginLeft(52).setFirstLineIndent(-12).setMarginTop(0);
			lugarPago.add(new Text("c. ").setFont(arialNarrowBold));
			lugarPago.add("El lugar de pago de todas las armadas se hará mediante depósito o efectivo en el domicilio de ");
			lugarPago.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
			lugarPago.add(" señalado en el introito del presente acto jurídico.");
			document.add(lugarPago);
		}

		if (esFinanciado) {
			// ── CUARTO: TÍTULO VALOR (solo financiado) ────────────────────────
			verificarEspacioYSalto(document, pdf, 0.15f);
			agregarEncabezadoClausula(document, arialNarrowBold, "TÍTULO VALOR:");

			Paragraph cuartoCuerpo = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

			cuartoCuerpo.add(new Text("CUARTO: ").setFont(arialNarrowBold));
			cuartoCuerpo.add("A fin de dar fiel cumplimiento a sus obligaciones pecuniarias establecido en la cláusula anterior, ");
			cuartoCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
			cuartoCuerpo.add(" " + verboGira + " a favor de ");
			cuartoCuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
			cuartoCuerpo.add(" ");
			cuartoCuerpo.add(new Text(contrato.getCantidadLetras() + " LETRAS DE CAMBIO").setFont(arialNarrowBold));
			cuartoCuerpo.add(", dichos títulos valores serán cancelados en las fechas de vencimiento establecido en los respectivos cambiales, más el pago de un interés del ");
			cuartoCuerpo.add(new Text("1% diario").setFont(arialNarrowBold));
			cuartoCuerpo.add(" del valor de la letra caso de incurrir en retraso.");
			document.add(cuartoCuerpo);

			agregarEquivalencia(document, arialNarrowBold, arialNarrow, "QUINTO:");
		} else {
			// ── CUARTO: EQUIVALENCIA (contado) ──────────────────────────────
			verificarEspacioYSalto(document, pdf, 0.15f);
			agregarEncabezadoClausula(document, arialNarrowBold, "EQUIVALENCIA:");

			Paragraph cuartoCuerpo = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

			cuartoCuerpo.add(new Text("CUARTO: ").setFont(arialNarrowBold));
			cuartoCuerpo.add("Los contratantes declaran que entre el lote vendido y el precio pactado existe justa equivalencia y que si hubiera alguna diferencia del área del lote de terreno vendido se pagará el reintegro o devolución al precio actualizado. Asimismo, las partes acuerdan que, el precio pactado en este contrato es solamente por la venta del terreno rústico en el que no están incluidos los trámites de aprobación de proyectos, desarrollo urbano; obras de habilitación urbana, los impuestos de ley y afines.");
			document.add(cuartoCuerpo);
		}

		if (esFinanciado) {
			agregarClausulaResolutoria(document, arialNarrowBold, arialNarrow, etiquetaComprador, numClientes);
			agregarReservaPropiedad(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarGravamen(document, arialNarrowBold, arialNarrow, "DÉCIMO PRIMERO:");
			agregarEscrituraFinanciado(document, arialNarrowBold, arialNarrow, etiquetaComprador, "DÉCIMO SEGUNDO:");
			agregarHabilitacionFinanciado(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarGastosTributos(document, arialNarrowBold, arialNarrow, etiquetaComprador, "DÉCIMO SEXTO:");
			agregarRenuncia(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarClausulaPenal(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarEntrega(document, arialNarrowBold, arialNarrow, etiquetaComprador, "DÉCIMO NOVENO:");
			agregarDomicilio(document, arialNarrowBold, arialNarrow, "VIGÉSIMO:");
			agregarCompetencia(document, arialNarrowBold, arialNarrow, "VIGÉSIMO PRIMERO:");
			agregarSupletoria(document, arialNarrowBold, arialNarrow, "VIGÉSIMO SEGUNDO:");
		} else {
			agregarGravamen(document, arialNarrowBold, arialNarrow, "QUINTO:");
			agregarEscrituraContado(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarHabilitacionContado(document, arialNarrowBold, arialNarrow, etiquetaComprador);
			agregarGastosTributos(document, arialNarrowBold, arialNarrow, etiquetaComprador, "NOVENO:");
			agregarEntrega(document, arialNarrowBold, arialNarrow, etiquetaComprador, "DÉCIMO:");
			agregarDomicilio(document, arialNarrowBold, arialNarrow, "DÉCIMO PRIMERO:");
			agregarCompetencia(document, arialNarrowBold, arialNarrow, "DÉCIMO SEGUNDO:");
			agregarSupletoria(document, arialNarrowBold, arialNarrow, "DÉCIMO TERCERO:");
		}

		// ── CIERRE Y FIRMAS ────────────────────────────────────────────────
		verificarEspacioYSalto(document, pdf, 0.15f);

		document.add(new Paragraph("")
				.setMarginTop(20f));

		document.add(new Paragraph()
				.add(new Text("Carabayllo, " + diaNum + "/" + mesNum + "/" + anioNum + ".").setFont(arialNarrow))
				.setFontSize(12)
				.setTextAlignment(TextAlignment.RIGHT)
				.setMarginTop(15));

		agregarBloqueFirmas(document, clientes, arialNarrowBold, etiquetaComprador);

		document.close();
		return out.toByteArray();
	}

	/* ========================================================================
	 * CLÁUSULAS COMPARTIDAS (Contado y Financiado)
	 * ======================================================================== */

	private static void agregarEquivalencia(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "EQUIVALENCIA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add("Los contratantes declaran que entre el lote vendido y el precio pactado existe justa equivalencia y que si hubiera alguna diferencia del área del lote de terreno vendido se pagará el reintegro o devolución al precio actualizado. Asimismo, las partes acuerdan que, el precio pactado en este contrato es solamente por la venta del terreno rústico en el que no están incluidos los trámites de aprobación de proyectos, desarrollo urbano; obras de habilitación urbana, los impuestos de ley y afines.");
		document.add(cuerpo);
	}

	private static void agregarGravamen(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "GRAVAMEN:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" declara que, sobre el bien que enajena no pesa ningún gravamen, hipoteca, medida judicial o extrajudicial, y en general, ningún acto o contrato que prive, limite o restrinja el derecho de propiedad, posesión o uso del bien, obligándose no obstante a la evicción o saneamiento de Ley.");
		document.add(cuerpo);
	}

	private static void agregarEscrituraContado(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ESCRITURA PÚBLICA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("SEXTO: ").setFont(arialNarrowBold));
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" se obliga a perfeccionar la transferencia del lote de terreno vendido a favor de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" cuando el predio de mayor extensión haya sido inscrito en los registros públicos y que el lote de terreno materia de venta sea independizado.");
		document.add(cuerpo);
	}

	private static void agregarEscrituraFinanciado(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ESCRITURA PÚBLICA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" se obliga a perfeccionar la transferencia del lote de terreno vendido a favor de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" cuando este haya cancelado el precio total del terreno y una vez culminado los trámites de habilitación urbana e inscripción registral del lote de terreno debidamente independizado.");
		document.add(cuerpo);
	}

	private static void agregarHabilitacionContado(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "HABILITACIÓN URBANA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("SÉTIMO: ").setFont(arialNarrowBold));
		cuerpo.add("Para la tramitación del cambio de uso, elaboración del proyecto de Habilitación Urbana e Independización del lote de terreno materia de venta, las partes acuerdan conceder a ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" la representación exclusiva en la elaboración de los mismos. Luego de cancelar el precio íntegro del lote de terreno, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" se obliga a cubrir en la proporción que le corresponde los gastos de habilitación urbana e independización de lotes que conforman el programa, para este fin ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" comunicará por carta simple y/o notarial el costo individual a cobrar en cada caso lo que será materia de un contrato anexo al presente documento.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph octavo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		octavo.add(new Text("OCTAVO: ").setFont(arialNarrowBold));
		octavo.add("Se acuerda que, los trámites del proyecto de habilitación urbana y ejecución de la misma correrá a cuenta de los clientes que conforman el programa de vivienda, obligándose ");
		octavo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		octavo.add(" prestar el apoyo necesario en la emisión de documentos que resulten necesarios para cumplir dicho fin, siendo que, la independización del lote de terreno materia de venta será a cuenta de ");
		octavo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		octavo.add(".");
		document.add(octavo);
	}

	private static void agregarHabilitacionFinanciado(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "HABILITACIÓN URBANA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("DÉCIMO TERCERO: ").setFont(arialNarrowBold));
		cuerpo.add("Para la tramitación del cambio de uso, elaboración del proyecto de Habilitación Urbana e Independización del lote de terreno materia de venta, las partes acuerdan conceder a ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" la representación exclusiva en la elaboración de los mismos. Luego de cancelar el precio íntegro del lote de terreno, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" se obliga a cubrir en la proporción que le corresponde los gastos de habilitación urbana e independización de lotes que conforman el programa, para este fin ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" comunicará por carta simple y/o notarial el costo individual a cobrar en cada caso lo que será materia de un contrato anexo al presente documento.");
		document.add(cuerpo);

		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add("Asimismo, las partes convienen que, el Proyecto de Habilitación Urbana en las que se incluyen instalación de agua potable y desagüe, electrificación, construcción de pistas y veredas, será elaborado exclusivamente por ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(". Los costos del proyecto, la ejecución de las obras, así como los aranceles y otros conceptos sean estos anexos o conexos que cobren las entidades respectivas serán asumidos única y exclusivamente por ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" en la proporción que le corresponda.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph c14 = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		c14.add(new Text("DÉCIMO CUARTO: ").setFont(arialNarrowBold));
		c14.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		c14.add(" se obliga a cuenta y costo realizar la limpieza de calles y pistas del programa de vivienda donde se halla el lote de terreno materia de venta.");
		document.add(c14);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph c15 = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		c15.add(new Text("DÉCIMO QUINTO: ").setFont(arialNarrowBold));
		c15.add("Se acuerda que, los trámites del proyecto de habilitación urbana y ejecución de la misma correrá a cuenta de los clientes que conforman el programa de vivienda, obligándose ");
		c15.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		c15.add(" prestar el apoyo necesario en la emisión de documentos que resulten necesarios para cumplir dicho fin, siendo que, la independización del lote de terreno materia de venta será a cuenta de ");
		c15.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		c15.add(".");
		document.add(c15);
	}

	private static void agregarGastosTributos(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "GASTOS Y TRIBUTOS:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add("Las partes acuerdan que, todos los gastos y tributos que originen la celebración, formalización y ejecución del presente contrato serán asumidos íntegramente por ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(".");
		document.add(cuerpo);
	}

	private static void agregarEntrega(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ENTREGA DEL TERRENO:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" hace entrega física del lote de terreno vendido a favor de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" en ese acto, con la sola suscripción de este contrato, en efecto, queda expresamente convenido que, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" al entrar en posesión del terreno materia de venta, se hará responsable absoluto a partir de la fecha el pago de impuestos ante la municipalidad distrital correspondiente, haciéndose cargo también de la defensa de su posesión frente a terceros por los medios permitidos por ley, respetando estrictamente sus linderos, medidas perimétricas y los derechos de posesión y de propiedad de sus vecinos, en caso de realizar construcción alguna, obligándose efectuar dicha construcción dentro del marco del reglamento nacional de construcciones y afines.");
		document.add(cuerpo);
	}

	private static void agregarDomicilio(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "DOMICILIO:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add("Para la validez de todas las comunicaciones y notificaciones a las partes, para las acciones legales que deriven del presente contrato, ambas partes señalan como sus respectivos domicilios los indicados en la introducción de este documento. El cambio de domicilio de cualquiera de las partes surtirá efecto después de cinco días útiles de efectuada la comunicación mediante carta notarial.");
		document.add(cuerpo);
	}

	private static void agregarCompetencia(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "COMPETENCIA TERRITORIAL:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add("Las partes dejan expresa constancia que, para todas las acciones legales que deriven del presente contrato se someten exclusivamente a los Jueces y Salas de la Corte Superior de Justicia de Lima Norte, renunciando así al fuero de sus respectivos domicilios.");
		document.add(cuerpo);
	}

	private static void agregarSupletoria(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String titulo) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "APLICACIÓN SUPLETORIA DE LA LEY:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text(titulo + " ").setFont(arialNarrowBold));
		cuerpo.add("En todo lo no previsto por las partes en el presente contrato, ambas se someten a lo establecido por las normas del código civil y demás del sistema jurídico que resulten aplicables.");
		document.add(cuerpo);
	}

	/* ========================================================================
	 * CLÁUSULAS EXCLUSIVAS DEL CONTRATO FINANCIADO
	 * ======================================================================== */

	private static void agregarClausulaResolutoria(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador, int numClientes) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "CLÁUSULA RESOLUTORIA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("SEXTO: ").setFont(arialNarrowBold));
		cuerpo.add("Los contratantes acuerdan como causal de resolución del presente contrato las siguientes:");
		document.add(cuerpo);

		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
				.setMarginLeft(32).setFirstLineIndent(-12).setMarginTop(0);
		cuerpo.add(new Text("a. ").setFont(arialNarrowBold));
		cuerpo.add("El incumplimiento de pago de tres cuotas pactadas consecutivas o alternadas por parte de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" según estipulado en el artículo 1561 del Código Civil concordante con el artículo 1428 del mismo cuerpo legal, en este caso, ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" conforme lo establece el artículo 1429 del Código Civil podrá requerir mediante carta notarial a fin que ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" satisfaga su prestación dentro de un plazo no menor de quince días, bajo apercibimiento de que, en caso contrario, el contrato queda resuelto. Si la prestación no se cumple dentro del plazo concedido, el contrato quedará resuelto de pleno derecho, sin responsabilidad y sin necesidad de declaración judicial alguna, dicha resolución se aplicará aun cuando ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" haya pagado el 85% del precio total del inmueble materia de transferencia.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("SÉTIMO: ").setFont(arialNarrowBold));
		cuerpo.add("De darse la resolución de la presente compra venta pactado en la cláusula anterior, se conviene expresamente que, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" realizará la entrega física del lote de terreno totalmente desocupado y libre de interferencias, a favor de ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" dentro del plazo perentorio de DIEZ días calendarios. Estableciéndose, asimismo, que por acuerdo de las partes todas las obras y/o mejoras de cualquier naturaleza que ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" hubiese introducido en el referido lote de terreno, quedará en beneficio de ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" sin obligación de reembolso de ninguna clase por parte de ésta ni al pago de mejoras por acuerdo libre de ambas partes.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("OCTAVO: ").setFont(arialNarrowBold));
		cuerpo.add("Los contratantes convienen libremente que, en los casos previstos en la cláusula sexta, es decir; en caso de resolución del contrato por incumplimiento de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" dará lugar a que ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" realice la devolución del dinero aportado, siendo que, dicha devolución se realizará previa liquidación, teniendo ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" la facultad absoluta de retener el ");
		cuerpo.add(new Text("25%").setFont(arialNarrowBold));
		cuerpo.add(" del valor total del terreno como indemnización por daños y perjuicios conforme lo establece el artículo 1563 del Código Civil, en consecuencia, la devolución del dinero se realizará cuando el lote de terreno materia de resolución sea vendido y según el número de letras giradas a favor de ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" según lo establecido en la cláusula cuarta.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("NOVENO: ").setFont(arialNarrowBold));
		cuerpo.add("En caso ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" haya pagado más del 85% del precio total del terreno materia de venta, ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(" perderá el derecho a resolver el contrato por causal de falta de pago, sin embargo, podrá dar por vencido el plazo de cancelación de todas las armadas y/o letras de cambio que estuvieren pendientes o por vencer, pudiendo exigir a ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" el inmediato pago del saldo del precio de compraventa del terreno.");
		document.add(cuerpo);
	}

	private static void agregarReservaPropiedad(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "PACTO DE RESERVA DE PROPIEDAD:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("DÉCIMO: ").setFont(arialNarrowBold));
		cuerpo.add("Las partes acuerdan incorporar en el presente contrato el pacto de reserva de propiedad a favor de ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(", en efecto, ésta conservará la propiedad del lote de terreno materia del presente contrato hasta que se haya pagado el precio íntegro del terreno materia de venta.");
		document.add(cuerpo);
	}

	private static void agregarRenuncia(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "RENUNCIA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("DÉCIMO SÉTIMO: ").setFont(arialNarrowBold));
		cuerpo.add("Por tratarse de un contrato de compraventa con pago por armadas las partes convienen que, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" puede solicitar la renuncia, justificando la razón o circunstancias, debiendo cumplir las siguientes condiciones:");
		document.add(cuerpo);

		com.itextpdf.layout.element.List lista = new com.itextpdf.layout.element.List()
				.setSymbolIndent(20).setListSymbol("\u2022")
				.setMarginLeft(30).setMarginTop(8);

		com.itextpdf.layout.element.ListItem item1 = new com.itextpdf.layout.element.ListItem();
		Paragraph p1 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p1.add("La renuncia debe ser presentada por escrito, siendo necesario para que se produzca sus efectos de aprobación por parte de ");
		p1.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		p1.setFont(arialNarrow).setFontSize(12);
		item1.add(p1);
		lista.add(item1);

		com.itextpdf.layout.element.ListItem item2 = new com.itextpdf.layout.element.ListItem();
		Paragraph p2 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p2.add("De ser aprobada la renuncia, ");
		p2.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		p2.add(" deberá retener el ");
		p2.add(new Text("25%").setFont(arialNarrowBold));
		p2.add(" del valor total del terreno como indemnización por daños y perjuicios conforme lo establece el artículo 1563 del Código Civil, siendo que, la devolución del dinero se realizará cuando el lote de terreno materia de resolución sea vendido y conforme lo aportado.");
		p2.setFont(arialNarrow).setFontSize(12);
		item2.add(p2);
		lista.add(item2);

		com.itextpdf.layout.element.ListItem item3 = new com.itextpdf.layout.element.ListItem();
		Paragraph p3 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p3.add("En caso de existir mejora alguna dentro de la propiedad materia de renuncia, estos quedarán en beneficio de ");
		p3.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		p3.add(" sin obligación de reembolso alguno.");
		p3.setFont(arialNarrow).setFontSize(12);
		item3.add(p3);
		lista.add(item3);

		document.add(lista);
	}

	private static void agregarClausulaPenal(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "CLÁUSULA PENAL:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("DÉCIMO OCTAVO: ").setFont(arialNarrowBold));
		cuerpo.add("De conformidad con lo establecido en los artículos 1341, 1342, 1343, 1344 y siguientes del Código Civil, queda establecido que si ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" no pagase oportunamente una o más letras consecutivas o alternadas, es decir; incurra en retraso, deberá solventar adicionalmente una penalidad determinada en función del ");
		cuerpo.add(new Text("1% diario").setFont(arialNarrowBold));
		cuerpo.add(" del valor de la letra por cada armada y/o letra de cambio, sin perjuicio del pago de los intereses y moras respectivos que continuarán devengándose con la tasa original de los derechos que asistan legalmente a ");
		cuerpo.add(new Text("LA VENDEDORA").setFont(arialNarrowBold));
		cuerpo.add(".");
		document.add(cuerpo);
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

	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes, PdfFont arialNarrowBold, String etiquetaComprador) {
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
				.setWidth(200f).setMarginBottom(2)
				.setHorizontalAlignment(HorizontalAlignment.CENTER);

		celdaC1.add(pLineaC1);
		celdaC1.add(new Paragraph(c1.getNombre().toUpperCase() + " " + c1.getApellidos().toUpperCase()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaC1.add(new Paragraph(etiquetaDocumento(c1) + c1.getNumDoc()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));

		if (clientes.size() == 1) {
			celdaC1.add(new Paragraph("\u201c" + etiquetaComprador + "\u201d").setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f));
		}
		fila1.addCell(celdaC1);
		fila1.addCell(new Cell().setBorder(Border.NO_BORDER));

		Cell celdaV = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
		Paragraph pLineaV = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
				.setWidth(160f).setMarginBottom(2)
				.setHorizontalAlignment(HorizontalAlignment.CENTER);

		celdaV.add(pLineaV);
		celdaV.add(new Paragraph("\u201cLA VENDEDORA\u201d").setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
		celdaV.add(new Paragraph("DNI N°" + representanteDni()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f));
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
						.setWidth(200f).setMarginBottom(2)
						.setHorizontalAlignment(HorizontalAlignment.CENTER);

				celdaExtra.add(pLineaExtra);
				celdaExtra.add(new Paragraph(ci.getNombre().toUpperCase() + " " + ci.getApellidos().toUpperCase()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
				celdaExtra.add(new Paragraph(etiquetaDocumento(ci) + ci.getNumDoc()).setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));

				if (i == clientes.size() - 1) {
					celdaExtra.add(new Paragraph("\u201c" + etiquetaComprador + "\u201d").setFont(arialNarrowBold).setFontSize(12).setFixedLeading(12f));
				}

				tablaExtra.addCell(celdaExtra);
				contenedorPrincipal.addCell(new Cell().add(tablaExtra).setBorder(Border.NO_BORDER));
			}
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

	/**
	 * Agrega el encabezado de una cláusula (título subrayado) en su propia línea,
	 * separado del número ordinal que irá al inicio del cuerpo. El espacio entre
	 * el título y el número se mantiene compacto (marginBottom 0) para que queden
	 * pegados, como el formato deseado:
	 *
	 *     TÍTULO DE LA CLÁUSULA:      (subrayado)
	 *     PRIMERA: texto del cuerpo...
	 */
	private static void agregarEncabezadoClausula(Document document, PdfFont arialNarrowBold, String titulo) {
		document.add(new Paragraph().add(new Text(titulo).setFont(arialNarrowBold).setUnderline())
				.setFontSize(12).setFixedLeading(12).setMarginTop(10).setMarginBottom(0));
	}
}