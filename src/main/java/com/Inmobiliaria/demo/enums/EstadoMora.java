package com.Inmobiliaria.demo.enums;

public enum EstadoMora {
    PENDIENTE,   // Mora generada, el cliente aún no la paga
    PAGADO,      // El cliente ya canceló esta mora
    ANULADO      // La mora fue anulada por el administrador (corrección de datos, etc.)
}