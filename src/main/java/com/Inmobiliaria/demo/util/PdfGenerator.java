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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.ByteArrayOutputStream;
import java.util.List;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

public class PdfGenerator {

    public static byte[] generarContratoFlorida(ContratoResponseDTO contrato) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(114, 85, 100, 85); //multiplicar los cm * 28.35 = numero dentro del parentesis 
        
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

     // --- PÁGINA 1: ENCABEZADO ---
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
        
     // --- PÁRRAFO INTRODUCTORIO ---
        Paragraph intro = new Paragraph()
            .setTextAlignment(TextAlignment.JUSTIFIED)
            .setFont(arialNormal)      // Fuente base Arial Normal
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

     // --- PRIMERA: PROPIEDAD ---
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
         .setMultipliedLeading(1.5f);

     primeraCuerpo.add(new Text("“LA VENDEDORA”").setFont(arialBoldItalic));
     primeraCuerpo.add(" es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, el cual forma parte de un predio de mayor extensión ubicado en las Provincia de Huarochirí, Lima y Canta, inscrito a fojas 515 del tomo 10-H, actualmente ");
     primeraCuerpo.add(new Text("Partida Electrónica 11049870 del Registro de Predios de Lima. --------------").setFont(arialBoldItalic));
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
     primeraCuerpo.add(", el mismo que se distribuye en los lotes y manzanas con sus respectivas áreas conforme al plano de Lotización. ----");

     document.add(primeraCuerpo);
     
     
        // --- SEGUNDA: OBJETO ---
        document.add(new Paragraph("\nSEGUNDA: OBJETO DEL CONTRATO").setBold().setFontSize(10));
        document.add(new Paragraph("Por el presente contrato LA VENDEDORA transfiere los derechos y acciones de un lote de terreno rústico ubicado la Manzana “"+lote.getManzana()+"” y asignado, con el lote Nº "+lote.getNumeroLote()+" del Programa de Vivienda “LA FLORIDA DE TORRE BLANCA” con un área de "+lote.getArea()+"M2. Encerrado dentro de los siguientes linderos y medidas perimétricas:").setFontSize(10).setTextAlignment(TextAlignment.JUSTIFIED));
        
        Paragraph linderos = new Paragraph().setFontSize(10).setMarginLeft(40).setFixedLeading(12);
        linderos.add("Por el frente: con la " + lote.getColindanteNorte() + " Con " + lote.getAncho1() + " m.l.\n");
        linderos.add("Por la derecha: con el " + lote.getColindanteEste() + " Con " + lote.getLargo1() + " m.l.\n");
        linderos.add("Por la izquierda: con el " + lote.getColindanteOeste() + " Con " + lote.getLargo2() + " m.l.\n");
        linderos.add("Por el fondo: con el " + lote.getColindanteSur() + " Con " + lote.getAncho2() + " m.l.");
        document.add(linderos);

        // --- FIRMAS PÁGINA 1 ---
        agregarBloqueFirmas(document, clientes);

        // --- PÁGINA 2: POSESIÓN ---
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        document.add(new Paragraph("DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO").setTextAlignment(TextAlignment.CENTER).setBold());
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("PRIMERO.- LA VENDEDORA en virtud del presente contrato da en venta real un lote de terreno rústico de "+lote.getArea()+"M2 en la Manzana “"+lote.getManzana()+"” lote Nº "+lote.getNumeroLote()+". TERCERO.- "+etiquetaComprador+" reconoce adeudar US$."+contrato.getSaldo()+" ("+montoTexto+") y constituyen PRIMERA HIPOTECA a favor de LA VENDEDORA conforme a los Artículos 1118 y 1119 del Código Civil.").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));
        
        agregarBloqueFirmas(document, clientes);

        document.close();
        return out.toByteArray();
    }

    private static String extraerNacionalidad(String celular, boolean esFemenino) {
        if (celular == null) return esFemenino ? "peruana" : "peruano";
        if (celular.startsWith("+51")) return esFemenino ? "peruana" : "peruano";
        if (celular.startsWith("+52")) return esFemenino ? "mexicana" : "mexicano";
        if (celular.startsWith("+57")) return esFemenino ? "colombiana" : "colombiano";
        if (celular.startsWith("+1")) return esFemenino ? "estadounidense" : "estadounidense";
        return esFemenino ? "peruana" : "peruano";
    }

    private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes) {
        document.add(new Paragraph("\n\n\n"));
        document.add(new Paragraph("….….……………………………………………….                 ...…………………………").setTextAlignment(TextAlignment.CENTER));
        
        String primerComprador = clientes.get(0).getNombre() + " " + clientes.get(0).getApellidos();
        String primerDni = "DNI N° " + clientes.get(0).getNumDoc();
        
        document.add(new Paragraph(primerComprador.toUpperCase() + "                                   “LA VENDEDORA”").setTextAlignment(TextAlignment.CENTER).setFontSize(9).setBold());
        document.add(new Paragraph(primerDni + "                                             DNI N° 19404451").setTextAlignment(TextAlignment.CENTER).setFontSize(9));
        
        if (clientes.size() > 1) {
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("….….……………………………………………….").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50));
            document.add(new Paragraph(clientes.get(1).getNombre().toUpperCase() + " " + clientes.get(1).getApellidos().toUpperCase()).setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9).setBold());
            document.add(new Paragraph("DNI N° " + clientes.get(1).getNumDoc()).setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9));
            document.add(new Paragraph("“LOS COMPRADORES”").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9).setBold());
        } else {
            document.add(new Paragraph("\n“EL COMPRADOR”").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9).setBold());
        }
    }
}