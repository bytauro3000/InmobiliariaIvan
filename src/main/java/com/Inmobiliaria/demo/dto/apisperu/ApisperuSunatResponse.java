package com.Inmobiliaria.demo.dto.apisperu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuSunatResponse {
    private Boolean success;
    private Object error;
    private String cdrZip;
    private Object cdrResponse;
    private String description;
    private String note;
}