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

public class PdfGenerator {

    public static byte[] generarContratoFlorida(ContratoResponseDTO contrato) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(25, 30, 25, 30);
        
        // --- PROCESAMIENTO DINÁMICO DE CLIENTES ---
        List<ClienteResponseDTO> clientes = contrato.getClientes();
        int numClientes = clientes.size();
        
        // Datos del primer cliente para dirección
        ClienteResponseDTO titular = clientes.get(0);
        String direccionCompleta = titular.getDireccion() + ", Distrito de " + titular.getDistrito();

        // Construcción del bloque de compradores
        StringBuilder sbCompradores = new StringBuilder();
        for (int i = 0; i < numClientes; i++) {
            ClienteResponseDTO c = clientes.get(i);
            
            // 🟢 Comparación exacta con tu Enum Genero
            String prefijo = "el Sr. "; // Valor por defecto
            if (c.getGenero() != null && c.getGenero().equals(Genero.Femenino)) {
                prefijo = "la Sra. ";
            }
            
            sbCompradores.append(prefijo).append(c.getNombre()).append(" ").append(c.getApellidos())
                         .append(", identificado con DNI N°").append(c.getNumDoc());
            
            if (numClientes > 1 && i == 0) {
                sbCompradores.append(", casado con ");
            }
        }

        String etiquetaComprador = (numClientes > 1) ? "LOS COMPRADORES" : "EL COMPRADOR";
        String etiquetaDomicilio = (numClientes > 1) ? "ambos con domicilio común en " : "con domicilio en ";

        // --- DATOS DEL LOTE Y PRECIO ---
        LoteResponseDTO lote = contrato.getLotes().get(0);
        LetraResponseDTO pL = contrato.getLetras().get(0);
        LetraResponseDTO uL = contrato.getLetras().get(contrato.getLetras().size() - 1);
        String montoTexto = pL.getImporteLetras().split(" POR ")[0];

        // --- PÁGINA 1: ENCABEZADO ---
        document.add(new Paragraph("PROGRAMA DE VIVIENDA").setTextAlignment(TextAlignment.CENTER).setBold());
        document.add(new Paragraph("“LA FLORIDA DE TORRE BLANCA”").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(13));
        document.add(new Paragraph("CONTRATO PRIVADO DE COMPRA-VENTA DE TERRENO RUSTICO").setTextAlignment(TextAlignment.CENTER).setBold().setUnderline());
        document.add(new Paragraph("\n"));

        // --- PÁRRAFO INTRODUCTORIO (CORREGIDO) ---
        Paragraph intro = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10);
        intro.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico con Reserva de Propiedad que celebran de una parte ");
        intro.add(new Text("“INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.”").setBold());
        intro.add(" con RUC Nº 20537853108 con domicilio Av. Alfredo Mendiola N°3623- Tercer Piso - Of. 301-A Urb. Panamericana Norte, Distrito de Los Olivos, representado por su Gerente General ");
        intro.add(new Text("OLMEDO SILVA LOPEZ").setBold());
        intro.add(" con DNI No.19404451 según consta del poder inscrito en la partida electrónica Nº 12561792, a quien en adelante se le denominará LA VENDEDORA; y de la otra parte ");
        intro.add(new Text(sbCompradores.toString().toUpperCase()).setBold());
        intro.add(", " + etiquetaDomicilio + direccionCompleta.toUpperCase() + ", a quien en adelante se le denominará ");
        intro.add(new Text(etiquetaComprador).setBold().setUnderline());
        intro.add(" en los términos y condiciones de las cláusulas siguientes: ----------------------------------------------------");
        document.add(intro);

        // --- PRIMERA: PROPIEDAD ---
        document.add(new Paragraph("\nPRIMERA: PROPIEDAD").setBold().setFontSize(10));
        document.add(new Paragraph("“LA VENDEDORA” es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, inscrito en la Partida Electrónica 11049870 del Registro de Predios de Lima. Fue adquirido mediante la minuta de fecha 06/11/2019 (15 Has.) y con fecha 29/03/2021 (51,224.03 m2) que le otorgo su anterior Propietaria INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C, con RUC N°20601878616.").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));

        // --- SEGUNDA: OBJETO ---
        document.add(new Paragraph("\nSEGUNDA: OBJETO DEL CONTRATO").setBold().setFontSize(10));
        document.add(new Paragraph("Por el presente contrato LA VENDEDORA transfiere los derechos y acciones de un lote de terreno rústico ubicado la Manzana “"+lote.getManzana()+"” y asignado, con el lote Nº "+lote.getNumeroLote()+" del Programa de Vivienda “LA FLORIDA DE TORRE BLANCA” con un área de "+lote.getArea()+"M2. Encerrado dentro de los siguientes linderos y medidas perimétricas:").setFontSize(10).setTextAlignment(TextAlignment.JUSTIFIED));
        
        Paragraph linderos = new Paragraph().setFontSize(10).setMarginLeft(40).setFixedLeading(12);
        linderos.add("Por el frente: con la " + lote.getColindanteNorte() + " Con " + lote.getAncho1() + " m.l.\n");
        linderos.add("Por la derecha: con el " + lote.getColindanteEste() + " Con " + lote.getLargo1() + " m.l.\n");
        linderos.add("Por la izquierda: con el " + lote.getColindanteOeste() + " Con " + lote.getLargo2() + " m.l.\n");
        linderos.add("Por el fondo: con el " + lote.getColindanteSur() + " Con " + lote.getAncho2() + " m.l.");
        document.add(linderos);

        // --- FIRMAS ---
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

    private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes) {
        document.add(new Paragraph("\n\n\n"));
        // Línea para Primer Comprador y Vendedora
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