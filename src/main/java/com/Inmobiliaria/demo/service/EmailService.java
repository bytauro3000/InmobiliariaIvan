package com.Inmobiliaria.demo.service;
 
import com.Inmobiliaria.demo.entity.PagoLetras;
import java.util.List;
 
public interface EmailService {
 
    // Envío individual a un destinatario específico
    void enviarComprobante(PagoLetras pago, String destinatario);
 
    // Envío a múltiples destinatarios (todos los clientes del contrato)
    void enviarComprobanteATodos(List<PagoLetras> pagos, List<String> destinatarios);
}