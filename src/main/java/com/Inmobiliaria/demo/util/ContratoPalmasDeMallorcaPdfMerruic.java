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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Plantilla de contrato para el PROGRAMA PALMAS DE MALLORCA (Inmobiliaria Merruic).
 * Genera el PDF del contrato de transferencia de posesión de terreno rústico en futura
 * habilitación urbana, tanto en su modalidad CONTADO como FINANCIADO.
 *
 * Los datos de la empresa (transfiriente) se toman dinámicamente de la tabla
 * "empresa" vía EmpresaContext, por lo que la misma plantilla sirve para
 * cualquier empresa activa (Merruic en este caso).
 */
public class ContratoPalmasDeMallorcaPdfMerruic {

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

	public static byte[] generarContratoPalmasDeMallorca(ContratoResponseDTO contrato, LetraCambio primeraLetraEntidad) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(122, 85, 57, 85);

		PdfFont arialNarrow;      // Arial Narrow regular (texto, SIN cursiva)
		PdfFont arialNarrowBold;  // Arial Narrow negrita (énfasis, SIN cursiva)

		try {
			byte[] nBytes = StreamUtil.inputStreamToArray(
					ContratoPalmasDeMallorcaPdfMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALN.TTF"));
			arialNarrow = PdfFontFactory.createFont(nBytes, PdfEncodings.WINANSI);

			byte[] nbBytes = StreamUtil.inputStreamToArray(
					ContratoPalmasDeMallorcaPdfMerruic.class.getClassLoader().getResourceAsStream("fonts/ARIALNB.TTF"));
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
		// Separar TITULARES/compradores de los AVALES. Si un cliente no trae rol
		// (contratos antiguos), se trata como titular (comportamiento histórico).
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

		// Etiqueta del comprador: siempre "EL ADQUIRIENTE"
		String etiquetaComprador = "EL ADQUIRIENTE";

		// ── DATOS DEL LOTE ─────────────────────────────────────────────────
		LoteResponseDTO lote = contrato.getLotes().get(0);

		// ── ENCABEZADO ─────────────────────────────────────────────────────
		// Tres líneas centradas, todas subrayadas y con el mismo espaciado
		// compacto (igual al de los encabezados de cláusula).
		document.add(new Paragraph("PROGRAMA PALMAS DE MALLORCA ROMA ALTA")
				.setFont(arialNarrowBold).setFontSize(12).setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12).setMarginBottom(0));

		document.add(new Paragraph("CONTRATO PRIVADO DE TRANSFERENCIA DE POSESION DE TERRENO RÚSTICO EN FUTURA")
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
		intro.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		intro.add(" y de la otra parte ");

		for (com.itextpdf.layout.element.IElement el : bloqueCompradores.getChildren()) {
			intro.add((com.itextpdf.layout.element.ILeafElement) el);
		}

		// Bloque de avales (garantes), si los hay
		if (!avales.isEmpty()) {
			for (com.itextpdf.layout.element.IElement el : bloqueAvales.getChildren()) {
				intro.add((com.itextpdf.layout.element.ILeafElement) el);
			}
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
		primeraCuerpo.add("EL SEÑOR HIPOLITO DONATO PEREZ CHAVARRIA identificado con DNI N°07991922 y el sr. JULIO MENDOZA QUISPE identificado con DNI N°07985863 son propietarios y poseedores únicos del bien inmueble, con un área de 32,681.00 m2 (treinta y dos mil seiscientos ochenta y uno metros cuadrados), ubicado en el FUNDO LA ROMA ALTA, DISTRITO DE CARABAYLLO, PROVINCIA Y DEPARTAMENTO DE LIMA. SE DEJA CONSTANCIA QUE A NIVEL MUNICIPAL EL INMUEBLE TIENE LA SIGUENTA DENOMINACION FUNDO LA ROMA ALTA, DISTRITO DE CARABAYLLO, PROVINCIA Y DEPARTAMENTO DE LIMA, CON CODIGO DE PREDIO N° ");
		primeraCuerpo.add(new Text("283941").setFont(arialNarrowBold));
		primeraCuerpo.add(", EL SEÑOR HIPOLITO DONATO PEREZ CHAVARRIA ADQUIRIO EL INMUEBLE MEDIANTE SUCESION INTESTADA DE FECHA 15/06/2004, QUE SE ENCUENTRA REGISTRADA COMO CONTRIBUYENTE EN LA MUNICIPALIDAD DISTRITAL DE CARABAYLLO, CON CODIGO DE CONTRIBUYENTE N°");
		primeraCuerpo.add(new Text("03756").setFont(arialNarrowBold));
		primeraCuerpo.add(", el señor HIPOLITO DONATO PEREZ CHAVARRIA da en traspaso el 53.437% de acciones y derechos del predio citado anteriormente, equivalente a 17,463.00 m2 (diecisiete mil cuatrocientos sesenta y tres con 00/100 metros cuadrados) del inmueble a la ");
		primeraCuerpo.add(new Text(empresa()).setFont(arialNarrowBold));
		primeraCuerpo.add(" en el que celebraron el CONTRATO PRIVADO DE TRANSFERENCIA DE POSESION con fecha 02/02/2023, actualmente con código de contribuyente N°");
		primeraCuerpo.add(new Text("0518738").setFont(arialNarrowBold));
		primeraCuerpo.add(", ubicado en el FUNDO LA ROMA ALTA UC 10630/Nro/Mz/Lt, con el 53.437% de acciones y derechos del predio citado anteriormente, equivalente a 17,463.00 m2.");

		document.add(primeraCuerpo);

		// ── SEGUNDA: OBJETO DEL CONTRATO (Naturaleza del terreno) ───────────
		verificarEspacioYSalto(document, pdf, 0.25f);
		Div divSegunda = new Div().setKeepTogether(true);

		agregarEncabezadoClausula(document, arialNarrowBold, "OBJETO DEL CONTRATO:");

		Paragraph segundaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		segundaIntro.add(new Text("SEGUNDA: ").setFont(arialNarrowBold));
		segundaIntro.add(new Text("\u201cEL TRANSFIRIENTE\u201d").setFont(arialNarrowBold));
		segundaIntro.add(" expresa que el lote de terreno que se vende por medio del presente documento, es de naturaleza rustico, sin habilitación urbana; en consecuencia, no cuenta con los servicios básicos, de luz agua desagüe lo que \u201cEL ADQUIRIENTE\u201d declaran conocer.");
		divSegunda.add(segundaIntro);

		Paragraph segundaConstancia = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f)
				.setMarginTop(10);
		segundaConstancia.add("Se deja Constancia que \u201cEL TRANSFIRIENTE\u201d ha adquirido dicho inmueble mediante Contrato de Transferencia de Posesión de terreno rustico de su anterior posesionario el señor HIPOLITO DONATO PEREZ CHAVARRÍA, en virtud de una constancia de posesión N°0739-2014/GDUR/MDC de la Municipalidad Distrital de carabayllo.");
		divSegunda.add(segundaConstancia);

		document.add(divSegunda);

		// ── TERCERA: Venta del terreno ────────────────────────────────────

		Paragraph terceraIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		terceraIntro.add(new Text("TERCERA: ").setFont(arialNarrowBold));
		terceraIntro.add("Por medio del presente documento ");
		terceraIntro.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		terceraIntro.add(" dan en venta real y enajenación perpetua a favor de ");
		terceraIntro.add(new Text("EL ADQUIRIENTE").setFont(arialNarrowBold));
		terceraIntro.add(" el lote de terreno rústico ubicado en el PROGRAMA DE VIVIENDA PALMAS DE MALLORCA ROMA ALTA, Manzana ");
		terceraIntro.add(new Text("\u201c" + lote.getManzana() + "\u201d").setFont(arialNarrowBold));
		terceraIntro.add(", Lote ");
		terceraIntro.add(new Text("\u201c" + lote.getNumeroLote() + "\u201d").setFont(arialNarrowBold));
		terceraIntro.add(", con un área de ");
		terceraIntro.add(new Text(lote.getArea() + " m2").setFont(arialNarrowBold));
		terceraIntro.add(", con un porcentaje de ");
		terceraIntro.add(new Text("0.76%").setFont(arialNarrowBold));
		terceraIntro.add(" situado en el Distrito de Carabayllo, Provincia y Departamento de Lima, encerrado dentro de los siguientes linderos y medidas perimétricas:");

		document.add(terceraIntro);

		// ── Tabla de linderos ──────────────────────────────────────────────
		Table tablaLinderos = new Table(UnitValue.createPercentArray(new float[]{30f, 45f, 25f}))
				.useAllAvailableWidth()
				.setBorder(Border.NO_BORDER)
				.setMarginBottom(20);

		agregarFilaLinderos(tablaLinderos, "Por el frente", lote.getColindanteNorte(), "Con    " + lote.getAncho1() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por la derecha", lote.getColindanteEste(), "Con  " + lote.getLargo1() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por la Izquierda", lote.getColindanteOeste(), "Con    " + lote.getLargo2() + "  m.l.", arialNarrow);
		agregarFilaLinderos(tablaLinderos, "Por el fondo", lote.getColindanteSur(), "Con    " + lote.getAncho2() + "  m.l.", arialNarrow);

		document.add(tablaLinderos);

		// ── CUARTA: PRECIO Y FORMA DE PAGO ────────────────────────────────
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

		terceraCuerpo.add(new Text("CUARTO: ").setFont(arialNarrowBold));
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
			terceraCuerpo.add(" " + "se obligan" + " cancelar de la siguiente forma:");
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
			lugarPago.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
			lugarPago.add(" señalado en el introito del presente acto jurídico.");
			document.add(lugarPago);
		}

		if (esFinanciado) {
			// ── QUINTO: TÍTULO VALOR (solo financiado) ────────────────────────
			verificarEspacioYSalto(document, pdf, 0.15f);
			agregarEncabezadoClausula(document, arialNarrowBold, "TÍTULO VALOR:");

			Paragraph cuartoCuerpo = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

			cuartoCuerpo.add(new Text("QUINTO: ").setFont(arialNarrowBold));
			cuartoCuerpo.add("A fin de dar fiel cumplimiento a sus obligaciones pecuniarias establecido en la cláusula anterior, ");
			cuartoCuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
			cuartoCuerpo.add(" " + "giran" + " a favor de ");
			cuartoCuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
			cuartoCuerpo.add(" ");
			cuartoCuerpo.add(new Text(contrato.getCantidadLetras() + " LETRAS DE CAMBIO").setFont(arialNarrowBold));
			cuartoCuerpo.add(", dichos títulos valores serán cancelados en las fechas de vencimiento establecido en los respectivos cambiales, más el pago de un interés del ");
			cuartoCuerpo.add(new Text("1% diario").setFont(arialNarrowBold));
			cuartoCuerpo.add(" del valor de la letra caso de incurrir en retraso.");
			document.add(cuartoCuerpo);

			agregarEquivalencia(document, arialNarrowBold, arialNarrow, "SEXTO:");
		} else {
			// ── QUINTO: EQUIVALENCIA (contado) ──────────────────────────────
			verificarEspacioYSalto(document, pdf, 0.15f);
			agregarEncabezadoClausula(document, arialNarrowBold, "EQUIVALENCIA:");

			Paragraph cuartoCuerpo = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

			cuartoCuerpo.add(new Text("QUINTO: ").setFont(arialNarrowBold));
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
			agregarGravamen(document, arialNarrowBold, arialNarrow, "SEXTO:");
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

		document.add(new Paragraph()
				.add(new Text("Carabayllo, " + diaNum + "/" + mesNum + "/" + anioNum + ".").setFont(arialNarrow))
				.setFontSize(12)
				.setTextAlignment(TextAlignment.RIGHT)
				.setMarginTop(5));

		agregarBloqueFirmas(document, titulares, avales, arialNarrowBold, etiquetaComprador);

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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" declara que, sobre el bien que enajena no pesa ningún gravamen, hipoteca, medida judicial o extrajudicial, y en general, ningún acto o contrato que prive, limite o restrinja el derecho de propiedad, posesión o uso del bien, obligándose no obstante a la evicción o saneamiento de Ley.");
		document.add(cuerpo);
	}

