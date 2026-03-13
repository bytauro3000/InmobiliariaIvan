package com.Inmobiliaria.demo.enums;

public enum EstadoContrato {
    ACTIVO,           // Contrato vigente, pagos al día
    MORA,             // Letras vencidas pero aún sin carta notarial
    CARTA_NOTARIAL,   // 2+ letras impagas → carta notarial enviada (cláusula 7)
    EN_RESOLUCION,    // Proceso de resolución iniciado, se retiene 30% (cláusula 7)
    RESUELTO,         // Contrato resuelto, lote recuperado por inmobiliaria
    CANCELADO,        // Cliente pagó el íntegro exitosamente (cláusula 9) — automático
    RENUNCIA,         // Cliente renunció voluntariamente — lote vuelve a Disponible
    TRANSFERIDO       // Cliente cedió el lote a otro — se crea nuevo contrato con saldo restante
}