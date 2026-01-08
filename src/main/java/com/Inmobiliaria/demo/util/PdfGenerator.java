package com.Inmobiliaria.demo.util;

import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.LetraResponseDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
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
import java.util.stream.Collectors;

public class PdfGenerator {

    public static byte[] generarContratoFlorida(ContratoResponseDTO contrato) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(20, 25, 20, 25);
        
        // --- DATOS DINÁMICOS ---
        String compradoresFull = contrato.getClientes().stream()
                .map(c -> c.getNombre() + " " + c.getApellidos() + " , identificado con DNI N°" + c.getNumDoc())
                .collect(Collectors.joining(", casada, con el Sr. "));
        
        LoteResponseDTO lote = contrato.getLotes().get(0);
        LetraResponseDTO pL = contrato.getLetras().get(0);
        LetraResponseDTO uL = contrato.getLetras().get(contrato.getLetras().size() - 1);
        String montoTexto = pL.getImporteLetras().split(" POR ")[0];

        // --- PÁGINA 1: CONTRATO ---
        document.add(new Paragraph("PROGRAMA DE VIVIENDA").setTextAlignment(TextAlignment.CENTER).setBold());
        document.add(new Paragraph("“LA FLORIDA DE TORRE BLANCA”").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(13));
        document.add(new Paragraph("CONTRATO PRIVADO DE COMPRA-VENTA DE TERRENO RUSTICO").setTextAlignment(TextAlignment.CENTER).setBold());
        document.add(new Paragraph("\n"));