	private static void agregarEscrituraContado(Document document, PdfFont arialNarrowBold, PdfFont arialNarrow, String etiquetaComprador) {
		verificarEspacioYSalto(document, null, 0.15f);
		agregarEncabezadoClausula(document, arialNarrowBold, "ESCRITURA PÚBLICA:");

		Paragraph cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add(new Text("SÉTIMO: ").setFont(arialNarrowBold));
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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

		cuerpo.add(new Text("OCTAVO: ").setFont(arialNarrowBold));
		cuerpo.add("Para la tramitación del cambio de uso, elaboración del proyecto de Habilitación Urbana e Independización del lote de terreno materia de venta, las partes acuerdan conceder a ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" la representación exclusiva en la elaboración de los mismos. Luego de cancelar el precio íntegro del lote de terreno, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" " + "se obligan" + " cubrir en la proporción que le corresponde los gastos de habilitación urbana e independización de lotes que conforman el programa, para este fin ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" " + "comunicarán" + " por carta simple y/o notarial el costo individual a cobrar en cada caso lo que será materia de un contrato anexo al presente documento.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph octavo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		octavo.add(new Text("NOVENO: ").setFont(arialNarrowBold));
		octavo.add("Se acuerda que, los trámites del proyecto de habilitación urbana y ejecución de la misma correrá a cuenta de los clientes que conforman el programa de vivienda, obligándose ");
		octavo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		octavo.add(" prestar el apoyo necesario en la emisión de documentos que resulten necesarios para cumplir dicho fin, siendo que, la independización del lote de terreno materia de venta será a cuenta de ");
		octavo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" la representación exclusiva en la elaboración de los mismos. Luego de cancelar el precio íntegro del lote de terreno, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" " + "se obligan" + " cubrir en la proporción que le corresponde los gastos de habilitación urbana e independización de lotes que conforman el programa, para este fin ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" " + "comunicarán" + " por carta simple y/o notarial el costo individual a cobrar en cada caso lo que será materia de un contrato anexo al presente documento.");
		document.add(cuerpo);

		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);

