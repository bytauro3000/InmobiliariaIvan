package com.Inmobiliaria.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.service.ParceleroService;
import com.Inmobiliaria.demo.service.ProgramaService;
import com.Inmobiliaria.demo.service.VendedorService;
import com.Inmobiliaria.demo.service.ClienteService; // Asegúrate de tener este import

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    @Autowired
    private VendedorService vendedorService;

    @Autowired
    private ParceleroService parceleroService;

    @Autowired
    private ProgramaService programaService;

    @Autowired
    private LoteService loteService;
    
    @Autowired
    private ClienteService clienteService; // Inyectamos clientes para la nueva tarjeta

    @GetMapping("/totales")
    public Map<String, Object> obtenerTotales() {
        Map<String, Object> respuesta = new HashMap<>();

        // 1. Conteos simples para las tarjetas
        respuesta.put("vendedores", (long) vendedorService.listarVendedores().size());
        respuesta.put("parceleros", (long) parceleroService.listarParceleros().size());
        respuesta.put("programas", (long) programaService.listProgramas().size());
        respuesta.put("lotes", (long) loteService.listarLotes().size());
        respuesta.put("clientes", (long) clienteService.listarClientes().size());

        // 2. Lógica para el gráfico de barras apiladas
        // Obtenemos la lista de Object[] desde el service (que ya llama al repo)
        List<Object[]> resultadosGrafico = loteService.obtenerConteoPorEstadoYPrograma();
        
        // Estructura para el gráfico: { "NombrePrograma": { "Disponible": 10, "Vendido": 5 }, ... }
        Map<String, Map<String, Long>> graficoLotes = new HashMap<>();

        for (Object[] fila : resultadosGrafico) {
            String nombrePrograma = (String) fila[0];
            String estado = fila[1].toString(); // El enum EstadoLote pasado a String
            Long cantidad = (Long) fila[2];

            // Si el programa no existe en el mapa, lo creamos con un mapa interno vacío
            graficoLotes.putIfAbsent(nombrePrograma, new HashMap<>());
            
            // Agregamos el conteo al estado correspondiente dentro de ese programa
            graficoLotes.get(nombrePrograma).put(estado, cantidad);
        }

        respuesta.put("graficoLotes", graficoLotes);

        return respuesta;
    }
}