        Paragraph intro = new Paragraph().setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10);
        intro.add("Conste por el presente documento de Contrato privado de Compra-Venta de terreno rústico con Reserva de Propiedad que celebran de una parte ");
        intro.add(new Text("“INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.”").setBold());
        intro.add(" con RUC Nº 20537853108 con domicilio Av. Alfredo Mendiola N°3623- Tercer Piso - Of. 301-A Urb. Panamericana Norte, Distrito de Los Olivos, Provincia y Departamento de Lima, debidamente representado por su ");
        intro.add(new Text("Gerente General OLMEDO SILVA LOPEZ").setBold());
        intro.add(" con DNI No.19404451 según consta del poder inscrito en la partida electrónica Nº 12561792 del Registro de Personas Jurídicas, a quien en adelante se le denominará LA VENDEDORA; y de la otra parte la Sra. ");
        intro.add(new Text(compradoresFull).setBold());
        intro.add(", ambos con domicilio común registrado, a quienes en adelante se les denominará ");
        intro.add(new Text("LOS COMPRADORES").setBold());
        intro.add(" en los términos y condiciones de las cláusulas siguientes: ----------------------------------------------------");
        document.add(intro);

        // CLAUSULAS
        document.add(new Paragraph("PRIMERA: PROPIEDAD").setBold().setFontSize(10));
        document.add(new Paragraph("“LA VENDEDORA” es propietaria de un lote de terreno rústico con un área superficial de 201,224.03 m2 Equivalente a 20 Has. 1,224.03 m2, que corresponde al 100% de las acciones y derechos del Predio denominado Sector Pampa San Antonio, Margen derecha del Kilómetro 23 de La Avenida Túpac Amaru, Distrito de Carabayllo, Provincia y Departamento De Lima, inscrito a fojas 515 del tomo 10-H, actualmente Partida Electrónica 11049870 del Registro de Predios de Lima. Fue adquirido mediante la minuta de fecha 06/11/2019 (15 Has.) y con fecha 29/03/2021 (51,224.03 m2) que le otorgo su anterior Propietaria INVERSIONES INMOBILIARIAS LAS PRADERAS S.A.C, identificada con RUC N°20601878616.").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));

        document.add(new Paragraph("SEGUNDA: OBJETO DEL CONTRATO").setBold().setFontSize(10));
        document.add(new Paragraph("LA VENDEDORA transfiere los derechos y acciones de un lote de terreno rústico ubicado la Manzana “"+lote.getManzana()+"” y asignado, con el lote Nº "+lote.getNumeroLote()+" del Programa de Vivienda “LA FLORIDA DE TORRE BLANCA” con un área de "+lote.getArea()+"M2. Linderos perimétricos:").setFontSize(10));
        document.add(new Paragraph("Por el frente: con la "+lote.getColindanteNorte()+" Con "+lote.getAncho1()+" m.l.\n" +
                                   "Por la derecha: con el "+lote.getColindanteEste()+" Con "+lote.getLargo1()+" m.l.\n" +
                                   "Por la izquierda: con el "+lote.getColindanteOeste()+" Con "+lote.getLargo2()+" m.l.\n" +
                                   "Por el fondo: con el "+lote.getColindanteSur()+" Con "+lote.getAncho2()+" m.l.").setMarginLeft(20).setFontSize(10));

        document.add(new Paragraph("TERCERA: PRECIO").setBold().setFontSize(10));
        document.add(new Paragraph("El precio asciende a US$."+contrato.getMontoTotal()+" ("+montoTexto+"), que “LOS COMPRADORES” se obliga a cancelar íntegramente según el cronograma: 3.1 Sin Cuota inicial. 3.2 El saldo de US$."+contrato.getSaldo()+" será cancelado en "+contrato.getCantidadLetras()+" letras de cambio (179 de US$."+pL.getImporte()+" y la última Nº180 de US$."+uL.getImporte()+"). 3.3 Letra No.01 vence el "+pL.getFechaVencimiento()+" y la última el "+uL.getFechaVencimiento()+".").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));

        document.add(new Paragraph("CUARTA A DECIMA QUINTA").setBold().setFontSize(10));
        document.add(new Paragraph("Incluye equivalencia justa, intereses del 5% mensual más 1 dólar diario en mora, pacto de reserva de propiedad (Art. 1583 C.C.), resolución por falta de dos letras con retención del 30%, merced conductiva del 3% mensual por demora en entrega y competencia en Lima Norte.").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));

        // --- SECCIÓN DE FIRMAS PÁGINA 1 ---
        agregarBloqueFirmas(document, contrato.getClientes());

        // --- PÁGINA 2: POSESIÓN ---
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        document.add(new Paragraph("DOCUMENTO DE SEÑALIZACION Y TOMA DE POSESION DE TERRENO").setTextAlignment(TextAlignment.CENTER).setBold());
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("PRIMERO.- LA VENDEDORA en virtud del presente contrato da en venta real un lote de terreno rústico de "+lote.getArea()+"M2 en la Manzana “"+lote.getManzana()+"” lote Nº "+lote.getNumeroLote()+". TERCERO.- LOS COMPRADORES reconocen adeudar US$."+contrato.getSaldo()+" ("+montoTexto+") y constituyen PRIMERA HIPOTECA a favor de LA VENDEDORA hasta por dicha suma conforme a los Artículos 1118 y 1119 del Código Civil.").setTextAlignment(TextAlignment.JUSTIFIED).setFontSize(10));
        
        agregarBloqueFirmas(document, contrato.getClientes());

        document.close();
        return out.toByteArray();
    }

    private static void agregarBloqueFirmas(Document document, List<ClienteResponseDTO> clientes) {
        document.add(new Paragraph("\n\n\n"));
        // Primera fila: Comprador 1 y Vendedor
        document.add(new Paragraph("….….……………………………………………….                 ...…………………………").setTextAlignment(TextAlignment.CENTER));
        
        String primerComprador = clientes.get(0).getNombre() + " " + clientes.get(0).getApellidos();
        String primerDni = "DNI N°" + clientes.get(0).getNumDoc();
        
        document.add(new Paragraph(primerComprador + "                                   “LA VENDEDORA”").setTextAlignment(TextAlignment.CENTER).setFontSize(9));
        document.add(new Paragraph(primerDni + "                                             DNI N°19404451").setTextAlignment(TextAlignment.CENTER).setFontSize(9));
        
        // Si hay un segundo comprador
        if (clientes.size() > 1) {
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("___________________________").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50));
            document.add(new Paragraph(clientes.get(1).getNombre() + " " + clientes.get(1).getApellidos()).setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9));
            document.add(new Paragraph("DNI N°" + clientes.get(1).getNumDoc()).setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9));
            document.add(new Paragraph("“LOS COMPRADORES”").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9));
        } else {
            document.add(new Paragraph("\n“LOS COMPRADORES”").setTextAlignment(TextAlignment.LEFT).setMarginLeft(50).setFontSize(9));
        }
    }
}