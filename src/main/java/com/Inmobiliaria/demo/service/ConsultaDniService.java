package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ConsultaDniDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class ConsultaDniService {

    @Value("${decolecta.api.token}")
    private String token;

    @Value("${decolecta.api.url}")
    private String url;

    public ConsultaDniDTO buscarEnReniec(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ConsultaDniDTO> response = restTemplate.exchange(
                url + dni,
                HttpMethod.GET,
                entity,
                ConsultaDniDTO.class
            );

            ConsultaDniDTO dto = response.getBody();
            if (dto != null) dto.setSuccess(true);
            return dto;

        } catch (Exception e) {
            ConsultaDniDTO errorDto = new ConsultaDniDTO();
            errorDto.setSuccess(false);
            return errorDto;
        }
    }
}