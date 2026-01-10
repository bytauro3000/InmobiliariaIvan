package com.Inmobiliaria.demo.util;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LetraResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

public class PdfGenerator {

	public static byte[] generarContratoFlorida(ContratoResponseDTO contrato) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(out);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);
		document.setMargins(114, 85, 85, 85); //multiplicar los cm * 28.35 = margenes

		// 🔹 CARGA DE FUENTES (NORMAL, NEGRITA Y NEGRITA-CURSIVA)
		PdfFont arialNormal;
		PdfFont arialBold;
		PdfFont arialBoldItalic;
		PdfFont arialItalic;

		try {
			// 1. Arial Normal (Para el cuerpo del texto)
			byte[] nBytes = com.itextpdf.io.util.StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIAL.TTF")
					);
			arialNormal = PdfFontFactory.createFont(nBytes, PdfEncodings.WINANSI);

			// 2. Arial Bold (Para nombres y datos en negrita)
			byte[] bBytes = com.itextpdf.io.util.StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALBD.TTF")
					);
			arialBold = PdfFontFactory.createFont(bBytes, PdfEncodings.WINANSI);

			// 3. Arial Bold Italic (Para el título del contrato)
			byte[] biBytes = com.itextpdf.io.util.StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALBI.TTF")
					);
			arialBoldItalic = PdfFontFactory.createFont(biBytes, PdfEncodings.WINANSI);

			// 4. Carga de Arial Italic (Solo Cursiva)
			byte[] iBytes = com.itextpdf.io.util.StreamUtil.inputStreamToArray(
					PdfGenerator.class.getClassLoader().getResourceAsStream("fonts/ARIALI.TTF")
					);
			arialItalic = PdfFontFactory.createFont(iBytes, PdfEncodings.WINANSI);

		} catch (Exception e) {
			throw new RuntimeException("Error cargando las fuentes Arial desde resources/fonts/", e);
		}


		// --- PROCESAMIENTO DINÁMICO DE CLIENTES ---
		List<ClienteResponseDTO> clientes = contrato.getClientes();
		int numClientes = clientes.size();
		ClienteResponseDTO titular = clientes.get(0);

		String nombreDistrito = (titular.getDistrito() != null) ? titular.getDistrito().getNombre() : "";
		String domicilioCalle = (titular.getDireccion() != null) ? titular.getDireccion().toUpperCase() : "";
		String ubicacionLima = ", Distrito de " + nombreDistrito + ", Provincia y Departamento de Lima";
		String direccionRealParaContrato = domicilioCalle + ubicacionLima;

		// 1. Construcción del bloque de compradores (Ajuste de minúsculas y concordancia)
		Paragraph bloqueCompradores = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10);
		for (int i = 0; i < numClientes; i++) {
			ClienteResponseDTO c = clientes.get(i);
			boolean esFemenino = (c.getGenero() != null && c.getGenero().equals(Genero.Femenino));

			String prefijo = esFemenino ? "la Sra. " : "el Sr. ";
			String nacionalidad = extraerNacionalidad(c.getCelular(), esFemenino);
			String identif = esFemenino ? "identificada" : "identificado";
			String estCivil = esFemenino ? "casada" : "casado";

			bloqueCompradores.add(prefijo);
			bloqueCompradores.add(new Text(c.getNombre().toUpperCase() + " " + c.getApellidos().toUpperCase()).setBold());
			bloqueCompradores.add(" , " + nacionalidad + ", " + identif + " con ");
			bloqueCompradores.add(new Text("DNI N°" + c.getNumDoc()).setBold());

			if (numClientes > 1 && i == 0) {
				bloqueCompradores.add(", " + estCivil + ", con ");
			}
		}

		String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES" : "EL COMPRADOR";
		String pronombreDenom = (numClientes > 1) ? "les" : "le";

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
				.add(new Text("PRIMERA:      PROPIEDAD").setFont(arialBoldItalic).setUnderline())
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
		 * PAGINA 2: CLAUSULA SEGUNDA - OBJETO 
		 * ========================================================= */

		// 1. Título de la Cláusula
		document.add(new Paragraph()
				.add(new Text("SEGUNDA:      OBJETO DEL CONTRATO").setFont(arialBoldItalic).setUnderline())
				.setFontSize(11)
				.setMarginTop(15));

		// 2. Primer bloque descriptivo
		Paragraph segundaCuerpo = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic) 
				.setFontSize(11)
				.setMultipliedLeading(1.0f);

		segundaCuerpo.add("Por el presente contrato ");
		segundaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaCuerpo.add(" transfiere los derechos y acciones de un lote de terreno rústico ubicado la Manzana ");
		segundaCuerpo.add(new Text("“" + lote.getManzana() + "”").setFont(arialBoldItalic));
		segundaCuerpo.add(" y asignado, con el lote ");
		segundaCuerpo.add(new Text("Nº " + lote.getNumeroLote()).setFont(arialBoldItalic));
		segundaCuerpo.add(" del Programa de Vivienda ");
		segundaCuerpo.add(new Text("“LA FLORIDA DE TORRE BLANCA”").setFont(arialBoldItalic));
		segundaCuerpo.add(" con un área total de ");
		segundaCuerpo.add(new Text(lote.getArea() + " M2.").setFont(arialBoldItalic));
		segundaCuerpo.add(" Encerrado dentro de los siguientes linderos y medidas perimétricas:");

		document.add(segundaCuerpo);

		// 3. Tabla de Linderos (Para alineamiento perfecto como en la imagen)
		// Usamos una tabla sin bordes de 3 columnas
		float[] columnWidths = {120f, 200f, 100f}; 
		Table tablaLinderos = new Table(columnWidths)
				.setMarginLeft(10)

				.setMarginTop(5)

				.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

		// Fila: Por el Frente
		tablaLinderos.addCell(new Cell().add(new Paragraph("Por el frente").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph(lote.getColindanteNorte()).setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph("Con    " + lote.getAncho1() + "  m.l.").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

		// Fila: Por la Derecha
		tablaLinderos.addCell(new Cell().add(new Paragraph("Por la derecha").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph(lote.getColindanteEste()).setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph("Con  " + lote.getLargo1() + "  m.l.").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

		// Fila: Por la Izquierda
		tablaLinderos.addCell(new Cell().add(new Paragraph("Por la Izquierda").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph(lote.getColindanteOeste()).setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph("Con    " + lote.getLargo2() + "  m.l.").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

		// Fila: Por el Fondo
		tablaLinderos.addCell(new Cell().add(new Paragraph("Por el fondo").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph(lote.getColindanteSur()).setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
		tablaLinderos.addCell(new Cell().add(new Paragraph("Con    " + lote.getAncho2() + "  m.l.").setFont(arialItalic).setFontSize(11)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

		document.add(tablaLinderos);

		// 4. Segundo bloque descriptivo (Final de la cláusula)
		Paragraph segundaFinal = new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFont(arialItalic)
				.setFontSize(11)
				.setMultipliedLeading(1.0f)
				.setMarginTop(10);

		segundaFinal.add("Por el presente contrato ");
		segundaFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		segundaFinal.add(" otorga en venta real un lote de terreno rustico con veredas, agua y luz provisional previo pago por cada servicio brindado a ");
		segundaFinal.add(new Text(etiquetaComprador).setFont(arialBoldItalic)); // EL COMPRADOR o LOS COMPRADORES
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
				.add(new Text("TERCERA:      PRECIO").setFont(arialBoldItalic).setUnderline())
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
			LetraResponseDTO primeraLetra = contrato.getLetras().get(0);

			terceraCuerpo.add(new Text("US$." + df.format(contrato.getMontoTotal())).setFont(arialBoldItalic));
			terceraCuerpo.add(new Text(" (" + montoTotalLetras + ")").setFont(arialBoldItalic));
			terceraCuerpo.add(", que ");
			terceraCuerpo.add(new Text("“" + etiquetaComprador + "”").setFont(arialBoldItalic));
			terceraCuerpo.add(" se obliga a cancelar en dinero, íntegramente y por armadas, según el cronograma de la siguiente forma:");
			document.add(terceraCuerpo);

			// 3. Sub-cláusulas 3.1, 3.2 y 3.3 con sangría (Margen izquierdo)
			// 3.1 Cuota Inicial
			// Subcláusula 3.1 (Cuota inicial)
			String textoInicial = (contrato.getInicial().doubleValue() > 0) ? "US$." + df.format(contrato.getInicial()) : "Sin Cuota inicial.";
			document.add(new Paragraph("3.1 " + textoInicial).setFont(arialItalic).setFontSize(11).setMarginLeft(40).setMarginTop(10));

			// Subcláusula 3.2 (Saldo)
			Paragraph subclausula32 = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialItalic).setFontSize(11).setMultipliedLeading(1.0f).setMarginLeft(40).setMarginTop(10);

			subclausula32.add("3.2 El saldo del precio de ");
			subclausula32.add(new Text("US$." + df.format(contrato.getSaldo())).setFont(arialBoldItalic));
			subclausula32.add(new Text(" (" + montoSaldoLetras + ")").setFont(arialBoldItalic));
			subclausula32.add(", que será cancelado en ");

			// Cálculo de letras (139 normales y 1 última diferente según tu ejemplo)
			int totalLetras = contrato.getCantidadLetras();
			LetraResponseDTO ultimaLetra = contrato.getLetras().get(totalLetras - 1);

			subclausula32.add(new Text(totalLetras + " letras de cambio ").setFont(arialBoldItalic));
			subclausula32.add("(" + (totalLetras - 1) + " letras de cambio de ");
			subclausula32.add(new Text("US$" + df.format(primeraLetra.getImporte())).setFont(arialBoldItalic));
			subclausula32.add(" y la última letra la ");
			subclausula32.add(new Text("Nº" + totalLetras).setFont(arialBoldItalic));
			subclausula32.add(" de ");
			subclausula32.add(new Text("US$" + df.format(ultimaLetra.getImporte())).setFont(arialBoldItalic));
			subclausula32.add(") debidamente aceptadas por ");
			subclausula32.add(new Text("“" + etiquetaComprador).setFont(arialBoldItalic));
			subclausula32.add(", según el detalle siguiente:");

			document.add(subclausula32);

			// 3.3 Fechas de Vencimiento
			Paragraph subclausula33 = new Paragraph()
					.setFont(arialItalic).setFontSize(11)
					.setMarginLeft(40).setMarginTop(10);

			// Definimos el formato: DIA/MES/AÑO
			java.time.format.DateTimeFormatter formatoLindo = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

			subclausula33.add("3.3 Letra ");
			subclausula33.add(new Text("No.01").setFont(arialBoldItalic));
			subclausula33.add(" por ");
			subclausula33.add(new Text("US$" + df.format(primeraLetra.getImporte())).setFont(arialBoldItalic)); // Con comas
			subclausula33.add(" con vencimiento el día ");
			subclausula33.add(new Text(primeraLetra.getFechaVencimiento().format(formatoLindo)).setFont(arialBoldItalic));


			subclausula33.add(" y la última ");
			subclausula33.add(new Text("Letra No." + totalLetras).setFont(arialBoldItalic));
			subclausula33.add(" por ");
			subclausula33.add(new Text("US$" + ultimaLetra.getImporte()).setFont(arialBoldItalic));
			subclausula33.add(" con vencimiento el día ");

			// Formateo directo asumiendo que el dato siempre existe
			subclausula33.add(new Text(ultimaLetra.getFechaVencimiento().format(formatoLindo)).setFont(arialBoldItalic));

			subclausula33.add(".");

			document.add(subclausula33);



			// 4. Párrafo final de garantía y domicilio de pago
			Paragraph terceraFinal = new Paragraph()
					.setTextAlignment(TextAlignment.JUSTIFIED)
					.setFont(arialItalic).setFontSize(11)
					.setMultipliedLeading(1.0f)
					.setMarginTop(15);

			terceraFinal.add("Así mismo a efectos de garantizar el cumplimiento de su obligación ");
			terceraFinal.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
			terceraFinal.add(" giran a favor de ");
			terceraFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			terceraFinal.add(" letras de cambio que se detallan en la cláusula tercera que serán cancelados en la fecha de vencimiento de los respectivos cambiales, más los correspondientes intereses en caso de mora. El lugar de pago de todas las armadas será el domicilio de ");
			terceraFinal.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
			terceraFinal.add(".");

			document.add(terceraFinal);

		}


		/* =========================================================
		 * PAGINA 2: CLAUSULA CUARTA - EQUIVALENCIA
		 * ========================================================= */

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
				.setMultipliedLeading(1.0f); // Interlineado 1.0 aplicado

		sextaCuerpo.add("Las partes contratantes acuerdan al amparo de lo dispuesto por del artículo 1583° del Código Civil incorporar en el presente contrato el pacto de reserva de propiedad a favor de ");
		sextaCuerpo.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		sextaCuerpo.add(". En consecuencia, este conservara la propiedad del bien materia del presente contrato aun cuando la posesión del mismo haya sido entregada a ");
		sextaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic)); // LOS COMPRADORES o EL COMPRADOR
		sextaCuerpo.add(". Así mismo se deja establecido que el pacto de reserva de propiedad a quien se refiere la cláusula anterior, tendrá vigencia hasta que la compradora cumpla con pagar la totalidad del precio pactado en la cláusula tercera de este contrato, más los intereses devengados.");

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
		septimaIntro.add(" tenga cancelado el ");
		septimaIntro.add(new Text("50%").setFont(arialBoldItalic));
		septimaIntro.add(" del valor del predio objeto del presente contrato y/o ");
		septimaIntro.add(new Text("dejen de pagar dos (02) letras consecutivas o alternadas").setUnderline());
		septimaIntro.add(", ");
		septimaIntro.add(new Text("LA VENDEDORA").setFont(arialBoldItalic));
		septimaIntro.add(" quedara facultada para ejercer las acciones legales correspondientes haciendo valer su derecho, siendo estas las siguientes:");

		document.add(septimaIntro);

		// USAMOS EL NOMBRE COMPLETO PARA EVITAR CONFLICTO CON java.util.List
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
		p2.add(" deberá pagar mensualmente al a vendedora el ");
		p2.add(new Text("3% del precio total").setFont(arialBoldItalic));
		p2.add(" estipulado en calidad de merced conductiva; incrementándose esta en un 50% cada año que venza, hasta su desocupación total.");
		item2.add(p2);
		listaObligaciones.add(item2); 

		// --- TERCER PUNTO ---
		ListItem item3 = new ListItem();
		Paragraph p3 = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		p3.add("En caso de resolución de contrato por motivos precedentes ");
		p3.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		p3.add(", desea continuar conservando el predio objeto del presente contrato, se restaurará al precio y/o valor por metro cuadra del momento en que se actualice el contrato.");
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
		octavaIntro.add(" puede solicitar a ");
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

		// Línea de separación punteada (estética como en la imagen)
		pc.add("\n------------------------------------------------------------------------------------------");

		itemC.add(pc);
		listaRenuncia.add(itemC);

		// --- APARTADO d. ---
		com.itextpdf.layout.element.ListItem itemD = new com.itextpdf.layout.element.ListItem();
		Paragraph pd = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.0f);
		pd.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pd.add(" se compromete a respetar exactamente las medidas perimétricas de su lote, materia de este contrato. En caso contrario es el único responsable de los daños y perjuicios que se puede ocasionar a sus vecinos colindantes. Asimismo, queda convenido que ");
		pd.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		pd.add(" para construir su vivienda deberá realizarla conforme a los planos elaborados por profesionales competentes, en consecuencia, queda ");
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
		novenaCuerpo.add("haya cancelado el íntegro del saldo deudor especificado en la cláusula tercera.");

		document.add(novenaCuerpo);

		/* =================================================================
		 * PAGINA 4: CLAUSULA DECIMA: ENTREGA DEL BIEN OBJETO DEL PRESENTE CONTRATO
		 * ================================================================= */

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
		decimaCuerpo.add("declara que la entrega física y real del predio objeto del presente contrato se realizara a la suscripción y legalización del presente contrato, por lo que a partir de ello el cuidado, administración y conservación del bien lo asume ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		decimaCuerpo.add(". Así mismo ");
		decimaCuerpo.add(new Text(etiquetaComprador).setFont(arialBoldItalic));
		decimaCuerpo.add(", declara conocer la situación física, real y legal del predio objeto de transferencia, el mismo que lo encuentra a su entera satisfacción, por tanto, renuncia a toda acción rescisoria por dolo, error, lesión y cualquiera que tienda a invalidar el presente contrato.");

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
		undecimaCuerpo.add(new Text("LA COMPRADORA").setFont(arialBoldItalic)); // En tu imagen aparece en femenino específico
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
		decimaTerceraCuerpo.add("no adeude suma alguna, ello toda vez que las partes declaran conocer el hecho que el presente contrato es privado y quedara sujeto al cumplimiento de los acuerdos establecidos en cada una de las cláusulas de este instrumento legal, para que opere la traslación de dominio; obligándose la compradora a no condicionar el cumplimiento de su obligación ante la eventual demora que pudiera resultar de la tramitación del mismo por parte de las autoridades competentes, gestión que se encuentra en proceso; Las partes contratantes de mutuo acuerdo se compromete a firmar una Minuta y la correspondiente Escritura Pública de compraventa cuando estas condiciones se materialicen, respetándose el integro de los pactos contenidos en el presente contrato.");
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

		// --- LÓGICA DE FECHA DINÁMICA CORREGIDA ---
		java.time.LocalDate hoy = java.time.LocalDate.now();
		String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", 
				"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

		String diaNum = String.format("%02d", hoy.getDayOfMonth());
		String mesNombre = nombresMeses[hoy.getMonthValue() - 1];
		String mesNum = String.format("%02d", hoy.getMonthValue());
		int anioNum = hoy.getYear();

		// CORRECCIÓN: Convertimos el int a BigDecimal para que NumeroALetras lo acepte
		java.math.BigDecimal anioBigDecimal = java.math.BigDecimal.valueOf(anioNum);
		String anioLetras = NumeroALetras.convertir(anioBigDecimal).split(" CON ")[0].trim(); 

		cierreFinal.add("Leído el presente contrato y estando las partes contratantes conformes con las cláusulas establecidas en el presente contrato, proceden a suscribirlo al ");
		cierreFinal.add(new Text("primer (" + diaNum + ") día del mes de " + mesNombre + " (" + mesNum + ") del Año " + anioLetras + " (" + anioNum + ").").setFont(arialBoldItalic));

		document.add(cierreFinal);

		//========================================================================================

		// 1. Firmas al final del contrato (Página 1 o la que corresponda)
		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		// ========================================================================================
		// DOCUMENTO DE SEÑALIZACIÓN (PÁGINA APARTE)
		// ========================================================================================

		// 2. Salto de página para el nuevo documento
		document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

		document.add(new Paragraph("DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO")
				.setTextAlignment(TextAlignment.CENTER)
				.setBold()
				.setFontSize(12));

		document.add(new Paragraph("\n"));

		// Usamos df.format y las variables de texto que ya procesamos anteriormente
		document.add(new Paragraph()
				.setTextAlignment(TextAlignment.JUSTIFIED)
				.setFontSize(10)
				.add("PRIMERO.- LA VENDEDORA en virtud del presente contrato da en venta real un lote de terreno rústico de " + lote.getArea() + "M2 en la Manzana “" + lote.getManzana() + "” lote Nº " + lote.getNumeroLote() + ". ")
				.add("TERCERO.- " + etiquetaComprador + " reconoce adeudar US$." + df.format(contrato.getSaldo()) + " (" + montoSaldoLetras + ") y constituyen PRIMERA HIPOTECA a favor de LA VENDEDORA conforme a los Artículos 1118 y 1119 del Código Civil."));

		// 3. Reutilizamos el bloque de firmas para el documento de posesión
		agregarBloqueFirmas(document, clientes, arialBoldItalic);

		// 4. Cierre final del flujo del PDF
		document.close();
		return out.toByteArray();
	} // 👈 Cierre del método generarContratoFlorida

	// --- MÉTODOS AUXILIARES ---

	private static String extraerNacionalidad(String celular, boolean esFemenino) {
		if (celular == null) return esFemenino ? "peruana" : "peruano";
		if (celular.startsWith("+51")) return esFemenino ? "peruana" : "peruano";
		if (celular.startsWith("+52")) return esFemenino ? "mexicana" : "mexicano";
		if (celular.startsWith("+57")) return esFemenino ? "colombiana" : "colombiano";
		if (celular.startsWith("+1")) return esFemenino ? "estadounidense" : "estadounidense";
		return esFemenino ? "peruana" : "peruano";
	}

	// 🔹 MÉTODO REUTILIZABLE DE FIRMAS (Arial 12 Bold Italic)
	private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes, PdfFont arialBoldItalic) {
	    document.add(new Paragraph("\n\n\n\n")); 

	    // 1. Tabla principal al 100% de ancho sin bordes
	    Table tablaFirmas = new Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{1f, 1f}))
	            .useAllAvailableWidth()
	            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

	    // --- BLOQUE IZQUIERDO: PRIMER COMPRADOR ---
	    ClienteResponseDTO c1 = clientes.get(0);
	    String nombreC1 = c1.getNombre().toUpperCase() + " " + c1.getApellidos().toUpperCase();
	    
	    // 🔹 LÍNEA AGRANDADA: Multiplicador aumentado a 2.1 para dar el 30% extra de ancho
	    String lineaPuntosC1 = ".".repeat((int)(nombreC1.length() * 2.1));

	    Cell celdaC1 = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
	            .setTextAlignment(TextAlignment.CENTER)
	            .setPaddingLeft(0); // 🔹 Pegado al ras del margen izquierdo

	    celdaC1.add(new Paragraph(lineaPuntosC1).setMarginBottom(0).setFixedLeading(10f));
	    celdaC1.add(new Paragraph(nombreC1).setFont(arialBoldItalic).setFontSize(12).setMarginBottom(0).setFixedLeading(12f));
	    celdaC1.add(new Paragraph("DNI N°" + c1.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setMarginBottom(0).setFixedLeading(12f));
	    
	    if (clientes.size() == 1) {
	        celdaC1.add(new Paragraph("“EL COMPRADOR”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
	    }

	    // --- BLOQUE DERECHO: LA VENDEDORA ---
	    String textoV = "“LA VENDEDORA”";
	    // 🔹 LÍNEA DE VENDEDORA: También escalada proporcionalmente
	    String lineaPuntosV = ".".repeat((int)(textoV.length() * 3.5)); 

	    Cell celdaV = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
	            .setTextAlignment(TextAlignment.CENTER)
	            .setPaddingRight(0); // 🔹 Pegado al ras del margen derecho

	    celdaV.add(new Paragraph(lineaPuntosV).setMarginBottom(0).setFixedLeading(10f));
	    celdaV.add(new Paragraph(textoV).setFont(arialBoldItalic).setFontSize(12).setMarginBottom(0).setFixedLeading(12f));
	    celdaV.add(new Paragraph("DNI N°19404451").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));

	    tablaFirmas.addCell(celdaC1);
	    tablaFirmas.addCell(celdaV);
	    document.add(tablaFirmas);

	    // --- FILAS ADICIONALES: COMPRADORES EXTRA ---
	    if (clientes.size() > 1) {
	        for (int i = 1; i < clientes.size(); i++) {
	            document.add(new Paragraph("\n\n")); 
	            ClienteResponseDTO ci = clientes.get(i);
	            String nombreCi = ci.getNombre().toUpperCase() + " " + ci.getApellidos().toUpperCase();
	            String lineaPuntosCi = ".".repeat((int)(nombreCi.length() * 2.1)); // 🔹 Misma escala

	            // Tabla de 50% de ancho alineada a la izquierda
	            Table tablaExtra = new Table(1)
	                    .setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(50))
	                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
	                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);

	            Cell celdaExtra = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
	                    .setTextAlignment(TextAlignment.CENTER)
	                    .setPaddingLeft(0); // 🔹 Alineación al ras izquierdo

	            celdaExtra.add(new Paragraph(lineaPuntosCi).setMarginBottom(0).setFixedLeading(10f));
	            celdaExtra.add(new Paragraph(nombreCi).setFont(arialBoldItalic).setFontSize(12).setMarginBottom(0).setFixedLeading(12f));
	            celdaExtra.add(new Paragraph("DNI N°" + ci.getNumDoc()).setFont(arialBoldItalic).setFontSize(12).setMarginBottom(0).setFixedLeading(12f));
	            
	            if (i == clientes.size() - 1) {
	                celdaExtra.add(new Paragraph("“LOS COMPRADORES”").setFont(arialBoldItalic).setFontSize(12).setFixedLeading(12f));
	            }
	            
	            tablaExtra.addCell(celdaExtra);
	            document.add(tablaExtra);
	        }
	    }
	}
}