package com.Inmobiliaria.demo.service;


import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArchivoService {

	
	private final Cloudinary cloudinary;

	@SuppressWarnings("unchecked")
	public Map <String, Object> subirArchivo(MultipartFile archivo, String nombreFinal) throws IOException{
		
		String nombreUnico = nombreFinal + "_" + System.currentTimeMillis();
		
		return (Map<String, Object>) cloudinary.uploader().upload(
	            archivo.getBytes(),
	            ObjectUtils.asMap(
	                    "resource_type", "auto",
	                    "public_id", nombreUnico
	            )
	    );
		
		
	}
	
}