		cuerpo.add("Asimismo, las partes convienen que, el Proyecto de Habilitación Urbana en las que se incluyen instalación de agua potable y desagüe, electrificación, construcción de pistas y veredas, será elaborado exclusivamente por ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(". Los costos del proyecto, la ejecución de las obras, así como los aranceles y otros conceptos sean estos anexos o conexos que cobren las entidades respectivas serán asumidos única y exclusivamente por ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" en la proporción que le corresponda.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph c14 = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		c14.add(new Text("DÉCIMO CUARTO: ").setFont(arialNarrowBold));
		c14.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		c14.add(" se obliga a cuenta y costo realizar la limpieza de calles y pistas del programa de vivienda donde se halla el lote de terreno materia de venta.");
		document.add(c14);

		verificarEspacioYSalto(document, null, 0.15f);
		Paragraph c15 = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		c15.add(new Text("DÉCIMO QUINTO: ").setFont(arialNarrowBold));
		c15.add("Se acuerda que, los trámites del proyecto de habilitación urbana y ejecución de la misma correrá a cuenta de los clientes que conforman el programa de vivienda, obligándose ");
		c15.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		c15.add(" prestar el apoyo necesario en la emisión de documentos que resulten necesarios para cumplir dicho fin, siendo que, la independización del lote de terreno materia de venta será a cuenta de ");
		c15.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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

		cuerpo.add(new Text("SÉPTIMO: ").setFont(arialNarrowBold));
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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" conforme lo establece el artículo 1429 del Código Civil podrá requerir mediante carta notarial a fin que ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" satisfaga su prestación dentro de un plazo no menor de quince días, bajo apercibimiento de que, en caso contrario, el contrato queda resuelto. Si la prestación no se cumple dentro del plazo concedido, el contrato quedará resuelto de pleno derecho, sin responsabilidad y sin necesidad de declaración judicial alguna, dicha resolución se aplicará aun cuando ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" haya pagado el 85% del precio total del inmueble materia de transferencia.");
		document.add(cuerpo);

