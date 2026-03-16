package com.Inmobiliaria.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.service.ParceleroService;
import com.Inmobiliaria.demo.service.ProgramaService;
import com.Inmobiliaria.demo.service.VendedorService;

import lombok.RequiredArgsConstructor;

import com.Inmobiliaria.demo.service.ClienteService;
import com.Inmobiliaria.demo.repository.ContratoRepository; 

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  
    private final VendedorService vendedorService;

    
    private final ParceleroService parceleroService;

    
    private final ProgramaService programaService;

    
    private final LoteService loteService;
    
   
    private final ClienteService clienteService;

    
    private final ContratoRepository contratoRepository; 

    @GetMapping("/totales")
    public Map<String, Object> obtenerTotales() {
        Map<String, Object> respuesta = new HashMap<>();

        // 1. Conteos para las tarjetas superiores
        respuesta.put("vendedores", (long) vendedorService.listarVendedores().size());
        respuesta.put("parceleros", (long) parceleroService.listarParceleros().size());
        respuesta.put("programas", (long) programaService.listProgramas().size());
        respuesta.put("lotes", (long) loteService.listarLotes().size());
        respuesta.put("clientes", (long) clienteService.listarClientes().size());

        // 2. Lógica para el gráfico de Lotes (Disponible, Vendido, Separado)
        List<Object[]> resultadosLotes = loteService.obtenerConteoPorEstadoYPrograma();
        respuesta.put("graficoLotes", procesarResultadosParaGrafico(resultadosLotes));
        // 3. Lógica para el gráfico de Contratos (CONTADO vs FINANCIADO)
        // Usamos la query que definimos en el ContratoRepository
        List<Object[]> resultadosContratos = contratoRepository.contarContratosPorProgramaYTipo();
        respuesta.put("graficoContratos", procesarResultadosParaGrafico(resultadosContratos));
        return respuesta;
    }

    /**
     * Método auxiliar genérico para convertir una lista de Object[] en el formato 
     * Map<Nombre, Map<Categoria, Cantidad>> que requiere el Frontend.
     */
    private Map<String, Map<String, Long>> procesarResultadosParaGrafico(List<Object[]> resultados) {
        Map<String, Map<String, Long>> mapaFinal = new HashMap<>();

        if (resultados != null) {
            for (Object[] fila : resultados) {
                String nombrePrograma = (String) fila[0];
                String categoria = fila[1].toString(); // Puede ser EstadoLote o TipoContrato
                Long cantidad = (Long) fila[2];

                mapaFinal.putIfAbsent(nombrePrograma, new HashMap<>());
                mapaFinal.get(nombrePrograma).put(categoria, cantidad);
            }
        }
        return mapaFinal;
    }
}