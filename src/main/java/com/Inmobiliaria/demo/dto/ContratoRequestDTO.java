package com.Inmobiliaria.demo.dto;

import java.util.List;
import com.Inmobiliaria.demo.enums.Moneda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequestDTO {
    @NotBlank
    private String fechaContrato;

    @NotBlank
    private String tipoContrato;

    @NotNull
    private Double montoTotal;

    private Double inicial;
    private Double saldo;
    private Integer cantidadLetras;
    private String observaciones;
    private Integer idVendedor;
    private Integer idUsuario;
    private Integer idSeparacion;

    /**
     * Clientes con su rol (TITULAR, AVAL, CONYUGE, etc.). Si viene null/vacío,
     * se usa {@link #idClientes} como fallback (todos como TITULAR, comportamiento
     * histórico de compatibilidad).
     */
    private List<ContratoClienteRequestDTO> clientes;

    /** Legacy: lista plana de IDs, todos TITULAR. Solo se usa si {@link #clientes} es null. */
    private List<Integer> idClientes;

    private List<Integer> idLotes;
    private Moneda moneda;
    private PagoInicialRequestDTO pagoInicial;
}