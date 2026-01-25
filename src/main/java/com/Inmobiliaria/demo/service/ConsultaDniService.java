package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ConsultaDniDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class ConsultaDniService {

    private final String TOKEN = "sk_13021.AR1OXiT2iR6aLPUIKNr7oL1bAD6tr8GS"; // Tu token verificado
    private final String URL = "https://api.decolecta.com/v1/reniec/dni?numero=";

    public ConsultaDniDTO buscarEnReniec(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN); // Configuración Bearer igual que en Postman
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Realizamos la petición GET con Body none
            ResponseEntity<ConsultaDniDTO> response = restTemplate.exchange(
                URL + dni, 
                HttpMethod.GET, 
                entity, 
                ConsultaDniDTO.class
            );
            
            ConsultaDniDTO dto = response.getBody();
            if (dto != null) dto.setSuccess(true);
            return dto;
            
        } catch (Exception e) {
            // Si falla el API o se acaban las 100 consultas, devolvemos success false
            ConsultaDniDTO errorDto = new ConsultaDniDTO();
            errorDto.setSuccess(false);
            return errorDto;
        }
    }
}