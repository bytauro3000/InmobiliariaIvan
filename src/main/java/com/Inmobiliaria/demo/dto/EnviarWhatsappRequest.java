package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;

public class EnviarWhatsappRequest {
    private Integer idContrato;
    private String celular;
    private String nombreClientes;
    private BigDecimal importeTotal;
    private int cantidadLetrasAtrasadas;
    private String moneda;

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getNombreClientes() { return nombreClientes; }
    public void setNombreClientes(String nombreClientes) { this.nombreClientes = nombreClientes; }

    public BigDecimal getImporteTotal() { return importeTotal; }
    public void setImporteTotal(BigDecimal importeTotal) { this.importeTotal = importeTotal; }

    public int getCantidadLetrasAtrasadas() { return cantidadLetrasAtrasadas; }
    public void setCantidadLetrasAtrasadas(int cantidadLetrasAtrasadas) { this.cantidadLetrasAtrasadas = cantidadLetrasAtrasadas; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
}