		// OCTAVO: umbral bajo para que entre en la página actual aunque sea parcial
		verificarEspacioYSalto(document, null, 0.05f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("OCTAVO: ").setFont(arialNarrowBold));
		cuerpo.add("De darse la resolución de la presente compra venta pactado en la cláusula anterior, se conviene expresamente que, ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" realizará la entrega física del lote de terreno totalmente desocupado y libre de interferencias, a favor de ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" dentro del plazo perentorio de DIEZ días calendarios. Estableciéndose, asimismo, que por acuerdo de las partes todas las obras y/o mejoras de cualquier naturaleza que ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" hubiese introducido en el referido lote de terreno, quedará en beneficio de ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" sin obligación de reembolso de ninguna clase por parte de ésta ni al pago de mejoras por acuerdo libre de ambas partes.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("NOVENO: ").setFont(arialNarrowBold));
		cuerpo.add("Los contratantes convienen libremente que, en los casos previstos en la cláusula anterior, es decir; en caso de resolución del contrato por incumplimiento de ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" dará lugar a que ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" realice la devolución del dinero aportado, siendo que, dicha devolución se realizará previa liquidación, teniendo ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" la facultad absoluta de retener el ");
		cuerpo.add(new Text("25%").setFont(arialNarrowBold));
		cuerpo.add(" del valor total del terreno como indemnización por daños y perjuicios conforme lo establece el artículo 1563 del Código Civil, en consecuencia, la devolución del dinero se realizará cuando el lote de terreno materia de resolución sea vendido y según el número de letras giradas a favor de ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		cuerpo.add(" según lo establecido en la cláusula cuarta.");
		document.add(cuerpo);

		verificarEspacioYSalto(document, null, 0.15f);
		cuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialNarrow).setFontSize(12).setMultipliedLeading(1.0f);
		cuerpo.add(new Text("DÉCIMO: ").setFont(arialNarrowBold));
		cuerpo.add("En caso ");
		cuerpo.add(new Text(etiquetaComprador).setFont(arialNarrowBold));
		cuerpo.add(" haya pagado más del 85% del precio total del terreno materia de venta, ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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

		cuerpo.add(new Text("DÉCIMO PRIMERO: ").setFont(arialNarrowBold));
		cuerpo.add("Las partes acuerdan incorporar en el presente contrato el pacto de reserva de propiedad a favor de ");
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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
		p1.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		p1.setFont(arialNarrow).setFontSize(12);
		item1.add(p1);
		lista.add(item1);

		com.itextpdf.layout.element.ListItem item2 = new com.itextpdf.layout.element.ListItem();
		Paragraph p2 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p2.add("De ser aprobada la renuncia, ");
		p2.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
		p2.add(" deberá retener el ");
		p2.add(new Text("25%").setFont(arialNarrowBold));
		p2.add(" del valor total del terreno como indemnización por daños y perjuicios conforme lo establece el artículo 1563 del Código Civil, siendo que, la devolución del dinero se realizará cuando el lote de terreno materia de resolución sea vendido y conforme lo aportado.");
		p2.setFont(arialNarrow).setFontSize(12);
		item2.add(p2);
		lista.add(item2);

		com.itextpdf.layout.element.ListItem item3 = new com.itextpdf.layout.element.ListItem();
		Paragraph p3 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p3.add("En caso de existir mejora alguna dentro de la propiedad materia de renuncia, estos quedarán en beneficio de ");
		p3.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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
		cuerpo.add(new Text("EL TRANSFIRIENTE").setFont(arialNarrowBold));
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

	private static float calcularAnchoLineaFirma(String nombreCompleto) {
		// Ancho de columna = 45% de pagina A4 (595pt) = 267.75pt
		float anchoColumna = 267.75f;
		int len = nombreCompleto.length();
		// Calcular proporcion exacta segun largo del nombre
		float proporcion;
		if (len <= 15) {
			proporcion = 0.50f;  // 50% de la columna
		} else if (len <= 20) {
			proporcion = 0.60f;  // 60%
		} else if (len <= 25) {
			proporcion = 0.70f;  // 70%
		} else if (len <= 30) {
			proporcion = 0.80f;  // 80%
		} else if (len <= 35) {
			proporcion = 0.90f;  // 90%
		} else {
			proporcion = 1.00f;  // 100%
		}
		return anchoColumna * proporcion;
	}

	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> titulares,
                                            List<ClienteResponseDTO> avales, PdfFont arialNarrowBold, String etiquetaComprador) {
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
