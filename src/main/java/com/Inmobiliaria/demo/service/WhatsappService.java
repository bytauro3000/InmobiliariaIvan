package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.EnviarWhatsappRequest;

import java.util.Map;

public interface WhatsappService {
    Map<String, Object> getQrLink();
    Map<String, Object> getStatus();
    Map<String, Object> enviarMensaje(EnviarWhatsappRequest request);
}
