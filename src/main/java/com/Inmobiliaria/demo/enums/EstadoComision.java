package com.Inmobiliaria.demo.enums;

public enum EstadoComision {
    PENDIENTE,   // comisión creada, sin adelanto registrado aún
    EN_PAGO,     // ya se pagó el adelanto y se están pagando las cuotas mensuales
    COMPLETADA,  // saldo pendiente = 0
    ANULADA      // contrato renunciado/resuelto
}