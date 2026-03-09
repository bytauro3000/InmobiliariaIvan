package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PagosMultiplesRequestDTO {
    private List<PagoLetraRequestDTO> pagos;
}