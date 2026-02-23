package com.Inmobiliaria.demo.mensajeria;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

	@Autowired
	private ArchivoService archivoService;
	
	@PostMapping("/subir")
	public ResponseEntity<?> subirArchivo(
	        @RequestParam("file") MultipartFile file,
	        @RequestParam(value = "nombrePersonalizado", required = false) String nombrePersonalizado) {

		
		//VALIDACION SI EL ARCHIVO ESTA VACIO
		if (file.isEmpty()) {
		    return ResponseEntity.badRequest().body("El archivo está vacío");
		}
		
		
		//VALIDACION NOMBRE ARCHIVO - si el archivo no tiene nombre alguno o un punto, que te diga ese mensaje
		String nombreOriginal = file.getOriginalFilename();

	    if (nombreOriginal == null || !nombreOriginal.contains(".")) {
	        return ResponseEntity.badRequest().body("Archivo inválido. Porfavor Ingrese un nombre valido al archivo.");
	    }
		
	    // Obtener extensión real DEL ARCHIVO
	    String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toLowerCase();

		
	 // Lista blanca de extensiones permitidas 
	    List<String> permitidas = List.of(
	            "pdf", "jpg", "jpeg", "png",
	            "doc", "docx",
	            "xls", "xlsx",
	            "txt", "zip", "rar"
	    );

	    if (!permitidas.contains(extension)) {
	        return ResponseEntity.badRequest().body("Extensión no permitida");
	    }

	    

		try {

	        // Si el usuario envía nombre personalizado se usa ese nombre
	        String nombreFinal;
	        
	        if (nombrePersonalizado != null && !nombrePersonalizado.trim().isEmpty()) {
	            nombreFinal = nombrePersonalizado.trim();
	        } else {
	            nombreFinal = nombreOriginal.substring(0, nombreOriginal.lastIndexOf("."));
	        }
	        
			
            Map<String, Object> resultado = archivoService.subirArchivo(file, nombreFinal);

            String url = resultado.get("secure_url") != null
                    ? resultado.get("secure_url").toString()
                    : null;
            
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("url", url);
            respuesta.put("nombre", nombreFinal + "." + extension);
            respuesta.put("tipo", extension);

            return ResponseEntity.ok(respuesta);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error al subir archivo");
        }
		
	}
	
}
