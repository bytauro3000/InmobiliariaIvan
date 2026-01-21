package com.Inmobiliaria.demo.util;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LetraResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
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
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

public class PdfGenerator {

	public static byte[] generarContratoFlorida(ContratoResponseDTO contrato) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(122, 85, 57, 85); //multiplicar los cm * 28.35 = margenes

		// 🔹 CARGA DE FUENTES (NORMAL, NEGRITA Y NEGRITA-CURSIVA)
		PdfFont arialNormal;
		PdfFont arialBold;
		PdfFont arialBoldItalic;
		PdfFont arialItalic;

		try {
			// 1. Arial Normal (Para el cuerpo del texto)
			byte[] nBytes = StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIAL.TTF")
					);
			arialNormal = PdfFontFactory.createFont(nBytes, PdfEncodings.WINANSI);

			// 2. Arial Bold (Para nombres y datos en negrita)
			byte[] bBytes = StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALBD.TTF")
					);
			arialBold = PdfFontFactory.createFont(bBytes, PdfEncodings.WINANSI);

			// 3. Arial Bold Italic (Para el título del contrato)
			byte[] biBytes = StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALBI.TTF")
					);
			arialBoldItalic = PdfFontFactory.createFont(biBytes, PdfEncodings.WINANSI);

			// 4. Carga de Arial Italic (Solo Cursiva)
			byte[] iBytes = StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALI.TTF")
					);
			arialItalic = PdfFontFactory.createFont(iBytes, PdfEncodings.WINANSI);

		} catch (Exception e) {
			throw new RuntimeException("Error cargando las fuentes Arial desde resources/fonts/", e);
		}


		// --- CONVERSIÓN DE FECHA DEL CONTRATO SEGURA (Solución al Error 403) ---
		Date fechaUtil = contrato.getFechaContrato();
		LocalDate fechaRegistro;

		if (fechaUtil != null) {
			// Usamos Calendar para extraer los datos sin importar si es java.util.Date o java.sql.Date
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(fechaUtil);
			fechaRegistro = LocalDate.of(
					cal.get(java.util.Calendar.YEAR),
					cal.get(java.util.Calendar.MONTH) + 1,
					cal.get(java.util.Calendar.DAY_OF_MONTH)
					);
		} else {
			fechaRegistro = LocalDate.now(); // Respaldo por si la base de datos devuelve nulo
		}

		String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", 
				"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

		String diaNum = String.format("%02d", fechaRegistro.getDayOfMonth());
		String mesNombre = nombresMeses[fechaRegistro.getMonthValue() - 1];
		String mesNum = String.format("%02d", fechaRegistro.getMonthValue());
		int anioNum = fechaRegistro.getYear();


		// --- PROCESAMIENTO DINÁMICO DE CLIENTES ---
		List<ClienteResponseDTO> clientes = contrato.getClientes();
		int numClientes = clientes.size();
		ClienteResponseDTO titular = clientes.get(0);

		String nombreDistrito = (titular.getDistrito() != null) ? titular.getDistrito().getNombre() : "";
		String domicilioCalle = (titular.getDireccion() != null) ? titular.getDireccion().toUpperCase() : "";

		// 🔹 INICIO DEL AJUSTE PARA EL CALLAO
		String ubicacionDinamica;
		if (nombreDistrito.toUpperCase().contains("CALLAO")) {
		    // Si el distrito es Callao, se usa Provincia del Callao y no Departamento de Lima
		    ubicacionDinamica = ", Distrito del Callao, Provincia Constitucional del Callao";
		} else {
		    // Caso estándar para los distritos de Lima
		    ubicacionDinamica = ", Distrito de " + nombreDistrito + ", Provincia y Departamento de Lima";
		}

		String direccionRealParaContrato = domicilioCalle + ubicacionDinamica;
		

		// 1. Construcción del bloque de compradores dinámica
		Paragraph bloqueCompradores = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10);
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
			boolean esFemenino = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));

			String prefijo = esFemenino ? "la Sra. " : "el Sr. ";
			String nacionalidad = extraerNacionalidad(c.getCelular(), esFemenino);
			String identif = esFemenino ? "identificada" : "identificado";

			// 🔹 LÓGICA DINÁMICA DE ESTADO CIVIL SEGÚN EL DTO
			String estCivil = "";
			if (c.getEstadoCivil() != null) {
				// Convierte el Enum a String y ajusta el género (ej: "Soltero" -> "soltera")
				estCivil = c.getEstadoCivil().toString().toLowerCase();
				if (esFemenino) {
					if (estCivil.equals("soltero")) estCivil = "soltera";
					if (estCivil.equals("casado")) estCivil = "casada";
					if (estCivil.equals("viudo")) estCivil = "viuda";
				}
			} else {
				estCivil = esFemenino ? "soltera" : "soltero"; // Respaldo por si es nulo
			}

			bloqueCompradores.add(prefijo);
			bloqueCompradores.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setBold());

			// Mostramos: ", peruano, soltero, identificado con..."
			bloqueCompradores.add(", " + nacionalidad + ", " + estCivil + ", " + identif + " con ");
			bloqueCompradores.add(new Text("DNI N°" + c.getNumDoc()).setBold());

			// Si hay más de un cliente, añadimos el separador "y" antes del último
			if (numClientes > 1 && i < numClientes - 1) {
				if (i == numClientes - 2) {
					bloqueCompradores.add(", y ");
				} else {
					bloqueCompradores.add(", ");
				}
			}
		}

		// Variables de concordancia (Singular/Plural)
		String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES" : "EL COMPRADOR";
		String pronombreDenom = (numClientes > 1) ? "les" : "le";
		String verboPuede = (numClientes > 1) ? "pueden" : "puede";        // <--- NUEVA
		String verboHaya = (numClientes > 1) ? "hayan" : "haya";          // <--- NUEVA
		String verboDeclara = (numClientes > 1) ? "declaran" : "declara";  // <--- NUEVA
		String verboSeObliga = (numClientes > 1) ? "se obligan" : "se obliga"; // <--- NUEVA
		String verboGira = (numClientes > 1) ? "giran" : "gira";          // <--- NUEVA
		String verboCumpla = (clientes.size() > 1) ? "cumplan" : "cumpla";
		String verboTenga = (numClientes > 1) ? "tengan" : "tenga";
		String verboDeje = (numClientes > 1) ? "dejen" : "deje";
		String verboDebera = (numClientes > 1) ? "deberán" : "deberá";
		String verboDesea = (numClientes > 1) ? "desean" : "desea";
		String verboCompromete = (numClientes > 1) ? "se comprometen" : "se compromete";
		String verboAdeude = (numClientes > 1) ? "adeuden" : "adeude";
		String verboReconoce = (numClientes > 1) ? "reconocen" : "reconoce";
		String verboCancele = (numClientes > 1) ? "cancelen" : "cancele";



		// --- DATOS DEL LOTE Y PRECIO (RESTAURADO) ---
		LoteResponseDTO lote = contrato.getLotes().get(0);
		LetraResponseDTO pL = contrato.getLetras().get(0);
		LetraResponseDTO uL = contrato.getLetras().get(contrato.getLetras().size() - 1);
		String montoTexto = pL.getImporteLetras().split(" POR ")[0];




		/* =========================================================
		 *  PAGINA 1: ENCABEZADO DEL CONTRATO
		 * ========================================================= */

		document.add(new Paragraph("PROGRAMA DE VIVIENDA")
				.setFont(arialBoldItalic)
				.setFontSize(18)
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(16) // 📏 Controla el espacio con la siguiente línea
				.setMarginBottom(0)
				);

		document.add(new Paragraph("“LA FLORIDA DE TORRE BLANCA”")
				.setFont(arialBoldItalic)
				.setFontSize(18)
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(16) // 📏 Mantener el mismo valor para consistencia
				.setMarginBottom(5)  // Un pequeño margen para separar del siguiente título
				);

		document.add(new Paragraph("CONTRATO PRIVADO DE COMPRA-VENTA DE TERRENO RUSTICO")
				.setFont(arialBoldItalic)
				.setFontSize(11)
				.setUnderline()
				.setTextAlignment(TextAlignment.CENTER)
				.setFixedLeading(12) // Interlineado más pequeño porque la letra es más chica (11)
				.setMarginBottom(15) 
				);

		/* =========================================================
		 *  PAGINA 1: INTRODUCCION
		 * ========================================================= */
		Paragraph intro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)      // Fuente base Arial Normal
				.setFontSize(12)           // Tamaño 12
				.setMultipliedLeading(1.5f); // Interlineado 1.5

		intro.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico con Reserva de Propiedad que celebran de una parte ");

		// Solo usamos arialBold para los fragmentos que requieren negrita
		intro.add(new Text("“INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.” ").setFont(arialBold));
		intro.add("con ");
		intro.add(new Text("RUC Nº 20537853108 ").setFont(arialBold));
		intro.add("con domicilio Av. Alfredo Mendiola N°3623- Tercer Piso - Of. 301-A Urb. Panamericana Norte, Distrito de Los Olivos, Provincia y Departamento de Lima, debidamente representado por su ");
		intro.add(new Text("Gerente General OLMEDO SILVA LOPEZ ").setFont(arialBold));
		intro.add("con ");
		intro.add(new Text("DNI No.19404451 ").setFont(arialBold));
		intro.add("según consta del poder inscrito en la partida electrónica Nº ");
		intro.add(new Text("12561792 ").setFont(arialBold));
		intro.add("del Registro de Personas Jurídicas, a quien en adelante se le denominará ");
		intro.add(new Text("LA VENDEDORA").setFont(arialBold));
		intro.add("; y de la otra parte ");

		// Bloque dinámico de compradores
		for (com.itextpdf.layout.element.IElement el : bloqueCompradores.getChildren()) {
			intro.add((com.itextpdf.layout.element.ILeafElement)el);
		}

		String etiquetaDomicilio = (numClientes > 1) ? "ambos con domicilio común en " : "con domicilio en ";
		intro.add(", " + etiquetaDomicilio + direccionRealParaContrato);
		intro.add(", a quien en adelante se " + pronombreDenom + " denominará ");
		intro.add(new Text(etiquetaComprador).setFont(arialBold).setUnderline());
		intro.add(" en los términos y condiciones de las cláusulas siguientes:");

		document.add(intro);

		/* =========================================================
		 *  PAGINA 1: CLAUSULA PRIMERA:	PROPIEDAD
		 * ========================================================= */
		// Título de la cláusula (Arial 11, Negrita y Cursiva)
		document.add(new Paragraph()
				.add(new Text("PRIMERA: PROPIEDAD").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(10));

		// Cuerpo de la cláusula
		// Seteamos arialItalic como fuente base del párrafo
		Paragraph primeraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) 
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		primeraCuerpo.add(new Text("“LA VENDEDORA”").setFont(arialBoldItalic));
		primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
		primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. ").setFont(arialBoldItalic));
		primeraCuerpo.add("\nFue adquirido mediante la minuta de Compra- Venta de Acciones y Derechos de Predio Rustico de la fecha ");
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
		primeraCuerpo.add(new Text("“LA FLORIDA DE TORRE BLANCA 1ERA.ETAPA”").setFont(arialBoldItalic));
		primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización.");

		document.add(primeraCuerpo);

		/* =========================================================
		 * PAGINA 2: CLAUSULA SEGUNDA - OBJETO DEL CONTRATO
		 * ========================================================= */
		//salto de pagina solo si el contenido de la pagina supera el 50%
		verificarEspacioYSalto(document, pdf, 0.4f);
		// 1. Título de la Cláusula
		document.add(new Paragraph()
				.add(new Text("SEGUNDA: OBJETO DEL CONTRATO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// --- LÓGICA DINÁMICA PARA AGRUPAR MANZANAS Y LOTES ---
		List<LoteResponseDTO> listaLotes = contrato.getLotes();
		BigDecimal areaTotal = BigDecimal.ZERO;
		for (LoteResponseDTO l : listaLotes) { areaTotal = areaTotal.add(l.getArea()); }

		// 2. Bloque descriptivo inicial
		Paragraph segundaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) 
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		segundaIntro.add("Por el presente contrato ");
		segundaIntro.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaIntro.add(" transfiere los derechos y acciones de un lote de terreno rústico ");

		// 🔹 INICIO DE CONSTRUCCIÓN DINÁMICA CON NEGRITAS
		if (listaLotes.size() == 1) {
			LoteResponseDTO single = listaLotes.get(0);
			segundaIntro.add("ubicado la Manzana “");
			segundaIntro.add(new Text(single.getManzana()).setFont(arialBoldItalic)); // 🔹 Manzana Negrita
			segundaIntro.add("” y asignado, con el lote Nº ");
			segundaIntro.add(new Text(single.getNumeroLote()).setFont(arialBoldItalic)); // 🔹 Lote Negrita
		} else {
			boolean mismaManzana = listaLotes.stream().map(LoteResponseDTO::getManzana).distinct().count() == 1;
			if (mismaManzana) {
				segundaIntro.add("ubicado la Manzana “");
				segundaIntro.add(new Text(listaLotes.get(0).getManzana()).setFont(arialBoldItalic));
				segundaIntro.add("” y asignado, con los lotes ");
				for (int i = 0; i < listaLotes.size(); i++) {
					segundaIntro.add("Nº ");
					segundaIntro.add(new Text(listaLotes.get(i).getNumeroLote()).setFont(arialBoldItalic));
					if (i < listaLotes.size() - 1) segundaIntro.add(" y ");
				}
			} else {
				for (int i = 0; i < listaLotes.size(); i++) {
					segundaIntro.add("la Manzana “");
					segundaIntro.add(new Text(listaLotes.get(i).getManzana()).setFont(arialBoldItalic));
					segundaIntro.add("” asignado, con el lote ");
					segundaIntro.add(new Text(listaLotes.get(i).getNumeroLote()).setFont(arialBoldItalic));
					if (i < listaLotes.size() - 1) segundaIntro.add(" y ");
				}
			}
		}

		segundaIntro.add(" del Programa de Vivienda ");
		segundaIntro.add(new Text("“LA FLORIDA DE TORRE BLANCA”").setFont(arialBoldItalic));
		segundaIntro.add(" con un área total de ");

		// 🔹 Área en Negrita
		segundaIntro.add(new Text(areaTotal + "M2.").setFont(arialBoldItalic));
		segundaIntro.add(" Encerrado dentro de los siguientes linderos y medidas perimétricas:");

		document.add(segundaIntro);

		// --- 3. BUCLE PARA MOSTRAR LINDEROS DE CADA LOTE ---
		for (LoteResponseDTO loteItem : listaLotes) {

			// 🔹 AJUSTE: Solo muestra el encabezado MZ/LT/ÀREA si hay más de un lote
			if (listaLotes.size() > 1) {
				document.add(new Paragraph()
						.add(new Text("MZ. " + loteItem.getManzana() + " LT." + loteItem.getNumeroLote() + " ÀREA:" + loteItem.getArea() + "M2")
								.setFont(arialBoldItalic))
						.setFontSize(11)
						.setMarginTop(10)
						.setMarginBottom(2));
			}

			
			Table tablaLinderos = new Table(UnitValue.createPercentArray(new float[]{30f, 45f, 25f}))
			        .useAllAvailableWidth()
			        .setBorder(Border.NO_BORDER);

			// Reutilizamos tu método de 5 parámetros
			agregarFilaLinderos(tablaLinderos, "Por el frente", loteItem.getColindanteNorte(), "Con    " + loteItem.getAncho1() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaLinderos, "Por la derecha", loteItem.getColindanteEste(), "Con  " + loteItem.getLargo1() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaLinderos, "Por la Izquierda", loteItem.getColindanteOeste(), "Con    " + loteItem.getLargo2() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaLinderos, "Por el fondo", loteItem.getColindanteSur(), "Con    " + loteItem.getAncho2() + "  m.l.", arialItalic);

			document.add(tablaLinderos);
		}
		// 4. Bloque descriptivo Final
		Paragraph segundaFinal = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f)
				.setMarginTop(10);

		segundaFinal.add("Por el presente contrato ");
		segundaFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaFinal.add(" otorga en venta real un lote de terreno rustico con veredas, agua y luz provisional previo pago por cada servicio brindado a ");
		segundaFinal.add(new Text(etiquetaComprador).setFont(arialBoldItalic)); 
		segundaFinal.add(" así mismo, correspondiéndole sus aires, usos, costumbres, entradas, salidas y todo cuanto de hecho y por derecho le corresponde sin reserva ni limitación alguna, toda vez que la finalidad del presente contrato es que surta todos sus efectos legales.");

		document.add(segundaFinal);
		/* =========================================================
		 * PAGINA 2: CLAUSULA TERCERA: PRECIO
		 * ========================================================= */

		// 1. Definir el formato de moneda con comas
		DecimalFormat df = new DecimalFormat("#,##0.00");

		// 2. Convertir montos a letras usando tu clase NumeroALetras
		String montoTotalLetras = NumeroALetras.convertir(contrato.getMontoTotal());
		String montoSaldoLetras = NumeroALetras.convertir(contrato.getSaldo());

		// 1. Título de la Cláusula
		document.add(new Paragraph()
				.add(new Text("TERCERA: PRECIO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo inicial de la cláusula
		Paragraph terceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		terceraCuerpo.add("El precio del bien objeto de la prestación a cargo de ");
		terceraCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		terceraCuerpo.add(" asciende a la suma de ");

		// --- Validación de Letras para evitar errores NullPointer ---
		if (contrato.getLetras() != null && !contrato.getLetras().isEmpty()) {
		    List<LetraResponseDTO> listaLetras = contrato.getLetras();
		    int totalLetras = contrato.getCantidadLetras();
		    
		    // 🔹 DECLARACIÓN DE VARIABLES PARA USO EN TODAS LAS SUBCLÁUSULAS
		    LetraResponseDTO primeraLetra = listaLetras.get(0);
		    LetraResponseDTO ultimaLetra = listaLetras.get(totalLetras - 1); 

		    terceraCuerpo.add(new Text("US$." + df.format(contrato.getMontoTotal())).setFont(arialBoldItalic));
		    terceraCuerpo.add(new Text(" (" + montoTotalLetras + ")").setFont(arialBoldItalic));
		    terceraCuerpo.add(", que ");
		    terceraCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBoldItalic));
		    terceraCuerpo.add(" " + verboSeObliga + " a cancelar en dinero, íntegramente y por armadas, según el cronograma de la siguiente forma:");    
		    document.add(terceraCuerpo);

		    // --- 3.1 CUOTA INICIAL ---
		    String textoInicial = (contrato.getInicial().doubleValue() > 0) ? "US$." + df.format(contrato.getInicial()) : "Sin Cuota inicial.";
		    document.add(new Paragraph("3.1 " + textoInicial).setFont(arialItalic).setFontSize(11).setMarginLeft(40).setMarginTop(10));

		 // --- 3.2 SALDO Y AGRUPACIÓN DINÁMICA CORREGIDO ---
		    Paragraph subclausula32 = new Paragraph()
		            .setTextAlignment(TextAlignment.JUSTIFIED)
		            .setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f).setMarginLeft(40).setMarginTop(10);

		    subclausula32.add("3.2 El saldo del precio de ");
		    // Solo el monto numérico en negrita
		    subclausula32.add(new Text("US$." + df.format(contrato.getSaldo())).setFont(arialBoldItalic));
		    // El monto en letras en negrita
		    subclausula32.add(new Text(" (" + montoSaldoLetras + ")").setFont(arialBoldItalic));
		    subclausula32.add(", que será cancelado en ");

		    // Lógica de agrupación por montos
		    Map<BigDecimal, Integer> gruposMonto = new LinkedHashMap<>();
		    for (LetraResponseDTO letra : listaLetras) {
		        BigDecimal importe = letra.getImporte();
		        gruposMonto.put(importe, gruposMonto.getOrDefault(importe, 0) + 1);
		    }

		    // Resaltamos solo el número total de letras
		    subclausula32.add(new Text(totalLetras + "").setFont(arialBoldItalic));
		    subclausula32.add(" letras de cambio "); // Texto normal

		    if (gruposMonto.size() == 1) {
		        // CASO 1: Todas iguales - Solo resaltamos el monto
		        BigDecimal montoUnico = gruposMonto.keySet().iterator().next();
		        subclausula32.add("de ");
		        subclausula32.add(new Text("US$" + df.format(montoUnico)).setFont(arialBoldItalic));
		    } else {
		        // CASO 2: Variantes - Resaltamos cantidad y monto de cada grupo
		        subclausula32.add("(");
		        List<String> partesPrendidas = new ArrayList<>();
		        
		        int indexGrupo = 0;
		        for (Map.Entry<BigDecimal, Integer> entry : gruposMonto.entrySet()) {
		            BigDecimal monto = entry.getKey();
		            Integer cantidad = entry.getValue();
		            
		            // Añadimos la cantidad en negrita
		            subclausula32.add(new Text(cantidad + "").setFont(arialBoldItalic));
		            // Texto descriptivo normal
		            subclausula32.add(cantidad == 1 ? " letra de " : " letras de cambio de ");
		            // Monto en negrita
		            subclausula32.add(new Text("US$" + df.format(monto)).setFont(arialBoldItalic));
		            
		            // Manejo de comas y "y" entre grupos
		            if (indexGrupo < gruposMonto.size() - 2) {
		                subclausula32.add(", ");
		            } else if (indexGrupo == gruposMonto.size() - 2) {
		                subclausula32.add(" y ");
		            }
		            indexGrupo++;
		        }
		        subclausula32.add(")");
		    }

		    subclausula32.add(" debidamente aceptadas por ");
		    // Etiqueta de comprador en negrita (pero sin cursiva según imagen 1, o arialBoldItalic si prefieres mantener el estilo)
		    subclausula32.add(new Text("“" + etiquetaComprador + "”").setFont(arialBoldItalic));
		    subclausula32.add(", según el detalle siguiente:");

		    document.add(subclausula32);

		    // --- 3.3 DETALLE DE VENCIMIENTOS ---
		    Paragraph subclausula33 = new Paragraph()
		            .setFont(arialItalic).setFontSize(11)
		            .setMarginLeft(40).setMarginTop(8);

		    DateTimeFormatter formatoLindo = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		    subclausula33.add("3.3 Letra ");
		    subclausula33.add(new Text("No.01").setFont(arialBoldItalic));
		    subclausula33.add(" por ");
		    subclausula33.add(new Text("US$" + df.format(primeraLetra.getImporte())).setFont(arialBoldItalic));
		    subclausula33.add(" con vencimiento el día ");
		    subclausula33.add(new Text(primeraLetra.getFechaVencimiento().format(formatoLindo)).setFont(arialBoldItalic));

		    subclausula33.add(" y la última ");
		    subclausula33.add(new Text("Letra No." + totalLetras).setFont(arialBoldItalic));
		    subclausula33.add(" por ");
		    subclausula33.add(new Text("US$" + df.format(ultimaLetra.getImporte())).setFont(arialBoldItalic)); 
		    subclausula33.add(" con vencimiento el día ");
		    subclausula33.add(new Text(ultimaLetra.getFechaVencimiento().format(formatoLindo)).setFont(arialBoldItalic));
		    subclausula33.add(".");

		    document.add(subclausula33);

			// 4. Párrafo final de garantía y domicilio de pago
			Paragraph terceraFinal = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialItalic).setFontSize(11)
					.setMultipliedLeading(1.0f)
					.setMarginTop(10);

			terceraFinal.add("Así mismo a efectos de garantizar el cumplimiento de su obligación ");
			terceraFinal.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
			terceraFinal.add(" " + verboGira + " a favor de ");
			terceraFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			terceraFinal.add(" letras de cambio que se detallan en la cláusula tercera que serán cancelados en la fecha de vencimiento de los respectivos cambiales, más los correspondientes intereses en caso de mora. El lugar de pago de todas las armadas será el domicilio de ");
			terceraFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			terceraFinal.add(".");

			document.add(terceraFinal);

		}

		/* =========================================================
		 * PAGINA 2: CLAUSULA CUARTA - EQUIVALENCIA
		 * ========================================================= */
		//salto de pagina solo si el contenido de la pagina supera el 50%
		verificarEspacioYSalto(document, pdf, 0.4f);
		
		// 1. Título de la Cláusula (Negrita, Cursiva y Subrayado)
		document.add(new Paragraph()
				.add(new Text("CUARTA: EQUIVALENCIA:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo de la cláusula (Todo en Cursiva)
		Paragraph cuartaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) // Arial 11 Cursiva
				.setFontSize(11)
				.setMultipliedLeading(1.0f); // Interlineado 1.0 

		cuartaCuerpo.add("Las partes contratantes declaran que entre el lote vendido y el precio pactado existe una justa y perfecta equivalencia y que si hubiera alguna diferencia de más o menos en el área de terreno al momento de entrega del referido bien se pagará el reintegro o devolución al precio actualizado. ");
		cuartaCuerpo.add("Así mismo, queda establecido entre las partes contratantes que el precio pactado en este contrato es solamente por la venta del terreno rustico en el que no están incluidos las obras de habilitación urbana, ni impuestos de ley, etc.");

		document.add(cuartaCuerpo);


		/* =================================================================
		 * PAGINA 3: CLAUSULA QUINTA: INTERESES COMPENSATORIOS Y MORATORIOS:
		 * ================================================================= */

		// 1. Título de la Cláusula
		document.add(new Paragraph()
				.add(new Text("QUINTA: INTERESES COMPENSATORIOS Y MORATORIOS:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo de la cláusula (Interlineado 1.0f)
		Paragraph quintaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f); // 👈 Interlineado 1.0 aplicado

		quintaCuerpo.add("Las partes contratantes, de común acuerdo establecen de que en caso de incumplimiento del pago establecido en la cláusula tercera del presente contrato, en las fechas programadas estas generaran un ");

		// Texto subrayado 1
		Text sub1 = new Text("interés compensatorio y moratorio mensual que es del 5% del valor de la letra vencida o impagada más 1 dólar diario hasta la cancelación de la misma")
				.setUnderline();
		quintaCuerpo.add(sub1);

		quintaCuerpo.add(", el mismo que se deberá de ser pagada de manera mensual, ");

		// Texto subrayado 2
		Text sub2 = new Text("sin perjuicio de ello el incumplimiento del pago de tres letras consecutivas o alternadas")
				.setUnderline();
		quintaCuerpo.add(sub2);

		quintaCuerpo.add(", facultara a ”");
		quintaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		quintaCuerpo.add("”, a dar por vencidas todas las letras pactadas y exigir el cumplimiento de todas las letras pendientes de pago o por resolver el presente contrato.");

		document.add(quintaCuerpo);

		/* =================================================================
		 * PAGINA 3: CLAUSULA SEXTA: PACTO DE RESERVA DE PROPIEDAD 
		 * ================================================================= */
		// 1. Título de la Cláusula (Negrita, Cursiva y Subrayado)
		document.add(new Paragraph()
				.add(new Text("SEXTA: PACTO DE RESERVA DE PROPIEDAD").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo de la cláusula (Interlineado 1.0f)
		Paragraph sextaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f); // Interlineado 1.0 aplicadoS

		sextaCuerpo.add("Las partes contratantes acuerdan al amparo de lo dispuesto por del artículo 1583° del Código Civil incorporar en el presente contrato el pacto de reserva de propiedad a favor de ");
		sextaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		sextaCuerpo.add(". En consecuencia, este conservara la propiedad del bien materia del presente contrato aun cuando la posesión del mismo haya sido entregada a ");
		sextaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));     
		sextaCuerpo.add(". Así mismo se deja establecido que el pacto de reserva de propiedad a quien se refiere la cláusula anterior, tendrá vigencia hasta que ");
		sextaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		sextaCuerpo.add(" " + verboCumpla + " con pagar la totalidad del precio pactado en la cláusula tercera de este contrato, más los intereses devengados.");

		document.add(sextaCuerpo);

		/* =================================================================
		 * PAGINA 3: CLAUSULA SEPTIMA: OBLIGACIONES DE LOS COMPRADORES
		 * ================================================================= */

		document.add(new Paragraph()
				.add(new Text("SEPTIMA: OBLIGACIONES DE LOS COMPRADORES").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		Paragraph septimaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		septimaIntro.add("Queda establecido que así ");
		septimaIntro.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		// 🔹 CAMBIO: verboTenga (tenga / tengan)
		septimaIntro.add(" " + verboTenga + " cancelado el ");
		septimaIntro.add(new Text("50%").setFont(arialBoldItalic));
		septimaIntro.add(" del valor del predio objeto del presente contrato y/o ");
		// 🔹 CAMBIO: verboDeje (deje / dejen)
		septimaIntro.add(new Text(verboDeje + " de pagar dos (02) letras consecutivas o alternadas").setUnderline());
		septimaIntro.add(", ");
		septimaIntro.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		septimaIntro.add(" quedara facultada para ejercer las acciones legales correspondientes haciendo valer su derecho, siendo estas las siguientes:");

		document.add(septimaIntro);

		com.itextpdf.layout.element.List listaObligaciones = new com.itextpdf.layout.element.List()
				.setListSymbol("• ") 
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginLeft(20)
				.setMarginTop(5);

		// --- PRIMER PUNTO ---
		ListItem item1 = new ListItem();
		Paragraph p1 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p1.add("Según lo dispuesto por el artículo 1561° del código civil pedir la resolución del contrato para cuyo efecto se remitirá a ");
		p1.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		p1.add(" una carta notarial en tal sentido. Se procederá a descontar el ");
		p1.add(new Text("30% del precio total").setFont(arialBoldItalic));
		p1.add(" a favor de ");
		p1.add(new Text("LA VENDEDORA,").setFont(arialBoldItalic));
		p1.add(" por concepto de indemnización de daños y perjuicios, lucro cesante y otros según el artículo 1563° del código civil.");
		item1.add(p1); 
		listaObligaciones.add(item1); 

		// --- SEGUNDO PUNTO ---
		ListItem item2 = new ListItem();
		Paragraph p2 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p2.add("Así mismo la devolución del saldo restante de lo recibido se hará cuando el lote sea vendido y conforme a lo aportado. En caso de resolución del contrato quedara en beneficio de ");
		p2.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		p2.add(" sin desembolso alguno para ella las mejoras introducidas en el inmueble o lote, sin obligación de reembolso de ninguna clase por parte de esta ni el pago de mejoras por acuerdo libre de ambas partes. Estipulándose así mismo que durante todo el tiempo que demore la devolución de dicho inmueble ");
		p2.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		// 🔹 CAMBIO: verboDebera (deberá / deberán)
		p2.add(" " + verboDebera + " pagar mensualmente a la vendedora el ");
		p2.add(new Text("3% del precio total").setFont(arialBoldItalic));
		p2.add(" estipulado en calidad de merced conductiva; incrementándose esta en un 50% cada año que venza, hasta su desocupación total.");
		item2.add(p2);
		listaObligaciones.add(item2); 

		// --- TERCER PUNTO ---
		ListItem item3 = new ListItem();
		Paragraph p3 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p3.add("En caso de resolución de contrato por motivos precedentes ");
		p3.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		// 🔹 CAMBIO: verboDesea (desea / desean)
		p3.add(", " + verboDesea + " continuar conservando el predio objeto del presente contrato, se restaurará al precio y/o valor por metro cuadra del momento en que se actualice el contrato.");
		item3.add(p3);
		listaObligaciones.add(item3); 

		document.add(listaObligaciones);

		/* =================================================================
		 * PAGINA 3: CLAUSULA OCTAVA: RENUNCIA
		 * ================================================================= */

		// 1. Título de la Cláusula (Arial 11, Negrita, Cursiva y Subrayado)
		document.add(new Paragraph()
				.add(new Text("OCTAVA: RENUNCIA").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Párrafo Introductorio
		Paragraph octavaIntro = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		octavaIntro.add("Por tratarse de un contrato de compra venta con pago por armadas las partes conviene que ");
		octavaIntro.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		octavaIntro.add(" " + verboPuede + " solicitar a ");
		octavaIntro.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		octavaIntro.add(" la renuncia, justificando la razón o circunstancias; debiendo cumplir las siguientes condiciones:");

		document.add(octavaIntro);

		// 3. Definición de la Lista con Letras (a, b, c, d)
		com.itextpdf.layout.element.List listaRenuncia = new com.itextpdf.layout.element.List(com.itextpdf.layout.properties.ListNumberingType.ENGLISH_LOWER)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginLeft(40)
				.setMarginTop(5);

		// --- APARTADO a. ---
		com.itextpdf.layout.element.ListItem itemA = new com.itextpdf.layout.element.ListItem();
		Paragraph pa = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		pa.add("la renuncia debe ser presentada por escrito, siendo necesario para que produzca sus efectos de aprobación por parte de ");
		pa.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		pa.add(" de la solicitud presentada por ");
		pa.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pa.add(" cuando los motivos invocados justifiquen dicha renuncia para sus efectos legales.");
		itemA.add(pa);
		listaRenuncia.add(itemA);

		// --- APARTADO b. ---
		com.itextpdf.layout.element.ListItem itemB = new com.itextpdf.layout.element.ListItem();
		Paragraph pb = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		pb.add("De ser aprobada la renuncia presentada, ");
		pb.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		pb.add(" deberá retener el ");
		pb.add(new Text("30% de valor total del terreno").setFont(arialBoldItalic));
		pb.add(" pactado en el presente contrato, más los gastos de cobranza, intereses, devengados, comisiones, impuestos y cualquier otro concepto derivado de la compra venta y su resolución.");
		itemB.add(pb);
		listaRenuncia.add(itemB);

		// --- APARTADO c. ---
		com.itextpdf.layout.element.ListItem itemC = new com.itextpdf.layout.element.ListItem();
		Paragraph pc = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		pc.add("La devolución del dinero abonado por ");
		pc.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pc.add(" se realizará cuando el lote materia de renuncia y/o resolución sea vendido y conforme a lo aportado.");

		itemC.add(pc);
		listaRenuncia.add(itemC);

		// --- APARTADO d. ---
		com.itextpdf.layout.element.ListItem itemD = new com.itextpdf.layout.element.ListItem();
		Paragraph pd = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		pd.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pd.add(" " + verboCompromete + " a respetar exactamente las medidas perimétricas de su lote, materia de este contrato. En caso contrario es el único responsable de los daños y perjuicios que se puede ocasionar a sus vecinos colindantes. Asimismo, queda convenido que ");
		pd.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pd.add(" para construir su vivienda " + verboDebera + " realizarla conforme a los planos elaborados por profesionales competentes, en consecuencia, queda ");
		pd.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		pd.add(" liberada de errores, fallas y demás anomalías que puedan presentarse en la construcción de sus viviendas y su correspondiente aprobación por el municipio.");
		itemD.add(pd);
		listaRenuncia.add(itemD);

		document.add(listaRenuncia);

		/* =================================================================
		 * PAGINA 4: CLAUSULA NOVENA: OBLIGACIONES DE LA VENDEDORA
		 * ================================================================= */

		// 1. Título de la Cláusula (Arial 11, Negrita, Cursiva y Subrayado)
		document.add(new Paragraph()
				.add(new Text("NOVENA: OBLIGACIONES DE LA VENDEDORA").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo de la cláusula
		Paragraph novenaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f); // Mantenemos interlineado simple según la imagen

		novenaCuerpo.add(new Text("LA VENDEDORA ").setFont(arialBoldItalic));
		novenaCuerpo.add("se obliga a entregar el bien objeto del presente contrato, cuando a su vez ");
		novenaCuerpo.add(new Text(etiquetaComprador + " ").setFont(arialBoldItalic));
		novenaCuerpo.add(" " + verboHaya + " cancelado el íntegro del saldo deudor especificado en la cláusula tercera.");

		document.add(novenaCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA: ENTREGA DEL BIEN OBJETO DEL PRESENTE CONTRATO
		 * ================================================================= */
		//salto de pagina solo si el contenido de la pagina supera el 50%
		verificarEspacioYSalto(document, pdf, 0.4f);
				
		// 1. Título de la Cláusula
		document.add(new Paragraph()
				.add(new Text("DECIMA: ENTREGA DEL BIEN OBJETO DEL PRESENTE CONTRATO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Cuerpo de la cláusula como un solo párrafo continuo
		Paragraph decimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		decimaCuerpo.add(new Text(etiquetaComprador + " ").setFont(arialBoldItalic));
		decimaCuerpo.add(verboDeclara + " que la entrega física y real del predio objeto del presente contrato se realizara a la suscripción y legalización del presente contrato, por lo que a partir de ello el cuidado, administración y conservación del bien lo asume ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		decimaCuerpo.add(". Así mismo ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		decimaCuerpo.add(", "+ verboDeclara +" conocer la situación física, real y legal del predio objeto de transferencia, el mismo que lo encuentra a su entera satisfacción, por tanto, renuncia a toda acción rescisoria por dolo, error, lesión y cualquiera que tienda a invalidar el presente contrato.");

		document.add(decimaCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA PRIMERA: GASTOS Y TRIBUTOS
		 * ================================================================= */
		document.add(new Paragraph()
				.add(new Text("DECIMA PRIMERA: GASTOS Y TRIBUTOS").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		Paragraph undecimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		undecimaCuerpo.add("Así mismo las partes contratantes establecen de mutuo acuerdo que todos los gastos que origine la formalización del presente contrato serán asumidos por ");
		// 🔹 CAMBIO AQUÍ: Usamos etiquetaComprador (EL COMPRADOR / LOS COMPRADORES)
		undecimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic)); 
		undecimaCuerpo.add(", incluyendo el impuesto de alcabala si estuviera afecto.");
		document.add(undecimaCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA SEGUNDA: GRAVAMEN
		 * ================================================================= */
		document.add(new Paragraph()
				.add(new Text("DECIMA SEGUNDA: GRAVAMEN").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		Paragraph duodecimaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		duodecimaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		duodecimaCuerpo.add(", declaran que sobre el inmueble materia del presente contrato, no pesa ninguna carga, ni hipoteca, gravamen, embargo, ni ninguna medida judicial o extrajudicial, que pudiera limitar o restringir su derecho de libre disposición, obligándose en todo caso al saneamiento por evicción.");
		document.add(duodecimaCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA TERCERA: ESCRITURA
		 * ================================================================= */
		document.add(new Paragraph()
				.add(new Text("DECIMA TERCERA: ESCRITURA").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		Paragraph decimaTerceraCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		decimaTerceraCuerpo.add("Queda establecido que el presente contrato será elevado a Escritura Pública una vez que ");
		decimaTerceraCuerpo.add(new Text("LA VENDEDORA ").setFont(arialBoldItalic));
		decimaTerceraCuerpo.add("haya concluido con todos los trámites pertinentes a la formalización de su derecho de propiedad, siempre y cuando que ");
		decimaTerceraCuerpo.add(new Text(etiquetaComprador + " ").setFont(arialBoldItalic));
		decimaTerceraCuerpo.add("no "+verboAdeude+" suma alguna, ello toda vez que las partes declaran conocer el hecho que el presente contrato es privado y quedara sujeto al cumplimiento de los acuerdos establecidos en cada una de las cláusulas de este instrumento legal, para que opere la traslación de dominio; obligándose la compradora a no condicionar el cumplimiento de su obligación ante la eventual demora que pudiera resultar de la tramitación del mismo por parte de las autoridades competentes, gestión que se encuentra en proceso; Las partes contratantes de mutuo acuerdo se compromete a firmar una Minuta y la correspondiente Escritura Pública de compraventa cuando estas condiciones se materialicen, respetándose el integro de los pactos contenidos en el presente contrato.");
		document.add(decimaTerceraCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA CUARTA: COMPETENCIA TERRITORIAL Y DOMICILIO
		 * ================================================================= */
		document.add(new Paragraph()
				.add(new Text("DECIMA CUARTA: COMPETENCIA TERRITORIAL Y DOMICILIO:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		Paragraph decimaCuartaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		decimaCuartaCuerpo.add("Las partes contratantes dejan expresa constancia que para efectos de cualquier controversia y/o para todas las acciones legales que se deriven de la celebración y ejecución del presente contrato se someten exclusivamente a la jurisdicción de los Jueces y Tribunales de la Corte Superior de Justicia de Lima Norte, y señalan como sus domicilios los indicados en la introducción del presente contrato, donde se les hará llegar las notificaciones a que hubiera lugar, renunciando expresamente a la ley de domicilio y al fuero del mismo nombre.");
		document.add(decimaCuartaCuerpo);


		/* =================================================================
		 * PÁGINA FINAL: CLÁUSULA DÉCIMA QUINTA Y CIERRE DEL CONTRATO
		 * ================================================================= */
		//salto de pagina solo si el contenido de la pagina supera el 50%
		verificarEspacioYSalto(document, pdf, 0.4f);

		// 1. Título de la Cláusula Décima Quinta
		document.add(new Paragraph()
				.add(new Text("DECIMA QUINTA: APLICACIÓN SUPLETORIA DE LA LEY:").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Bloque único de texto (Cláusula 15 + Aceptación + Fecha)
		Paragraph cierreFinal = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		// Cuerpo de la Décima Quinta
		cierreFinal.add("En todo lo no previsto por las partes en este contrato, se aplicará supletoriamente las normas del Código Civil y demás dispositivos legales del sistema jurídico que resulten aplicables al presente contrato. ");

		// Párrafo de Aceptación
		cierreFinal.add("Las partes contratantes declaran aceptar todas y cada una de las cláusulas contenidas en el presente contrato, expresando que suscriben la misma bajo libre expresión de su voluntad, no habiendo mediado presión, dolo, violencia u otro medio ilícito análogo, renunciando a cualquier acción legal ulterior destinado a enervar los efectos legales del presente contrato. ");

		// --- REEMPLAZA EL BLOQUE DE FECHA AL FINAL DEL CONTRATO POR ESTE ---
		// --- CIERRE DINÁMICO DEL CONTRATO (Versión limpia) ---
		BigDecimal diaLetrasBD = BigDecimal.valueOf(fechaRegistro.getDayOfMonth());
		String diaLetras = NumeroALetras.convertir(diaLetrasBD).split(" CON ")[0].trim().toLowerCase(); 

		BigDecimal anioBigDecimal = BigDecimal.valueOf(anioNum);
		// 1. Obtenemos el año en minúsculas primero
		String anioBase = NumeroALetras.convertir(anioBigDecimal).split(" CON ")[0].trim().toLowerCase(); 

		// 2. Convertimos a formato Título (Primeras letras Mayúsculas)
		StringBuilder formatAnio = new StringBuilder();
		for (String palabra : anioBase.split(" ")) {
		    if (palabra.length() > 0) {
		        formatAnio.append(Character.toUpperCase(palabra.charAt(0)))
		                  .append(palabra.substring(1)).append(" ");
		    }
		}
		String anioLetrasFinal = formatAnio.toString().trim();

		cierreFinal.add("Leído el presente contrato y estando las partes contratantes conformes con las cláusulas establecidas en el presente contrato, proceden a suscribirlo ");

		// 3. Añadimos el texto usando arialItalic (que no es negrita) para que coincida con la imagen 1
		cierreFinal.add(new Text("a los " + diaLetras + " (" + diaNum + ") días del mes de " + mesNombre + " (" + mesNum + ") del año " + anioLetrasFinal + " (" + anioNum + ").")
		        .setFont(arialItalic));

		document.add(cierreFinal);

		/* ==================================================================================
		 * FIRMAS DEL CONTRATO
		 * ==================================================================================*/
		/* ==================================================================================
		 * FIRMAS DEL CONTRATO
		 * ==================================================================================*/
		// 1. Crea un párrafo vacío que solo sirva de margen/espacio
		document.add(new Paragraph("").setMarginTop(20f));

		// 1. Firmas al final del contrato (Página 1 o la que corresponda)
		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		// ========================================================================================
		// DOCUMENTO DE SEÑALIZACIÓN (PÁGINA APARTE) - SOLO INTRODUCCIÓN
		// ========================================================================================

		// 1. Salto de página para iniciar el nuevo documento
		document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		
		document.setMargins(122, 85, 30, 85);

		// 2. Título: Arial 12, Negrita, Cursiva y Subrayado
		document.add(new Paragraph()
				.add(new Text("DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO")
						.setFont(arialBoldItalic)
						.setUnderline())
				.setFontSize(12)
				.setTextAlignment(TextAlignment.CENTER)
				.setMarginBottom(20));

		// 3. Introducción del Documento
		Paragraph introPosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) // Fuente base cursiva según tu imagen
				.setFontSize(11)
				.setMultipliedLeading(1.2f);

		introPosesion.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico con Reserva de Propiedad que celebran de una parte ");

		// Bloque de LA VENDEDORA en Negrita
		introPosesion.add(new Text("“INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.” ").setFont(arialBold));
		introPosesion.add("con RUC No. ");
		introPosesion.add(new Text("20537853108 ").setFont(arialBold));
		introPosesion.add("con domicilio Av. Alfredo Mendiola N°3623-tercer piso – Ofc. 301 Urb. Panamericana Norte, Distrito de Los Olivos, Provincia y Departamento de Lima, debidamente representado por su ");
		introPosesion.add(new Text("Gerente General OLMEDO SILVA LOPEZ ").setFont(arialBold));
		introPosesion.add("con ");
		introPosesion.add(new Text("DNI No.19404451 ").setFont(arialBold));
		introPosesion.add("según consta del poder inscrito en la partida electrónica No. ");
		introPosesion.add(new Text("12561792 ").setFont(arialBold));
		introPosesion.add("del Registro de Personas Jurídicas, a quien en adelante se le denominara ");
		introPosesion.add(new Text("LA VENDEDORA; ").setFont(arialBold));

		introPosesion.add("y de la otra parte ");

		// 4. Bloque dinámico de COMPRADORES en Negrita con lógica de Estado Civil
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
			boolean esFem = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));

			String pref = esFem ? "la Sra. " : "el Sr. ";
			String ident = esFem ? "identificada" : "identificado";
			String nacion = extraerNacionalidad(c.getCelular(), esFem);

			// 🔹 LÓGICA DINÁMICA DE ESTADO CIVIL (Corregido)
			String estCivTexto = "";
			if (c.getEstadoCivil() != null) {
				estCivTexto = c.getEstadoCivil().toString().toLowerCase();
				// Ajuste de género: soltero -> soltera / casado -> casada
				if (esFem) {
					if (estCivTexto.equals("soltero")) estCivTexto = "soltera";
					else if (estCivTexto.equals("casado")) estCivTexto = "casada";
					else if (estCivTexto.equals("viudo")) estCivTexto = "viuda";
				}
			} else {
				estCivTexto = esFem ? "soltera" : "soltero"; // Respaldo
			}

			introPosesion.add(pref);
			// Nombre en Negrita
			introPosesion.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setFont(arialBold));

			// Resultado: ", peruana, soltera, identificada con DNI..."
			introPosesion.add(", " + nacion + ", " + estCivTexto + ", " + ident + " con ");

			// DNI en Negrita
			introPosesion.add(new Text("DNI N°" + c.getNumDoc()).setFont(arialBold));

			// Separador inteligente entre compradores
			if (numClientes > 1 && i < numClientes - 1) {
				introPosesion.add(i == numClientes - 2 ? " y " : ", ");
			}
		}

		introPosesion.add(", " + etiquetaDomicilio + direccionRealParaContrato);
		introPosesion.add(", a quien en adelante se les denominará ");
		// Denominación final en Negrita
		introPosesion.add(new Text(etiquetaComprador).setFont(arialBold));
		introPosesion.add(" en los términos y condiciones de las cláusulas siguientes:");

		document.add(introPosesion);

		/* ===========================================================================================  
		 * CLAUSULA PRIMERA: DETALLE DEL LOTE (DINÁMICO)
		==============================================================================================*/

		// 1. Lógica de agrupación y cálculo de área (Se mantiene intacta)
		BigDecimal areaTotalPosesion = BigDecimal.ZERO; 
		List<LoteResponseDTO> listaLotesPosesion = contrato.getLotes();
		for (LoteResponseDTO l : listaLotesPosesion) { areaTotalPosesion = areaTotalPosesion.add(l.getArea()); }

		Paragraph primeroCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginTop(10);

		primeroCuerpo.add(new Text("PRIMERO.").setFont(arialBold).setUnderline());
		primeroCuerpo.add(" - ");
		primeroCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		primeroCuerpo.add(" en virtud del presente contrato de Compra - Venta celebrado con ");
		primeroCuerpo.add(new Text("\"" + etiquetaComprador + "\"").setFont(arialBold));

		DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		primeroCuerpo.add(" con fecha ");
		primeroCuerpo.add(new Text(fechaRegistro.format(fmtFecha)).setFont(arialBold));
		primeroCuerpo.add(" dio en venta real ");

		// 🔹 INICIO DE CONSTRUCCIÓN DINÁMICA CON NEGRITAS (Reemplaza al ubicacionPosesion.toString())
		if (listaLotesPosesion.size() == 1) {
			LoteResponseDTO single = listaLotesPosesion.get(0);
			primeroCuerpo.add("un lote de terreno rústico de ");
			primeroCuerpo.add(new Text(single.getArea() + "M2").setFont(arialBold)); // 🔹 Área Negrita
			primeroCuerpo.add(". El mismo que se ubica en la Manzana “");
			primeroCuerpo.add(new Text(single.getManzana()).setFont(arialBold));      // 🔹 Manzana Negrita
			primeroCuerpo.add("” y se encuentra signado con el lote Nº ");
			primeroCuerpo.add(new Text(single.getNumeroLote()).setFont(arialBold));  // 🔹 Lote Negrita
		} else {
			boolean mismaMz = listaLotesPosesion.stream().map(LoteResponseDTO::getManzana).distinct().count() == 1;
			primeroCuerpo.add("lotes de terreno rústico ubicados ");
			if (mismaMz) {
				primeroCuerpo.add("en la Manzana “");
				primeroCuerpo.add(new Text(listaLotesPosesion.get(0).getManzana()).setFont(arialBold)); // 🔹 Manzana Negrita
				primeroCuerpo.add("” y asignados con los lotes ");
				for (int i = 0; i < listaLotesPosesion.size(); i++) {
					primeroCuerpo.add("Nº ");
					primeroCuerpo.add(new Text(listaLotesPosesion.get(i).getNumeroLote()).setFont(arialBold)); // 🔹 Lote Negrita
					if (i < listaLotesPosesion.size() - 1) primeroCuerpo.add(" y ");
				}
			} else {
				for (int i = 0; i < listaLotesPosesion.size(); i++) {
					primeroCuerpo.add("en la Manzana “");
					primeroCuerpo.add(new Text(listaLotesPosesion.get(i).getManzana()).setFont(arialBold)); // 🔹 Manzana Negrita
					primeroCuerpo.add("” lote ");
					primeroCuerpo.add(new Text(listaLotesPosesion.get(i).getNumeroLote()).setFont(arialBold)); // 🔹 Lote Negrita
					if (i < listaLotesPosesion.size() - 1) primeroCuerpo.add(" y ");
				}
			}
		}

		primeroCuerpo.add(" correspondiente al Programa de Vivienda ");
		primeroCuerpo.add(new Text("“LA FLORIDA DE TORRE BLANCA”").setFont(arialBold));
		primeroCuerpo.add(" del Distrito de Carabayllo, Provincia y Departamento de Lima; cuyos linderos y medidas perimétricas son las siguientes:");

		document.add(primeroCuerpo);

		// --- 2. BUCLE DINÁMICO DE LINDEROS ---
		for (LoteResponseDTO loteItem : listaLotesPosesion) {

			// Solo muestra el encabezado MZ/LT si hay más de un lote
			if (listaLotesPosesion.size() > 1) {
				document.add(new Paragraph()
						.add(new Text("MZ. " + loteItem.getManzana() + " LT." + loteItem.getNumeroLote() + " ÀREA:" + loteItem.getArea() + "M2").setFont(arialBoldItalic))
						.setFontSize(11).setMarginTop(10).setMarginBottom(2));
			}

			float[] colWidthsLinderos = {120f, 200f, 100f}; 
			Table tablaPosesionLinderos = new Table(colWidthsLinderos)
					.setMarginLeft(20).setMarginTop(2).setMarginBottom(5)
					.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

			agregarFilaLinderos(tablaPosesionLinderos, "Por el frente", loteItem.getColindanteNorte(), "Con    " + loteItem.getAncho1() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaPosesionLinderos, "Por la derecha", loteItem.getColindanteEste(), "Con  " + loteItem.getLargo1() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaPosesionLinderos, "Por la Izquierda", loteItem.getColindanteOeste(), "Con    " + loteItem.getLargo2() + "  m.l.", arialItalic);
			agregarFilaLinderos(tablaPosesionLinderos, "Por el fondo", loteItem.getColindanteSur(), "Con    " + loteItem.getAncho2() + "  m.l.", arialItalic);

			document.add(tablaPosesionLinderos);
		}

		// --- 3. PÁRRAFO FINAL: UBICACIÓN MAYOR ---
		Paragraph parrafoFinalPosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.1f);

		parrafoFinalPosesion.add("Dicho lote se encuentra ubicado en el predio denominado lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
		parrafoFinalPosesion.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima.").setFont(arialItalic));

		document.add(parrafoFinalPosesion);

		/* ===========================================================================================  
         				 CLAUSULA SEGUNDA: DEL DOCUMENTO DE SEÑALIZACION
         ==============================================================================================*/
		//salto de pagina solo si el contenido de la pagina supera el 50%
		Paragraph segundoCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) // Fuente base cursiva
				.setFontSize(11)      // Tamaño de letra 11 solicitado
				.setMarginTop(10)	    // Espacio respecto a la tabla anterior
				.setMultipliedLeading(1.0f)  //epacio interleniado
				.setKeepTogether(true);

		// Título: SEGUNDO en Negrita, Cursiva y Subrayado
		segundoCuerpo.add(new Text("SEGUNDO.").setFont(arialBoldItalic).setUnderline());
		segundoCuerpo.add(" - ");

		// Denominación de la Vendedora en Negrita
		segundoCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		segundoCuerpo.add(" mediante el presente documento, da en posesión efectiva a ");

		// Denominación de los Compradores en Negrita
		segundoCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBold));
		segundoCuerpo.add(" el lote de terreno señalado en la cláusula anterior, quien declara haberlo recepcionado a su entera y completa satisfacción.");

		document.add(segundoCuerpo);

		/* ===========================================================================================  
			CLAUSULA TERCERA: RECONOCIMIENTO DE DEUDA E HIPOTECA 
		==============================================================================================*/  
		//--- LÓGICA DE CÁLCULO DINÁMICO ---
		BigDecimal saldoParaClausula;
		int cantidadLetrasParaClausula;

		//Obtenemos los valores base del contrato
		BigDecimal saldoContrato = contrato.getSaldo();
		BigDecimal inicialContrato = contrato.getInicial();
		int totalLetras = contrato.getCantidadLetras();

		//Lógica de validación: ¿Paga la primera letra hoy? (Inicial = 0)
		if (inicialContrato == null || inicialContrato.compareTo(BigDecimal.ZERO) == 0) {
			// Si no hay inicial, se asume que pagó la 1ra letra hoy.
			// Calculamos el monto de una letra promedio (Saldo / TotalLetras)
			BigDecimal montoUnaLetra = saldoContrato.divide(new BigDecimal(totalLetras), 2, RoundingMode.HALF_UP);

			// El reconocimiento de deuda es por el saldo menos la letra ya pagada
			saldoParaClausula = saldoContrato.subtract(montoUnaLetra);
			cantidadLetrasParaClausula = totalLetras - 1;
		} else {
			// Si hubo inicial, el reconocimiento es por el saldo total y todas las letras
			saldoParaClausula = saldoContrato;
			cantidadLetrasParaClausula = totalLetras;
		}

		//Convertimos el nuevo saldo y cantidad a letras para el documento
		String saldoTextoClausula = NumeroALetras.convertir(saldoParaClausula);
		BigDecimal cantLetrasBD = BigDecimal.valueOf(cantidadLetrasParaClausula);
		String letrasEnTextoClausula = NumeroALetras.convertir(cantLetrasBD).split(" CON ")[0];

		//--- CONSTRUCCIÓN DEL PÁRRAFO ---
		Paragraph terceroCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginTop(10)
				.setMultipliedLeading(1.2f)
				.setKeepTogether(true);


		terceroCuerpo.add(new Text("TERCERO.").setFont(arialBoldItalic).setUnderline());
		terceroCuerpo.add(" - ");

		terceroCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBold));
		terceroCuerpo.add(" " + verboReconoce + " encontrarse adeudando a ");
		terceroCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		terceroCuerpo.add(" a la fecha de la entrega y toma de posesión del terreno, la cantidad de ");

		//Monto calculado (Saldo Real Adeudado)
		terceroCuerpo.add(new Text("US$." + df.format(saldoParaClausula)).setFont(arialBold));
		terceroCuerpo.add(new Text(" (" + saldoTextoClausula + ")").setFont(arialBold));

		terceroCuerpo.add(" representado por ");

		//Cantidad calculada (Total letras - 1 si aplica)
		terceroCuerpo.add(new Text(cantidadLetrasParaClausula + " (" + letrasEnTextoClausula + ")").setFont(arialBold));
		terceroCuerpo.add(" letras de cambio aceptadas e Impagadas por cuyo saldo de precio y posibles costas de juicio, faculta expresamente otorgar garantía real hipotecaria a favor de los propietarios del inmueble que se viene adquiriendo, consintiendo en consecuencia y constituyendo ");

		terceroCuerpo.add(new Text("primera y preferencia Hipoteca ").setFont(arialBold));
		terceroCuerpo.add("a favor de ");
		terceroCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		terceroCuerpo.add(", hasta por la suma de ");

		//Repetición del monto calculado para la Hipoteca
		terceroCuerpo.add(new Text("US$." + df.format(saldoParaClausula)).setFont(arialBold));
		terceroCuerpo.add(new Text(" (" + saldoTextoClausula + ")").setFont(arialBold));

		terceroCuerpo.add(" conforme a los Artículos 1118 y 1119 Código Civil Vigente, la presente hipoteca se hace extensiva a todas sus partes integrantes, accesorias y anexos, y a todo cuanto en el futuro se edifique, instale o implante sobre el inmueble materia de la presente, conforme lo faculta el artículo 1101 del Código Civil Vigente.");

		document.add(terceroCuerpo);

		/* ===========================================================================================  
			CLAUSULA CUARTA: DEL DOCUMENTO DE SEÑALIZACION 
		============================================================================================*/
		Paragraph cuartoCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginTop(10)
				.setMultipliedLeading(1.0f);


		cuartoCuerpo.add(new Text("CUARTO.").setFont(arialBoldItalic).setUnderline());
		cuartoCuerpo.add(" - Queda expresamente convenido que ");
		cuartoCuerpo.add(new Text("\"LA VENDEDORA\"").setFont(arialBold));
		cuartoCuerpo.add(" se reserva la Propiedad hasta que ");
		cuartoCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBold));
		cuartoCuerpo.add(" "+verboCancele+" las cuotas estipuladas en la cláusula Tercera del Contrato de Compra-Venta, conforme a lo dispuesto por el artículo 1583 del Código Civil, no pudiendo en consecuencia, gravar, vender ni afectar en forma alguna el lote de terreno materia de la presente venta, mientras no consolide su derecho de propiedad con el pago total del precio de venta, salvo con autorización e intervención expresa de la ");
		cuartoCuerpo.add(new Text("“PROPIETARIA”").setFont(arialBold));
		cuartoCuerpo.add(" o ");
		cuartoCuerpo.add(new Text("\"VENDEDORA.").setFont(arialBold));

		document.add(cuartoCuerpo);

		/* ===========================================================================================  
			CLAUSULA QUINTA: DEL DOCUMENTO DE SEÑALIZACION 
		============================================================================================*/
		Paragraph quintoCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginTop(15)
				.setMultipliedLeading(1.0f);


		quintoCuerpo.add(new Text("QUINTO.").setFont(arialBoldItalic).setUnderline());
		quintoCuerpo.add(" - Queda entendido que al entrar ");
		quintoCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBold));
		quintoCuerpo.add(" en posesión del terreno signado en la fecha es de su responsabilidad absoluta a partir de la misma, el pago de impuestos, haciéndose cargo también de la defensa de su posesión frente a terceros por los medios permitidos por ley, respetando estrictamente sus linderos, medidas perimétricas y los derechos de posesión y de propiedad de sus vecinos, así como los estipulados por el Reglamento Nacional de Construcciones. Así mismo se compromete también a pagar proporcionalmente los gastos efectuados y por efectuarse respecto a las obras de urbanización como son: Obras de agua, Desagüe, Electrificación y otras que puedan introducirse en bien del Programa de vivienda.");

		document.add(quintoCuerpo);

		/* ===========================================================================================  
		 * PARRAFO DE CIERRE: DEL DOCUMENTO DE SEÑALIZACION 
		 * ===============================================================================================*/
		Paragraph cierrePosesion = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMarginTop(20)
				.setMultipliedLeading(1.0f);

		cierrePosesion.add("Conformes ambas partes con el contenido del presente documento, lo firman por duplicado a los ");

		// 🔹 CORRECCIÓN FINAL: Usamos las variables globales diaNum, mesNombre y anioNum
		// Se quita el setBold() para que sea texto normal como pediste en el contrato
		String fechaTextoCierre = diaNum + " días del mes de " + mesNombre.toLowerCase() + " del año " + anioNum + ".";
		cierrePosesion.add(new Text(fechaTextoCierre).setFont(arialItalic));

		document.add(cierrePosesion);

		// 4. Agregar las firmas en la hoja de posesión
		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		// 5. FINALIZAR DOCUMENTO Y RETORNAR BYTES
		document.close();
		return out.toByteArray(); // 👈 ESTO ES LO QUE EL COMPILADOR TE PIDE

	} 

	/* ==================================================================================
	 * METODOS AUXILIARES
	 * ==================================================================================*/

	// 🔹 Versión para el CONTRATO (5 parámetros)
	private static void agregarFilaLinderos(Table tabla, String etiqueta, String colindante, String medidaCompleta, PdfFont font) {
	    // Celda 1: Posición (frente, derecha, etc.)
	    tabla.addCell(new Cell()
	            .add(new Paragraph(etiqueta).setFont(font).setFontSize(11).setFixedLeading(11f))
	            .setBorder(Border.NO_BORDER)
	            .setPaddingTop(0f).setPaddingBottom(0f));

	    // Celda 2: Colindante (Calle, Lote, etc.)
	    tabla.addCell(new Cell()
	            .add(new Paragraph(colindante).setFont(font).setFontSize(11).setFixedLeading(11f))
	            .setBorder(Border.NO_BORDER)
	            .setPaddingTop(0f).setPaddingBottom(0f));

	    // Celda 3: Medida (LA QUE QUIERES ALINEAR)
	    tabla.addCell(new Cell()
	            // 🔹 CAMBIO AQUÍ: Alineamos el texto a la derecha para que los "m.l." coincidan
	            .setTextAlignment(TextAlignment.RIGHT) 
	            .add(new Paragraph(medidaCompleta).setFont(font).setFontSize(11).setFixedLeading(11f))
	            .setBorder(Border.NO_BORDER)
	            .setPaddingTop(0f).setPaddingBottom(0f));
	}

	private static String extraerNacionalidad(String celular, boolean esFemenino) {
		if (celular == null) return esFemenino ? "peruana" : "peruano";
		if (celular.startsWith("+51")) return esFemenino ? "peruana" : "peruano";
		if (celular.startsWith("+52")) return esFemenino ? "mexicana" : "mexicano";
		if (celular.startsWith("+57")) return esFemenino ? "colombiana" : "colombiano";
		if (celular.startsWith("+1")) return esFemenino ? "estadounidense" : "estadounidense";
		return esFemenino ? "peruana" : "peruano";
	}


	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes, PdfFont arialBoldItalic) {
	    // 1. Contenedor Maestro: Una tabla de 1 sola columna que envuelve TODO
	    // Esto garantiza que si el bloque no cabe, TODO el conjunto de firmas salte a la siguiente hoja.
	    Table contenedorPrincipal = new Table(1)
	            .useAllAvailableWidth()
	            .setBorder(Border.NO_BORDER)
	            .setMarginTop(50f)
	            .setKeepTogether(true); 

	    // 2. Tabla para la primera fila (Comprador 1 y Vendedora)
	    Table fila1 = new Table(UnitValue.createPercentArray(new float[]{45f, 10f, 45f}))
	            .useAllAvailableWidth()
	            .setBorder(Border.NO_BORDER);

	    // --- BLOQUE IZQUIERDO: COMPRADOR 1 ---
	    ClienteResponseDTO c1 = clientes.get(0);
	    Cell celdaC1 = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
	    
	    // Línea de firma centrada en su bloque
	    Paragraph pLineaC1 = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
	            .setWidth(200f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
	    
	    celdaC1.add(pLineaC1);
	    celdaC1.add(new Paragraph(c1.getNombre().toUpperCase() + " " + c1.getApellidos().toUpperCase()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
	    celdaC1.add(new Paragraph("DNI N°" + c1.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
	    
	    if (clientes.size() == 1) {
	        celdaC1.add(new Paragraph("“EL COMPRADOR”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
	    }
	    fila1.addCell(celdaC1);
	    fila1.addCell(new Cell().setBorder(Border.NO_BORDER)); // Espacio

	    // --- BLOQUE DERECHO: LA VENDEDORA ---
	    Cell celdaV = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
	    Paragraph pLineaV = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
	            .setWidth(160f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

	    celdaV.add(pLineaV);
	    celdaV.add(new Paragraph("“LA VENDEDORA”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
	    celdaV.add(new Paragraph("DNI N°19404451").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
	    fila1.addCell(celdaV);

	    // Añadimos la fila 1 al contenedor
	    contenedorPrincipal.addCell(new Cell().add(fila1).setBorder(Border.NO_BORDER));

	    // --- COMPRADORES ADICIONALES (Dentro del mismo contenedor) ---
	    if (clientes.size() > 1) {
	        for (int i = 1; i < clientes.size(); i++) {
	            ClienteResponseDTO ci = clientes.get(i);
	            
	            // Tabla pequeña para cada comprador extra
	            Table tablaExtra = new Table(new float[]{45f})
	                    .setWidth(UnitValue.createPercentValue(45))
	                    .setBorder(Border.NO_BORDER)
	                    .setMarginTop(50f); // Espacio entre firmas

	            Cell celdaExtra = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(0);
	            Paragraph pLineaExtra = new Paragraph().setBorderTop(new com.itextpdf.layout.borders.SolidBorder(1f))
	                    .setWidth(200f).setMarginBottom(2).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
	            
	            celdaExtra.add(pLineaExtra);
	            celdaExtra.add(new Paragraph(ci.getNombre().toUpperCase() + " " + ci.getApellidos().toUpperCase()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));
	            celdaExtra.add(new Paragraph("DNI N°" + ci.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f).setMarginBottom(0));

	            if (i == clientes.size() - 1) {
	                celdaExtra.add(new Paragraph("“LOS COMPRADORES”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
	            }
	            
	            tablaExtra.addCell(celdaExtra);
	            // IMPORTANTE: Se añade al contenedor principal para que no se separe
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
	            
	            // Si el espacio libre es menor al porcentaje solicitado (ej. 0.4f), salta de página
	            if (espacioLibre < (altoPagina * porcentajeRequerido)) {
	                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
	            }
	        }
	    }
	}
}